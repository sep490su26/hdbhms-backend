DELIMITER //
CREATE PROCEDURE drop_fk_if_exists()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'leads'
          AND CONSTRAINT_NAME = 'fk_lead_assigned'
          AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    ) THEN
        ALTER TABLE leads DROP FOREIGN KEY fk_lead_assigned;
    END IF;
END//
DELIMITER ;
CALL drop_fk_if_exists();
DROP PROCEDURE drop_fk_if_exists;

DELIMITER //
CREATE PROCEDURE drop_index_if_exists(IN idx_name VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'leads'
          AND INDEX_NAME = idx_name
    ) THEN
        SET @stmt = CONCAT('DROP INDEX ', idx_name, ' ON leads');
        PREPARE s FROM @stmt;
        EXECUTE s;
        DEALLOCATE PREPARE s;
    END IF;
END//
DELIMITER ;

CALL drop_index_if_exists('idx_lead_status');
CALL drop_index_if_exists('idx_lead_assigned');
DROP PROCEDURE drop_index_if_exists;

DELIMITER //
CREATE PROCEDURE drop_columns_if_needed()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'leads'
                 AND COLUMN_NAME = 'source') THEN
        ALTER TABLE leads DROP COLUMN source;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'leads'
                 AND COLUMN_NAME = 'status') THEN
        ALTER TABLE leads DROP COLUMN status;
    END IF;
END//
DELIMITER ;
CALL drop_columns_if_needed();
DROP PROCEDURE drop_columns_if_needed;

ALTER TABLE leads
    ADD CONSTRAINT fk_lead_assigned
        FOREIGN KEY (assigned_user_id) REFERENCES users (id);

ALTER TABLE leads
    ADD INDEX idx_lead_assigned (assigned_user_id, created_at);