-- Idempotent version: rename contract_id, add deposit_agreement_id

DELIMITER //

CREATE PROCEDURE drop_old_contract_objects()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
               WHERE CONSTRAINT_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'invoices'
                 AND CONSTRAINT_NAME = 'fk_inv_contract'
                 AND CONSTRAINT_TYPE = 'FOREIGN KEY') THEN
        ALTER TABLE invoices DROP FOREIGN KEY fk_inv_contract;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
               WHERE CONSTRAINT_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'invoices'
                 AND CONSTRAINT_NAME = 'uq_invoice_contract_period_type_rev') THEN
        ALTER TABLE invoices DROP INDEX uq_invoice_contract_period_type_rev;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.STATISTICS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'invoices'
                 AND INDEX_NAME = 'idx_invoice_contract') THEN
        ALTER TABLE invoices DROP INDEX idx_invoice_contract;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'invoices'
                 AND COLUMN_NAME = 'active_invoice_key') THEN
        ALTER TABLE invoices DROP COLUMN active_invoice_key;
    END IF;
END//

DELIMITER ;
CALL drop_old_contract_objects();
DROP PROCEDURE drop_old_contract_objects;

-- Rename column (only if still named contract_id)
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'invoices'
                     AND COLUMN_NAME = 'contract_id');
SET @sql = IF(@col_exists = 1,
              'ALTER TABLE invoices CHANGE COLUMN contract_id lease_contract_id BIGINT UNSIGNED NULL',
              'SELECT ''Column already renamed''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add deposit_agreement_id if missing
DELIMITER //
CREATE PROCEDURE add_deposit_agreement_column()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'invoices'
                     AND COLUMN_NAME = 'deposit_agreement_id') THEN
        ALTER TABLE invoices
            ADD COLUMN deposit_agreement_id BIGINT UNSIGNED NULL AFTER lease_contract_id,
            ADD INDEX idx_invoice_dep_agreement (deposit_agreement_id),
            ADD CONSTRAINT fk_inv_deposit_agreement
                FOREIGN KEY (deposit_agreement_id) REFERENCES deposit_agreements(id);
    END IF;
END//
DELIMITER ;
CALL add_deposit_agreement_column();
DROP PROCEDURE add_deposit_agreement_column;

-- Recreate lease_contract objects if missing
-- Foreign key
SET @fk_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                  WHERE CONSTRAINT_SCHEMA = DATABASE()
                    AND TABLE_NAME = 'invoices'
                    AND CONSTRAINT_NAME = 'fk_inv_lease_contract');
SET @sql = IF(@fk_exists = 0,
              'ALTER TABLE invoices ADD CONSTRAINT fk_inv_lease_contract FOREIGN KEY (lease_contract_id) REFERENCES lease_contracts(id)',
              'SELECT ''FK already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Index
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'invoices'
                     AND INDEX_NAME = 'idx_invoice_lease_contract');
SET @sql = IF(@idx_exists = 0,
              'ALTER TABLE invoices ADD INDEX idx_invoice_lease_contract (lease_contract_id)',
              'SELECT ''Index already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Unique constraint
SET @uc_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                  WHERE CONSTRAINT_SCHEMA = DATABASE()
                    AND TABLE_NAME = 'invoices'
                    AND CONSTRAINT_NAME = 'uq_invoice_contract_period_type_rev');
SET @sql = IF(@uc_exists = 0,
              'ALTER TABLE invoices ADD UNIQUE KEY uq_invoice_contract_period_type_rev (lease_contract_id, billing_period, invoice_type, revision_no)',
              'SELECT ''Unique key already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Generated column
SET @gen_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'invoices'
                     AND COLUMN_NAME = 'active_invoice_key');
SET @sql = IF(@gen_exists = 0,
              'ALTER TABLE invoices ADD COLUMN active_invoice_key VARCHAR(255) GENERATED ALWAYS AS (
                  IF(status <> ''VOIDED'' AND lease_contract_id IS NOT NULL AND billing_period IS NOT NULL,
                     CONCAT(lease_contract_id, '':'', billing_period, '':'', invoice_type), NULL)
              ) VIRTUAL',
              'SELECT ''Generated column already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;