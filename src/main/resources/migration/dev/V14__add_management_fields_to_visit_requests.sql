DELIMITER //

CREATE PROCEDURE add_visit_request_management_fields()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'visit_requests'
          AND COLUMN_NAME = 'status'
    ) THEN
        ALTER TABLE visit_requests
            ADD COLUMN status ENUM ('PENDING','VIEWED','CANCELLED') NOT NULL DEFAULT 'PENDING' AFTER preferred_start;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'visit_requests'
          AND COLUMN_NAME = 'source'
    ) THEN
        ALTER TABLE visit_requests
            ADD COLUMN source ENUM ('ZALO','FACEBOOK','PHONE','WALK_IN','OTHER') NULL AFTER status;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'visit_requests'
          AND COLUMN_NAME = 'updated_at'
    ) THEN
        ALTER TABLE visit_requests
            ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) AFTER created_at;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'visit_requests'
          AND COLUMN_NAME = 'deleted_at'
    ) THEN
        ALTER TABLE visit_requests
            ADD COLUMN deleted_at DATETIME(6) NULL AFTER updated_at;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'visit_requests'
          AND INDEX_NAME = 'idx_visit_management'
    ) THEN
        ALTER TABLE visit_requests
            ADD INDEX idx_visit_management (property_id, room_id, status, preferred_start, deleted_at);
    END IF;
END//

DELIMITER ;

CALL add_visit_request_management_fields();
DROP PROCEDURE add_visit_request_management_fields;
