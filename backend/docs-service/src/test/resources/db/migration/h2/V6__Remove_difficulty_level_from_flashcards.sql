-- Migration: Remove difficulty level from flashcards (H2 version)
-- Description: Remove the difficulty_level column as it's no longer needed
-- Author: System
-- Date: 2025 Nov 18

ALTER TABLE flashcards DROP COLUMN difficulty_level;
