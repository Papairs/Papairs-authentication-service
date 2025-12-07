package com.papairs.docs.dto.request;

/**
 * Request DTO for updating flashcard learned status
 */
public record UpdateLearnedRequest(
    Boolean learned
) {
}
