-- Add yjs_state column to store Y.js CRDT document state
-- Using LONGBLOB for MySQL to store binary data
ALTER TABLE page ADD COLUMN IF NOT EXISTS yjs_state LONGBLOB;
