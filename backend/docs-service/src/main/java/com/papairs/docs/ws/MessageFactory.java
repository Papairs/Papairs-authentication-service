package com.papairs.docs.ws;

import com.papairs.docs.model.OT.AppliedOp;
import com.papairs.docs.model.OT.DeleteOp;
import com.papairs.docs.model.OT.InsertOp;
import com.papairs.docs.model.OT.Op;
import org.springframework.stereotype.Component;

/**
 * Factory for creating WebSocket messages.
 * Centralizes message creation logic for consistent formatting.
 */
@Component
public class MessageFactory {

    /**
     * Creates a snapshot message for client initialization.
     * Contains current document content and version.
     */
    public AppliedOp createSnapshot(DocumentSession document) {
        AppliedOp snapshot = new AppliedOp();
        snapshot.type = "snapshot";
        snapshot.version = document.getVersion();
        snapshot.content = document.getContent();
        return snapshot;
    }

    /**
     * Creates an applied operation message for broadcasting.
     * Converts operation to format suitable for client consumption.
     */
    public AppliedOp createAppliedOperation(Op operation, int version) {
        AppliedOp applied = new AppliedOp();
        applied.type = operation.type;
        applied.version = version;
        applied.clientId = operation.clientId;
        applied.opId = operation.opId;
        applied.pos = operation.pos;
        
        // Set operation-specific fields
        switch (operation) {
            case InsertOp insert -> applied.text = insert.text;
            case DeleteOp delete -> applied.length = delete.length;
            default -> { /* No additional fields for base operation */ }
        }
        
        return applied;
    }

    /**
     * Creates an error message for client notification.
     */
    public AppliedOp createErrorMessage(String errorMessage, int version) {
        AppliedOp error = new AppliedOp();
        error.type = "error";
        error.version = version;
        error.content = errorMessage;
        return error;
    }

    /**
     * Creates a user joined notification message.
     */
    public AppliedOp createUserJoinedMessage(String userId, int version) {
        AppliedOp message = new AppliedOp();
        message.type = "user_joined";
        message.version = version;
        message.clientId = userId;
        return message;
    }

    /**
     * Creates a user left notification message.
     */
    public AppliedOp createUserLeftMessage(String userId, int version) {
        AppliedOp message = new AppliedOp();
        message.type = "user_left";
        message.version = version;
        message.clientId = userId;
        return message;
    }
}