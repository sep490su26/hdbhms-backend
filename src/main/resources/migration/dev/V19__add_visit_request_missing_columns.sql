-- V19__add_visit_request_missing_columns.sql
-- Add missing columns expected by VisitRequest entity.

SET @sql := (
    SELECT IF(
        NOT EXISTS (
            SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'visit_requests'
              AND COLUMN_NAME = 'deleted_at'
        ),
        'ALTER TABLE visit_requests ADD COLUMN deleted_at DATETIME(6) NULL',
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
              AND TABLE_NAME = 'visit_requests'
              AND COLUMN_NAME = 'deleted_by'
        ),
        'ALTER TABLE visit_requests ADD COLUMN deleted_by BIGINT NULL',
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
              AND TABLE_NAME = 'visit_requests'
              AND COLUMN_NAME = 'status'
        ),
        'ALTER TABLE visit_requests ADD COLUMN status VARCHAR(50) NULL',
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
              AND TABLE_NAME = 'visit_requests'
              AND COLUMN_NAME = 'updated_at'
        ),
        'ALTER TABLE visit_requests ADD COLUMN updated_at DATETIME(6) NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
