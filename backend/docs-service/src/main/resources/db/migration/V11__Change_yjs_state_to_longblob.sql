-- Change yjs_state column from TEXT/LONGTEXT to LONGBLOB for binary Y.js state storage
ALTER TABLE page MODIFY COLUMN yjs_state LONGBLOB;
