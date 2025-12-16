-- Add page_id column to user_file table
ALTER TABLE user_file 
ADD COLUMN page_id VARCHAR(36);

-- For existing files without a page_id, you may want to:
-- 1. Set them to a default page
-- 2. Delete them
-- 3. Manually assign them to pages
-- 
-- Example: Set to a default page for each user (uncomment if needed)
-- UPDATE user_file uf
-- SET page_id = (
--     SELECT page_id 
--     FROM page p 
--     WHERE p.owner_id = uf.user_id 
--     LIMIT 1
-- )
-- WHERE page_id IS NULL;

-- Make page_id non-nullable after migration
ALTER TABLE user_file 
MODIFY COLUMN page_id VARCHAR(36) NOT NULL;

-- Add index for better query performance
CREATE INDEX idx_user_file_page_id ON user_file(page_id);

-- Add foreign key constraint (optional, but recommended)
ALTER TABLE user_file
ADD CONSTRAINT fk_user_file_page
FOREIGN KEY (page_id) REFERENCES page(page_id)
ON DELETE CASCADE;
