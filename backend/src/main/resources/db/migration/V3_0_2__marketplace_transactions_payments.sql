CREATE TABLE marketplace.transactions (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    transaction_number VARCHAR(40) NOT NULL UNIQUE,
    listing_id BIGINT NOT NULL REFERENCES marketplace.vehicle_listings(id),
    accepted_offer_id BIGINT REFERENCES marketplace.offers(id),
    buyer_id BIGINT NOT NULL REFERENCES ebike_auth.users(id),
    seller_id BIGINT NOT NULL REFERENCES ebike_auth.users(id),
    agreed_price NUMERIC(18,2) NOT NULL CHECK (agreed_price > 0),
    deposit_amount NUMERIC(18,2) NOT NULL CHECK (deposit_amount >= 0 AND deposit_amount <= agreed_price),
    remaining_amount NUMERIC(18,2) GENERATED ALWAYS AS (agreed_price - deposit_amount) STORED,
    platform_fee NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK (platform_fee >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    status VARCHAR(40) NOT NULL DEFAULT 'CREATED'
        CHECK (status IN ('CREATED', 'DEPOSIT_PENDING', 'DEPOSIT_PAID', 'INSPECTION_PENDING', 'INSPECTION_PASSED', 'INSPECTION_FAILED', 'FINAL_PAYMENT_PENDING', 'FULLY_PAID', 'HANDOVER_CONFIRMED', 'COMPLETED', 'CANCELLATION_REQUESTED', 'CANCELLED', 'DISPUTED', 'REFUNDED')),
    inspection_deadline TIMESTAMPTZ,
    final_payment_deadline TIMESTAMPTZ,
    handover_deadline TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    cancellation_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CHECK (buyer_id <> seller_id)
);

CREATE UNIQUE INDEX uq_marketplace_active_transaction_listing ON marketplace.transactions(listing_id)
WHERE status NOT IN ('COMPLETED', 'CANCELLED', 'REFUNDED');
CREATE INDEX idx_marketplace_transaction_buyer ON marketplace.transactions(buyer_id, created_at DESC);
CREATE INDEX idx_marketplace_transaction_seller ON marketplace.transactions(seller_id, created_at DESC);
CREATE INDEX idx_marketplace_transaction_status ON marketplace.transactions(status, created_at DESC);

CREATE TABLE marketplace.transaction_events (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL REFERENCES marketplace.transactions(id) ON DELETE CASCADE,
    actor_id BIGINT REFERENCES ebike_auth.users(id),
    from_status VARCHAR(40),
    to_status VARCHAR(40) NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    note TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_marketplace_transaction_events ON marketplace.transaction_events(transaction_id, created_at);

CREATE TABLE marketplace_payment.payments (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    transaction_id BIGINT NOT NULL REFERENCES marketplace.transactions(id),
    payment_reference VARCHAR(64) NOT NULL UNIQUE,
    payment_stage VARCHAR(20) NOT NULL CHECK (payment_stage IN ('DEPOSIT', 'FINAL', 'FEE')),
    provider VARCHAR(30) NOT NULL CHECK (provider IN ('VNPAY', 'BANK_TRANSFER', 'INTERNAL')),
    amount NUMERIC(18,2) NOT NULL CHECK (amount > 0),
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'PROCESSING', 'PAID', 'FAILED', 'CANCELLED', 'REFUND_PENDING', 'PARTIALLY_REFUNDED', 'REFUNDED')),
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    provider_transaction_id VARCHAR(160),
    provider_response JSONB NOT NULL DEFAULT '{}'::jsonb,
    expires_at TIMESTAMPTZ,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_marketplace_payments_transaction ON marketplace_payment.payments(transaction_id, payment_stage);
CREATE INDEX idx_marketplace_payments_provider_txn ON marketplace_payment.payments(provider, provider_transaction_id);

CREATE TABLE marketplace_payment.payment_events (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES marketplace_payment.payments(id) ON DELETE CASCADE,
    provider_event_id VARCHAR(180),
    event_type VARCHAR(80) NOT NULL,
    signature_valid BOOLEAN,
    payload JSONB NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ,
    processing_error TEXT,
    UNIQUE (payment_id, provider_event_id)
);

CREATE TABLE marketplace_payment.refunds (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    payment_id BIGINT NOT NULL REFERENCES marketplace_payment.payments(id),
    requested_by BIGINT REFERENCES ebike_auth.users(id),
    amount NUMERIC(18,2) NOT NULL CHECK (amount > 0),
    reason TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED'
        CHECK (status IN ('REQUESTED', 'APPROVED', 'PROCESSING', 'COMPLETED', 'REJECTED', 'FAILED')),
    provider_refund_id VARCHAR(180),
    reviewed_by BIGINT REFERENCES ebike_auth.users(id),
    reviewed_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE marketplace_payment.payouts (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    transaction_id BIGINT NOT NULL REFERENCES marketplace.transactions(id),
    seller_id BIGINT NOT NULL REFERENCES ebike_auth.users(id),
    gross_amount NUMERIC(18,2) NOT NULL CHECK (gross_amount > 0),
    fee_amount NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK (fee_amount >= 0),
    net_amount NUMERIC(18,2) NOT NULL CHECK (net_amount > 0),
    status VARCHAR(30) NOT NULL DEFAULT 'ON_HOLD'
        CHECK (status IN ('ON_HOLD', 'READY', 'PROCESSING', 'PAID', 'FAILED', 'CANCELLED')),
    payout_reference VARCHAR(80) UNIQUE,
    bank_account_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    released_at TIMESTAMPTZ,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE marketplace.disputes (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    transaction_id BIGINT NOT NULL REFERENCES marketplace.transactions(id),
    opened_by BIGINT NOT NULL REFERENCES ebike_auth.users(id),
    assigned_to BIGINT REFERENCES ebike_auth.users(id),
    reason_code VARCHAR(80) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN', 'UNDER_REVIEW', 'WAITING_EVIDENCE', 'RESOLVED_BUYER', 'RESOLVED_SELLER', 'CLOSED')),
    resolution TEXT,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

