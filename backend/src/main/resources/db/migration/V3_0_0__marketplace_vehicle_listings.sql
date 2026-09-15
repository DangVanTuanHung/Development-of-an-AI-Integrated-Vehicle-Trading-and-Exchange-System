CREATE SCHEMA IF NOT EXISTS marketplace;
CREATE SCHEMA IF NOT EXISTS marketplace_payment;
CREATE SCHEMA IF NOT EXISTS marketplace_ai;

CREATE TABLE marketplace.vehicle_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(140) NOT NULL UNIQUE,
    parent_id BIGINT REFERENCES marketplace.vehicle_categories(id) ON DELETE SET NULL,
    icon_key VARCHAR(80),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE marketplace.vehicle_brands (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    slug VARCHAR(140) NOT NULL UNIQUE,
    logo_url TEXT,
    country_code CHAR(2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE marketplace.vehicle_models (
    id BIGSERIAL PRIMARY KEY,
    brand_id BIGINT NOT NULL REFERENCES marketplace.vehicle_brands(id),
    category_id BIGINT NOT NULL REFERENCES marketplace.vehicle_categories(id),
    name VARCHAR(160) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    start_year SMALLINT CHECK (start_year BETWEEN 1886 AND 2200),
    end_year SMALLINT CHECK (end_year IS NULL OR end_year BETWEEN 1886 AND 2200),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (brand_id, slug)
);

CREATE TABLE marketplace.vehicle_listings (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    seller_id BIGINT NOT NULL REFERENCES ebike_auth.users(id),
    model_id BIGINT REFERENCES marketplace.vehicle_models(id),
    category_id BIGINT NOT NULL REFERENCES marketplace.vehicle_categories(id),
    brand_id BIGINT REFERENCES marketplace.vehicle_brands(id),
    title VARCHAR(220) NOT NULL,
    slug VARCHAR(260) NOT NULL UNIQUE,
    description TEXT NOT NULL,
    listing_type VARCHAR(20) NOT NULL DEFAULT 'SALE'
        CHECK (listing_type IN ('SALE', 'EXCHANGE', 'SALE_OR_EXCHANGE')),
    condition VARCHAR(20) NOT NULL
        CHECK (condition IN ('NEW', 'LIKE_NEW', 'USED', 'RESTORED', 'DAMAGED')),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'PUBLISHED', 'RESERVED', 'SOLD', 'REJECTED', 'SUSPENDED', 'EXPIRED', 'ARCHIVED')),
    price NUMERIC(18,2) NOT NULL CHECK (price >= 0),
    negotiable BOOLEAN NOT NULL DEFAULT TRUE,
    exchange_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    province VARCHAR(120) NOT NULL,
    district VARCHAR(120),
    address_text VARCHAR(500),
    latitude NUMERIC(10,7),
    longitude NUMERIC(10,7),
    view_count BIGINT NOT NULL DEFAULT 0,
    favorite_count BIGINT NOT NULL DEFAULT 0,
    published_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_marketplace_listing_status_created ON marketplace.vehicle_listings(status, created_at DESC);
CREATE INDEX idx_marketplace_listing_seller ON marketplace.vehicle_listings(seller_id, status);
CREATE INDEX idx_marketplace_listing_category ON marketplace.vehicle_listings(category_id, status);
CREATE INDEX idx_marketplace_listing_brand ON marketplace.vehicle_listings(brand_id, status);
CREATE INDEX idx_marketplace_listing_price ON marketplace.vehicle_listings(price);
CREATE INDEX idx_marketplace_listing_location ON marketplace.vehicle_listings(province, district);

CREATE TABLE marketplace.vehicle_details (
    listing_id BIGINT PRIMARY KEY REFERENCES marketplace.vehicle_listings(id) ON DELETE CASCADE,
    manufacture_year SMALLINT CHECK (manufacture_year BETWEEN 1886 AND 2200),
    registration_year SMALLINT CHECK (registration_year BETWEEN 1886 AND 2200),
    mileage_km BIGINT CHECK (mileage_km >= 0),
    exterior_color VARCHAR(80),
    fuel_type VARCHAR(30) CHECK (fuel_type IN ('GASOLINE', 'DIESEL', 'ELECTRIC', 'HYBRID', 'PLUG_IN_HYBRID', 'OTHER')),
    transmission VARCHAR(30) CHECK (transmission IN ('MANUAL', 'AUTOMATIC', 'SEMI_AUTOMATIC', 'CVT', 'SINGLE_SPEED', 'OTHER')),
    engine_capacity_cc INTEGER CHECK (engine_capacity_cc >= 0),
    battery_capacity_kwh NUMERIC(8,2) CHECK (battery_capacity_kwh >= 0),
    range_km INTEGER CHECK (range_km >= 0),
    seats SMALLINT CHECK (seats > 0),
    owners_count SMALLINT CHECK (owners_count >= 0),
    license_plate_masked VARCHAR(30),
    vin_hash VARCHAR(64),
    origin VARCHAR(120),
    accident_history BOOLEAN,
    service_history_available BOOLEAN NOT NULL DEFAULT FALSE,
    inspection_valid_until DATE,
    registration_document_verified BOOLEAN NOT NULL DEFAULT FALSE,
    attributes JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE marketplace.vehicle_images (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL REFERENCES marketplace.vehicle_listings(id) ON DELETE CASCADE,
    object_key TEXT NOT NULL,
    public_url TEXT,
    storage_provider VARCHAR(30) NOT NULL DEFAULT 'MINIO',
    bucket VARCHAR(120) NOT NULL DEFAULT 'vehicle-marketplace',
    mime_type VARCHAR(100),
    file_size BIGINT CHECK (file_size IS NULL OR file_size >= 0),
    checksum_sha256 VARCHAR(64),
    sort_order INTEGER NOT NULL DEFAULT 0,
    primary_image BOOLEAN NOT NULL DEFAULT FALSE,
    moderation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (moderation_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (bucket, object_key)
);

CREATE INDEX idx_marketplace_vehicle_images_listing ON marketplace.vehicle_images(listing_id, sort_order);
CREATE UNIQUE INDEX uq_marketplace_listing_primary_image ON marketplace.vehicle_images(listing_id) WHERE primary_image;

CREATE TABLE marketplace.listing_favorites (
    user_id BIGINT NOT NULL REFERENCES ebike_auth.users(id) ON DELETE CASCADE,
    listing_id BIGINT NOT NULL REFERENCES marketplace.vehicle_listings(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, listing_id)
);

CREATE TABLE marketplace.listing_moderations (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL REFERENCES marketplace.vehicle_listings(id) ON DELETE CASCADE,
    reviewer_id BIGINT REFERENCES ebike_auth.users(id),
    decision VARCHAR(20) NOT NULL CHECK (decision IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED')),
    reason_code VARCHAR(80),
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO marketplace.vehicle_categories(name, slug) VALUES
    ('Ô tô', 'oto'),
    ('Xe máy', 'xe-may'),
    ('Xe điện', 'xe-dien'),
    ('Xe thương mại', 'xe-thuong-mai'),
    ('Phương tiện khác', 'phuong-tien-khac')
ON CONFLICT (slug) DO NOTHING;

