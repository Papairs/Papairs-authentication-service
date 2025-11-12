package com.papairs.docs.controller;

import com.papairs.docs.model.ApiResponse;
import com.papairs.docs.model.Flashcard;
import com.papairs.docs.repository.FlashcardRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/docs/flashcards")
@CrossOrigin(origins = "http://localhost:3000")
public class FlashcardController {

    private final FlashcardRepository flashcardRepository;

    public FlashcardController(FlashcardRepository flashcardRepository) {
        this.flashcardRepository = flashcardRepository;
    }

    /**
     * Create a new flashcard
     */
    @PostMapping
    public ResponseEntity<ApiResponse> createFlashcard(
            @RequestHeader("User-Id") String userId,
            @RequestBody Flashcard flashcard) {
        
        flashcard.setOwnerId(userId);
        Flashcard saved = flashcardRepository.save(flashcard);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Flashcard created successfully", saved));
    }

    /**
     * Get all flashcards for the current user
     */
    @GetMapping
    public ResponseEntity<ApiResponse> getUserFlashcards(
            @RequestHeader("User-Id") String userId) {
        
        List<Flashcard> flashcards = flashcardRepository.findByOwnerId(userId);
        return ResponseEntity.ok(ApiResponse.success("Flashcards retrieved successfully", flashcards));
    }

    /**
     * Get flashcards for a specific page
     */
    @GetMapping("/page/{pageId}")
    public ResponseEntity<ApiResponse> getPageFlashcards(
            @RequestHeader("User-Id") String userId,
            @PathVariable String pageId) {
        
        List<Flashcard> flashcards = flashcardRepository.findByPageIdAndOwnerId(pageId, userId);
        return ResponseEntity.ok(ApiResponse.success("Page flashcards retrieved successfully", flashcards));
    }

    /**
     * Delete a flashcard
     */
    @DeleteMapping("/{flashcardId}")
    public ResponseEntity<ApiResponse> deleteFlashcard(
            @RequestHeader("User-Id") String userId,
            @PathVariable String flashcardId) {
        
        Flashcard flashcard = flashcardRepository.findById(flashcardId)
                .orElseThrow(() -> new RuntimeException("Flashcard not found"));
        
        if (!flashcard.getOwnerId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You don't have permission to delete this flashcard"));
        }
        
        flashcardRepository.delete(flashcard);
        return ResponseEntity.ok(ApiResponse.success("Flashcard deleted successfully", null));
    }
}
