ALTER TABLE marketplace.offers
 ADD COLUMN proposed_by BIGINT REFERENCES ebike_auth.users(id),
 ADD COLUMN exchange_listing_id BIGINT REFERENCES marketplace.vehicle_listings(id),
 ADD COLUMN exchange_value NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK(exchange_value>=0);
UPDATE marketplace.offers SET proposed_by=buyer_id;
ALTER TABLE marketplace.offers ALTER COLUMN proposed_by SET NOT NULL;
ALTER TABLE marketplace.offers ADD CONSTRAINT chk_offer_exchange CHECK
 ((exchange_listing_id IS NULL AND exchange_value=0) OR (exchange_listing_id IS NOT NULL AND exchange_value>0 AND exchange_listing_id<>listing_id));
ALTER TABLE marketplace.offers DROP CONSTRAINT offers_check;
ALTER TABLE marketplace.offers ADD CONSTRAINT offers_deposit_amount_check CHECK(deposit_amount>=0 AND deposit_amount<=abs(amount-exchange_value));

ALTER TABLE marketplace.transactions
 ADD COLUMN exchange_listing_id BIGINT REFERENCES marketplace.vehicle_listings(id),
 ADD COLUMN exchange_value NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK(exchange_value>=0),
 ADD COLUMN payable_amount NUMERIC(18,2) GENERATED ALWAYS AS(abs(agreed_price-exchange_value)) STORED,
 ADD COLUMN cancellation_requested_by BIGINT REFERENCES ebike_auth.users(id),
 ADD COLUMN cancellation_previous_status VARCHAR(40),
 ADD COLUMN dispute_previous_status VARCHAR(40),
 ADD COLUMN refund_policy VARCHAR(80) NOT NULL DEFAULT 'FULL_REFUND_ON_AGREED_CANCEL_V1',
 ADD COLUMN handover_at TIMESTAMPTZ,
 ADD COLUMN handover_location VARCHAR(500),
 ADD COLUMN handover_note VARCHAR(2000),
 ADD COLUMN handover_revision INT NOT NULL DEFAULT 0,
 ADD COLUMN buyer_plan_agreed_at TIMESTAMPTZ,
 ADD COLUMN seller_plan_agreed_at TIMESTAMPTZ,
 ADD COLUMN buyer_handover_confirmed_at TIMESTAMPTZ,
 ADD COLUMN seller_handover_confirmed_at TIMESTAMPTZ;
ALTER TABLE marketplace.transactions DROP COLUMN remaining_amount;
ALTER TABLE marketplace.transactions ADD COLUMN remaining_amount NUMERIC(18,2)
 GENERATED ALWAYS AS(abs(agreed_price-exchange_value)-deposit_amount) STORED;
ALTER TABLE marketplace.transactions ADD CONSTRAINT chk_transaction_cash_deposit CHECK(deposit_amount<=abs(agreed_price-exchange_value));
ALTER TABLE marketplace.transactions DROP CONSTRAINT transactions_check;
ALTER TABLE marketplace.transactions ADD CONSTRAINT transactions_deposit_amount_check CHECK(deposit_amount>=0);
ALTER TABLE marketplace.transactions DROP CONSTRAINT transactions_status_check;
ALTER TABLE marketplace.transactions ADD CONSTRAINT transactions_status_check CHECK(status IN
 ('CREATED','DEPOSIT_PENDING','DEPOSIT_PAID','INSPECTION_PENDING','INSPECTION_PASSED','INSPECTION_FAILED','FINAL_PAYMENT_PENDING','FULLY_PAID','HANDOVER_CONFIRMED','COMPLETED','CANCELLATION_REQUESTED','CANCELLED','DISPUTED','REFUND_PENDING','REFUNDED'));
CREATE UNIQUE INDEX uq_active_exchange_vehicle ON marketplace.transactions(exchange_listing_id)
 WHERE exchange_listing_id IS NOT NULL AND status NOT IN ('COMPLETED','CANCELLED','REFUNDED');

ALTER TABLE marketplace_payment.payments ADD COLUMN submitted_at TIMESTAMPTZ;
ALTER TABLE marketplace_payment.payments ADD COLUMN transfer_reference VARCHAR(160);
ALTER TABLE marketplace_payment.refunds ADD COLUMN sent_at TIMESTAMPTZ;
ALTER TABLE marketplace_payment.refunds ADD COLUMN received_at TIMESTAMPTZ;
ALTER TABLE marketplace_payment.refunds ADD COLUMN transfer_reference VARCHAR(160);
CREATE UNIQUE INDEX uq_payment_cancellation_refund ON marketplace_payment.refunds(payment_id)
 WHERE status IN ('REQUESTED','APPROVED','PROCESSING','COMPLETED');
CREATE UNIQUE INDEX uq_open_transaction_dispute ON marketplace.disputes(transaction_id)
 WHERE status IN ('OPEN','UNDER_REVIEW','WAITING_EVIDENCE');

CREATE TABLE marketplace.transaction_evidence (
 id BIGSERIAL PRIMARY KEY,
 public_id UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
 transaction_id BIGINT NOT NULL REFERENCES marketplace.transactions(id),
 uploaded_by BIGINT NOT NULL REFERENCES ebike_auth.users(id),
 file_key VARCHAR(100) NOT NULL UNIQUE,
 file_name VARCHAR(200) NOT NULL,
 mime_type VARCHAR(80) NOT NULL,
 file_size BIGINT NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE marketplace.vehicle_listings DROP CONSTRAINT vehicle_listings_status_check;
ALTER TABLE marketplace.vehicle_listings ADD CONSTRAINT vehicle_listings_status_check CHECK
 (status IN ('DRAFT','PENDING_REVIEW','PUBLISHED','RESERVED','SOLD','EXCHANGED','REJECTED','SUSPENDED','EXPIRED','ARCHIVED','HIDDEN'));

-- Preserve staff-opened disputes created by the previous operations screen.
UPDATE marketplace.transactions t SET dispute_previous_status=coalesce(
 (SELECT e.from_status FROM marketplace.transaction_events e WHERE e.transaction_id=t.id AND e.event_type='STAFF_OPENED_DISPUTE' ORDER BY e.id DESC LIMIT 1),
 CASE WHEN (SELECT coalesce(sum(p.amount),0) FROM marketplace_payment.payments p WHERE p.transaction_id=t.id AND p.status='PAID')>=t.payable_amount THEN 'FULLY_PAID'
      WHEN EXISTS(SELECT 1 FROM marketplace_payment.payments p WHERE p.transaction_id=t.id AND p.status='PAID') THEN 'DEPOSIT_PAID' ELSE 'DEPOSIT_PENDING' END)
WHERE t.status='DISPUTED' AND t.dispute_previous_status IS NULL;
INSERT INTO marketplace.disputes(transaction_id,opened_by,reason_code,description)
SELECT t.id,coalesce((SELECT e.actor_id FROM marketplace.transaction_events e WHERE e.transaction_id=t.id AND e.event_type='STAFF_OPENED_DISPUTE' ORDER BY e.id DESC LIMIT 1),t.buyer_id),
 'LEGACY_STAFF_CASE','Hồ sơ tranh chấp được chuyển tiếp từ phiên bản trước.'
FROM marketplace.transactions t WHERE t.status='DISPUTED' AND NOT EXISTS(SELECT 1 FROM marketplace.disputes d WHERE d.transaction_id=t.id AND d.status IN ('OPEN','UNDER_REVIEW','WAITING_EVIDENCE'));
