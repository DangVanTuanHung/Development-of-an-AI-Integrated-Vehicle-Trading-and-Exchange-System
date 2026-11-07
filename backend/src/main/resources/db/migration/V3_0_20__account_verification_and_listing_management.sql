ALTER TABLE ebike_auth.users ADD COLUMN phone_verified_at TIMESTAMP;
ALTER TABLE ebike_auth.users ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE ebike_auth.account_challenges (
    public_id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES ebike_auth.users(id) ON DELETE CASCADE,
    purpose VARCHAR(30) NOT NULL CHECK (purpose IN ('RESET_PASSWORD','VERIFY_EMAIL','VERIFY_PHONE')),
    target VARCHAR(255) NOT NULL,
    code_hash VARCHAR(100),
    provider_reference VARCHAR(100),
    attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL DEFAULT now() + interval '10 minutes',
    consumed_at TIMESTAMPTZ
);
CREATE INDEX idx_account_challenges_user ON ebike_auth.account_challenges(user_id, purpose, created_at DESC);

ALTER TABLE marketplace.vehicle_listings DROP CONSTRAINT vehicle_listings_status_check;
ALTER TABLE marketplace.vehicle_listings ADD CONSTRAINT vehicle_listings_status_check
CHECK (status IN ('DRAFT','PENDING_REVIEW','PUBLISHED','RESERVED','SOLD','REJECTED','SUSPENDED','EXPIRED','ARCHIVED','HIDDEN'));
ALTER TABLE marketplace.vehicle_listings ADD COLUMN renewed_at TIMESTAMPTZ;
UPDATE marketplace.vehicle_listings SET expires_at=now()+interval '30 days' WHERE status='PUBLISHED' AND expires_at IS NULL;

CREATE TABLE marketplace.listing_price_history (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL REFERENCES marketplace.vehicle_listings(id) ON DELETE CASCADE,
    actor_id BIGINT REFERENCES ebike_auth.users(id),
    previous_price NUMERIC(18,2),
    price NUMERIC(18,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
INSERT INTO marketplace.listing_price_history(listing_id,actor_id,price)
SELECT id,seller_id,price FROM marketplace.vehicle_listings;
CREATE INDEX idx_listing_price_history ON marketplace.listing_price_history(listing_id,created_at DESC);

CREATE TABLE marketplace.listing_engagements (
    listing_id BIGINT NOT NULL REFERENCES marketplace.vehicle_listings(id) ON DELETE CASCADE,
    visitor_key VARCHAR(64) NOT NULL,
    event_type VARCHAR(10) NOT NULL CHECK(event_type IN ('VIEW','CONTACT')),
    event_date DATE NOT NULL DEFAULT current_date,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY(listing_id,visitor_key,event_type,event_date)
);
