-- Migration: Rename token to token_hash in sessions table
-- Description: Rename token column to token_hash for clarity
-- Date: 2025 12. Apr

ALTER TABLE sessions RENAME COLUMN token TO token_hash;

DROP INDEX IF EXISTS idx_session_token;
CREATE UNIQUE INDEX IF NOT EXISTS idx_session_token_hash ON sessions(token_hash);