SET NAMES utf8mb4;

-- Align the Hai Dang 1 room master with the August 2026 collection sheet.
SET @property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
    LIMIT 1
);

UPDATE hdbhms.rooms
SET listed_price = CASE room_code
    WHEN '101' THEN 2200000
    WHEN '102' THEN 2200000
    WHEN '103' THEN 2100000
    WHEN '104' THEN 2000000
    WHEN '105' THEN 2000000
    WHEN '106' THEN 2000000
    WHEN '201' THEN 2200000
    WHEN '202' THEN 2200000
    WHEN '203' THEN 2100000
    WHEN '204' THEN 2000000
    WHEN '205' THEN 2000000
    WHEN '206' THEN 2000000
    WHEN '207' THEN 2000000
    WHEN '208' THEN 2100000
    WHEN '301' THEN 2200000
    WHEN '302' THEN 2200000
    WHEN '303' THEN 2100000
    WHEN '304' THEN 2000000
    WHEN '305' THEN 2000000
    WHEN '306' THEN 2000000
    WHEN '307' THEN 2000000
    WHEN '308' THEN 2100000
    WHEN '401' THEN 2200000
    WHEN '402' THEN 2200000
    WHEN '403' THEN 2100000
    WHEN '404' THEN 2000000
    WHEN '405' THEN 2000000
    WHEN '406' THEN 2000000
    WHEN '407' THEN 2000000
    WHEN '408' THEN 2100000
    WHEN '501' THEN 2200000
    WHEN '502' THEN 2200000
    WHEN '503' THEN 2100000
    WHEN '504' THEN 2000000
    WHEN '505' THEN 2000000
    WHEN '506' THEN 2000000
    WHEN '507' THEN 2100000
    ELSE listed_price
END
WHERE property_id = @property_id
  AND room_code IN (
      '101', '102', '103', '104', '105', '106',
      '201', '202', '203', '204', '205', '206', '207', '208',
      '301', '302', '303', '304', '305', '306', '307', '308',
      '401', '402', '403', '404', '405', '406', '407', '408',
      '501', '502', '503', '504', '505', '506', '507'
  );

-- The Excel sheet contains electricity only. Keep historical water rows untouched.
INSERT INTO hdbhms.meters (
    room_id,
    meter_type,
    meter_code,
    status,
    installed_at,
    created_at
)
SELECT
    r.room_id,
    'ELECTRICITY',
    CONCAT('HD1-E-', r.room_code),
    'ACTIVE',
    '2025-01-01',
    '2025-01-01 08:00:00'
FROM hdbhms.rooms r
WHERE r.property_id = @property_id
  AND r.room_code IN (
      '101', '102', '103', '104', '105', '106',
      '201', '202', '203', '204', '205', '206', '207', '208',
      '301', '302', '303', '304', '305', '306', '307', '308',
      '401', '402', '403', '404', '405', '406', '407', '408',
      '501', '502', '503', '504', '505', '506', '507'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.meters existing_meter
      WHERE existing_meter.room_id = r.room_id
        AND existing_meter.meter_type = 'ELECTRICITY'
        AND existing_meter.status = 'ACTIVE'
  );

-- The notice is issued in August, but its electricity readings belong to July.
INSERT INTO hdbhms.meter_reading_batches (
    property_id,
    reading_period,
    status,
    confirmed_at,
    created_at,
    total_rooms,
    completed_rooms,
    anomaly_count
)
VALUES (
    @property_id,
    '2026-07',
    'CONFIRMED',
    '2026-07-31 23:59:59',
    '2026-07-31 23:59:59',
    37,
    37,
    0
)
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    confirmed_at = VALUES(confirmed_at),
    total_rooms = VALUES(total_rooms),
    completed_rooms = VALUES(completed_rooms),
    anomaly_count = VALUES(anomaly_count);

SET @batch_id := (
    SELECT meter_reading_batch_id
    FROM hdbhms.meter_reading_batches
    WHERE property_id = @property_id
      AND reading_period = '2026-07'
    LIMIT 1
);

INSERT INTO hdbhms.meter_readings (
    batch_id,
    meter_id,
    room_id,
    reading_period,
    revision_no,
    previous_value,
    current_value,
    reading_date,
    status,
    purpose,
    source,
    review_status,
    review_count,
    created_at
)
SELECT
    @batch_id,
    m.meter_id,
    r.room_id,
    '2026-07',
    1,
    x.previous_value,
    x.current_value,
    '2026-07-31',
    'CONFIRMED',
    'MONTHLY',
    'EXCEL_IMPORT',
    'NONE',
    0,
    '2026-07-31 23:59:59'
FROM (
    SELECT '101' AS room_code, 2209 AS previous_value, 2428 AS current_value
    UNION ALL SELECT '102', 1885, 1896
    UNION ALL SELECT '103', 2566, 2626
    UNION ALL SELECT '104', 2982, 3301
    UNION ALL SELECT '105', 2495, 2648
    UNION ALL SELECT '106', 6590, 6705
    UNION ALL SELECT '201', 3772, 4069
    UNION ALL SELECT '202', 2650, 2790
    UNION ALL SELECT '203', 2142, 2163
    UNION ALL SELECT '204', 2699, 2869
    UNION ALL SELECT '205', 1948, 1989
    UNION ALL SELECT '206', 2506, 2631
    UNION ALL SELECT '207', 2951, 3304
    UNION ALL SELECT '208', 1910, 2101
    UNION ALL SELECT '301', 3061, 3545
    UNION ALL SELECT '302', 2854, 3093
    UNION ALL SELECT '303', 2516, 2772
    UNION ALL SELECT '304', 1945, 1955
    UNION ALL SELECT '305', 1309, 1447
    UNION ALL SELECT '306', 2053, 2305
    UNION ALL SELECT '307', 2051, 2338
    UNION ALL SELECT '308', 2486, 2614
    UNION ALL SELECT '401', 1960, 2261
    UNION ALL SELECT '402', 3220, 3553
    UNION ALL SELECT '403', 2323, 2348
    UNION ALL SELECT '404', 2471, 2490
    UNION ALL SELECT '405', 1463, 1661
    UNION ALL SELECT '406', 1362, 1446
    UNION ALL SELECT '407', 867, 869
    UNION ALL SELECT '408', 2493, 2614
    UNION ALL SELECT '501', 2691, 2970
    UNION ALL SELECT '502', 3736, 3945
    UNION ALL SELECT '503', 2377, 2535
    UNION ALL SELECT '504', 1722, 1874
    UNION ALL SELECT '505', 526, 568
    UNION ALL SELECT '506', 2098, 2314
    UNION ALL SELECT '507', 1966, 2187
) x
JOIN hdbhms.rooms r
    ON r.property_id = @property_id
   AND r.room_code = x.room_code
JOIN hdbhms.meters m
    ON m.room_id = r.room_id
   AND m.meter_type = 'ELECTRICITY'
   AND m.status = 'ACTIVE'
ON DUPLICATE KEY UPDATE
    batch_id = VALUES(batch_id),
    room_id = VALUES(room_id),
    previous_value = VALUES(previous_value),
    current_value = VALUES(current_value),
    reading_date = VALUES(reading_date),
    status = VALUES(status),
    void_reason = NULL,
    purpose = VALUES(purpose),
    source = VALUES(source),
    review_status = VALUES(review_status),
    review_count = VALUES(review_count);
