CREATE TABLE marketplace.appointments (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    listing_id BIGINT NOT NULL REFERENCES marketplace.vehicle_listings(id) ON DELETE CASCADE,
    buyer_id BIGINT NOT NULL REFERENCES ebike_auth.users(id),
    seller_id BIGINT NOT NULL REFERENCES ebike_auth.users(id),
    scheduled_at TIMESTAMPTZ NOT NULL,
    location VARCHAR(500) NOT NULL,
    note VARCHAR(2000),
    seller_note VARCHAR(2000),
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED'
        CHECK (status IN ('REQUESTED', 'CONFIRMED', 'DECLINED', 'COMPLETED', 'CANCELLED')),
    responded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (buyer_id <> seller_id)
);

CREATE INDEX idx_marketplace_appointments_buyer ON marketplace.appointments(buyer_id, scheduled_at DESC);
CREATE INDEX idx_marketplace_appointments_seller ON marketplace.appointments(seller_id, scheduled_at DESC);
CREATE INDEX idx_marketplace_appointments_listing ON marketplace.appointments(listing_id, scheduled_at DESC);
