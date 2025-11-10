package com.papairs.docs.ws;

import com.papairs.docs.model.OT.Op;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Manages state for a collaborative document editing session.
 * Thread-safe container for document content, operation history, and user sessions.
 */
public class DocumentSession {
    private final String documentId;
    private volatile String content = "";
    private volatile int version = 0;
    private final List<Op> operationHistory = Collections.synchronizedList(new ArrayList<>());
    private final Set<WebSocketSession> activeSessions = Collections.synchronizedSet(new HashSet<>());
    private final Map<WebSocketSession, String> sessionToUser = new ConcurrentHashMap<>();
    private volatile boolean isDirty = false;
    private volatile ScheduledFuture<?> saveTimer;

    public DocumentSession(String documentId) {
        this.documentId = documentId;
    }

    public DocumentSession(String documentId, String initialContent) {
        this.documentId = documentId;
        this.content = initialContent;
    }

    // Document state methods
    public String getContent() { return content; }
    
    public void setContent(String content) {
        this.content = content;
        this.isDirty = true;
    }

    public int getVersion() { return version; }
    
    public void incrementVersion() { this.version++; }

    public boolean isDirty() { return isDirty; }
    
    public void markClean() { this.isDirty = false; }

    public String getDocumentId() { return documentId; }

    // Operation history methods
    public void addOperation(Op operation) {
        operationHistory.add(operation);
    }

    public Op getOperation(int index) {
        return index < operationHistory.size() ? operationHistory.get(index) : null;
    }

    public int getHistorySize() { return operationHistory.size(); }

    // Session management methods
    public void addSession(WebSocketSession session, String userId) {
        activeSessions.add(session);
        sessionToUser.put(session, userId);
    }

    public void removeSession(WebSocketSession session) {
        activeSessions.remove(session);
        sessionToUser.remove(session);
    }

    public Set<WebSocketSession> getActiveSessions() {
        return new HashSet<>(activeSessions);
    }

    public String getUserForSession(WebSocketSession session) {
        return sessionToUser.get(session);
    }

    public Collection<String> getActiveUsers() {
        return new ArrayList<>(sessionToUser.values());
    }

    public boolean hasActiveSessions() {
        return !activeSessions.isEmpty();
    }

    public String getAnyActiveUser() {
        return sessionToUser.values().stream().findFirst().orElse("system");
    }

    // Auto-save timer management
    public ScheduledFuture<?> getSaveTimer() { return saveTimer; }
    
    public void setSaveTimer(ScheduledFuture<?> timer) { this.saveTimer = timer; }

    public void cancelSaveTimer() {
        if (saveTimer != null && !saveTimer.isDone()) {
            saveTimer.cancel(false);
        }
    }
}