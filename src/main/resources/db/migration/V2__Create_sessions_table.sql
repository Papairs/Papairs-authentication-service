-- Migration: Create sessions table
-- Description: Initial schema for user sessions
-- Author: Gustaw
-- Date: 2025 3. Nov

CREATE TABLE IF NOT EXISTS sessions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_active_at TIMESTAMP NULL,

    CONSTRAINT fk_session_user
        FOREIGN KEY (user_id)
            REFERENCES users(id)
            ON DELETE CASCADE
);

CREATE INDEX idx_user_id ON sessions (user_id);
CREATE INDEX idx_token ON sessions (token);
CREATE INDEX idx_expires_at ON sessions (expires_at);
