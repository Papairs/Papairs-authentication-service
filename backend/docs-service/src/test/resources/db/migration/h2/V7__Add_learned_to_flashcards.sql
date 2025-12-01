-- Add learned column to flashcards table (H2 version)
ALTER TABLE flashcards
ADD COLUMN learned BOOLEAN NOT NULL DEFAULT false;
