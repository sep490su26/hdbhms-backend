ALTER TABLE hdbhms.notification_outbox
    MODIFY COLUMN recipient_user_id BIGINT UNSIGNED NULL;
