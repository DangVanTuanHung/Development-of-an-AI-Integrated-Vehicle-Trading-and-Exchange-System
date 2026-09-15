INSERT INTO ebike_product.categories(name,slug,description,is_active) VALUES
('Xe máy điện','xe-may-dien','Xe máy điện đô thị và hiệu năng cao',true),
('Xe đạp điện','xe-dap-dien','Xe đạp trợ lực cho cá nhân và gia đình',true),
('Scooter điện','scooter-dien','Scooter điện gấp gọn cho đô thị',true),
('Pin & sạc','pin-sac','Pin, bộ sạc và giải pháp năng lượng',true),
('Phụ kiện','phu-kien','Phụ kiện an toàn và tiện ích',true)
ON CONFLICT (slug) DO UPDATE SET name=EXCLUDED.name,description=EXCLUDED.description,is_active=true;

WITH catalog(category_slug,name,slug,description,price,discount,stock,rating,reviews,featured,brand,vtype,btype,capacity,speed,range_km,power,brake,drive,warranty) AS (VALUES
('xe-may-dien','VinFast Evo200 Lite','vinfast-evo200-lite','Xe máy điện đô thị tầm xa, cốp rộng và vận hành êm ái.',22000000,19900000,28,4.80,126,true,'VinFast','E_MOTORBIKE','LFP',22.0,49.0,160.0,1500,'CBS','HUB_MOTOR',36),
('xe-may-dien','VinFast Feliz S','vinfast-feliz-s','Thiết kế thanh lịch, chống nước tốt, phù hợp đi làm hằng ngày.',29900000,27900000,18,4.70,92,true,'VinFast','E_MOTORBIKE','LFP',35.0,78.0,198.0,3000,'CBS','HUB_MOTOR',36),
('xe-may-dien','Dat Bike Weaver 200','dat-bike-weaver-200','Mô-tô điện cá tính với mô-men xoắn lớn và phạm vi di chuyển dài.',64900000,61900000,9,4.90,48,true,'Dat Bike','E_MOTORBIKE','LITHIUM_ION',68.0,90.0,200.0,6000,'DISC','CHAIN',36),
('xe-may-dien','Yadea Orla','yadea-orla','Mẫu xe thời trang nhỏ gọn dành cho học sinh và người đi làm.',19990000,18490000,32,4.60,74,false,'Yadea','E_MOTORBIKE','LEAD_ACID',22.0,50.0,80.0,1200,'DISC','HUB_MOTOR',24),
('xe-may-dien','Yadea Voltguard P','yadea-voltguard-p','Khung chắc chắn, hệ thống phanh an toàn và động cơ bền bỉ.',27990000,25990000,15,4.70,55,false,'Yadea','E_MOTORBIKE','LITHIUM_ION',30.0,65.0,120.0,2500,'CBS','HUB_MOTOR',36),
('xe-may-dien','Pega Aura S Plus','pega-aura-s-plus','Xe điện tiện dụng với sàn để chân rộng và nhiều màu trẻ trung.',18500000,16900000,24,4.50,61,false,'Pega','E_MOTORBIKE','LEAD_ACID',22.0,50.0,90.0,1500,'DISC','HUB_MOTOR',24),
('xe-dap-dien','Giant Explore E+ 2','giant-explore-e-plus-2','Xe đạp trợ lực cao cấp cho đường trường và hành trình dài.',78900000,74900000,7,4.90,31,true,'Giant','E_BIKE','LITHIUM_ION',16.0,25.0,140.0,500,'DISC','MID_DRIVE',24),
('xe-dap-dien','Himo Z20','himo-z20','Xe đạp điện gấp gọn, dễ mang theo trong ô tô hoặc căn hộ.',21900000,19900000,20,4.60,87,false,'Himo','E_BIKE','LITHIUM_ION',10.0,25.0,80.0,350,'DISC','HUB_MOTOR',18),
('xe-dap-dien','Aima Orla G5','aima-orla-g5','Xe đạp điện phong cách tối giản, yên êm và tiết kiệm năng lượng.',15900000,14500000,35,4.40,42,false,'Aima','E_BIKE','LITHIUM_ION',12.0,35.0,75.0,500,'DRUM','HUB_MOTOR',18),
('xe-dap-dien','DK Bike Samurai','dk-bike-samurai','Thiết kế khỏe khoắn, giỏ trước tiện dụng và tải trọng tốt.',16990000,15490000,17,4.50,36,false,'DK Bike','E_BIKE','LEAD_ACID',20.0,40.0,70.0,800,'DISC','HUB_MOTOR',18),
('scooter-dien','Xiaomi Electric Scooter 4 Ultra','xiaomi-scooter-4-ultra','Scooter gấp gọn cao cấp, giảm xóc kép và kết nối ứng dụng.',25990000,23990000,14,4.80,103,true,'Xiaomi','E_SCOOTER','LITHIUM_ION',12.0,25.0,70.0,940,'DISC','HUB_MOTOR',18),
('scooter-dien','Segway Ninebot Max G2','segway-ninebot-max-g2','Scooter tầm xa với kiểm soát lực kéo và đèn báo rẽ tích hợp.',27990000,25990000,11,4.80,88,true,'Segway','E_SCOOTER','LITHIUM_ION',15.3,35.0,70.0,1000,'DRUM','HUB_MOTOR',24),
('scooter-dien','Niu KQi3 Pro','niu-kqi3-pro','Thân xe rộng, phanh tái tạo năng lượng và màn hình trực quan.',18900000,17500000,22,4.60,67,false,'NIU','E_SCOOTER','LITHIUM_ION',10.4,32.0,50.0,700,'DISC','HUB_MOTOR',18),
('scooter-dien','Yadea KS5 Pro','yadea-ks5-pro','Scooter linh hoạt cho quãng đường ngắn trong đô thị.',14990000,13490000,27,4.40,39,false,'Yadea','E_SCOOTER','LITHIUM_ION',10.4,30.0,45.0,700,'DISC','HUB_MOTOR',18),
('pin-sac','Pin LFP MotionX 72V 30Ah','pin-lfp-motionx-72v-30ah','Bộ pin LFP tuổi thọ cao, có BMS thông minh và bảo vệ đa lớp.',24500000,22900000,16,4.90,22,false,'MotionX','BATTERY','LFP',30.0,0.0,0.0,0,'DISC','HUB_MOTOR',36),
('pin-sac','Pin Lithium MotionX 48V 20Ah','pin-lithium-motionx-48v-20ah','Pin thay thế gọn nhẹ dành cho xe đạp điện và scooter.',9900000,9200000,30,4.70,45,false,'MotionX','BATTERY','LITHIUM_ION',20.0,0.0,0.0,0,'DISC','HUB_MOTOR',24),
('pin-sac','Sạc nhanh MotionX 72V 10A','sac-nhanh-motionx-72v-10a','Bộ sạc nhanh có quạt làm mát và tự ngắt khi đầy.',3490000,3190000,42,4.60,57,false,'MotionX','ACCESSORY','LITHIUM_ION',0.0,0.0,0.0,720,'DISC','HUB_MOTOR',18),
('phu-kien','Mũ bảo hiểm thông minh MX Halo','mu-bao-hiem-thong-minh-mx-halo','Mũ đạt chuẩn với đèn cảnh báo, Bluetooth và SOS.',2890000,2490000,55,4.80,114,true,'MotionX','ACCESSORY','LITHIUM_ION',2.0,0.0,0.0,0,'DISC','HUB_MOTOR',12),
('phu-kien','Khóa chống trộm GPS MX Secure','khoa-gps-mx-secure','Theo dõi vị trí, cảnh báo rung và quản lý trên điện thoại.',1790000,1490000,64,4.70,98,false,'MotionX','ACCESSORY','LITHIUM_ION',1.0,0.0,0.0,0,'DISC','HUB_MOTOR',12),
('phu-kien','Thùng sau đa năng MX Box 35L','thung-sau-mx-box-35l','Thùng chống nước, khóa an toàn, chứa vừa một mũ bảo hiểm.',1390000,1190000,48,4.50,63,false,'MotionX','ACCESSORY','LITHIUM_ION',0.0,0.0,0.0,0,'DISC','HUB_MOTOR',12)
), inserted AS (
INSERT INTO ebike_product.products(category_id,name,slug,description,price,discount_price,stock_quantity,rating,review_count,is_featured,is_active)
SELECT c.id,x.name,x.slug,x.description,x.price,x.discount,x.stock,x.rating,x.reviews,x.featured,true FROM catalog x JOIN ebike_product.categories c ON c.slug=x.category_slug
ON CONFLICT(slug) DO UPDATE SET category_id=EXCLUDED.category_id,name=EXCLUDED.name,description=EXCLUDED.description,price=EXCLUDED.price,discount_price=EXCLUDED.discount_price,stock_quantity=GREATEST(ebike_product.products.stock_quantity,EXCLUDED.stock_quantity),is_active=true
RETURNING id,slug
)
INSERT INTO ebike_product.product_specifications(product_id,model_code,brand,vehicle_type,battery_type,battery_capacity_ah,max_speed_kmh,max_range_km,motor_power_watts,brake_type,drive_type,warranty_months,smart_features)
SELECT p.id,upper(replace(substring(x.slug,1,18),'-','')),x.brand,x.vtype,x.btype,x.capacity,x.speed,x.range_km,x.power,x.brake,x.drive,x.warranty,'Ứng dụng quản lý, chống trộm và chẩn đoán thông minh'
FROM catalog x JOIN ebike_product.products p ON p.slug=x.slug
ON CONFLICT(product_id) DO UPDATE SET brand=EXCLUDED.brand,vehicle_type=EXCLUDED.vehicle_type,battery_type=EXCLUDED.battery_type,battery_capacity_ah=EXCLUDED.battery_capacity_ah,max_speed_kmh=EXCLUDED.max_speed_kmh,max_range_km=EXCLUDED.max_range_km,motor_power_watts=EXCLUDED.motor_power_watts,warranty_months=EXCLUDED.warranty_months;

INSERT INTO ebike_product.product_variants(product_id,sku,variant_name,color_name,color_hex,battery_capacity_ah,additional_price,stock_quantity,is_default,is_active)
SELECT p.id,upper('MX-'||substring(md5(p.slug||v.color),1,10)),v.color||' tiêu chuẩn',v.color,v.hex,coalesce(s.battery_capacity_ah,0),v.extra,GREATEST(2,p.stock_quantity/3),v.ord=1,true
FROM ebike_product.products p JOIN ebike_product.product_specifications s ON s.product_id=p.id
CROSS JOIN (VALUES ('Đen nhám','#171717',0,1),('Trắng ngọc','#f5f5f4',300000,2),('Tím Aurora','#7c3aed',500000,3)) v(color,hex,extra,ord)
WHERE p.slug IN (SELECT slug FROM (VALUES ('vinfast-evo200-lite'),('vinfast-feliz-s'),('dat-bike-weaver-200'),('yadea-orla'),('yadea-voltguard-p'),('pega-aura-s-plus'),('giant-explore-e-plus-2'),('himo-z20'),('aima-orla-g5'),('dk-bike-samurai'),('xiaomi-scooter-4-ultra'),('segway-ninebot-max-g2'),('niu-kqi3-pro'),('yadea-ks5-pro')) q(slug))
ON CONFLICT(sku) DO NOTHING;

INSERT INTO ebike_product.product_images(product_id,image_url,alt_text,sort_order,is_primary,status)
SELECT p.id,'https://images.unsplash.com/photo-1558981806-ec527fa84c39?auto=format&fit=crop&w=1200&q=85&sig='||p.id,p.name,0,true,'ACTIVE'
FROM ebike_product.products p WHERE p.slug IN ('vinfast-evo200-lite','vinfast-feliz-s','dat-bike-weaver-200','yadea-orla','yadea-voltguard-p','pega-aura-s-plus','giant-explore-e-plus-2','himo-z20','aima-orla-g5','dk-bike-samurai','xiaomi-scooter-4-ultra','segway-ninebot-max-g2','niu-kqi3-pro','yadea-ks5-pro','pin-lfp-motionx-72v-30ah','pin-lithium-motionx-48v-20ah','sac-nhanh-motionx-72v-10a','mu-bao-hiem-thong-minh-mx-halo','khoa-gps-mx-secure','thung-sau-mx-box-35l')
AND NOT EXISTS(SELECT 1 FROM ebike_product.product_images i WHERE i.product_id=p.id);
