CREATE TABLE marketplace.listing_comments (
 id BIGSERIAL PRIMARY KEY, public_id UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
 listing_id BIGINT NOT NULL REFERENCES marketplace.vehicle_listings(id),
 author_id BIGINT NOT NULL REFERENCES ebike_auth.users(id), parent_id BIGINT REFERENCES marketplace.listing_comments(id),
 content VARCHAR(2000) NOT NULL, status VARCHAR(12) NOT NULL DEFAULT 'VISIBLE' CHECK(status IN ('VISIBLE','HIDDEN','DELETED')),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_comment_listing ON marketplace.listing_comments(listing_id,created_at DESC);
CREATE TABLE marketplace.user_blocks (
 user_id BIGINT NOT NULL REFERENCES ebike_auth.users(id), blocked_id BIGINT NOT NULL REFERENCES ebike_auth.users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), PRIMARY KEY(user_id,blocked_id), CHECK(user_id<>blocked_id)
);
CREATE TABLE marketplace.community_reports (
 id BIGSERIAL PRIMARY KEY, reporter_id BIGINT NOT NULL REFERENCES ebike_auth.users(id),
 target_type VARCHAR(16) NOT NULL CHECK(target_type IN ('COMMENT','REVIEW','MESSAGE','USER')),
 target_id VARCHAR(64) NOT NULL, target_user_id BIGINT NOT NULL REFERENCES ebike_auth.users(id),
 reason VARCHAR(80) NOT NULL, description VARCHAR(2000) NOT NULL, snapshot TEXT NOT NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'OPEN' CHECK(status IN ('OPEN','RESOLVED','DISMISSED')),
 resolution VARCHAR(2000), resolved_by BIGINT REFERENCES ebike_auth.users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), resolved_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_open_community_report ON marketplace.community_reports(reporter_id,target_type,target_id) WHERE status='OPEN';
ALTER TABLE marketplace.seller_reviews DROP CONSTRAINT seller_reviews_transaction_id_key;
ALTER TABLE marketplace.seller_reviews ADD CONSTRAINT uq_transaction_reviewer UNIQUE(transaction_id,reviewer_id);
ALTER TABLE marketplace.seller_reviews ADD COLUMN status VARCHAR(12) NOT NULL DEFAULT 'VISIBLE' CHECK(status IN ('VISIBLE','HIDDEN','DELETED'));
ALTER TABLE marketplace.seller_reviews ADD COLUMN reply VARCHAR(2000);
ALTER TABLE marketplace.seller_reviews ADD COLUMN replied_at TIMESTAMPTZ;
CREATE TABLE marketplace.saved_searches (
 id BIGSERIAL PRIMARY KEY,user_id BIGINT NOT NULL REFERENCES ebike_auth.users(id),name VARCHAR(100) NOT NULL,
 filters JSONB NOT NULL,created_at TIMESTAMPTZ NOT NULL DEFAULT now(),checked_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE marketplace.search_alerts (
 search_id BIGINT NOT NULL REFERENCES marketplace.saved_searches(id) ON DELETE CASCADE,
 listing_id BIGINT NOT NULL REFERENCES marketplace.vehicle_listings(id),PRIMARY KEY(search_id,listing_id)
);
CREATE TABLE marketplace.recent_views (
 user_id BIGINT NOT NULL REFERENCES ebike_auth.users(id),listing_id BIGINT NOT NULL REFERENCES marketplace.vehicle_listings(id),
 viewed_at TIMESTAMPTZ NOT NULL DEFAULT now(),PRIMARY KEY(user_id,listing_id)
);
CREATE TABLE marketplace.comparison_lists (
 user_id BIGINT PRIMARY KEY REFERENCES ebike_auth.users(id),listing_ids JSONB NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE marketplace.appointments ADD COLUMN proposed_by BIGINT REFERENCES ebike_auth.users(id);
UPDATE marketplace.appointments SET proposed_by=buyer_id;
ALTER TABLE marketplace.appointments ADD COLUMN revision INTEGER NOT NULL DEFAULT 0;
ALTER TABLE marketplace.appointments ADD COLUMN reminder_at TIMESTAMPTZ;
ALTER TABLE marketplace.appointments ADD CONSTRAINT uq_buyer_listing_time UNIQUE(buyer_id,listing_id,scheduled_at);
CREATE TABLE ebike_support.ticket_messages (
 id BIGSERIAL PRIMARY KEY,ticket_id BIGINT NOT NULL REFERENCES ebike_support.support_tickets(id),
 author_id BIGINT NOT NULL REFERENCES ebike_auth.users(id),content VARCHAR(4000) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_ticket_messages ON ebike_support.ticket_messages(ticket_id,id);
-- Preserve earlier public replies as history rather than replacing them on the next reply.
INSERT INTO ebike_support.ticket_messages(ticket_id,author_id,content,created_at)
SELECT t.id,u.id,t.staff_note,t.updated_at FROM ebike_support.support_tickets t
JOIN ebike_auth.users u ON u.username=t.assigned_to WHERE nullif(trim(t.staff_note),'') IS NOT NULL;

CREATE TABLE marketplace.notification_delivery (
 notification_id BIGINT PRIMARY KEY REFERENCES ebike_user.notifications(id) ON DELETE CASCADE,
 email_requested BOOLEAN NOT NULL DEFAULT false,streamed BOOLEAN NOT NULL DEFAULT false,
 email_status VARCHAR(12) NOT NULL DEFAULT 'PENDING',attempts INTEGER NOT NULL DEFAULT 0,
 next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE FUNCTION marketplace.notify(recipient BIGINT,title TEXT,message TEXT,target TEXT,mail BOOLEAN DEFAULT false) RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE notification BIGINT;
BEGIN
 IF recipient IS NULL OR NOT EXISTS(SELECT 1 FROM ebike_auth.users WHERE id=recipient AND is_active) THEN RETURN; END IF;
 INSERT INTO ebike_user.notifications(user_id,type,title,message,target_url,created_at)
 VALUES(recipient,'MARKETPLACE',left(title,180),message,left(target,255),now()) RETURNING id INTO notification;
 INSERT INTO marketplace.notification_delivery(notification_id,email_requested) VALUES(notification,mail);
END $$;
