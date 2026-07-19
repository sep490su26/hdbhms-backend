SET @schema_name = 'hdbhms';

ALTER TABLE hdbhms.scheduled_tasks
    MODIFY COLUMN task_type ENUM (
        'INVOICE_REMINDER',
        'DEBT_WARNING',
        'CONTRACT_EXPIRY',
        'ROOM_STATUS_AUTOMATION',
        'MAINTENANCE_FOLLOWUP',
        'ROOM_HOLD_EXPIRATION',
        'UTILITY_MONTHLY_RUN',
        'SCHEDULED_BILLING_CHARGES',
        'DEBT_OVERDUE_SCAN',
        'INVOICE_OVERDUE_WARNINGS',
        'NOTIFICATION_OUTBOX_DISPATCH',
        'EXPIRED_ROOM_HOLD_RECONCILE',
        'VISIT_REQUEST_TRASH_CLEANUP',
        'ROOM_TRANSFER_TIMEOUT',
        'CONTRACT_LIFECYCLE_SCAN',
        'OTHER'
    ) NOT NULL;

ALTER TABLE hdbhms.scheduled_tasks
    MODIFY COLUMN status ENUM ('PENDING', 'PROCESSING', 'DONE', 'FAILED', 'CANCELLED') DEFAULT 'PENDING' NOT NULL;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'scheduled_tasks' AND COLUMN_NAME = 'idempotency_key');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE hdbhms.scheduled_tasks ADD COLUMN idempotency_key VARCHAR(180) NULL AFTER payload', 'SELECT ''idempotency_key exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'scheduled_tasks' AND COLUMN_NAME = 'recurring');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE hdbhms.scheduled_tasks ADD COLUMN recurring TINYINT(1) NOT NULL DEFAULT 0 AFTER payload', 'SELECT ''recurring exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'scheduled_tasks' AND COLUMN_NAME = 'schedule_expression');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE hdbhms.scheduled_tasks ADD COLUMN schedule_expression VARCHAR(100) NULL AFTER recurring', 'SELECT ''schedule_expression exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'scheduled_tasks' AND COLUMN_NAME = 'last_error');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE hdbhms.scheduled_tasks ADD COLUMN last_error TEXT NULL AFTER schedule_expression', 'SELECT ''last_error exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'scheduled_tasks' AND COLUMN_NAME = 'claimed_at');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE hdbhms.scheduled_tasks ADD COLUMN claimed_at DATETIME(6) NULL AFTER last_error', 'SELECT ''claimed_at exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'scheduled_tasks' AND COLUMN_NAME = 'claimed_by');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE hdbhms.scheduled_tasks ADD COLUMN claimed_by VARCHAR(120) NULL AFTER claimed_at', 'SELECT ''claimed_by exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'scheduled_tasks' AND COLUMN_NAME = 'lock_until');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE hdbhms.scheduled_tasks ADD COLUMN lock_until DATETIME(6) NULL AFTER claimed_by', 'SELECT ''lock_until exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'scheduled_tasks' AND INDEX_NAME = 'idx_tasks_claimable');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_tasks_claimable ON hdbhms.scheduled_tasks (status, due_at, lock_until)', 'SELECT ''idx_tasks_claimable exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

DELETE duplicate_task
FROM hdbhms.scheduled_tasks duplicate_task
JOIN hdbhms.scheduled_tasks keeper
  ON keeper.task_type = duplicate_task.task_type
 AND keeper.target_type = duplicate_task.target_type
 AND keeper.target_id = duplicate_task.target_id
 AND keeper.scheduled_task_id < duplicate_task.scheduled_task_id
WHERE duplicate_task.target_type = 'SYSTEM_JOB'
  AND duplicate_task.target_id = 0
  AND duplicate_task.recurring = 1;

DELETE duplicate_task
FROM hdbhms.scheduled_tasks duplicate_task
JOIN hdbhms.scheduled_tasks keeper
  ON keeper.task_type = duplicate_task.task_type
 AND keeper.target_type = duplicate_task.target_type
 AND keeper.target_id = duplicate_task.target_id
 AND keeper.status IN ('PENDING', 'PROCESSING')
 AND keeper.scheduled_task_id < duplicate_task.scheduled_task_id
WHERE duplicate_task.task_type = 'ROOM_HOLD_EXPIRATION'
  AND duplicate_task.target_type = 'RoomHold'
  AND duplicate_task.status IN ('PENDING', 'PROCESSING');

UPDATE hdbhms.scheduled_tasks
SET idempotency_key = CONCAT('SYSTEM_JOB:', task_type)
WHERE target_type = 'SYSTEM_JOB'
  AND target_id = 0
  AND recurring = 1
  AND idempotency_key IS NULL;

UPDATE hdbhms.scheduled_tasks
SET idempotency_key = CONCAT(task_type, ':', target_type, ':', target_id)
WHERE task_type = 'ROOM_HOLD_EXPIRATION'
  AND target_type = 'RoomHold'
  AND status IN ('PENDING', 'PROCESSING')
  AND idempotency_key IS NULL;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'scheduled_tasks' AND INDEX_NAME = 'uk_scheduled_tasks_idempotency_key');
SET @sql = IF(@index_exists = 0, 'CREATE UNIQUE INDEX uk_scheduled_tasks_idempotency_key ON hdbhms.scheduled_tasks (idempotency_key)', 'SELECT ''uk_scheduled_tasks_idempotency_key exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS hdbhms.scheduled_task_type_locks
(
    task_type  VARCHAR(50)  NOT NULL,
    claimed_by VARCHAR(120) NOT NULL,
    lock_until DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (task_type),
    INDEX idx_scheduled_task_type_locks_until (lock_until)
);

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'room_transfer_requests' AND COLUMN_NAME = 'positive_difference_settlement_type');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE hdbhms.room_transfer_requests ADD COLUMN positive_difference_settlement_type ENUM (''TENANT_PAY_MORE'', ''ADD_TO_NEXT_INVOICE'', ''REFUND_NOW'', ''CREDIT_NEXT_CONTRACT'', ''NO_DIFFERENCE'') NULL AFTER status', 'ALTER TABLE hdbhms.room_transfer_requests MODIFY COLUMN positive_difference_settlement_type ENUM (''TENANT_PAY_MORE'', ''ADD_TO_NEXT_INVOICE'', ''REFUND_NOW'', ''CREDIT_NEXT_CONTRACT'', ''NO_DIFFERENCE'') NULL');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'transfer_settlements' AND COLUMN_NAME = 'positive_difference_settlement_type');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE hdbhms.transfer_settlements ADD COLUMN positive_difference_settlement_type ENUM (''TENANT_PAY_MORE'', ''ADD_TO_NEXT_INVOICE'', ''REFUND_NOW'', ''CREDIT_NEXT_CONTRACT'', ''NO_DIFFERENCE'') NULL AFTER settlement_type', 'ALTER TABLE hdbhms.transfer_settlements MODIFY COLUMN positive_difference_settlement_type ENUM (''TENANT_PAY_MORE'', ''ADD_TO_NEXT_INVOICE'', ''REFUND_NOW'', ''CREDIT_NEXT_CONTRACT'', ''NO_DIFFERENCE'') NULL');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE hdbhms.transfer_settlements
    MODIFY COLUMN settlement_type ENUM ('TENANT_PAY_MORE', 'ADD_TO_NEXT_INVOICE', 'REFUND_NOW', 'CREDIT_NEXT_CONTRACT', 'NO_DIFFERENCE') NOT NULL;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'room_transfer_requests' AND COLUMN_NAME = 'approved_by');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE hdbhms.room_transfer_requests ADD COLUMN approved_by BIGINT UNSIGNED NULL AFTER target_holder_rejected_at', 'SELECT ''approved_by exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'room_transfer_requests' AND COLUMN_NAME = 'approved_at');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE hdbhms.room_transfer_requests ADD COLUMN approved_at DATETIME(6) NULL AFTER approved_by', 'SELECT ''approved_at exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'room_transfer_requests' AND COLUMN_NAME = 'executed_at');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE hdbhms.room_transfer_requests ADD COLUMN executed_at DATETIME(6) NULL AFTER approved_at', 'SELECT ''executed_at exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'room_transfer_requests' AND COLUMN_NAME = 'completed_at');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE hdbhms.room_transfer_requests ADD COLUMN completed_at DATETIME(6) NULL AFTER executed_at', 'SELECT ''completed_at exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'room_transfer_requests' AND COLUMN_NAME = 'eligibility_checked_at');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE hdbhms.room_transfer_requests ADD COLUMN eligibility_checked_at DATETIME(6) NULL AFTER positive_difference_settlement_type', 'SELECT ''eligibility_checked_at exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'room_transfer_requests' AND COLUMN_NAME = 'is_eligible_at_creation');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE hdbhms.room_transfer_requests ADD COLUMN is_eligible_at_creation TINYINT(1) NULL AFTER eligibility_checked_at', 'SELECT ''is_eligible_at_creation exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'room_transfer_requests' AND COLUMN_NAME = 'eligibility_snapshot');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE hdbhms.room_transfer_requests ADD COLUMN eligibility_snapshot JSON NULL AFTER is_eligible_at_creation', 'SELECT ''eligibility_snapshot exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'room_transfer_requests' AND COLUMN_NAME = 'violation_snapshot');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE hdbhms.room_transfer_requests ADD COLUMN violation_snapshot JSON NULL AFTER eligibility_snapshot', 'SELECT ''violation_snapshot exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'room_transfer_requests' AND COLUMN_NAME = 'transfer_history_snapshot');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE hdbhms.room_transfer_requests ADD COLUMN transfer_history_snapshot JSON NULL AFTER violation_snapshot', 'SELECT ''transfer_history_snapshot exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'utility_billing_run_items' AND COLUMN_NAME = 'service_fee_unit_price');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE hdbhms.utility_billing_run_items ADD COLUMN service_fee_unit_price BIGINT UNSIGNED NOT NULL DEFAULT 0 AFTER water_amount', 'SELECT ''service_fee_unit_price exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'utility_billing_run_items' AND COLUMN_NAME = 'service_fee_amount');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE hdbhms.utility_billing_run_items ADD COLUMN service_fee_amount BIGINT UNSIGNED NOT NULL DEFAULT 0 AFTER service_fee_unit_price', 'SELECT ''service_fee_amount exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'utility_billing_run_items' AND COLUMN_NAME = 'service_fee_waived');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE hdbhms.utility_billing_run_items ADD COLUMN service_fee_waived TINYINT(1) NOT NULL DEFAULT 0 AFTER service_fee_amount', 'SELECT ''service_fee_waived exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'utility_billing_run_items' AND COLUMN_NAME = 'service_fee_waive_reason');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE hdbhms.utility_billing_run_items ADD COLUMN service_fee_waive_reason VARCHAR(500) NULL AFTER service_fee_waived', 'SELECT ''service_fee_waive_reason exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'utility_billing_run_items' AND COLUMN_NAME = 'service_fee_line_required');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE hdbhms.utility_billing_run_items ADD COLUMN service_fee_line_required TINYINT(1) NOT NULL DEFAULT 0 AFTER service_fee_waive_reason', 'SELECT ''service_fee_line_required exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
