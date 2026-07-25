SET @schema_name := DATABASE();

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'maintenance_tickets'
      AND COLUMN_NAME = 'repair_requested'
);

SET @sql := IF(
    @column_exists = 0,
    'ALTER TABLE hdbhms.maintenance_tickets ADD COLUMN repair_requested BOOLEAN NOT NULL DEFAULT TRUE AFTER description',
    'SELECT ''repair_requested exists'''
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
