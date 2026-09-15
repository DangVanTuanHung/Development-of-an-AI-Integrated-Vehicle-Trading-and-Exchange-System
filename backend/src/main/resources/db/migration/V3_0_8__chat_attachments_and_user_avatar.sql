ALTER TABLE ebike_auth.users ADD COLUMN IF NOT EXISTS avatar_url TEXT;

ALTER TABLE marketplace.messages DROP CONSTRAINT IF EXISTS messages_message_type_check;
ALTER TABLE marketplace.messages ADD CONSTRAINT messages_message_type_check
    CHECK (message_type IN ('TEXT', 'IMAGE', 'VIDEO', 'DOCUMENT', 'LOCATION', 'OFFER', 'SYSTEM'));
