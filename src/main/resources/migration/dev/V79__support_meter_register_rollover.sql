ALTER TABLE hdbhms.meters
    ADD COLUMN counter_capacity DECIMAL(15, 3) NOT NULL DEFAULT 100000
    AFTER meter_code;

ALTER TABLE hdbhms.meter_readings
    ADD COLUMN rollover_count INT UNSIGNED NOT NULL DEFAULT 0
    AFTER current_value,
    ADD COLUMN counter_capacity_snapshot DECIMAL(15, 3) NOT NULL DEFAULT 0
    AFTER rollover_count;

ALTER TABLE hdbhms.meter_readings
    DROP COLUMN usage_amount;

ALTER TABLE hdbhms.meter_readings
    ADD COLUMN usage_amount DECIMAL(15, 3)
    GENERATED ALWAYS AS (
        CASE
            WHEN rollover_count > 0
                THEN current_value - previous_value + counter_capacity_snapshot * rollover_count
            WHEN current_value >= previous_value
                THEN current_value - previous_value
            ELSE NULL
        END
    ) STORED
    AFTER counter_capacity_snapshot;
