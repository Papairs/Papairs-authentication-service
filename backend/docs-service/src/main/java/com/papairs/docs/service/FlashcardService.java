package com.papairs.docs.service;

import com.papairs.docs.exception.UnauthorizedAccessException;
import com.papairs.docs.exception.ResourceNotFoundException;
import com.papairs.docs.model.Flashcard;
import com.papairs.docs.repository.FlashcardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FlashcardService {
    private final FlashcardRepository flashcardRepository;

    public FlashcardService(FlashcardRepository flashcardRepository) {
        this.flashcardRepository = flashcardRepository;
    }

    /**
     * Creates a new flashcard for a specific user
     * @param flashcard The flashcard entity to create
     * @param userId The ID of the user creating the flashcard
     * @return The newly created flashcard
     */
    @Transactional
    public Flashcard createFlashcard(Flashcard flashcard, String userId) {
        flashcard.setOwnerId(userId);
        return flashcardRepository.save(flashcard);
    }

    /**
     * Retrieves all flashcards for a specific user
     * @param userId The ID of the user
     * @return A list of flashcards owned by the user
     */
    public List<Flashcard> getUserFlashcards(String userId) {
        return flashcardRepository.findByOwnerId(userId);
    }

    /**
     * Retrieves all flashcards for a specific page owned by a user
     * @param pageId The ID of the page
     * @param userId The ID of the user
     * @return A list of flashcards for the page and user
     */
    public List<Flashcard> getPageFlashcards(String pageId, String userId) {
        return flashcardRepository.findByPageIdAndOwnerId(pageId, userId);
    }

    /**
     * Updates the learned status of a flashcard
     * @param flashcardId The ID of the flashcard
     * @param userId The ID of the user requesting the update
     * @param learned The new learned status
     * @return The updated flashcard
     * @throws ResourceNotFoundException if the flashcard is not found
     * @throws UnauthorizedAccessException if the user doesn't own the flashcard
     */
    @Transactional
    public Flashcard updateLearnedStatus(String flashcardId, String userId, Boolean learned) {
        Flashcard flashcard = flashcardRepository.findById(flashcardId)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard not found"));

        if (!flashcard.getOwnerId().equals(userId)) {
            throw new UnauthorizedAccessException("You don't have permission to update this flashcard");
        }

        flashcard.setLearned(learned);
        return flashcardRepository.save(flashcard);
    }

    /**
     * Resets all flashcards to unlearned status for a user
     * @param userId The ID of the user
     * @return The number of flashcards reset
     */
    @Transactional
    public int resetAllFlashcards(String userId) {
        List<Flashcard> flashcards = flashcardRepository.findByOwnerId(userId);

        for (Flashcard flashcard : flashcards) {
            flashcard.setLearned(false);
        }

        flashcardRepository.saveAll(flashcards);
        return flashcards.size();
    }

    /**
     * Deletes a flashcard
     * @param flashcardId The ID of the flashcard to delete
     * @param userId The ID of the user requesting the deletion
     * @throws ResourceNotFoundException if the flashcard is not found
     * @throws UnauthorizedAccessException if the user doesn't own the flashcard
     */
    @Transactional
    public void deleteFlashcard(String flashcardId, String userId) {
        Flashcard flashcard = flashcardRepository.findById(flashcardId)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard not found"));

        if (!flashcard.getOwnerId().equals(userId)) {
            throw new UnauthorizedAccessException("You don't have permission to delete this flashcard");
        }

        flashcardRepository.delete(flashcard);
    }
}
