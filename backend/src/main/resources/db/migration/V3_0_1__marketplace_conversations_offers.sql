CREATE TABLE marketplace.conversations (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    listing_id BIGINT REFERENCES marketplace.vehicle_listings(id) ON DELETE SET NULL,
    conversation_type VARCHAR(20) NOT NULL DEFAULT 'LISTING'
        CHECK (conversation_type IN ('LISTING', 'TRANSACTION', 'SUPPORT', 'DISPUTE')),
    last_message_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE marketplace.conversation_members (
    conversation_id BIGINT NOT NULL REFERENCES marketplace.conversations(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES ebike_auth.users(id) ON DELETE CASCADE,
    member_role VARCHAR(20) NOT NULL DEFAULT 'MEMBER'
        CHECK (member_role IN ('BUYER', 'SELLER', 'SUPPORT', 'MEMBER')),
    last_read_at TIMESTAMPTZ,
    muted BOOLEAN NOT NULL DEFAULT FALSE,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (conversation_id, user_id)
);

CREATE TABLE marketplace.messages (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    conversation_id BIGINT NOT NULL REFERENCES marketplace.conversations(id) ON DELETE CASCADE,
    sender_id BIGINT REFERENCES ebike_auth.users(id) ON DELETE SET NULL,
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT'
        CHECK (message_type IN ('TEXT', 'IMAGE', 'OFFER', 'SYSTEM', 'DOCUMENT')),
    content TEXT,
    attachment_key TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    edited_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (content IS NOT NULL OR attachment_key IS NOT NULL)
);

CREATE INDEX idx_marketplace_messages_conversation ON marketplace.messages(conversation_id, created_at DESC);

CREATE TABLE marketplace.offers (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    listing_id BIGINT NOT NULL REFERENCES marketplace.vehicle_listings(id),
    conversation_id BIGINT REFERENCES marketplace.conversations(id),
    buyer_id BIGINT NOT NULL REFERENCES ebike_auth.users(id),
    seller_id BIGINT NOT NULL REFERENCES ebike_auth.users(id),
    amount NUMERIC(18,2) NOT NULL CHECK (amount > 0),
    deposit_amount NUMERIC(18,2) NOT NULL CHECK (deposit_amount >= 0 AND deposit_amount <= amount),
    message TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'COUNTERED', 'ACCEPTED', 'REJECTED', 'WITHDRAWN', 'EXPIRED')),
    parent_offer_id BIGINT REFERENCES marketplace.offers(id),
    expires_at TIMESTAMPTZ NOT NULL,
    responded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (buyer_id <> seller_id)
);

CREATE INDEX idx_marketplace_offers_listing ON marketplace.offers(listing_id, status, created_at DESC);
CREATE INDEX idx_marketplace_offers_buyer ON marketplace.offers(buyer_id, status);
CREATE INDEX idx_marketplace_offers_seller ON marketplace.offers(seller_id, status);

