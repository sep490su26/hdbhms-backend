SET @schema_name = 'hdbhms';

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'invoices'
      AND COLUMN_NAME = 'invoice_reason'
);
SET @sql = IF(
    @col_exists = 0,
    'ALTER TABLE hdbhms.invoices ADD COLUMN invoice_reason ENUM (''MONTHLY'', ''TRANSFER'', ''ROOM_CLOSE'', ''MANUAL'') NULL AFTER billing_period',
    'SELECT ''invoice_reason already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @active_key_has_reason = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'invoices'
      AND COLUMN_NAME = 'active_invoice_key'
      AND LOWER(GENERATION_EXPRESSION) LIKE '%invoice_reason%'
);
SET @active_key_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'invoices'
      AND INDEX_NAME = 'uq_invoice_active_key'
);
SET @sql = IF(
    @active_key_has_reason = 0 AND @active_key_index_exists > 0,
    'ALTER TABLE hdbhms.invoices DROP INDEX uq_invoice_active_key',
    'SELECT ''active invoice key index drop skipped'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @active_key_has_reason = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'invoices'
      AND COLUMN_NAME = 'active_invoice_key'
      AND LOWER(GENERATION_EXPRESSION) LIKE '%invoice_reason%'
);
SET @sql = IF(
    @active_key_has_reason = 0,
    'ALTER TABLE hdbhms.invoices MODIFY COLUMN active_invoice_key VARCHAR(255) GENERATED ALWAYS AS (IF((`status` <> ''VOIDED'') AND (`lease_contract_id` IS NOT NULL) AND (`billing_period` IS NOT NULL), CONCAT(`lease_contract_id`, '':'', `billing_period`, '':'', `invoice_type`, '':'', COALESCE(`invoice_reason`, ''MONTHLY'')), NULL)) VIRTUAL',
    'SELECT ''active invoice key already includes invoice_reason'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @active_key_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'invoices'
      AND INDEX_NAME = 'uq_invoice_active_key'
);
SET @sql = IF(
    @active_key_index_exists = 0,
    'ALTER TABLE hdbhms.invoices ADD UNIQUE KEY uq_invoice_active_key (active_invoice_key)',
    'SELECT ''active invoice key index already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS hdbhms.room_utility_baselines
(
    room_utility_baseline_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    room_id                  BIGINT UNSIGNED                          NOT NULL,
    meter_id                 BIGINT UNSIGNED                          NOT NULL,
    last_billed_reading      DECIMAL(12, 3)                           NOT NULL,
    last_invoice_id          BIGINT UNSIGNED                          NULL,
    created_at               DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    updated_at               DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_room_utility_baseline_meter
        UNIQUE (meter_id),
    CONSTRAINT fk_rub_invoice
        FOREIGN KEY (last_invoice_id) REFERENCES hdbhms.invoices (invoice_id),
    CONSTRAINT fk_rub_meter
        FOREIGN KEY (meter_id) REFERENCES hdbhms.meters (meter_id),
    CONSTRAINT fk_rub_room
        FOREIGN KEY (room_id) REFERENCES hdbhms.rooms (room_id),
    INDEX idx_room_utility_baseline_room (room_id)
);

CREATE TABLE IF NOT EXISTS hdbhms.utility_billing_runs
(
    utility_billing_run_id   BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    property_id              BIGINT UNSIGNED                                                        NOT NULL,
    billing_period           CHAR(7)                                                                NOT NULL,
    invoice_reason           ENUM ('MONTHLY', 'TRANSFER', 'ROOM_CLOSE', 'MANUAL') DEFAULT 'MONTHLY' NOT NULL,
    status                   ENUM ('DRAFT', 'PREVIEWED', 'CONFIRMED', 'INVOICES_CREATED', 'CANCELLED')
                                                                                   DEFAULT 'DRAFT'   NOT NULL,
    total_rooms              INT                                                  DEFAULT 0         NOT NULL,
    ready_count              INT                                                  DEFAULT 0         NOT NULL,
    warning_count            INT                                                  DEFAULT 0         NOT NULL,
    skipped_count            INT                                                  DEFAULT 0         NOT NULL,
    generated_invoice_count  INT                                                  DEFAULT 0         NOT NULL,
    subtotal_amount          BIGINT UNSIGNED                                      DEFAULT 0         NOT NULL,
    discount_amount          BIGINT UNSIGNED                                      DEFAULT 0         NOT NULL,
    total_amount             BIGINT UNSIGNED                                      DEFAULT 0         NOT NULL,
    created_by               BIGINT UNSIGNED                                                        NULL,
    generated_by             BIGINT UNSIGNED                                                        NULL,
    generated_at             DATETIME(6)                                                            NULL,
    created_at               DATETIME(6)                                         DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    updated_at               DATETIME(6)                                         DEFAULT CURRENT_TIMESTAMP(6) NOT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_utility_billing_run_period
        UNIQUE (property_id, billing_period, invoice_reason),
    CONSTRAINT fk_utility_billing_run_created
        FOREIGN KEY (created_by) REFERENCES hdbhms.users (user_id),
    CONSTRAINT fk_utility_billing_run_generated
        FOREIGN KEY (generated_by) REFERENCES hdbhms.users (user_id),
    CONSTRAINT fk_utility_billing_run_property
        FOREIGN KEY (property_id) REFERENCES hdbhms.properties (property_id),
    INDEX idx_utility_billing_run_status (property_id, billing_period, status)
);

CREATE TABLE IF NOT EXISTS hdbhms.utility_billing_run_items
(
    utility_billing_run_item_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    run_id                      BIGINT UNSIGNED                        NOT NULL,
    room_id                     BIGINT UNSIGNED                        NOT NULL,
    lease_contract_id           BIGINT UNSIGNED                        NULL,
    electricity_reading_id      BIGINT UNSIGNED                        NULL,
    water_reading_id            BIGINT UNSIGNED                        NULL,
    electricity_previous        DECIMAL(12, 3)                         NULL,
    electricity_current         DECIMAL(12, 3)                         NULL,
    electricity_usage           DECIMAL(12, 3)                         NULL,
    electricity_quantity        INT                         DEFAULT 0  NOT NULL,
    electricity_unit_price      BIGINT UNSIGNED             DEFAULT 0  NOT NULL,
    electricity_amount          BIGINT UNSIGNED             DEFAULT 0  NOT NULL,
    water_previous              DECIMAL(12, 3)                         NULL,
    water_current               DECIMAL(12, 3)                         NULL,
    water_usage                 DECIMAL(12, 3)                         NULL,
    water_quantity              INT                         DEFAULT 0  NOT NULL,
    water_unit_price            BIGINT UNSIGNED             DEFAULT 0  NOT NULL,
    water_amount                BIGINT UNSIGNED             DEFAULT 0  NOT NULL,
    subtotal_amount             BIGINT UNSIGNED             DEFAULT 0  NOT NULL,
    discount_amount             BIGINT UNSIGNED             DEFAULT 0  NOT NULL,
    total_amount                BIGINT UNSIGNED             DEFAULT 0  NOT NULL,
    warning_message             TEXT                                  NULL,
    adjustment_reason           VARCHAR(500)                          NULL,
    status                      ENUM ('READY', 'WARNING', 'SKIPPED', 'INVOICED')
                                                            DEFAULT 'READY' NOT NULL,
    invoice_id                  BIGINT UNSIGNED                       NULL,
    CONSTRAINT uq_utility_billing_run_item_room
        UNIQUE (run_id, room_id),
    CONSTRAINT fk_utility_billing_item_electricity_reading
        FOREIGN KEY (electricity_reading_id) REFERENCES hdbhms.meter_readings (meter_reading_id),
    CONSTRAINT fk_utility_billing_item_invoice
        FOREIGN KEY (invoice_id) REFERENCES hdbhms.invoices (invoice_id),
    CONSTRAINT fk_utility_billing_item_room
        FOREIGN KEY (room_id) REFERENCES hdbhms.rooms (room_id),
    CONSTRAINT fk_utility_billing_item_run
        FOREIGN KEY (run_id) REFERENCES hdbhms.utility_billing_runs (utility_billing_run_id),
    CONSTRAINT fk_utility_billing_item_contract
        FOREIGN KEY (lease_contract_id) REFERENCES hdbhms.lease_contracts (lease_contract_id),
    CONSTRAINT fk_utility_billing_item_water_reading
        FOREIGN KEY (water_reading_id) REFERENCES hdbhms.meter_readings (meter_reading_id),
    INDEX idx_utility_billing_run_item_status (run_id, status)
);

SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'room_utility_baselines'
      AND INDEX_NAME = 'idx_room_utility_baseline_room'
);
SET @sql = IF(
    @idx_exists = 0,
    'ALTER TABLE hdbhms.room_utility_baselines ADD INDEX idx_room_utility_baseline_room (room_id)',
    'SELECT ''room utility baseline room index already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'utility_billing_runs'
      AND INDEX_NAME = 'idx_utility_billing_run_status'
);
SET @sql = IF(
    @idx_exists = 0,
    'ALTER TABLE hdbhms.utility_billing_runs ADD INDEX idx_utility_billing_run_status (property_id, billing_period, status)',
    'SELECT ''utility billing run status index already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'utility_billing_run_items'
      AND INDEX_NAME = 'idx_utility_billing_run_item_status'
);
SET @sql = IF(
    @idx_exists = 0,
    'ALTER TABLE hdbhms.utility_billing_run_items ADD INDEX idx_utility_billing_run_item_status (run_id, status)',
    'SELECT ''utility billing run item status index already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
