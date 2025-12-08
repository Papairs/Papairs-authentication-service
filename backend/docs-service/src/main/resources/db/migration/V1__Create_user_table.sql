-- Migration: Create user table
-- Description: Initial schema for users in docs service
-- Author: Gustaw
-- Date: 2025 Nov 12

CREATE TABLE IF NOT EXISTS user (
    user_id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
