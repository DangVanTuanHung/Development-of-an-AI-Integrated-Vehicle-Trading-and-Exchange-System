INSERT INTO marketplace.vehicle_brands(name, slug, country_code) VALUES
 ('Kia','kia','KR'), ('Hyundai','hyundai','KR'), ('Tesla','tesla','US'), ('Dat Bike','dat-bike','VN'),
 ('Suzuki','suzuki','JP'), ('Isuzu','isuzu','JP'), ('Giant','giant','TW')
ON CONFLICT (slug) DO NOTHING;

INSERT INTO marketplace.vehicle_listings(seller_id,category_id,brand_id,title,slug,description,listing_type,condition,status,price,negotiable,exchange_allowed,province,district,published_at) VALUES
(1,(SELECT id FROM marketplace.vehicle_categories WHERE slug='oto'),(SELECT id FROM marketplace.vehicle_brands WHERE slug='kia'),'Kia Carnival Signature 2023','kia-carnival-signature-2023-demo2','MPV 7 chỗ rộng rãi, nội thất da, cửa lùa điện và lịch sử bảo dưỡng đầy đủ.','SALE','LIKE_NEW','PUBLISHED',1280000000,true,false,'TP. Hồ Chí Minh','Thủ Đức',CURRENT_TIMESTAMP),
(2,(SELECT id FROM marketplace.vehicle_categories WHERE slug='oto'),(SELECT id FROM marketplace.vehicle_brands WHERE slug='hyundai'),'Hyundai Santa Fe Dầu 2022','hyundai-santafe-diesel-2022-demo2','SUV gia đình máy dầu tiết kiệm, dẫn động bốn bánh, camera toàn cảnh.','SALE_OR_EXCHANGE','USED','PUBLISHED',1050000000,true,true,'Hà Nội','Long Biên',CURRENT_TIMESTAMP),
(1,(SELECT id FROM marketplace.vehicle_categories WHERE slug='xe-dien'),(SELECT id FROM marketplace.vehicle_brands WHERE slug='tesla'),'Tesla Model 3 Long Range 2024','tesla-model-3-long-range-demo2','Sedan điện tầm hoạt động cao, hỗ trợ sạc nhanh, khoang lái tối giản hiện đại.','SALE','LIKE_NEW','PUBLISHED',1690000000,true,false,'Đà Nẵng','Sơn Trà',CURRENT_TIMESTAMP),
(2,(SELECT id FROM marketplace.vehicle_categories WHERE slug='xe-dien'),(SELECT id FROM marketplace.vehicle_brands WHERE slug='dat-bike'),'Dat Bike Weaver++ 2024','dat-bike-weaver-plus-demo2','Xe máy điện Việt Nam, pin khỏe, phù hợp đi phố và có bộ sạc chính hãng.','SALE_OR_EXCHANGE','LIKE_NEW','PUBLISHED',52000000,true,true,'TP. Hồ Chí Minh','Quận 3',CURRENT_TIMESTAMP),
(1,(SELECT id FROM marketplace.vehicle_categories WHERE slug='xe-may'),(SELECT id FROM marketplace.vehicle_brands WHERE slug='suzuki'),'Suzuki V-Strom 250SX 2023','suzuki-vstrom-250sx-demo2','Adventure cỡ nhỏ, tư thế lái thoải mái, có thùng touring và chống đổ.','EXCHANGE','USED','PUBLISHED',112000000,false,true,'Lâm Đồng','Đà Lạt',CURRENT_TIMESTAMP),
(2,(SELECT id FROM marketplace.vehicle_categories WHERE slug='xe-thuong-mai'),(SELECT id FROM marketplace.vehicle_brands WHERE slug='isuzu'),'Isuzu QKR 270 Thùng Kín 2022','isuzu-qkr-270-2022-demo2','Xe tải nhẹ thùng kín sạch đẹp, đăng kiểm còn dài, sẵn sàng vận chuyển hàng.','SALE','USED','PUBLISHED',465000000,true,false,'Bình Dương','Dĩ An',CURRENT_TIMESTAMP),
(1,(SELECT id FROM marketplace.vehicle_categories WHERE slug='phuong-tien-khac'),(SELECT id FROM marketplace.vehicle_brands WHERE slug='giant'),'Giant Explore E+ 2 GTS','giant-explore-eplus-demo2','Xe đạp trợ lực điện touring, pin tháo rời, phanh đĩa thủy lực.','SALE','LIKE_NEW','PUBLISHED',78000000,true,false,'Hà Nội','Tây Hồ',CURRENT_TIMESTAMP),
(2,(SELECT id FROM marketplace.vehicle_categories WHERE slug='oto'),(SELECT id FROM marketplace.vehicle_brands WHERE slug='toyota'),'Toyota Corolla Cross Hybrid 2024','toyota-corolla-cross-hybrid-demo2','Crossover hybrid tiết kiệm nhiên liệu, xe chính hãng còn bảo hành.','SALE','NEW','PUBLISHED',955000000,false,false,'Cần Thơ','Ninh Kiều',CURRENT_TIMESTAMP)
ON CONFLICT (slug) DO NOTHING;

INSERT INTO marketplace.vehicle_details(listing_id,manufacture_year,mileage_km,exterior_color,fuel_type,transmission,engine_capacity_cc,range_km,seats,owners_count,origin)
SELECT l.id, d.year, d.km, d.color, d.fuel, d.transmission, d.cc, d.range_km, d.seats, d.owners, d.origin
FROM marketplace.vehicle_listings l JOIN (VALUES
 ('kia-carnival-signature-2023-demo2',2023,18000,'Trắng','GASOLINE','AUTOMATIC',3470,NULL,7,1,'Lắp ráp trong nước'),
 ('hyundai-santafe-diesel-2022-demo2',2022,42000,'Đen','DIESEL','AUTOMATIC',2200,NULL,7,1,'Lắp ráp trong nước'),
 ('tesla-model-3-long-range-demo2',2024,9000,'Xám','ELECTRIC','SINGLE_SPEED',NULL,629,5,1,'Nhập khẩu'),
 ('dat-bike-weaver-plus-demo2',2024,3200,'Đen','ELECTRIC','SINGLE_SPEED',NULL,200,2,1,'Sản xuất trong nước'),
 ('suzuki-vstrom-250sx-demo2',2023,12500,'Vàng','GASOLINE','MANUAL',249,NULL,2,1,'Nhập khẩu'),
 ('isuzu-qkr-270-2022-demo2',2022,68000,'Trắng','DIESEL','MANUAL',2771,NULL,3,1,'Lắp ráp trong nước'),
 ('giant-explore-eplus-demo2',2023,1200,'Xanh','ELECTRIC','SINGLE_SPEED',NULL,120,1,1,'Nhập khẩu'),
 ('toyota-corolla-cross-hybrid-demo2',2024,80,'Đỏ','HYBRID','CVT',1798,NULL,5,1,'Lắp ráp trong nước')
) AS d(slug,year,km,color,fuel,transmission,cc,range_km,seats,owners,origin) ON l.slug=d.slug
ON CONFLICT (listing_id) DO NOTHING;

INSERT INTO marketplace.vehicle_images(listing_id,object_key,public_url,storage_provider,bucket,mime_type,sort_order,primary_image,moderation_status)
SELECT l.id,'demo2/'||l.slug||'.jpg',CASE l.slug
 WHEN 'kia-carnival-signature-2023-demo2' THEN 'https://images.unsplash.com/photo-1549317661-bd32c8ce0db2?auto=format&fit=crop&w=1200&q=85'
 WHEN 'hyundai-santafe-diesel-2022-demo2' THEN 'https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?auto=format&fit=crop&w=1200&q=85'
 WHEN 'tesla-model-3-long-range-demo2' THEN 'https://images.unsplash.com/photo-1560958089-b8a1929cea89?auto=format&fit=crop&w=1200&q=85'
 WHEN 'dat-bike-weaver-plus-demo2' THEN 'https://images.unsplash.com/photo-1571068316344-75bc76f77890?auto=format&fit=crop&w=1200&q=85'
 WHEN 'suzuki-vstrom-250sx-demo2' THEN 'https://images.unsplash.com/photo-1558981806-ec527fa84c39?auto=format&fit=crop&w=1200&q=85'
 WHEN 'isuzu-qkr-270-2022-demo2' THEN 'https://images.unsplash.com/photo-1586191582056-b7f2f749d7e9?auto=format&fit=crop&w=1200&q=85'
 WHEN 'giant-explore-eplus-demo2' THEN 'https://images.unsplash.com/photo-1571333250630-f0230c320b6d?auto=format&fit=crop&w=1200&q=85'
 ELSE 'https://images.unsplash.com/photo-1590362891991-f776e747a588?auto=format&fit=crop&w=1200&q=85' END,
 'REMOTE','demo2','image/jpeg',0,true,'APPROVED'
FROM marketplace.vehicle_listings l WHERE l.slug LIKE '%-demo2'
ON CONFLICT (bucket,object_key) DO NOTHING;
