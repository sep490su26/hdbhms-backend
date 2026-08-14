ALTER TABLE lease_contracts
    ADD COLUMN activation_electricity_value DECIMAL(12, 3) NULL AFTER rent_start_date,
    ADD COLUMN activation_reading_date DATE NULL AFTER activation_electricity_value;
