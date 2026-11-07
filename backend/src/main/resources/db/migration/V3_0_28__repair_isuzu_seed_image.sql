-- Bundle the image with the backend rather than depending on the broken CDN URL.
UPDATE marketplace.vehicle_images
SET public_url='/api/v1/media/vehicles/isuzu-qkr270.jpg',mime_type='image/jpeg'
WHERE public_url LIKE 'https://images.unsplash.com/photo-1586191582056-b7f2f749d7e9%';
