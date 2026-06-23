ALTER TABLE payment_intents
    ADD COLUMN provider_order_code VARCHAR(255) NULL AFTER payment_content;