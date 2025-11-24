-- Migration: Create folder table
-- Description: H2-compatible schema for document folders
-- Note: H2 equivalent of MySQL V2 migration
-- Date: 2025-11-12 (original deployment date)

CREATE TABLE folder (
    folder_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    parent_folder_id VARCHAR(36) DEFAULT NULL,
    owner_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (folder_id)
);

CREATE INDEX idx_folder_owner ON folder(owner_id);
CREATE INDEX idx_folder_parent ON folder(parent_folder_id);
CREATE INDEX idx_folder_owner_parent ON folder(owner_id, parent_folder_id);

ALTER TABLE folder ADD CONSTRAINT folder_ibfk_1 
    FOREIGN KEY (parent_folder_id) REFERENCES folder(folder_id) ON DELETE CASCADE;
    
ALTER TABLE folder ADD CONSTRAINT folder_ibfk_2 
    FOREIGN KEY (owner_id) REFERENCES "user"(user_id) ON DELETE CASCADE;
