SET @has_room_transfer_positive_difference_settlement_type := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = 'hdbhms'
      AND table_name = 'room_transfer_requests'
      AND column_name = 'positive_difference_settlement_type'
);

SET @add_room_transfer_positive_difference_settlement_type := IF(
    @has_room_transfer_positive_difference_settlement_type = 0,
    'ALTER TABLE hdbhms.room_transfer_requests ADD COLUMN positive_difference_settlement_type ENUM (''TENANT_PAY_MORE'', ''CREDIT_NEXT_CONTRACT'') NULL AFTER status',
    'SELECT 1'
);

PREPARE add_room_transfer_positive_difference_settlement_type_stmt FROM @add_room_transfer_positive_difference_settlement_type;
EXECUTE add_room_transfer_positive_difference_settlement_type_stmt;
DEALLOCATE PREPARE add_room_transfer_positive_difference_settlement_type_stmt;
