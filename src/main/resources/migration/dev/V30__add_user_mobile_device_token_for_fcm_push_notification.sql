CREATE TABLE user_mobile_device_tokens
(
    id         BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT UNSIGNED        NOT NULL,
    token      VARCHAR(500)           NOT NULL,
    platform   ENUM ('ANDROID','IOS') NOT NULL DEFAULT 'ANDROID',
    is_active  BOOLEAN                NOT NULL DEFAULT TRUE,
    created_at DATETIME(6)            NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_device_token (token),
    CONSTRAINT fk_token_user FOREIGN KEY (user_id) REFERENCES users (id)
);
ALTER TABLE notification_outbox
    ADD COLUMN is_read BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE notification_outbox
    MODIFY COLUMN channel ENUM ('PUSH','WEB','IN_APP','EMAIL','SMS') NOT NULL;
ALTER TABLE notification_outbox
    ADD COLUMN next_retry_at DATETIME(6) NULL;
ALTER TABLE notification_outbox
    MODIFY COLUMN status ENUM ('PENDING','PROCESSING','SENT','FAILED','CANCELLED','DEAD_LETTER') NOT NULL DEFAULT 'PENDING';
UPDATE notification_outbox SET status = 'DEAD_LETTER' WHERE status IN ('FAILED', 'CANCELLED');
ALTER TABLE notification_outbox
    MODIFY COLUMN status ENUM ('PENDING','PROCESSING','SENT','DEAD_LETTER') NOT NULL DEFAULT 'PENDING';
ALTER TABLE notification_outbox
    DROP INDEX idx_outbox_pending;
ALTER TABLE notification_outbox
    ADD INDEX idx_outbox_pending (status, next_retry_at);