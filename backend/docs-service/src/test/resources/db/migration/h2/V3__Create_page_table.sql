-- Migration: Create page table
-- Description: H2-compatible schema for document pages
-- Note: H2 equivalent of MySQL V3 migration
-- Date: 2025-11-12 (original deployment date)

CREATE TABLE page (
    page_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL DEFAULT 'Untitled',
    folder_id VARCHAR(36) DEFAULT NULL,
    owner_id VARCHAR(36) NOT NULL,
    content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (page_id)
);

CREATE INDEX idx_page_owner ON page(owner_id);
CREATE INDEX idx_page_folder ON page(folder_id);

ALTER TABLE page ADD CONSTRAINT page_ibfk_1 
    FOREIGN KEY (folder_id) REFERENCES folder(folder_id) ON DELETE SET NULL;
    
ALTER TABLE page ADD CONSTRAINT page_ibfk_2 
    FOREIGN KEY (owner_id) REFERENCES "user"(user_id) ON DELETE CASCADE;
