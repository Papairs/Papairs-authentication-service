-- Migration: Create page_member table
-- Description: Page member permissions and sharing
-- Author: Gustaw
-- Date: 2025 Nov 12

CREATE TABLE IF NOT EXISTS page_member (
    id VARCHAR(36) PRIMARY KEY,
    page_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'VIEWER',
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_page_id (page_id),
    INDEX idx_user_id (user_id),
    UNIQUE KEY unique_page_member (page_id, user_id),
    
    CONSTRAINT fk_page_member_page
        FOREIGN KEY (page_id)
        REFERENCES page(page_id)
        ON DELETE CASCADE,
        
    CONSTRAINT fk_page_member_user
        FOREIGN KEY (user_id)
        REFERENCES user(user_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
