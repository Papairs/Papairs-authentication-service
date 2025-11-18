package com.papairs.docs.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papairs.docs.model.Message;
import com.papairs.docs.model.Page;
import com.papairs.docs.service.PageService;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * WebSocket handler for real-time collaborative document editing.
 * Orchestrates document sessions, operational transforms, and auto-save functionality.
 */
@Component
public class DocWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = Logger.getLogger(DocWebSocketHandler.class.getName());

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, DocumentSession> documentSessions = new ConcurrentHashMap<>();
    
    private final PageService pageService;
    private final OperationHandler operationHandler;
    private final AutoSaveManager autoSaveManager;
    private final MessageFactory messageFactory;
    private final WebSocketMessageBroker messageBroker;

    public DocWebSocketHandler(
            PageService pageService,
            OperationHandler operationHandler,
            AutoSaveManager autoSaveManager,
            MessageFactory messageFactory,
            WebSocketMessageBroker messageBroker) {
        this.pageService = pageService;
        this.operationHandler = operationHandler;
        this.autoSaveManager = autoSaveManager;
        this.messageFactory = messageFactory;
        this.messageBroker = messageBroker;
    }

    /**
     * Handles incoming WebSocket text messages for document collaboration.
     * Processes join requests and operational transforms.
     */
    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        try {
            logger.info("Received WebSocket message: " + message.getPayload().substring(0, Math.min(200, message.getPayload().length())));
            var msg = objectMapper.readValue(message.getPayload(), Message.class);
            var docId = sanitizeDocumentId(msg.docId);
            var userId = sanitizeUserId(msg.userId);
            
            if (msg.op != null) {
                logger.info("Operation type: " + msg.op.type + ", has htmlContent: " + (msg.op.htmlContent != null));
                if (msg.op.htmlContent != null) {
                    logger.info("HTML content length: " + msg.op.htmlContent.length());
                }
            }
            
            var document = getOrCreateDocumentSession(docId, userId);

            switch (msg.action) {
                case "join" -> handleJoinRequest(session, document, userId);
                case "op" -> handleOperationRequest(msg.op, document, session, userId);
                default -> logger.warning("Unknown action: " + msg.action);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling WebSocket message", e);
            var errorMessage = messageFactory.createErrorMessage("Invalid message format", 0);
            messageBroker.sendToSession(session, errorMessage);
        }
    }

    /**
     * Handles client joining a document session.
     */
    private void handleJoinRequest(WebSocketSession session, DocumentSession document, String userId) {
        document.addSession(session, userId);
        var snapshot = messageFactory.createSnapshot(document);
        messageBroker.sendToSession(session, snapshot);
        
        // Notify other users about the join
        var joinMessage = messageFactory.createUserJoinedMessage(userId, document.getVersion());
        messageBroker.broadcastToOthers(document, joinMessage, session);
        
        logger.info("User " + userId + " joined document " + document.getDocumentId());
    }

    /**
     * Handles operational transform for collaborative editing.
     * Now handles HTML-based operations to preserve formatting.
     */
    private void handleOperationRequest(Object incomingOp, DocumentSession document, 
                                      WebSocketSession session, String userId) {
        if (incomingOp == null) {
            logger.warning("Received null operation from user: " + userId);
            return;
        }

        try {
            // Cast to operation (the objectMapper should have mapped this correctly)
            var operation = (com.papairs.docs.model.OT.Op) incomingOp;
            
            logger.info("User " + userId + " performed " + operation.type + 
                       " operation on document " + document.getDocumentId());

            // For HTML operations, use the HTML content directly
            String newContent;
            if (operation instanceof com.papairs.docs.model.OT.HtmlUpdateOp && operation.htmlContent != null) {
                // HTML-based operation - use the HTML content directly
                newContent = operation.htmlContent;
                logger.info("Processing HTML operation with content length: " + newContent.length());
            } else {
                // Legacy text-based operation - apply transformation
                var transformed = operationHandler.transformOperation(operation, document);
                newContent = operationHandler.applyOperation(document.getContent(), transformed);
                operation = transformed; // Use transformed operation
            }
            
            // Update document state
            document.setContent(newContent);
            document.addOperation(operationHandler.cloneOperation(operation));
            document.incrementVersion();

            // Broadcast operation with HTML content to all clients
            var appliedOp = messageFactory.createAppliedOperation(operation, document.getVersion(), newContent);
            messageBroker.broadcastToDocument(document, appliedOp);
            
            // Schedule auto-save
            autoSaveManager.scheduleDelayedSave(document);
            
        } catch (ClassCastException e) {
            logger.log(Level.WARNING, "Invalid operation format from user: " + userId, e);
            var errorMessage = messageFactory.createErrorMessage("Invalid operation format", document.getVersion());
            messageBroker.sendToSession(session, errorMessage);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing operation from user: " + userId, e);
            var errorMessage = messageFactory.createErrorMessage("Error processing operation", document.getVersion());
            messageBroker.sendToSession(session, errorMessage);
        }
    }

    /**
     * Gets or creates a document session for the given document ID.
     */
    private DocumentSession getOrCreateDocumentSession(String docId, String userId) {
        return documentSessions.computeIfAbsent(docId, k -> {
            try {
                Page page = pageService.getPage(k, userId);
                String initialContent = page.getContent();
                
                // Ensure we have valid HTML content for Tiptap
                if (initialContent == null || initialContent.isEmpty()) {
                    initialContent = "<p></p>"; // Default empty paragraph for Tiptap
                }
                
                logger.info("Loaded existing content for document: " + k + " (length: " + initialContent.length() + ")");
                return new DocumentSession(k, initialContent);
            } catch (Exception e) {
                logger.info("Starting with default content for new document: " + k);
                return new DocumentSession(k, "<p></p>"); // Default empty paragraph for Tiptap
            }
        });
    }

    /**
     * Sanitizes document ID input.
     */
    private String sanitizeDocumentId(String docId) {
        return (docId == null || docId.isBlank()) ? "default" : docId.trim();
    }

    /**
     * Sanitizes user ID input.
     */
    private String sanitizeUserId(String userId) {
        return (userId == null || userId.isBlank()) ? "anonymous" : userId.trim();
    }

    /**
     * Handles WebSocket connection closure.
     * Auto-saves documents when last user disconnects.
     */
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        documentSessions.values().forEach(document -> {
            String userId = document.getUserForSession(session);
            document.removeSession(session);
            
            if (userId != null) {
                logger.info("User " + userId + " disconnected from document " + document.getDocumentId());
                
                // Notify remaining users about the disconnect
                var leaveMessage = messageFactory.createUserLeftMessage(userId, document.getVersion());
                messageBroker.broadcastToDocument(document, leaveMessage);
            }
            
            // Auto-save when last user disconnects
            if (!document.hasActiveSessions() && document.isDirty()) {
                autoSaveManager.saveImmediately(document);
                logger.info("Auto-saved document " + document.getDocumentId() + " on disconnect");
            }
        });
        
        // Clean up empty document sessions
        cleanupEmptyDocuments();
    }

    /**
     * Removes document sessions that have no active users.
     */
    private void cleanupEmptyDocuments() {
        documentSessions.entrySet().removeIf(entry -> {
            DocumentSession document = entry.getValue();
            if (!document.hasActiveSessions()) {
                logger.info("Removing empty document session: " + entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * Gracefully shuts down resources.
     */
    public void shutdown() {
        // Save all dirty documents before shutdown
        documentSessions.values().stream()
            .filter(DocumentSession::isDirty)
            .forEach(autoSaveManager::saveImmediately);
            
        autoSaveManager.shutdown();
        logger.info("WebSocket handler shut down successfully");
    }
}
