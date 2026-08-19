SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Restore the July electricity readings for empty rooms from Invoice_7_2026.xlsx.
SET @property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
      AND deleted_at IS NULL
    LIMIT 1
);

UPDATE hdbhms.meter_readings reading
JOIN hdbhms.rooms room
  ON room.room_id = reading.room_id
JOIN hdbhms.meters meter
  ON meter.meter_id = reading.meter_id
 AND meter.room_id = room.room_id
 AND meter.meter_type = 'ELECTRICITY'
SET reading.previous_value = CASE room.room_code
        WHEN '304' THEN 1945
        WHEN '403' THEN 2323
        WHEN '404' THEN 2471
        WHEN '407' THEN 867
    END,
    reading.current_value = CASE room.room_code
        WHEN '304' THEN 1955
        WHEN '403' THEN 2348
        WHEN '404' THEN 2490
        WHEN '407' THEN 869
    END
WHERE room.property_id = @property_id
  AND room.room_code IN ('304', '403', '404', '407')
  AND reading.reading_period = '2026-07'
  AND reading.status = 'CONFIRMED';
