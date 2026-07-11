SET @property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
    LIMIT 1
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
    data.mime_type,
    0,
    NULL,
    'ROOM_IMAGE',
    FALSE,
    NOW(6),
    NULL
FROM (
    SELECT 'room-samples/P102/p102_1.jpg' AS storage_key, 'p102_1.jpg' AS original_name, 'image/jpeg' AS mime_type UNION ALL
    SELECT 'room-samples/P102/p102_2.jpg', 'p102_2.jpg', 'image/jpeg' UNION ALL
    SELECT 'room-samples/P102/p102_3.jpg', 'p102_3.jpg', 'image/jpeg' UNION ALL
    SELECT 'room-samples/P102/p102_4.jpg', 'p102_4.jpg', 'image/jpeg' UNION ALL
    SELECT 'room-samples/P102/p102_5.jpg', 'p102_5.jpg', 'image/jpeg' UNION ALL
    SELECT 'room-samples/P208/p208.jpg', 'p208.jpg', 'image/jpeg' UNION ALL
    SELECT 'room-samples/P208/p208_2.jpg', 'p208_2.jpg', 'image/jpeg' UNION ALL
    SELECT 'room-samples/P208/p208_3.jpg', 'p208_3.jpg', 'image/jpeg' UNION ALL
    SELECT 'room-samples/P404/p404_1.jpg', 'p404_1.jpg', 'image/jpeg' UNION ALL
    SELECT 'room-samples/P404/p404_2.jpg', 'p404_2.jpg', 'image/jpeg' UNION ALL
    SELECT 'room-samples/P404/p404_3.jpg', 'p404_3.jpg', 'image/jpeg' UNION ALL
    SELECT 'room-samples/P404/p404_5.jpg', 'p404_5.jpg', 'image/jpeg' UNION ALL
    SELECT 'room-samples/P408/p408_1.png', 'p408_1.png', 'image/png' UNION ALL
    SELECT 'room-samples/P408/p408_2.png', 'p408_2.png', 'image/png' UNION ALL
    SELECT 'room-samples/P501/p501_1.png', 'p501_1.png', 'image/png' UNION ALL
    SELECT 'room-samples/P501/p501_2.png', 'p501_2.png', 'image/png' UNION ALL
    SELECT 'room-samples/P501/p501_3.png', 'p501_3.png', 'image/png' UNION ALL
    SELECT 'room-samples/P501/p501_4.png', 'p501_4.png', 'image/png' UNION ALL
    SELECT 'room-samples/P502/p502_1.jpg', 'p502_1.jpg', 'image/jpeg' UNION ALL
    SELECT 'room-samples/P502/p502_2.jpg', 'p502_2.jpg', 'image/jpeg' UNION ALL
    SELECT 'room-samples/P502/p502_3.jpg', 'p502_3.jpg', 'image/jpeg' UNION ALL
    SELECT 'room-samples/P502/p502_5.jpg', 'p502_5.jpg', 'image/jpeg' UNION ALL
    SELECT 'room-samples/P502/p502_6.jpg', 'p502_6.jpg', 'image/jpeg' UNION ALL
    SELECT 'room-samples/P502/p502_7.jpg', 'p502_7.jpg', 'image/jpeg'
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
    room_data.sort_order,
    NOW(6)
FROM (
    SELECT '102' AS room_code, 'room-samples/P102/p102_1.jpg' AS storage_key, 0 AS sort_order UNION ALL
    SELECT '102', 'room-samples/P102/p102_2.jpg', 1 UNION ALL
    SELECT '102', 'room-samples/P102/p102_3.jpg', 2 UNION ALL
    SELECT '102', 'room-samples/P102/p102_4.jpg', 3 UNION ALL
    SELECT '102', 'room-samples/P102/p102_5.jpg', 4 UNION ALL
    SELECT '208', 'room-samples/P208/p208.jpg', 0 UNION ALL
    SELECT '208', 'room-samples/P208/p208_2.jpg', 1 UNION ALL
    SELECT '208', 'room-samples/P208/p208_3.jpg', 2 UNION ALL
    SELECT '404', 'room-samples/P404/p404_1.jpg', 0 UNION ALL
    SELECT '404', 'room-samples/P404/p404_2.jpg', 1 UNION ALL
    SELECT '404', 'room-samples/P404/p404_3.jpg', 2 UNION ALL
    SELECT '404', 'room-samples/P404/p404_5.jpg', 3 UNION ALL
    SELECT '408', 'room-samples/P408/p408_1.png', 0 UNION ALL
    SELECT '408', 'room-samples/P408/p408_2.png', 1 UNION ALL
    SELECT '501', 'room-samples/P501/p501_1.png', 0 UNION ALL
    SELECT '501', 'room-samples/P501/p501_2.png', 1 UNION ALL
    SELECT '501', 'room-samples/P501/p501_3.png', 2 UNION ALL
    SELECT '501', 'room-samples/P501/p501_4.png', 3 UNION ALL
    SELECT '502', 'room-samples/P502/p502_1.jpg', 0 UNION ALL
    SELECT '502', 'room-samples/P502/p502_2.jpg', 1 UNION ALL
    SELECT '502', 'room-samples/P502/p502_3.jpg', 2 UNION ALL
    SELECT '502', 'room-samples/P502/p502_5.jpg', 3 UNION ALL
    SELECT '502', 'room-samples/P502/p502_6.jpg', 4 UNION ALL
    SELECT '502', 'room-samples/P502/p502_7.jpg', 5
) room_data
JOIN hdbhms.rooms r
    ON r.property_id = @property_id
   AND r.room_code = room_data.room_code
JOIN hdbhms.file_metadata fm
    ON fm.storage_key = room_data.storage_key
   AND fm.category = 'ROOM_IMAGE'
   AND fm.deleted_at IS NULL
WHERE @property_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.room_images ri
      WHERE ri.room_id = r.room_id
        AND ri.file_id = fm.file_metadata_id
  );
