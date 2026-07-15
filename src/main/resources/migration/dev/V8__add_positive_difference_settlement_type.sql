SET @has_positive_difference_settlement_type := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = 'hdbhms'
      AND table_name = 'transfer_settlements'
      AND column_name = 'positive_difference_settlement_type'
);

SET @add_positive_difference_settlement_type := IF(
    @has_positive_difference_settlement_type = 0,
    'ALTER TABLE hdbhms.transfer_settlements ADD COLUMN positive_difference_settlement_type ENUM (''TENANT_PAY_MORE'', ''CREDIT_NEXT_CONTRACT'') NULL AFTER settlement_type',
    'SELECT 1'
);

PREPARE add_positive_difference_settlement_type_stmt FROM @add_positive_difference_settlement_type;
EXECUTE add_positive_difference_settlement_type_stmt;
DEALLOCATE PREPARE add_positive_difference_settlement_type_stmt;
