package com.papairs.docs.ws;

import com.papairs.docs.model.OT.DeleteOp;
import com.papairs.docs.model.OT.InsertOp;
import com.papairs.docs.model.OT.Op;
import com.papairs.docs.util.OtTransform;
import org.springframework.stereotype.Component;

/**
 * Handles operational transform operations for collaborative editing.
 * Provides methods for transforming, applying, and cloning operations.
 */
@Component
public class OperationHandler {

    /**
     * Transforms an operation against the document's operation history.
     * Resolves conflicts by transforming against concurrent operations.
     */
    public Op transformOperation(Op operation, DocumentSession document) {
        Op transformed = operation;
        for (int i = operation.baseVersion; i < document.getVersion(); i++) {
            Op historicalOp = document.getOperation(i);
            if (historicalOp != null) {
                transformed = OtTransform.transform(transformed, historicalOp);
            }
        }
        return transformed;
    }

    /**
     * Applies an operation to document content.
     * Handles insert and delete operations with position validation.
     */
    public String applyOperation(String content, Op operation) {
        return switch (operation.type) {
            case "insert" -> applyInsertOperation(content, (InsertOp) operation);
            case "delete" -> applyDeleteOperation(content, (DeleteOp) operation);
            default -> content;
        };
    }

    /**
     * Creates a deep copy of an operation for history storage.
     */
    public Op cloneOperation(Op operation) {
        return switch (operation.type) {
            case "insert" -> cloneInsertOperation((InsertOp) operation);
            case "delete" -> cloneDeleteOperation((DeleteOp) operation);
            default -> operation;
        };
    }

    private String applyInsertOperation(String content, InsertOp operation) {
        int position = clamp(operation.pos, 0, content.length());
        return content.substring(0, position) + operation.text + content.substring(position);
    }

    private String applyDeleteOperation(String content, DeleteOp operation) {
        int start = clamp(operation.pos, 0, content.length());
        int end = clamp(start + operation.length, 0, content.length());
        return content.substring(0, start) + content.substring(end);
    }

    private InsertOp cloneInsertOperation(InsertOp original) {
        InsertOp clone = new InsertOp();
        clone.type = "insert";
        clone.pos = original.pos;
        clone.baseVersion = original.baseVersion;
        clone.clientId = original.clientId;
        clone.opId = original.opId;
        clone.text = original.text;
        return clone;
    }

    private DeleteOp cloneDeleteOperation(DeleteOp original) {
        DeleteOp clone = new DeleteOp();
        clone.type = "delete";
        clone.pos = original.pos;
        clone.baseVersion = original.baseVersion;
        clone.clientId = original.clientId;
        clone.opId = original.opId;
        clone.length = original.length;
        return clone;
    }

    /**
     * Clamps a value between minimum and maximum bounds.
     */
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}