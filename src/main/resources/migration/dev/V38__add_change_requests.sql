CREATE TABLE change_requests
(
    id               BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    request_code     VARCHAR(80)                                                                                NOT NULL,
    request_type     ENUM (
        'METER_READING_CORRECTION',
        'INVOICE_ADJUSTMENT',
        'RENT_PRICE_ADJUSTMENT',
        'DEPOSIT_REFUND_REQUEST',
        'ROOM_TRANSFER',
        'MOVE_OUT',
        'COMPLAINT'
        )                                                                                                       NOT NULL,
    requester_id     BIGINT UNSIGNED                                                                            NOT NULL,
    requester_role   ENUM ('TENANT','MANAGER','ACCOUNTANT')                                                     NOT NULL,
    target_type      ENUM ('METER_READING','INVOICE','CONTRACT','DEPOSIT','OTHER')                              NOT NULL,
    target_id        BIGINT UNSIGNED                                                                            NULL,
    title            VARCHAR(255)                                                                               NOT NULL,
    description      TEXT                                                                                       NOT NULL,
    request_payload  JSON                                                                                       NULL,
    evidence_file_id BIGINT UNSIGNED                                                                            NULL,
    assigned_role    ENUM ('OWNER','MANAGER','ACCOUNTANT')                                                      NOT NULL,
    assigned_to      BIGINT UNSIGNED                                                                            NULL,
    status           ENUM ('PENDING','UNDER_REVIEW','APPROVED','REJECTED','PROCESSING','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    resolution_note  TEXT                                                                                       NULL,
    resolved_by      BIGINT UNSIGNED                                                                            NULL,
    resolved_at      DATETIME(6)                                                                                NULL,
    created_at       DATETIME(6)                                                                                NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)                                                                                NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_request_code (request_code),
    KEY idx_request_status (status, created_at),
    KEY idx_request_type (request_type, status),
    KEY idx_request_requester (requester_id, status),
    KEY idx_request_target (target_type, target_id),
    CONSTRAINT fk_cr_requester FOREIGN KEY (requester_id) REFERENCES users (id),
    CONSTRAINT fk_cr_assigned FOREIGN KEY (assigned_to) REFERENCES users (id),
    CONSTRAINT fk_cr_resolved FOREIGN KEY (resolved_by) REFERENCES users (id),
    CONSTRAINT fk_cr_evidence FOREIGN KEY (evidence_file_id) REFERENCES file_metadata (id)
) ENGINE = InnoDB;

CREATE TABLE change_request_events
(
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    request_id  BIGINT UNSIGNED NOT NULL,
    from_status VARCHAR(50)     NULL,
    to_status   VARCHAR(50)     NOT NULL,
    note        TEXT            NULL,
    acted_by    BIGINT UNSIGNED NULL,
    acted_at    DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_cre_request (request_id, acted_at),
    CONSTRAINT fk_cre_request FOREIGN KEY (request_id) REFERENCES change_requests (id),
    CONSTRAINT fk_cre_user FOREIGN KEY (acted_by) REFERENCES users (id)
) ENGINE = InnoDB;

UPDATE room_transfer_requests
SET status = 'OLD_ROOM_HANDOVER'
WHERE status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED');

DELIMITER //
CREATE PROCEDURE drop_transfer_fks_if_exist()
BEGIN
    IF EXISTS (SELECT 1
               FROM information_schema.TABLE_CONSTRAINTS
               WHERE CONSTRAINT_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'room_transfer_requests'
                 AND CONSTRAINT_NAME = 'fk_tr_approved'
                 AND CONSTRAINT_TYPE = 'FOREIGN KEY') THEN
        ALTER TABLE room_transfer_requests
            DROP FOREIGN KEY fk_tr_approved;
    END IF;
END//
DELIMITER ;

CALL drop_transfer_fks_if_exist();
DROP PROCEDURE drop_transfer_fks_if_exist;

DELIMITER //
CREATE PROCEDURE drop_transfer_obsolete_columns()
BEGIN
    IF EXISTS (SELECT 1
               FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'room_transfer_requests'
                 AND COLUMN_NAME = 'approved_by') THEN
        ALTER TABLE room_transfer_requests
            DROP COLUMN approved_by;
    END IF;

    IF EXISTS (SELECT 1
               FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'room_transfer_requests'
                 AND COLUMN_NAME = 'approved_at') THEN
        ALTER TABLE room_transfer_requests
            DROP COLUMN approved_at;
    END IF;

    IF EXISTS (SELECT 1
               FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'room_transfer_requests'
                 AND COLUMN_NAME = 'rejection_reason') THEN
        ALTER TABLE room_transfer_requests
            DROP COLUMN rejection_reason;
    END IF;

    IF EXISTS (SELECT 1
               FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'room_transfer_requests'
                 AND COLUMN_NAME = 'eligibility_checked_at') THEN
        ALTER TABLE room_transfer_requests
            DROP COLUMN eligibility_checked_at;
    END IF;

    IF EXISTS (SELECT 1
               FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'room_transfer_requests'
                 AND COLUMN_NAME = 'is_eligible_at_creation') THEN
        ALTER TABLE room_transfer_requests
            DROP COLUMN is_eligible_at_creation;
    END IF;

    IF EXISTS (SELECT 1
               FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = 'room_transfer_requests'
                 AND COLUMN_NAME = 'eligibility_snapshot') THEN
        ALTER TABLE room_transfer_requests
            DROP COLUMN eligibility_snapshot;
    END IF;
END//
DELIMITER ;

CALL drop_transfer_obsolete_columns();
DROP PROCEDURE drop_transfer_obsolete_columns;

ALTER TABLE room_transfer_requests
    MODIFY COLUMN status ENUM (
        'OLD_ROOM_HANDOVER',
        'SETTLEMENT_PENDING',
        'NEW_CONTRACT_CREATED',
        'COMPLETED'
        ) NOT NULL DEFAULT 'OLD_ROOM_HANDOVER';