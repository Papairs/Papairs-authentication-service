package com.papairs.docs.ws;

import com.papairs.docs.model.OT.AppliedOp;
import com.papairs.docs.model.OT.Op;
import com.papairs.docs.security.HtmlSanitizer;
import org.springframework.stereotype.Component;

/**
 * Factory for creating WebSocket messages
 */
@Component
public class MessageFactory {

    private final HtmlSanitizer htmlSanitizer;

    public MessageFactory(HtmlSanitizer htmlSanitizer) {
        this.htmlSanitizer = htmlSanitizer;
    }

    /**
     * Creates a snapshot message for client initialization
     */
    public AppliedOp createSnapshot(DocumentSession document) {
        AppliedOp snapshot = new AppliedOp();
        snapshot.type = "snapshot";
        snapshot.version = document.getVersion();
        snapshot.content = htmlSanitizer.sanitize(document.getContent());
        return snapshot;
    }

    /**
     * Creates an applied operation message with HTML content
     */
    public AppliedOp createAppliedOperation(Op operation, int version, String htmlContent) {
        AppliedOp applied = new AppliedOp();
        applied.type = operation.type;
        applied.version = version;
        applied.clientId = operation.clientId;
        applied.opId = operation.opId;
        applied.htmlContent = htmlContent;
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