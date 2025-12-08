-- Migration: Create page table
-- Description: Initial schema for document pages
-- Author: Gustaw
-- Date: 2025 Nov 12

CREATE TABLE IF NOT EXISTS page (
    page_id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    folder_id VARCHAR(36),
    owner_id VARCHAR(36) NOT NULL,
    content TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_folder_id (folder_id),
    INDEX idx_owner_id (owner_id),
    
    CONSTRAINT fk_page_folder
        FOREIGN KEY (folder_id)
        REFERENCES folder(folder_id)
        ON DELETE SET NULL,
        
    CONSTRAINT fk_page_owner
        FOREIGN KEY (owner_id)
        REFERENCES user(user_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
