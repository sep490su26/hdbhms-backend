UPDATE visit_requests
SET status = CASE
                 WHEN status IN ('PENDING', 'SCHEDULED', 'REQUESTED') OR status IS NULL THEN 'NOT_VIEWED'
                 WHEN status = 'VIEWED' THEN 'VIEWED'
                 WHEN status IN ('CANCELLED', 'COMPLETED', 'NO_SHOW') THEN 'DISMISSED'
                 ELSE status
    END;

-- Step 2: Change the column type to ENUM only if it’s not already an ENUM
SET @col_type := (
    SELECT COLUMN_TYPE
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'visit_requests'
      AND COLUMN_NAME = 'status'
);

SET @sql := IF(
        @col_type IS NULL OR @col_type != 'enum(\'NOT_VIEWED\',\'VIEWED\',\'DISMISSED\')',
        'ALTER TABLE visit_requests MODIFY COLUMN status ENUM(\'NOT_VIEWED\',\'VIEWED\',\'DISMISSED\') NOT NULL DEFAULT \'NOT_VIEWED\'',
        'SELECT 1'
            );

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;