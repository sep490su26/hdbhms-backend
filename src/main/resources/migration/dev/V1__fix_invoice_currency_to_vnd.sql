SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'invoices'
              AND column_name = 'currency'
        ),
        'ALTER TABLE invoices DROP COLUMN currency',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'invoices'
              AND column_name = 'subtotal_amount'
        ),
        'ALTER TABLE invoices MODIFY COLUMN subtotal_amount BIGINT UNSIGNED NOT NULL DEFAULT 0',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'invoices'
              AND column_name = 'discount_amount'
        ),
        'ALTER TABLE invoices MODIFY COLUMN discount_amount BIGINT UNSIGNED NOT NULL DEFAULT 0',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'invoices'
              AND column_name = 'total_amount'
        ),
        'ALTER TABLE invoices MODIFY COLUMN total_amount BIGINT UNSIGNED NOT NULL DEFAULT 0',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'invoices'
              AND column_name = 'paid_amount'
        ),
        'ALTER TABLE invoices MODIFY COLUMN paid_amount BIGINT UNSIGNED NOT NULL DEFAULT 0',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'invoices'
              AND column_name = 'remaining_amount'
        ),
        'ALTER TABLE invoices MODIFY COLUMN remaining_amount BIGINT UNSIGNED NOT NULL DEFAULT 0',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
