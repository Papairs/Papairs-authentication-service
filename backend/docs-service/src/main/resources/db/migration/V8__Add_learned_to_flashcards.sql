-- Add learned column to flashcards table
ALTER TABLE flashcards
ADD COLUMN learned BOOLEAN NOT NULL DEFAULT false;
