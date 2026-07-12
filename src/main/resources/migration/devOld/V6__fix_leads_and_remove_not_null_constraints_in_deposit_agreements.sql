DELIMITER //

-- Safely drop constraint
DROP PROCEDURE IF EXISTS drop_deposit_source_check //
CREATE PROCEDURE drop_deposit_source_check()
BEGIN
    IF EXISTS (SELECT 1
               FROM information_schema.CHECK_CONSTRAINTS
               WHERE CONSTRAINT_SCHEMA = DATABASE()
                 AND CONSTRAINT_NAME = 'chk_deposit_source') THEN
        ALTER TABLE deposit_agreements
            DROP CONSTRAINT chk_deposit_source;
    END IF;
END //

-- Safely rename column
DROP PROCEDURE IF EXISTS safe_rename_lead_column //
CREATE PROCEDURE safe_rename_lead_column()
BEGIN
    DECLARE col_exists INT DEFAULT 0;
    SELECT COUNT(*)
    INTO col_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'leads'
      AND COLUMN_NAME = 'assigned_user_id';

    IF col_exists > 0 THEN
        ALTER TABLE leads
            CHANGE COLUMN assigned_user_id user_id BIGINT UNSIGNED NULL;
    END IF;
END //

DELIMITER ;

-- Execute
CALL drop_deposit_source_check();
CALL safe_rename_lead_column();

-- Clean up
DROP PROCEDURE IF EXISTS drop_deposit_source_check;
DROP PROCEDURE IF EXISTS safe_rename_lead_column;