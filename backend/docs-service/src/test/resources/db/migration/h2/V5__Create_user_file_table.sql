-- Migration: Create user_file table
-- Description: Schema for user-uploaded files stored in Backblaze B2
-- Author: AI Context Feature
-- Date: 2025 Nov 17

CREATE TABLE IF NOT EXISTS user_file (
    file_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    b2_file_id VARCHAR(255) NOT NULL,
    b2_file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_user_filename UNIQUE (user_id, filename)
);

CREATE INDEX idx_user_id ON user_file(user_id);
CREATE INDEX idx_filename ON user_file(filename);
CREATE INDEX idx_b2_file_id ON user_file(b2_file_id);

-- Add foreign key constraint
ALTER TABLE user_file
    ADD CONSTRAINT fk_file_user
        FOREIGN KEY (user_id)
        REFERENCES "user"(user_id)
        ON DELETE CASCADE;
