ALTER TABLE marketplace_payment.payments DROP CONSTRAINT IF EXISTS payments_provider_check;
ALTER TABLE marketplace_payment.payments
    ADD CONSTRAINT payments_provider_check
    CHECK (provider IN ('VNPAY', 'BANK_TRANSFER', 'CASH_ON_DELIVERY', 'INTERNAL'));
