SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Legacy room-transfer change requests used hard-coded target ids. Resolve the
-- relationship by request code before the ids can be reused by a new request.
UPDATE hdbhms.change_requests change_request
JOIN hdbhms.room_transfer_requests transfer_request
  ON transfer_request.request_code = change_request.request_code
SET change_request.target_id = transfer_request.room_transfer_request_id,
    change_request.updated_at = CURRENT_TIMESTAMP(6)
WHERE change_request.request_type = 'ROOM_TRANSFER'
  AND change_request.target_type = 'CONTRACT';

SET @add_active_transfer_key := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE hdbhms.change_requests ADD COLUMN active_tenant_transfer_key BIGINT UNSIGNED GENERATED ALWAYS AS (IF(requester_role = ''TENANT'' AND request_type = ''ROOM_TRANSFER'' AND status IN (''PENDING'', ''UNDER_REVIEW'', ''APPROVED'', ''PROCESSING''), requester_id, NULL)) VIRTUAL',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'change_requests'
      AND column_name = 'active_tenant_transfer_key'
);
PREPARE add_active_transfer_key_statement FROM @add_active_transfer_key;
EXECUTE add_active_transfer_key_statement;
DEALLOCATE PREPARE add_active_transfer_key_statement;

SET @add_active_transfer_key_index := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE hdbhms.change_requests ADD UNIQUE KEY uq_active_tenant_transfer (active_tenant_transfer_key)',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'change_requests'
      AND index_name = 'uq_active_tenant_transfer'
);
PREPARE add_active_transfer_key_index_statement FROM @add_active_transfer_key_index;
EXECUTE add_active_transfer_key_index_statement;
DEALLOCATE PREPARE add_active_transfer_key_index_statement;
