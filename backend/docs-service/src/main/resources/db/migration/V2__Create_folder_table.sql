-- Migration: Create folder table
-- Description: Folder structure for organizing documents
-- Author: Gustaw
-- Date: 2025 Nov 12

CREATE TABLE IF NOT EXISTS folder (
    folder_id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    parent_folder_id VARCHAR(36),
    owner_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_parent_folder_id (parent_folder_id),
    INDEX idx_owner_id (owner_id),
    
    CONSTRAINT fk_folder_parent
        FOREIGN KEY (parent_folder_id)
        REFERENCES folder(folder_id)
        ON DELETE CASCADE,
        
    CONSTRAINT fk_folder_owner
        FOREIGN KEY (owner_id)
        REFERENCES user(user_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
