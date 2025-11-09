package com.papairs.docs.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papairs.docs.model.Message;
import com.papairs.docs.model.OT.AppliedOp;
import com.papairs.docs.model.OT.DeleteOp;
import com.papairs.docs.model.OT.InsertOp;
import com.papairs.docs.model.OT.Op;
import com.papairs.docs.service.DocumentService;
import com.papairs.docs.util.OtTransform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket handler for real-time collaborative document editing.
 * Handles operational transform, document state management, and auto-save.
 */
@Component
public class DocWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, DocState> docs = new ConcurrentHashMap<>();
    private final DocumentService documentService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @Autowired
    public DocWebSocketHandler(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Document state container for collaborative editing session.
     */
    private static class DocState {
        String content = "";
        int version = 0;
        final List<Op> history = new ArrayList<>();
        final Set<WebSocketSession> sessions = Collections.synchronizedSet(new HashSet<>());
        final Map<WebSocketSession, String> sessionUsers = new ConcurrentHashMap<>();
        boolean isDirty = false;
        String docId;
        ScheduledFuture<?> saveTimer;
        
        DocState(String docId) {
            this.docId = docId;
        }
    }

    /**
     * Handles incoming WebSocket text messages for document collaboration.
     * Processes join requests and operational transforms.
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        var msg = mapper.readValue(message.getPayload(), Message.class);
        var docId = msg.docId == null || msg.docId.isBlank() ? "default" : msg.docId;
        var userId = msg.userId == null || msg.userId.isBlank() ? "anonymous" : msg.userId;
        
        var doc = docs.computeIfAbsent(docId, k -> {
            var state = new DocState(k);
            // Load existing content from database with user permission check
            try {
                state.content = documentService.getDocumentContent(k, userId);
            } catch (Exception e) {
                // If user doesn't have access or page doesn't exist, start with empty content
                state.content = "";
                System.out.println("[WebSocket] Starting with empty content for new page: " + k);
            }
            return state;
        });

        switch (msg.action) {
            case "join" -> handleJoin(session, doc, userId);
            case "op" -> handleOperation(msg.op, doc, session, userId);
        }
    }

    /**
     * Handles client joining a document session.
     * Adds session to document state and sends current snapshot.
     */
    private void handleJoin(WebSocketSession session, DocState doc, String userId) throws Exception {
        doc.sessions.add(session);
        doc.sessionUsers.put(session, userId);
        send(session, createSnapshot(doc));
        System.out.println("User " + userId + " joined document " + doc.docId);
    }

    /**
     * Handles operational transform for collaborative editing.
     * Applies operation, updates document state, and broadcasts to all clients.
     */
    private void handleOperation(Op incoming, DocState doc, WebSocketSession session, String userId) throws Exception {
        if (incoming == null) return;

        System.out.println("User " + userId + " made operation on document " + doc.docId + ": " + incoming.type);

        // Transform against concurrent operations
        var transformed = transformOp(incoming, doc);
        
        // Apply to document content
        doc.content = applyOp(doc.content, transformed);
        doc.isDirty = true; // Mark for saving
        
        // Update history and version
        doc.history.add(cloneOp(transformed));
        doc.version++;

        // Broadcast to all clients
        broadcast(doc, createAppliedOp(transformed, doc.version));
        
        // Schedule save 10 seconds after input stops
        scheduleDelayedSave(doc);
    }

    /**
     * Schedules a delayed save 10 seconds after last input.
     * Cancels existing timer if present.
     */
    private void scheduleDelayedSave(DocState doc) {
        // Cancel existing timer if any
        if (doc.saveTimer != null && !doc.saveTimer.isDone()) {
            doc.saveTimer.cancel(false);
        }
        
        // Schedule new save timer for 10 seconds from now
        if (doc.isDirty) {
            doc.saveTimer = scheduler.schedule(() -> {
                if (doc.isDirty) {
                    saveDocumentToDatabase(doc);
                }
            }, 10, TimeUnit.SECONDS);
        }
    }

    /**
     * Saves document content to database.
     * Called by the delayed save timer.
     */
    private void saveDocumentToDatabase(DocState doc) {
        try {
            // Get any user from the current session to perform the save
            // In a real application, you might want to save with the document owner's permissions
            String userId = doc.sessionUsers.values().stream().findFirst().orElse("system");
            documentService.saveDocument(doc.docId, doc.content, userId);
            doc.isDirty = false;
            System.out.println("[WebSocket] Saved document " + doc.docId + " to database");
        } catch (Exception e) {
            System.err.println("Failed to save page " + doc.docId + ": " + e.getMessage());
        }
    }

    /**
     * Transforms operation against document history for conflict resolution.
     */
    private Op transformOp(Op op, DocState doc) {
        for (int i = op.baseVersion; i < doc.version; i++) {
            op = OtTransform.transform(op, doc.history.get(i));
        }
        return op;
    }

    /**
     * Applies operation to document content.
     * Handles insert and delete operations with position clamping.
     */
    private String applyOp(String content, Op op) {
        return switch (op.type) {
            case "insert" -> {
                var ins = (InsertOp) op;
                var pos = clamp(ins.pos, 0, content.length());
                yield content.substring(0, pos) + ins.text + content.substring(pos);
            }
            case "delete" -> {
                var del = (DeleteOp) op;
                var start = clamp(del.pos, 0, content.length());
                var end = clamp(start + del.length, 0, content.length());
                yield content.substring(0, start) + content.substring(end);
            }
            default -> content;
        };
    }

    /**
     * Creates snapshot message for client initialization.
     */
    private AppliedOp createSnapshot(DocState doc) {
        var snap = new AppliedOp();
        snap.type = "snapshot";
        snap.version = doc.version;
        snap.content = doc.content;
        return snap;
    }

    /**
     * Creates applied operation message for broadcasting to clients.
     */
    private AppliedOp createAppliedOp(Op op, int version) {
        var applied = new AppliedOp();
        applied.type = op.type;
        applied.version = version;
        applied.clientId = op.clientId;
        applied.opId = op.opId;
        applied.pos = op.pos;
        
        if (op instanceof InsertOp ins) applied.text = ins.text;
        if (op instanceof DeleteOp del) applied.length = del.length;
        
        return applied;
    }

    /**
     * Creates deep copy of operation for history storage.
     */
    private Op cloneOp(Op op) {
        return switch (op.type) {
            case "insert" -> {
                var ins = (InsertOp) op;
                var clone = new InsertOp();
                clone.type = "insert"; clone.pos = ins.pos; clone.baseVersion = ins.baseVersion;
                clone.clientId = ins.clientId; clone.opId = ins.opId; clone.text = ins.text;
                yield clone;
            }
            case "delete" -> {
                var del = (DeleteOp) op;
                var clone = new DeleteOp();
                clone.type = "delete"; clone.pos = del.pos; clone.baseVersion = del.baseVersion;
                clone.clientId = del.clientId; clone.opId = del.opId; clone.length = del.length;
                yield clone;
            }
            default -> op;
        };
    }

    /**
     * Sends JSON message to specific WebSocket session.
     */
    private void send(WebSocketSession session, Object obj) throws Exception {
        session.sendMessage(new TextMessage(mapper.writeValueAsString(obj)));
    }

    /**
     * Broadcasts operation to all sessions in document.
     */
    private void broadcast(DocState doc, AppliedOp op) throws Exception {
        var json = mapper.writeValueAsString(op);
        synchronized (doc.sessions) {
            for (var session : doc.sessions) {
                if (session.isOpen()) session.sendMessage(new TextMessage(json));
            }
        }
    }

    /**
     * Clamps value between min and max bounds.
     */
    private int clamp(int x, int min, int max) { 
        return Math.max(min, Math.min(max, x)); 
    }

    /**
     * Handles WebSocket connection closure.
     * Auto-saves documents when last user disconnects.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        docs.values().forEach(doc -> {
            String userId = doc.sessionUsers.remove(session);
            doc.sessions.remove(session);
            if (userId != null) {
                System.out.println("User " + userId + " disconnected from document " + doc.docId);
            }
            // Save document when last user disconnects
            if (doc.sessions.isEmpty() && doc.isDirty) {
                // Cancel any pending timer and save immediately
                if (doc.saveTimer != null && !doc.saveTimer.isDone()) {
                    doc.saveTimer.cancel(false);
                }
                try {
                    // Use system user for final save when all users disconnect
                    documentService.saveDocument(doc.docId, doc.content, "system");
                    doc.isDirty = false;
                    System.out.println("[WebSocket] Auto-saved document " + doc.docId + " on disconnect");
                } catch (Exception e) {
                    System.err.println("Failed to auto-save page " + doc.docId + " on disconnect: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Shuts down the scheduler and cleans up resources.
     */
    public void cleanup() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
