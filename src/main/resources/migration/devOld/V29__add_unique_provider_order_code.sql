ALTER TABLE payment_intents
    ADD UNIQUE KEY uq_payment_intents_provider_order_code (provider_order_code);
