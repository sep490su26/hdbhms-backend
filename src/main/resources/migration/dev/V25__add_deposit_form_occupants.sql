ALTER TABLE deposit_forms
    ADD COLUMN occupant_count TINYINT UNSIGNED NOT NULL DEFAULT 1 AFTER payment_cycle_months;

CREATE TABLE deposit_form_co_occupants
(
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    deposit_form_id BIGINT UNSIGNED NOT NULL,
    full_name       VARCHAR(255)    NOT NULL,
    phone           VARCHAR(30)     NOT NULL,
    display_order   TINYINT UNSIGNED NOT NULL,
    UNIQUE KEY uq_deposit_form_co_occupant_order (deposit_form_id, display_order),
    KEY idx_deposit_form_co_occupants_form (deposit_form_id),
    CONSTRAINT fk_deposit_form_co_occupants_form
        FOREIGN KEY (deposit_form_id) REFERENCES deposit_forms (id)
        ON DELETE CASCADE
) ENGINE = InnoDB;
