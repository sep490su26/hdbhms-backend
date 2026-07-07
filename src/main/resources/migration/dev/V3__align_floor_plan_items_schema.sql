DELIMITER //

CREATE PROCEDURE migrate_floor_plan_items_v3()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'floor_plan_items'
          AND column_name = 'item_type'
    ) THEN
        UPDATE floor_plan_items
        SET metadata_json = JSON_SET(
                COALESCE(metadata_json, JSON_OBJECT()),
                '$.label', COALESCE(label, ''),
                '$.rotation', rotation,
                '$.sortOrder', sort_order
            );

        ALTER TABLE floor_plan_items
            CHANGE COLUMN item_type type VARCHAR(50) NOT NULL,
            CHANGE COLUMN x position_x INT NOT NULL,
            CHANGE COLUMN y position_y INT NOT NULL,
            CHANGE COLUMN width width INT NOT NULL,
            CHANGE COLUMN height height INT NOT NULL,
            CHANGE COLUMN metadata_json metadata JSON NULL,
            DROP COLUMN label,
            DROP COLUMN rotation,
            DROP COLUMN sort_order;
    END IF;
END//

CALL migrate_floor_plan_items_v3()//
DROP PROCEDURE migrate_floor_plan_items_v3//

DELIMITER ;
