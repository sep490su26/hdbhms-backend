ALTER TABLE hdbhms.deposit_forms
    ADD COLUMN contract_term_months INT UNSIGNED NULL AFTER deposit_months;

UPDATE hdbhms.deposit_forms
SET contract_term_months = 12
WHERE contract_term_months IS NULL;
