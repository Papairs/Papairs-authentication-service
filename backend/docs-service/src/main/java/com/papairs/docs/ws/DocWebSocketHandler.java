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

@Component
public class DocWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, DocState> docs = new ConcurrentHashMap<>();
    private final DocumentService documentService;

    @Autowired
    public DocWebSocketHandler(DocumentService documentService) {
        this.documentService = documentService;
    }

    // Smart document state container
    private static class DocState {
        String content = "";
        int version = 0;
        final List<Op> history = new ArrayList<>();
        final Set<WebSocketSession> sessions = Collections.synchronizedSet(new HashSet<>());
        boolean isDirty = false; // Track if document needs saving
        String docId;
        
        DocState(String docId) {
            this.docId = docId;
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        var msg = mapper.readValue(message.getPayload(), Message.class);
        var docId = msg.docId == null || msg.docId.isBlank() ? "default" : msg.docId;
        var doc = docs.computeIfAbsent(docId, k -> {
            var state = new DocState(k);
            // Load existing content from database
            state.content = documentService.getDocumentContent(k);
            return state;
        });

        switch (msg.action) {
            case "join" -> handleJoin(session, doc);
            case "op" -> handleOperation(msg.op, doc);
        }
    }

    // Handle client joining document
    private void handleJoin(WebSocketSession session, DocState doc) throws Exception {
        doc.sessions.add(session);
        send(session, createSnapshot(doc));
    }

    // Handle operational transform
    private void handleOperation(Op incoming, DocState doc) throws Exception {
        if (incoming == null) return;

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
        
        // Save to database asynchronously (every few operations or after delay)
        saveDocumentIfNeeded(doc);
    }

    // Smart database saving strategy
    private void saveDocumentIfNeeded(DocState doc) {
        if (doc.isDirty && doc.version % 5 == 0) { // Save every 5 operations
            saveDocumentToDatabase(doc);
        }
        // You could also implement time-based saving here
    }

    private void saveDocumentToDatabase(DocState doc) {
        try {
            documentService.saveDocument(doc.docId, doc.content, "Document " + doc.docId);
            doc.isDirty = false;
            System.out.println("Saved document " + doc.docId + " to database");
        } catch (Exception e) {
            System.err.println("Failed to save document " + doc.docId + ": " + e.getMessage());
        }
    }

    // Smart operation transformation
    private Op transformOp(Op op, DocState doc) {
        for (int i = op.baseVersion; i < doc.version; i++) {
            op = OtTransform.transform(op, doc.history.get(i));
        }
        return op;
    }

    // Clever content application
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

    // Factory methods for response objects
    private AppliedOp createSnapshot(DocState doc) {
        var snap = new AppliedOp();
        snap.type = "snapshot";
        snap.version = doc.version;
        snap.content = doc.content;
        return snap;
    }

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

    // Smart operation cloning
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

    // Utility functions
    private void send(WebSocketSession session, Object obj) throws Exception {
        session.sendMessage(new TextMessage(mapper.writeValueAsString(obj)));
    }

    private void broadcast(DocState doc, AppliedOp op) throws Exception {
        var json = mapper.writeValueAsString(op);
        synchronized (doc.sessions) {
            for (var session : doc.sessions) {
                if (session.isOpen()) session.sendMessage(new TextMessage(json));
            }
        }
    }

    private int clamp(int x, int min, int max) { 
        return Math.max(min, Math.min(max, x)); 
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        docs.values().forEach(doc -> {
            doc.sessions.remove(session);
            // Save document when last user disconnects
            if (doc.sessions.isEmpty() && doc.isDirty) {
                saveDocumentToDatabase(doc);
            }
        });
    }
}
