CREATE TABLE IF NOT EXISTS hdbhms.deposit_contact_events
(
    deposit_contact_event_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    deposit_agreement_id     BIGINT UNSIGNED                          NOT NULL,
    outcome                  VARCHAR(30)                              NOT NULL,
    note                     TEXT                                     NOT NULL,
    contacted_by             BIGINT UNSIGNED                          NOT NULL,
    contacted_at             DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_dce_deposit
        FOREIGN KEY (deposit_agreement_id) REFERENCES hdbhms.deposit_agreements (deposit_agreement_id),
    CONSTRAINT fk_dce_user
        FOREIGN KEY (contacted_by) REFERENCES hdbhms.users (user_id)
);

CREATE INDEX idx_deposit_contact_latest
    ON hdbhms.deposit_contact_events (deposit_agreement_id, contacted_at);

UPDATE hdbhms.deposit_agreements
SET deposit_expires_at = DATE_ADD(expected_move_in_date, INTERVAL 14 DAY)
WHERE deposit_expires_at IS NULL;

UPDATE hdbhms.deposit_forms
SET deposit_expires_at = DATE_ADD(expected_move_in_date, INTERVAL 14 DAY)
WHERE deposit_expires_at IS NULL;
