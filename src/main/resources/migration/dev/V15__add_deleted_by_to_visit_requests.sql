DELIMITER //

CREATE PROCEDURE add_visit_request_deleted_by()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'visit_requests'
          AND COLUMN_NAME = 'deleted_by'
    ) THEN
        ALTER TABLE visit_requests
            ADD COLUMN deleted_by BIGINT UNSIGNED NULL AFTER deleted_at,
            ADD CONSTRAINT fk_visit_deleted_by
                FOREIGN KEY (deleted_by) REFERENCES users (id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'visit_requests'
          AND INDEX_NAME = 'idx_visit_requests_deleted_at'
    ) THEN
        ALTER TABLE visit_requests
            ADD INDEX idx_visit_requests_deleted_at (deleted_at);
    END IF;
END//

DELIMITER ;

CALL add_visit_request_deleted_by();
DROP PROCEDURE add_visit_request_deleted_by;
