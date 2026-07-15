ALTER TABLE notification_outbox
    ADD COLUMN read_at DATETIME(6) NULL AFTER is_read;

UPDATE notification_outbox
SET read_at = COALESCE(sent_at, created_at)
WHERE is_read = TRUE
  AND read_at IS NULL;
