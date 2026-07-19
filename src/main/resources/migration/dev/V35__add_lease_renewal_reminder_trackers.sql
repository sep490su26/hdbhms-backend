SET @schema_name = 'hdbhms';

CREATE TABLE IF NOT EXISTS hdbhms.reminder_trackers
(
    reminder_tracker_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    reminder_key        VARCHAR(100) NOT NULL,
    target_type         VARCHAR(50)  NOT NULL,
    target_id           BIGINT UNSIGNED NOT NULL,
    audience            VARCHAR(50)  NOT NULL,
    recipient_user_id   BIGINT UNSIGNED NULL,
    status              VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    sent_count          INT          NOT NULL DEFAULT 0,
    last_sent_at        DATETIME(6)  NULL,
    next_due_at         DATETIME(6)  NULL,
    completed_at        DATETIME(6)  NULL,
    related_task_id     BIGINT UNSIGNED NULL,
    metadata            JSON         NULL,
    created_at          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_reminder_tracker_recipient
        FOREIGN KEY (recipient_user_id) REFERENCES hdbhms.users (user_id),
    CONSTRAINT fk_reminder_tracker_related_task
        FOREIGN KEY (related_task_id) REFERENCES hdbhms.manager_tasks (manager_task_id)
);

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'reminder_trackers' AND INDEX_NAME = 'idx_reminder_tracker_due');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_reminder_tracker_due ON hdbhms.reminder_trackers (status, next_due_at)', 'SELECT ''idx_reminder_tracker_due exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'reminder_trackers' AND INDEX_NAME = 'idx_reminder_tracker_target');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_reminder_tracker_target ON hdbhms.reminder_trackers (reminder_key, target_type, target_id, audience)', 'SELECT ''idx_reminder_tracker_target exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'manager_tasks' AND COLUMN_NAME = 'task_type');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE hdbhms.manager_tasks ADD COLUMN task_type VARCHAR(80) NULL AFTER description', 'SELECT ''task_type exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'manager_tasks' AND COLUMN_NAME = 'idempotency_key');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE hdbhms.manager_tasks ADD COLUMN idempotency_key VARCHAR(180) NULL AFTER task_type', 'SELECT ''idempotency_key exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'manager_tasks' AND INDEX_NAME = 'idx_manager_task_type_contract');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX idx_manager_task_type_contract ON hdbhms.manager_tasks (task_type, lease_contract_id, status)', 'SELECT ''idx_manager_task_type_contract exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'manager_tasks' AND INDEX_NAME = 'uk_manager_task_idempotency_key');
SET @sql = IF(@index_exists = 0, 'CREATE UNIQUE INDEX uk_manager_task_idempotency_key ON hdbhms.manager_tasks (idempotency_key)', 'SELECT ''uk_manager_task_idempotency_key exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
