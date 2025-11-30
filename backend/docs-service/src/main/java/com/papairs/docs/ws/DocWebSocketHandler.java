package com.papairs.docs.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papairs.docs.model.DocumentSession;
import com.papairs.docs.model.Message;
import com.papairs.docs.model.OT.AppliedOp;
import com.papairs.docs.service.CollaborationService;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * WebSocket controller for collaborative document editing.
 * Routes messages and broadcasts to clients.
 */
@Component
public class DocWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = Logger.getLogger(DocWebSocketHandler.class.getName());

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CollaborationService collaborationService;

    public DocWebSocketHandler(CollaborationService collaborationService) {
        this.collaborationService = collaborationService;
    }

    /**
     * Handles incoming WebSocket text messages for document collaboration.
     * Processes join requests and operational transforms.
     */
    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        try {
            var msg = objectMapper.readValue(message.getPayload(), Message.class);
            
            // Validate required fields
            if (msg.docId == null || msg.docId.isBlank()) {
                sendError(session, "Document ID is required", 0);
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }
            
            if (msg.userId == null || msg.userId.isBlank()) {
                sendError(session, "User ID is required", 0);
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }
            
            var docId = msg.docId.trim();
            var userId = msg.userId.trim();

            switch (msg.action) {
                case "join" -> handleJoinRequest(session, docId, userId);
                case "op" -> handleOperationRequest(session, docId, userId, msg.op);
                default -> logger.warning("Unknown action: " + msg.action);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling WebSocket message", e);
            sendError(session, "Invalid message format", 0);
        }
    }

    /**
     * Handles client joining a document session.
     */
    private void handleJoinRequest(WebSocketSession session, String docId, String userId) {
        DocumentSession document = collaborationService.joinDocument(docId, userId, session);
        sendSnapshot(session, document);
        broadcastToOthers(document, createUserMessage("user_joined", userId, document.getVersion()), session);
    }

    /**
     * Handles HTML-based collaborative editing operations
     */
    private void handleOperationRequest(WebSocketSession session, String docId, String userId, Object incomingOp) {
        if (incomingOp == null) {
            logger.warning("Received null operation from user: " + userId);
            return;
        }

        DocumentSession document = collaborationService.getSession(docId);
        if (document == null) {
            sendError(session, "Document session not found", 0);
            return;
        }

        try {
            var operation = (com.papairs.docs.model.OT.Op) incomingOp;
            String sanitizedHtml = collaborationService.applyOperation(document, operation, userId);
            broadcastOperation(document, operation, sanitizedHtml);
            
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Invalid operation: " + e.getMessage(), e);
            sendError(session, e.getMessage(), document.getVersion());
        } catch (ClassCastException e) {
            logger.log(Level.WARNING, "Invalid operation format from user: " + userId, e);
            sendError(session, "Invalid operation format", document.getVersion());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing operation from user: " + userId, e);
            sendError(session, "Error processing operation", document.getVersion());
        }
    }

    /**
     * Handles WebSocket connection closure.
     * Auto-saves documents when last user disconnects.
     */
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        collaborationService.disconnectUser(session);
    }

    /**
     * Gracefully shuts down resources.
     */
    public void shutdown() {
        collaborationService.shutdown();
        logger.info("WebSocket handler shut down successfully");
    }

    // Message helpers
    private void sendSnapshot(WebSocketSession session, DocumentSession document) {
        var snapshot = new AppliedOp();
        snapshot.type = "snapshot";
        snapshot.version = document.getVersion();
        snapshot.content = document.getContent();
        sendMessage(session, snapshot);
    }

    private void sendError(WebSocketSession session, String error, int version) {
        var message = new AppliedOp();
        message.type = "error";
        message.version = version;
        message.content = error;
        sendMessage(session, message);
    }

    private AppliedOp createUserMessage(String type, String userId, int version) {
        var message = new AppliedOp();
        message.type = type;
        message.version = version;
        message.clientId = userId;
        return message;
    }

    private void broadcastOperation(DocumentSession document, com.papairs.docs.model.OT.Op operation, String htmlContent) {
        var message = new AppliedOp();
        message.type = operation.type;
        message.version = document.getVersion();
        message.clientId = operation.clientId;
        message.opId = operation.opId;
        message.htmlContent = htmlContent;
        broadcastToAll(document, message);
    }

    private void sendMessage(WebSocketSession session, AppliedOp message) {
        if (!session.isOpen()) return;
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to send message", e);
        }
    }

    private void broadcastToAll(DocumentSession document, AppliedOp message) {
        try {
            var textMessage = new TextMessage(objectMapper.writeValueAsString(message));
            document.getActiveSessions().stream()
                .filter(WebSocketSession::isOpen)
                .forEach(s -> {
                    try { s.sendMessage(textMessage); } 
                    catch (Exception e) { logger.log(Level.WARNING, "Send failed", e); }
                });
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Broadcast failed", e);
        }
    }

    private void broadcastToOthers(DocumentSession document, AppliedOp message, WebSocketSession sender) {
        try {
            var textMessage = new TextMessage(objectMapper.writeValueAsString(message));
            document.getActiveSessions().stream()
                .filter(s -> !s.equals(sender) && s.isOpen())
                .forEach(s -> {
                    try { s.sendMessage(textMessage); } 
                    catch (Exception e) { logger.log(Level.WARNING, "Send failed", e); }
                });
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Broadcast failed", e);
        }
    }
}
