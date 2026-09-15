ALTER TABLE ebike_auth.users ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20);
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_phone_number ON ebike_auth.users(phone_number) WHERE phone_number IS NOT NULL;
