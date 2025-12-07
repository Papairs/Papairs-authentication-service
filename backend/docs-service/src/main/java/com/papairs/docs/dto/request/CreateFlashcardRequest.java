package com.papairs.docs.dto.request;

import java.util.List;

/**
 * Request DTO for creating a new flashcard
 */
public record CreateFlashcardRequest(
    String pageId,
    String question,
    String answer,
    List<String> tags
) {
}
