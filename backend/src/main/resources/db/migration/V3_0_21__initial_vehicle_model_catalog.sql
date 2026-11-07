-- Starter model names for the existing brands/categories; no inferred specifications.
INSERT INTO marketplace.vehicle_models(brand_id,category_id,name,slug)
SELECT b.id,c.id,v.name,v.slug
FROM (VALUES
 ('toyota','oto','Camry','camry'),
 ('toyota','oto','Corolla Cross','corolla-cross'),
 ('honda','xe-may','SH','sh'),
 ('yamaha','xe-may','XMAX','xmax'),
 ('ford','xe-thuong-mai','Transit','transit'),
 ('mercedes-benz','oto','C-Class','c-class'),
 ('vinfast','oto','VF 8','vf-8'),
 ('kia','oto','Carnival','carnival'),
 ('hyundai','oto','Santa Fe','santa-fe'),
 ('tesla','xe-dien','Model 3','model-3'),
 ('dat-bike','xe-dien','Weaver++','weaver-plus-plus'),
 ('suzuki','xe-may','V-Strom 250SX','v-strom-250sx'),
 ('isuzu','xe-thuong-mai','QKR','qkr'),
 ('giant','phuong-tien-khac','Explore E+','explore-e-plus')
) AS v(brand_slug,category_slug,name,slug)
JOIN marketplace.vehicle_brands b ON b.slug=v.brand_slug
JOIN marketplace.vehicle_categories c ON c.slug=v.category_slug
ON CONFLICT (brand_id,slug) DO NOTHING;
