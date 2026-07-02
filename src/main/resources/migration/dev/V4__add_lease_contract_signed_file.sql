SET @schema_name := DATABASE();

SET @sql := IF(
    (SELECT COUNT(*)
     FROM information_schema.columns
     WHERE table_schema = @schema_name
       AND table_name = 'lease_contracts'
       AND column_name = 'signed_file_id') = 0,
    'ALTER TABLE lease_contracts ADD COLUMN signed_file_id BIGINT UNSIGNED NULL AFTER contract_file_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*)
     FROM information_schema.columns
     WHERE table_schema = @schema_name
       AND table_name = 'lease_contracts'
       AND column_name = 'signed_uploaded_by') = 0,
    'ALTER TABLE lease_contracts ADD COLUMN signed_uploaded_by BIGINT UNSIGNED NULL AFTER signed_file_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*)
     FROM information_schema.referential_constraints
     WHERE constraint_schema = @schema_name
       AND constraint_name = 'fk_lc_signed_file') = 0,
    'ALTER TABLE lease_contracts ADD CONSTRAINT fk_lc_signed_file FOREIGN KEY (signed_file_id) REFERENCES file_metadata (file_metadata_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*)
     FROM information_schema.referential_constraints
     WHERE constraint_schema = @schema_name
       AND constraint_name = 'fk_lc_signed_by') = 0,
    'ALTER TABLE lease_contracts ADD CONSTRAINT fk_lc_signed_by FOREIGN KEY (signed_uploaded_by) REFERENCES users (user_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
