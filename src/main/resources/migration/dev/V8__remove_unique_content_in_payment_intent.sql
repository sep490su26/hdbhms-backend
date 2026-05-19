DELIMITER //
CREATE PROCEDURE drop_uq_payment_content_if_exists()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'payment_intents'
          AND INDEX_NAME = 'uq_payment_content'
    ) THEN
        ALTER TABLE payment_intents DROP INDEX uq_payment_content;
    END IF;
END//
DELIMITER ;

CALL drop_uq_payment_content_if_exists();
DROP PROCEDURE drop_uq_payment_content_if_exists;