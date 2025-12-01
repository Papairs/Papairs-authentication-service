package com.papairs.docs.repository;

import com.papairs.docs.model.Flashcard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlashcardRepository extends JpaRepository<Flashcard, String> {
    
    /**
     * Find all flashcards owned by a specific user
     */
    List<Flashcard> findByOwnerId(String ownerId);
    
    /**
     * Find all flashcards for a specific page
     */
    List<Flashcard> findByPageId(String pageId);
    
    /**
     * Find all flashcards for a specific page owned by a user
     */
    List<Flashcard> findByPageIdAndOwnerId(String pageId, String ownerId);
}
