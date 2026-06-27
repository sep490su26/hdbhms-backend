-- 1. meter_reading_batches
ALTER TABLE meter_reading_batches
    ADD COLUMN total_rooms INT NOT NULL DEFAULT 0,
    ADD COLUMN completed_rooms INT NOT NULL DEFAULT 0,
    ADD COLUMN anomaly_count INT NOT NULL DEFAULT 0,
    DROP COLUMN source;

-- 2. meter_readings
ALTER TABLE meter_readings
    DROP COLUMN source,
    ADD COLUMN purpose ENUM('MONTHLY', 'MOVE_OUT', 'TRANSFER', 'HANDOVER', 'CONTRACT_START') NOT NULL DEFAULT 'MONTHLY',
    ADD COLUMN source ENUM('MANUAL', 'EXCEL_IMPORT', 'API') NOT NULL DEFAULT 'MANUAL';

ALTER TABLE meter_readings
    DROP CONSTRAINT chk_meter_index;

-- 3. meter_reading_anomalies
ALTER TABLE meter_reading_anomalies
    ADD COLUMN batch_id BIGINT UNSIGNED NULL,
    ADD CONSTRAINT fk_mra_batch FOREIGN KEY (batch_id) REFERENCES meter_reading_batches (id);

-- 4. meter_reading_import_rows
ALTER TABLE meter_reading_import_rows
    ADD COLUMN row_no INT NOT NULL DEFAULT 0;
