-- Change yjs_state column from TEXT/LONGTEXT to BLOB for binary Y.js state storage (H2 version)
ALTER TABLE page ALTER COLUMN yjs_state BINARY LARGE OBJECT;
