SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

SET @property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
    LIMIT 1
);

-- Replace the old room-specific sample links for this property.
DELETE ri
FROM hdbhms.room_images ri
JOIN hdbhms.rooms r
    ON r.room_id = ri.room_id
JOIN hdbhms.file_metadata fm
    ON fm.file_metadata_id = ri.file_id
WHERE r.property_id = @property_id
  AND fm.category = 'ROOM_IMAGE'
  AND fm.storage_key IN (
      'room-samples/P102/p102_1.jpg',
      'room-samples/P102/p102_2.jpg',
      'room-samples/P102/p102_3.jpg',
      'room-samples/P102/p102_4.jpg',
      'room-samples/P102/p102_5.jpg',
      'room-samples/P208/p208.jpg',
      'room-samples/P208/p208_2.jpg',
      'room-samples/P208/p208_3.jpg',
      'room-samples/P404/p404_1.jpg',
      'room-samples/P404/p404_2.jpg',
      'room-samples/P404/p404_3.jpg',
      'room-samples/P404/p404_5.jpg',
      'room-samples/P408/p408_1.png',
      'room-samples/P408/p408_2.png',
      'room-samples/P501/p501_1.png',
      'room-samples/P501/p501_2.png',
      'room-samples/P501/p501_3.png',
      'room-samples/P501/p501_4.png',
      'room-samples/P502/p502_1.jpg',
      'room-samples/P502/p502_2.jpg',
      'room-samples/P502/p502_3.jpg',
      'room-samples/P502/p502_5.jpg',
      'room-samples/P502/p502_6.jpg',
      'room-samples/P502/p502_7.jpg'
  );

INSERT INTO hdbhms.file_metadata (
    owner_user_id,
    storage_key,
    original_name,
    mime_type,
    size_bytes,
    sha256_checksum,
    category,
    is_sensitive,
    created_at,
    deleted_at
)
SELECT
    NULL,
    data.storage_key,
    data.original_name,
    'image/png',
    data.size_bytes,
    NULL,
    'ROOM_IMAGE',
    FALSE,
    '2026-08-01 00:00:00',
    NULL
FROM (
    SELECT 'room-samples/normal/1.png' AS storage_key, '1.png' AS original_name, 1158252 AS size_bytes UNION ALL
    SELECT 'room-samples/normal/2.png', '2.png', 1129850 UNION ALL
    SELECT 'room-samples/normal/3.png', '3.png', 1091271 UNION ALL
    SELECT 'room-samples/premium/1.png', '1.png', 949865 UNION ALL
    SELECT 'room-samples/premium/2.png', '2.png', 776364 UNION ALL
    SELECT 'room-samples/premium/3.png', '3.png', 3163277 UNION ALL
    SELECT 'room-samples/premium/4.png', '4.png', 3608536
) data
WHERE @property_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.file_metadata fm
      WHERE fm.storage_key = data.storage_key
        AND fm.category = 'ROOM_IMAGE'
        AND fm.deleted_at IS NULL
  );

INSERT INTO hdbhms.room_images (
    room_id,
    file_id,
    sort_order,
    created_at
)
SELECT
    r.room_id,
    fm.file_metadata_id,
    image_data.sort_order,
    '2026-08-01 00:00:00'
FROM (
    SELECT 'normal' AS tier, 'room-samples/normal/1.png' AS storage_key, 0 AS sort_order UNION ALL
    SELECT 'normal', 'room-samples/normal/2.png', 1 UNION ALL
    SELECT 'normal', 'room-samples/normal/3.png', 2 UNION ALL
    SELECT 'premium', 'room-samples/premium/1.png', 0 UNION ALL
    SELECT 'premium', 'room-samples/premium/2.png', 1 UNION ALL
    SELECT 'premium', 'room-samples/premium/3.png', 2 UNION ALL
    SELECT 'premium', 'room-samples/premium/4.png', 3
) image_data
JOIN hdbhms.rooms r
    ON r.property_id = @property_id
   AND CASE
           WHEN r.listed_price = 2000000 THEN 'normal'
           WHEN r.listed_price > 2000000 THEN 'premium'
       END = image_data.tier
JOIN hdbhms.file_metadata fm
    ON fm.storage_key = image_data.storage_key
   AND fm.category = 'ROOM_IMAGE'
   AND fm.deleted_at IS NULL
WHERE @property_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.room_images existing_image
      WHERE existing_image.room_id = r.room_id
        AND existing_image.file_id = fm.file_metadata_id
  );
