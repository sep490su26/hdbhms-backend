SET @has_old_floor_plan_items_schema := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = 'hdbhms'
      AND table_name = 'floor_plan_items'
      AND column_name = 'item_type'
);

SET @migrate_floor_plan_items_metadata := IF(
    @has_old_floor_plan_items_schema > 0,
    'UPDATE hdbhms.floor_plan_items
     SET metadata_json = JSON_SET(
         COALESCE(metadata_json, JSON_OBJECT()),
         ''$.label'', COALESCE(label, CHAR(0)),
         ''$.rotation'', rotation,
         ''$.sortOrder'', sort_order
     )',
    'SELECT 1'
);

PREPARE migrate_floor_plan_items_metadata_stmt FROM @migrate_floor_plan_items_metadata;
EXECUTE migrate_floor_plan_items_metadata_stmt;
DEALLOCATE PREPARE migrate_floor_plan_items_metadata_stmt;

SET @align_floor_plan_items_schema := IF(
    @has_old_floor_plan_items_schema > 0,
    'ALTER TABLE hdbhms.floor_plan_items
         CHANGE COLUMN item_type type VARCHAR(50) NOT NULL,
         CHANGE COLUMN x position_x INT NOT NULL,
         CHANGE COLUMN y position_y INT NOT NULL,
         CHANGE COLUMN width width INT NOT NULL,
         CHANGE COLUMN height height INT NOT NULL,
         CHANGE COLUMN metadata_json metadata JSON NULL,
         DROP COLUMN label,
         DROP COLUMN rotation,
         DROP COLUMN sort_order',
    'SELECT 1'
);

PREPARE align_floor_plan_items_schema_stmt FROM @align_floor_plan_items_schema;
EXECUTE align_floor_plan_items_schema_stmt;
DEALLOCATE PREPARE align_floor_plan_items_schema_stmt;
