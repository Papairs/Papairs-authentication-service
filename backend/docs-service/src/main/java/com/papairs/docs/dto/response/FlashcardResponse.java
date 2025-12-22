package com.papairs.docs.dto.response;

import com.papairs.docs.model.Flashcard;

import java.util.List;

/**
 * Response DTO for Flashcard - only includes essential fields needed by the client
 * Excludes internal fields like timesReviewed, timesCorrect, timestamps, and ownerId
 */
public record FlashcardResponse(
    String flashcardId,
    String pageId,
    String question,
    String answer,
    List<String> tags,
    Boolean learned
) {
    /**
     * Factory method to create FlashcardResponse from Flashcard entity
     */
    public static FlashcardResponse from(Flashcard flashcard) {
        return new FlashcardResponse(
            flashcard.getFlashcardId(),
            flashcard.getPageId(),
            flashcard.getQuestion(),
            flashcard.getAnswer(),
            flashcard.getTags(),
            flashcard.getLearned()
        );
    }
}
