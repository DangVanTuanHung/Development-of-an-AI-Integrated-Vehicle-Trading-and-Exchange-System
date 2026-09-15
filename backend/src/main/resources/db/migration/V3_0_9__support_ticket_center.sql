CREATE SCHEMA IF NOT EXISTS ebike_support;

CREATE TABLE ebike_support.support_tickets (
    id BIGSERIAL PRIMARY KEY,
    ticket_code VARCHAR(24) NOT NULL UNIQUE,
    user_id BIGINT NULL REFERENCES ebike_auth.users(id) ON DELETE SET NULL,
    requester_name VARCHAR(160) NOT NULL,
    requester_email VARCHAR(255) NOT NULL,
    category VARCHAR(40) NOT NULL DEFAULT 'GENERAL',
    subject VARCHAR(220) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    staff_note TEXT NULL,
    assigned_to VARCHAR(120) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    CONSTRAINT chk_support_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'WAITING_CUSTOMER', 'RESOLVED', 'CLOSED')),
    CONSTRAINT chk_support_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT'))
);

CREATE INDEX idx_support_tickets_user ON ebike_support.support_tickets(user_id, created_at DESC);
CREATE INDEX idx_support_tickets_queue ON ebike_support.support_tickets(status, priority, created_at);
