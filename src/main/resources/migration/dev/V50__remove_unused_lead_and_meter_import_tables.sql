-- These legacy tables are not part of the current booking or meter-reading flows.
ALTER TABLE hdbhms.deposit_forms
    DROP FOREIGN KEY fk_dep_form_lead,
    DROP INDEX idx_deposit_form_lead,
    DROP COLUMN lead_id;

DROP TABLE hdbhms.leads;
DROP TABLE hdbhms.meter_reading_import_rows;

-- Payments now use the invoice/payment itself; no separate collection account is selected.
ALTER TABLE hdbhms.payment_transactions
    DROP FOREIGN KEY fk_pt_account,
    DROP COLUMN collection_account_id;

ALTER TABLE hdbhms.invoices
    DROP FOREIGN KEY fk_inv_account,
    DROP COLUMN collection_account_id;

ALTER TABLE hdbhms.invoice_lines
    DROP FOREIGN KEY fk_il_collection_account,
    DROP COLUMN collection_account_id;

ALTER TABLE hdbhms.invoice_payment_groups
    DROP FOREIGN KEY fk_ipg_account,
    DROP INDEX uq_invoice_payment_group,
    DROP COLUMN collection_account_id,
    ADD UNIQUE KEY uq_invoice_payment_group (invoice_id, group_type);

ALTER TABLE hdbhms.payment_intents
    DROP FOREIGN KEY fk_pi_account,
    DROP FOREIGN KEY fk_pi_ipg,
    DROP COLUMN collection_account_id,
    DROP COLUMN invoice_payment_group_id;

DROP TABLE hdbhms.collection_accounts;
DROP TABLE hdbhms.invoice_payment_groups;
