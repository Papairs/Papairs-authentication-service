-- Migration: Create page_member table
-- Description: H2-compatible schema for page members
-- Note: H2 equivalent of MySQL V4 migration
-- Date: 2025-11-12 (original deployment date)

CREATE TYPE IF NOT EXISTS page_member_role AS ENUM ('EDITOR', 'VIEWER');

CREATE TABLE page_member (
    page_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    role VARCHAR(50) NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (page_id, user_id)
);

CREATE INDEX idx_page_member_user ON page_member(user_id);
CREATE INDEX idx_page_member_role ON page_member(page_id, role);

ALTER TABLE page_member ADD CONSTRAINT page_member_ibfk_1
    FOREIGN KEY (page_id) REFERENCES page(page_id) ON DELETE CASCADE;

ALTER TABLE page_member ADD CONSTRAINT page_member_ibfk_2
    FOREIGN KEY (user_id) REFERENCES "user"(user_id) ON DELETE CASCADE;
