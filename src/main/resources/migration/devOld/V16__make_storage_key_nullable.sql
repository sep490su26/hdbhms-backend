DELIMITER //
CREATE PROCEDURE make_storage_key_nullable_if_needed()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'file_metadata'
                 AND COLUMN_NAME = 'storage_key'
                 AND IS_NULLABLE = 'NO') THEN
        ALTER TABLE file_metadata
            MODIFY COLUMN storage_key VARCHAR(1000) NULL;
    END IF;
END//
DELIMITER ;

CALL make_storage_key_nullable_if_needed();
DROP PROCEDURE make_storage_key_nullable_if_needed;