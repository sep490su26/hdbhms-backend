ALTER TABLE deposit_forms
    ADD COLUMN dob DATE NULL AFTER full_name,
    ADD COLUMN permanent_address VARCHAR(1000) NULL AFTER phone,
    ADD COLUMN id_issue_date DATE NULL AFTER id_number,
    ADD COLUMN id_issue_place VARCHAR(255) NULL AFTER id_issue_date;