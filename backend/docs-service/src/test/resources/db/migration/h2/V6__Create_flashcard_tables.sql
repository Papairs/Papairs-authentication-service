-- Migration: Create flashcard tables (H2 version)
-- Description: Schema for flashcard system with spaced repetition tracking
-- Author: System
-- Date: 2025 Nov 17

-- Drop tables if they exist (to clean up Hibernate-created tables)
DROP TABLE IF EXISTS flashcard_tags;
DROP TABLE IF EXISTS flashcards;

-- Create flashcards table
CREATE TABLE flashcards (
    flashcard_id VARCHAR(36) PRIMARY KEY,
    page_id VARCHAR(36) NOT NULL,
    owner_id VARCHAR(36) NOT NULL,
    question VARCHAR(500) NOT NULL,
    answer VARCHAR(1000) NOT NULL,
    difficulty_level VARCHAR(20) DEFAULT 'MEDIUM',
    times_reviewed INT DEFAULT 0,
    times_correct INT DEFAULT 0,
    last_reviewed TIMESTAMP NULL,
    next_review_date TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_flashcard_page
        FOREIGN KEY (page_id)
        REFERENCES page(page_id)
        ON DELETE CASCADE,
        
    CONSTRAINT fk_flashcard_owner
        FOREIGN KEY (owner_id)
        REFERENCES "user"(user_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_flashcard_page_id ON flashcards(page_id);
CREATE INDEX idx_flashcard_owner_id ON flashcards(owner_id);
CREATE INDEX idx_flashcard_next_review ON flashcards(next_review_date);

-- Create flashcard_tags table
CREATE TABLE flashcard_tags (
    flashcard_id VARCHAR(36) NOT NULL,
    tag VARCHAR(255) NOT NULL,
    
    PRIMARY KEY (flashcard_id, tag),
    
    CONSTRAINT fk_flashcard_tags_flashcard
        FOREIGN KEY (flashcard_id)
        REFERENCES flashcards(flashcard_id)
        ON DELETE CASCADE
);
