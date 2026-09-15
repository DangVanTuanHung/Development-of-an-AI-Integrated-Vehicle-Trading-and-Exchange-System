CREATE TABLE marketplace_ai.valuations (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    listing_id BIGINT REFERENCES marketplace.vehicle_listings(id) ON DELETE SET NULL,
    requested_by BIGINT REFERENCES ebike_auth.users(id) ON DELETE SET NULL,
    suggested_min_price NUMERIC(18,2),
    suggested_price NUMERIC(18,2),
    suggested_max_price NUMERIC(18,2),
    confidence NUMERIC(5,4) CHECK (confidence BETWEEN 0 AND 1),
    model_name VARCHAR(120) NOT NULL,
    model_version VARCHAR(80),
    input_snapshot JSONB NOT NULL,
    explanation TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE marketplace_ai.listing_reviews (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL REFERENCES marketplace.vehicle_listings(id) ON DELETE CASCADE,
    risk_level VARCHAR(20) NOT NULL CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    quality_score NUMERIC(5,2) CHECK (quality_score BETWEEN 0 AND 100),
    decision VARCHAR(20) NOT NULL CHECK (decision IN ('PASS', 'MANUAL_REVIEW', 'REJECT')),
    flags JSONB NOT NULL DEFAULT '[]'::jsonb,
    image_findings JSONB NOT NULL DEFAULT '[]'::jsonb,
    model_name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE marketplace_ai.fraud_risk_assessments (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT REFERENCES marketplace.transactions(id) ON DELETE CASCADE,
    listing_id BIGINT REFERENCES marketplace.vehicle_listings(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES ebike_auth.users(id) ON DELETE CASCADE,
    risk_score NUMERIC(5,4) NOT NULL CHECK (risk_score BETWEEN 0 AND 1),
    risk_level VARCHAR(20) NOT NULL CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    signals JSONB NOT NULL DEFAULT '[]'::jsonb,
    recommended_action VARCHAR(40) NOT NULL,
    model_name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (transaction_id IS NOT NULL OR listing_id IS NOT NULL OR user_id IS NOT NULL)
);

CREATE TABLE marketplace.audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_id BIGINT REFERENCES ebike_auth.users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(100),
    ip_address INET,
    user_agent TEXT,
    before_data JSONB,
    after_data JSONB,
    correlation_id UUID NOT NULL DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_marketplace_audit_entity ON marketplace.audit_logs(entity_type, entity_id, created_at DESC);
CREATE INDEX idx_marketplace_audit_actor ON marketplace.audit_logs(actor_id, created_at DESC);

INSERT INTO ebike_auth.permissions(code, name, module_name, action_name, description) VALUES
    ('listing:create', 'Tạo tin đăng', 'marketplace', 'create_listing', 'Tạo tin đăng phương tiện'),
    ('listing:manage_own', 'Quản lý tin đăng cá nhân', 'marketplace', 'manage_own_listing', 'Sửa, ẩn và gia hạn tin cá nhân'),
    ('listing:moderate', 'Kiểm duyệt tin đăng', 'marketplace', 'moderate_listing', 'Duyệt, từ chối hoặc đình chỉ tin'),
    ('offer:create', 'Gửi đề nghị', 'marketplace', 'create_offer', 'Gửi đề nghị mua hoặc trao đổi'),
    ('transaction:manage_own', 'Quản lý giao dịch cá nhân', 'marketplace', 'manage_own_transaction', 'Theo dõi và xác nhận giao dịch'),
    ('transaction:operate', 'Vận hành giao dịch', 'marketplace', 'operate_transaction', 'Xử lý giao dịch và ngoại lệ'),
    ('payment:deposit', 'Thanh toán đặt cọc', 'payment', 'pay_deposit', 'Tạo thanh toán tiền cọc'),
    ('payment:final', 'Thanh toán còn lại', 'payment', 'pay_final', 'Thanh toán phần còn lại'),
    ('payment:refund', 'Xử lý hoàn tiền', 'payment', 'refund', 'Duyệt và xử lý hoàn tiền'),
    ('payout:release', 'Giải ngân người bán', 'payment', 'release_payout', 'Giải ngân sau khi hoàn tất bàn giao'),
    ('dispute:create', 'Tạo tranh chấp', 'marketplace', 'create_dispute', 'Mở tranh chấp giao dịch'),
    ('dispute:resolve', 'Giải quyết tranh chấp', 'marketplace', 'resolve_dispute', 'Điều tra và giải quyết tranh chấp')
ON CONFLICT (code) DO NOTHING;

INSERT INTO ebike_auth.role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM ebike_auth.roles r
JOIN ebike_auth.permissions p ON p.code IN (
    'listing:create', 'listing:manage_own', 'offer:create', 'transaction:manage_own',
    'payment:deposit', 'payment:final', 'dispute:create'
)
WHERE r.name = 'CUSTOMER'
ON CONFLICT DO NOTHING;

INSERT INTO ebike_auth.role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM ebike_auth.roles r
JOIN ebike_auth.permissions p ON p.code IN (
    'listing:moderate', 'transaction:operate', 'payment:refund', 'payout:release', 'dispute:resolve'
)
WHERE r.name IN ('MANAGER', 'ADMIN')
ON CONFLICT DO NOTHING;
