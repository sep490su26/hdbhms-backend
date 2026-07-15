UPDATE hdbhms.meter_readings
SET reading_period = CASE
    WHEN reading_period REGEXP '^[0-9]{4}-(0?[1-9]|1[0-2])$' THEN
        CONCAT(
            LPAD(SUBSTRING_INDEX(reading_period, '-', -1), 2, '0'),
            '-',
            SUBSTRING_INDEX(reading_period, '-', 1)
        )
    WHEN reading_period REGEXP '^(0?[1-9]|1[0-2])/[0-9]{4}$' THEN
        CONCAT(
            LPAD(SUBSTRING_INDEX(reading_period, '/', 1), 2, '0'),
            '-',
            SUBSTRING_INDEX(reading_period, '/', -1)
        )
    WHEN reading_period REGEXP '^(0?[1-9]|1[0-2])-[0-9]{4}$' THEN
        CONCAT(
            LPAD(SUBSTRING_INDEX(reading_period, '-', 1), 2, '0'),
            '-',
            SUBSTRING_INDEX(reading_period, '-', -1)
        )
    ELSE reading_period
END
WHERE reading_period REGEXP '^[0-9]{4}-(0?[1-9]|1[0-2])$'
   OR reading_period REGEXP '^(0?[1-9]|1[0-2])/[0-9]{4}$'
   OR reading_period REGEXP '^(0?[1-9]|1[0-2])-[0-9]{4}$';

UPDATE hdbhms.meter_reading_batches
SET reading_period = CASE
    WHEN reading_period REGEXP '^[0-9]{4}-(0?[1-9]|1[0-2])$' THEN
        CONCAT(
            LPAD(SUBSTRING_INDEX(reading_period, '-', -1), 2, '0'),
            '-',
            SUBSTRING_INDEX(reading_period, '-', 1)
        )
    WHEN reading_period REGEXP '^(0?[1-9]|1[0-2])/[0-9]{4}$' THEN
        CONCAT(
            LPAD(SUBSTRING_INDEX(reading_period, '/', 1), 2, '0'),
            '-',
            SUBSTRING_INDEX(reading_period, '/', -1)
        )
    WHEN reading_period REGEXP '^(0?[1-9]|1[0-2])-[0-9]{4}$' THEN
        CONCAT(
            LPAD(SUBSTRING_INDEX(reading_period, '-', 1), 2, '0'),
            '-',
            SUBSTRING_INDEX(reading_period, '-', -1)
        )
    ELSE reading_period
END
WHERE reading_period REGEXP '^[0-9]{4}-(0?[1-9]|1[0-2])$'
   OR reading_period REGEXP '^(0?[1-9]|1[0-2])/[0-9]{4}$'
   OR reading_period REGEXP '^(0?[1-9]|1[0-2])-[0-9]{4}$';
