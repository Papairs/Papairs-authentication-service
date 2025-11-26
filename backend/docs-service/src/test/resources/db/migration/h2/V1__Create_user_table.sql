-- Migration: Create user table
-- Description: H2-compatible schema for users in docs service
-- Note: H2 equivalent of MySQL V1 migration
-- Date: 2025-11-12 (original deployment date)

CREATE TABLE "user" (
    user_id VARCHAR(36) NOT NULL,
    username VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id)
);
