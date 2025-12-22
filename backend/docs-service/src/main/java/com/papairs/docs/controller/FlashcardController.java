package com.papairs.docs.controller;

import com.papairs.docs.dto.request.CreateFlashcardRequest;
import com.papairs.docs.dto.request.UpdateLearnedRequest;
import com.papairs.docs.dto.response.FlashcardResponse;
import com.papairs.docs.model.ApiResponse;
import com.papairs.docs.model.Flashcard;
import com.papairs.docs.service.FlashcardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/docs/flashcards")
public class FlashcardController {

    private final FlashcardService flashcardService;

    public FlashcardController(FlashcardService flashcardService) {
        this.flashcardService = flashcardService;
    }

    /**
     * Create a new flashcard
     */
    @PostMapping
    public ResponseEntity<ApiResponse> createFlashcard(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody CreateFlashcardRequest request) {
        
        Flashcard flashcard = new Flashcard();
        flashcard.setPageId(request.pageId());
        flashcard.setQuestion(request.question());
        flashcard.setAnswer(request.answer());
        flashcard.setTags(request.tags());
        
        Flashcard saved = flashcardService.createFlashcard(flashcard, userId);
        FlashcardResponse response = FlashcardResponse.from(saved);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Flashcard created successfully", response));
    }

    /**
     * Get all flashcards for the current user
     */
    @GetMapping
    public ResponseEntity<ApiResponse> getUserFlashcards(
            @RequestHeader("X-User-Id") String userId) {
        
        List<Flashcard> flashcards = flashcardService.getUserFlashcards(userId);
        List<FlashcardResponse> responses = flashcards.stream()
                .map(FlashcardResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Flashcards retrieved successfully", responses));
    }

    /**
     * Get flashcards for a specific page
     */
    @GetMapping("/page/{pageId}")
    public ResponseEntity<ApiResponse> getPageFlashcards(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String pageId) {
        
        List<Flashcard> flashcards = flashcardService.getPageFlashcards(pageId, userId);
        List<FlashcardResponse> responses = flashcards.stream()
                .map(FlashcardResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Page flashcards retrieved successfully", responses));
    }

    /**
     * Update learned status of a flashcard
     */
    @PutMapping("/{flashcardId}/learned")
    public ResponseEntity<ApiResponse> updateLearnedStatus(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String flashcardId,
            @RequestBody UpdateLearnedRequest request) {
        
        Flashcard updated = flashcardService.updateLearnedStatus(flashcardId, userId, request.learned());
        FlashcardResponse response = FlashcardResponse.from(updated);
        
        return ResponseEntity.ok(ApiResponse.success("Flashcard learned status updated", response));
    }

    /**
     * Reset all flashcards to unlearned for the current user
     */
    @PutMapping("/reset")
    public ResponseEntity<ApiResponse> resetAllFlashcards(
            @RequestHeader("X-User-Id") String userId) {
        
        int count = flashcardService.resetAllFlashcards(userId);
        
        return ResponseEntity.ok(ApiResponse.success(
            "All " + count + " flashcards reset to unlearned", 
            count
        ));
    }

    /**
     * Delete a flashcard
     */
    @DeleteMapping("/{flashcardId}")
    public ResponseEntity<ApiResponse> deleteFlashcard(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String flashcardId) {
        
        flashcardService.deleteFlashcard(flashcardId, userId);
        return ResponseEntity.ok(ApiResponse.success("Flashcard deleted successfully", null));
    }
}
