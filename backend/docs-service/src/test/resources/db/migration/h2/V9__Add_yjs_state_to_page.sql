-- Add yjs_state column to store Y.js CRDT document state (H2 version)
ALTER TABLE page ADD COLUMN yjs_state BINARY LARGE OBJECT;
