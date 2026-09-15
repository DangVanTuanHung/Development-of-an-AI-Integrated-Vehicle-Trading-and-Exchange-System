INSERT INTO marketplace.vehicle_brands(name, slug, country_code) VALUES
    ('Toyota', 'toyota', 'JP'), ('Honda', 'honda', 'JP'), ('VinFast', 'vinfast', 'VN'),
    ('Mercedes-Benz', 'mercedes-benz', 'DE'), ('Yamaha', 'yamaha', 'JP'), ('Ford', 'ford', 'US')
ON CONFLICT (slug) DO NOTHING;

INSERT INTO marketplace.vehicle_listings(
    seller_id, category_id, brand_id, title, slug, description, listing_type, condition,
    status, price, negotiable, exchange_allowed, province, district, published_at
) VALUES
    (1, (SELECT id FROM marketplace.vehicle_categories WHERE slug='oto'), (SELECT id FROM marketplace.vehicle_brands WHERE slug='toyota'),
     'Toyota Camry 2.5Q 2022 chính chủ', 'toyota-camry-25q-2022-demo', 'Xe gia đình sử dụng kỹ, lịch sử bảo dưỡng đầy đủ, nội thất sạch và giấy tờ hợp lệ.', 'SALE', 'LIKE_NEW', 'PUBLISHED', 1120000000, TRUE, FALSE, 'TP. Hồ Chí Minh', 'Quận 7', CURRENT_TIMESTAMP),
    (1, (SELECT id FROM marketplace.vehicle_categories WHERE slug='oto'), (SELECT id FROM marketplace.vehicle_brands WHERE slug='vinfast'),
     'VinFast VF 8 Eco 2023 pin thuê', 'vinfast-vf8-eco-2023-demo', 'Xe điện vận hành ổn định, hỗ trợ sạc nhanh, phù hợp gia đình và di chuyển đường dài.', 'SALE_OR_EXCHANGE', 'USED', 'PUBLISHED', 785000000, TRUE, TRUE, 'Hà Nội', 'Cầu Giấy', CURRENT_TIMESTAMP),
    (2, (SELECT id FROM marketplace.vehicle_categories WHERE slug='xe-may'), (SELECT id FROM marketplace.vehicle_brands WHERE slug='honda'),
     'Honda SH 160i ABS 2024', 'honda-sh160i-abs-2024-demo', 'Xe ít sử dụng, odo thấp, hai chìa khóa và hồ sơ mua bán đầy đủ.', 'SALE', 'LIKE_NEW', 'PUBLISHED', 108000000, TRUE, FALSE, 'TP. Hồ Chí Minh', 'Bình Thạnh', CURRENT_TIMESTAMP),
    (2, (SELECT id FROM marketplace.vehicle_categories WHERE slug='oto'), (SELECT id FROM marketplace.vehicle_brands WHERE slug='mercedes-benz'),
     'Mercedes-Benz C200 Avantgarde 2021', 'mercedes-c200-2021-demo', 'Sedan cao cấp, bảo dưỡng hãng, không đâm đụng và có thể kiểm tra tại garage theo yêu cầu.', 'SALE_OR_EXCHANGE', 'USED', 'PUBLISHED', 1290000000, TRUE, TRUE, 'Đà Nẵng', 'Hải Châu', CURRENT_TIMESTAMP),
    (1, (SELECT id FROM marketplace.vehicle_categories WHERE slug='xe-may'), (SELECT id FROM marketplace.vehicle_brands WHERE slug='yamaha'),
     'Yamaha XMAX 300 nhập khẩu 2022', 'yamaha-xmax300-2022-demo', 'Dòng touring đô thị, máy nguyên bản, có phụ kiện touring và lịch sử chăm sóc định kỳ.', 'EXCHANGE', 'USED', 'PUBLISHED', 118000000, FALSE, TRUE, 'Hà Nội', 'Nam Từ Liêm', CURRENT_TIMESTAMP),
    (2, (SELECT id FROM marketplace.vehicle_categories WHERE slug='xe-thuong-mai'), (SELECT id FROM marketplace.vehicle_brands WHERE slug='ford'),
     'Ford Transit Premium 2023', 'ford-transit-premium-2023-demo', 'Xe phục vụ doanh nghiệp, nội thất rộng, hồ sơ pháp lý rõ ràng và xuất hóa đơn.', 'SALE', 'LIKE_NEW', 'PUBLISHED', 845000000, TRUE, FALSE, 'Bình Dương', 'Thủ Dầu Một', CURRENT_TIMESTAMP)
ON CONFLICT (slug) DO NOTHING;

INSERT INTO marketplace.vehicle_images(listing_id, object_key, public_url, storage_provider, bucket, mime_type, sort_order, primary_image, moderation_status)
SELECT l.id, 'demo/' || l.slug || '.jpg',
    CASE l.slug
      WHEN 'toyota-camry-25q-2022-demo' THEN 'https://images.unsplash.com/photo-1621007947382-bb3c3994e3fb?auto=format&fit=crop&w=1200&q=85'
      WHEN 'vinfast-vf8-eco-2023-demo' THEN 'https://images.unsplash.com/photo-1593941707882-a5bba14938c7?auto=format&fit=crop&w=1200&q=85'
      WHEN 'honda-sh160i-abs-2024-demo' THEN 'https://images.unsplash.com/photo-1558981806-ec527fa84c39?auto=format&fit=crop&w=1200&q=85'
      WHEN 'mercedes-c200-2021-demo' THEN 'https://images.unsplash.com/photo-1618843479313-40f8afb4b4d8?auto=format&fit=crop&w=1200&q=85'
      WHEN 'yamaha-xmax300-2022-demo' THEN 'https://images.unsplash.com/photo-1568772585407-9361f9bf3a87?auto=format&fit=crop&w=1200&q=85'
      ELSE 'https://images.unsplash.com/photo-1551830820-330a71b99659?auto=format&fit=crop&w=1200&q=85'
    END,
    'REMOTE', 'demo', 'image/jpeg', 0, TRUE, 'APPROVED'
FROM marketplace.vehicle_listings l
WHERE l.slug LIKE '%-demo'
ON CONFLICT (bucket, object_key) DO NOTHING;
