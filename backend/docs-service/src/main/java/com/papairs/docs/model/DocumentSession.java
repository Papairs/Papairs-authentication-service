package com.papairs.docs.model;

import com.papairs.docs.model.OT.Op;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Thread-safe session state for a collaborative document.
 * Tracks content, version, active users, and save timer.
 */
public class DocumentSession {
    private final String documentId;
    private volatile String content = "";
    private volatile int version = 0;
    private final List<Op> operationHistory = Collections.synchronizedList(new ArrayList<>());
    private final Set<WebSocketSession> activeSessions = Collections.synchronizedSet(new HashSet<>());
    private final Map<WebSocketSession, String> sessionToUser = new ConcurrentHashMap<>();
    private final Map<String, CursorPosition> userCursors = new ConcurrentHashMap<>();
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

    // Cursor tracking methods
    public void updateCursor(String userId, CursorPosition cursor) {
        if (cursor != null) {
            userCursors.put(userId, cursor);
        } else {
            userCursors.remove(userId);
        }
    }

    public CursorPosition getCursor(String userId) {
        return userCursors.get(userId);
    }

    public Map<String, CursorPosition> getAllCursors() {
        return new HashMap<>(userCursors);
    }

    public void removeCursor(String userId) {
        userCursors.remove(userId);
    }

    /**
     * Transform cursor positions after content change
     * Note: Cursor transformation is disabled for HTML-based editing.
     * ProseMirror positions don't map directly to HTML character positions.
     * Each client manages cursor positions based on their own document state.
     * @deprecated This method is no longer used for HTML-based collaboration
     */
    @Deprecated
    public void transformCursorsAfterEdit(String editingUserId, int editPosition, int oldLength, int newLength) {
        // Cursor transformation disabled - clients manage their own cursor positions
        // based on their ProseMirror document state
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