ALTER TABLE hdbhms.room_transfer_requests
    ADD COLUMN positive_difference_settlement_type ENUM ('TENANT_PAY_MORE', 'CREDIT_NEXT_CONTRACT') NULL
        AFTER status;