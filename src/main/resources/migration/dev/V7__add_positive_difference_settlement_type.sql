ALTER TABLE hdbhms.transfer_settlements
    ADD COLUMN positive_difference_settlement_type ENUM ('TENANT_PAY_MORE', 'CREDIT_NEXT_CONTRACT') NULL
        AFTER settlement_type;