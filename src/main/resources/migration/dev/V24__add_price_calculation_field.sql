-- V21__add_deposit_months_to_deposit_forms.sql

SET @sql := (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'deposit_forms'
              AND COLUMN_NAME = 'deposit_months'
        ),
        'ALTER TABLE deposit_forms ADD COLUMN deposit_months INT UNSIGNED NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'deposit_forms'
              AND COLUMN_NAME = 'payment_cycle_months'
        ),
        'ALTER TABLE deposit_forms ADD COLUMN payment_cycle_months TINYINT UNSIGNED NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;