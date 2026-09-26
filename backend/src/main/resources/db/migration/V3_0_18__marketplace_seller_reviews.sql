CREATE TABLE marketplace.seller_reviews (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    transaction_id BIGINT NOT NULL UNIQUE REFERENCES marketplace.transactions(id) ON DELETE CASCADE,
    reviewer_id BIGINT NOT NULL REFERENCES ebike_auth.users(id),
    seller_id BIGINT NOT NULL REFERENCES ebike_auth.users(id),
    rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (reviewer_id <> seller_id)
);

CREATE INDEX idx_marketplace_seller_reviews_seller
    ON marketplace.seller_reviews(seller_id, created_at DESC);
