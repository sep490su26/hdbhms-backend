SET @schema_name = DATABASE();

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'utility_billing_run_items'
      AND COLUMN_NAME = 'electricity_waived'
);
SET @sql = IF(
    @column_exists = 0,
    'ALTER TABLE utility_billing_run_items ADD COLUMN electricity_waived TINYINT(1) NOT NULL DEFAULT 0 AFTER electricity_amount',
    'SELECT ''electricity_waived exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'utility_billing_run_items'
      AND COLUMN_NAME = 'electricity_waive_reason'
);
SET @sql = IF(
    @column_exists = 0,
    'ALTER TABLE utility_billing_run_items ADD COLUMN electricity_waive_reason VARCHAR(500) NULL AFTER electricity_waived',
    'SELECT ''electricity_waive_reason exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
