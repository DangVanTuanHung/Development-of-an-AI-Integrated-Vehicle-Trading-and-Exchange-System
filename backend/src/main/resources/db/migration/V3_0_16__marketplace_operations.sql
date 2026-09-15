CREATE TABLE marketplace.listing_reports (
 id BIGSERIAL PRIMARY KEY,
 listing_id BIGINT NOT NULL REFERENCES marketplace.vehicle_listings(id),
 reporter_id BIGINT NOT NULL REFERENCES ebike_auth.users(id),
 reason TEXT NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','RESOLVED','DISMISSED')),
 resolution VARCHAR(40),
 resolution_note TEXT,
 resolved_by BIGINT REFERENCES ebike_auth.users(id),
 resolved_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_listing_open_report ON marketplace.listing_reports(listing_id, reporter_id) WHERE status='OPEN';
INSERT INTO marketplace.listing_reports(listing_id, reporter_id, reason, created_at)
SELECT DISTINCT ON (listing_id, reviewer_id) listing_id, reviewer_id, coalesce(note,'Báo cáo tin đăng'), created_at
FROM marketplace.listing_moderations WHERE reason_code='USER_REPORT' AND reviewer_id IS NOT NULL
ORDER BY listing_id, reviewer_id, created_at DESC;
CREATE INDEX idx_listing_reports_status ON marketplace.listing_reports(status, created_at DESC);
