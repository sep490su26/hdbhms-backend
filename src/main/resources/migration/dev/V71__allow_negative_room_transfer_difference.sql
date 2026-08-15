-- A cheaper destination room produces a negative rent difference.
ALTER TABLE hdbhms.transfer_settlements
    MODIFY COLUMN difference_amount BIGINT SIGNED NOT NULL DEFAULT 0;
