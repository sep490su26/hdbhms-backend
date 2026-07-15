DELIMITER //

-- 1. Drop foreign keys if they exist
CREATE PROCEDURE drop_visit_fks_if_exist()
BEGIN
    IF EXISTS (SELECT 1
               FROM information_schema.TABLE_CONSTRAINTS
               WHERE CONSTRAINT_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'visit_requests'
                 AND CONSTRAINT_NAME = 'fk_visit_lead'
                 AND CONSTRAINT_TYPE = 'FOREIGN KEY') THEN
        ALTER TABLE visit_requests
            DROP FOREIGN KEY fk_visit_lead;
    END IF;

    IF EXISTS (SELECT 1
               FROM information_schema.TABLE_CONSTRAINTS
               WHERE CONSTRAINT_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'visit_requests'
                 AND CONSTRAINT_NAME = 'fk_visit_creator'
                 AND CONSTRAINT_TYPE = 'FOREIGN KEY') THEN
        ALTER TABLE visit_requests
            DROP FOREIGN KEY fk_visit_creator;
    END IF;
END//

DELIMITER ;
CALL drop_visit_fks_if_exist();
DROP PROCEDURE drop_visit_fks_if_exist;

ALTER TABLE visit_requests
    DROP COLUMN lead_id,
    DROP COLUMN preferred_end,
    DROP COLUMN status,
    DROP COLUMN created_by,
    DROP COLUMN updated_at;

ALTER TABLE visit_requests
    ADD FULLTEXT INDEX ft_visit_search (visitor_name, visitor_email, visitor_phone);
