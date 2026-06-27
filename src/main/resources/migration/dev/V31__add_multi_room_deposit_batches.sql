-- V31: Support multi-room deposit batch checkout
-- Goal: one customer can reserve multiple rooms with one total invoice/payment intent,
-- while still keeping one deposit_form and one deposit_agreement per room.

CREATE TABLE deposit_batches
(
    id                       BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    batch_code               VARCHAR(80)  NOT NULL,
    property_id              BIGINT UNSIGNED NOT NULL,

    full_name                VARCHAR(255) NOT NULL,
    phone                    VARCHAR(30)  NOT NULL,
    email                    VARCHAR(255) NULL,
    id_number                VARCHAR(50)  NULL,

    expected_move_in_date    DATE         NOT NULL,
    expected_lease_sign_date DATE         NOT NULL,
    total_deposit_amount     BIGINT UNSIGNED NOT NULL DEFAULT 0,
    invoice_id               BIGINT UNSIGNED NULL,
    payment_intent_id        BIGINT UNSIGNED NULL,

    status                   ENUM (
        'DRAFT',
        'PENDING_PAYMENT',
        'PAID',
        'CONFIRMED',
        'EXPIRED',
        'CANCELLED',
        'REFUND_REQUIRED',
        'REFUNDED'
    ) NOT NULL DEFAULT 'DRAFT',

    created_at               DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at               DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version                  BIGINT UNSIGNED NOT NULL DEFAULT 0,

    UNIQUE KEY uq_deposit_batch_code (batch_code),
    KEY idx_deposit_batch_property (property_id),
    KEY idx_deposit_batch_status (status),
    KEY idx_deposit_batch_phone (phone),
    KEY idx_deposit_batch_expected_move_in (expected_move_in_date),

    CONSTRAINT fk_deposit_batch_property
        FOREIGN KEY (property_id) REFERENCES properties (id)
) ENGINE = InnoDB;

CREATE TABLE deposit_batch_items
(
    id                   BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    batch_id             BIGINT UNSIGNED NOT NULL,
    room_id              BIGINT UNSIGNED NOT NULL,
    room_hold_id         BIGINT UNSIGNED NULL,
    deposit_form_id      BIGINT UNSIGNED NULL,
    deposit_agreement_id BIGINT UNSIGNED NULL,

    deposit_amount       BIGINT UNSIGNED NOT NULL DEFAULT 0,
    occupant_count       INT UNSIGNED NOT NULL DEFAULT 1,

    status               ENUM (
        'PENDING_PAYMENT',
        'PAID',
        'CONFIRMED',
        'EXPIRED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'PENDING_PAYMENT',

    created_at           DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version              BIGINT UNSIGNED NOT NULL DEFAULT 0,

    UNIQUE KEY uq_deposit_batch_item_room (batch_id, room_id),
    UNIQUE KEY uq_deposit_batch_item_hold (room_hold_id),
    UNIQUE KEY uq_deposit_batch_item_form (deposit_form_id),
    UNIQUE KEY uq_deposit_batch_item_agreement (deposit_agreement_id),
    KEY idx_deposit_batch_item_batch_status (batch_id, status),
    KEY idx_deposit_batch_item_room_status (room_id, status),

    CONSTRAINT fk_deposit_batch_item_batch
        FOREIGN KEY (batch_id) REFERENCES deposit_batches (id),
    CONSTRAINT fk_deposit_batch_item_room
        FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_deposit_batch_item_hold
        FOREIGN KEY (room_hold_id) REFERENCES room_holds (id),
    CONSTRAINT fk_deposit_batch_item_form
        FOREIGN KEY (deposit_form_id) REFERENCES deposit_forms (id),
    CONSTRAINT fk_deposit_batch_item_agreement
        FOREIGN KEY (deposit_agreement_id) REFERENCES deposit_agreements (id),

    CONSTRAINT chk_deposit_batch_item_occupant_count
        CHECK (occupant_count > 0)
) ENGINE = InnoDB;

ALTER TABLE payment_intents
    ADD COLUMN deposit_batch_id BIGINT UNSIGNED NULL AFTER deposit_agreement_id,
    MODIFY COLUMN status ENUM (
        'CREATED','PENDING','SUCCEEDED','FAILED','EXPIRED','CANCELLED','REFUND_REQUIRED'
    ) NOT NULL DEFAULT 'CREATED',
    ADD KEY idx_payment_intents_deposit_batch (deposit_batch_id),
    ADD CONSTRAINT fk_payment_intents_deposit_batch
        FOREIGN KEY (deposit_batch_id) REFERENCES deposit_batches (id);

ALTER TABLE invoices
    ADD COLUMN deposit_batch_id BIGINT UNSIGNED NULL AFTER deposit_agreement_id,
    ADD KEY idx_invoices_deposit_batch (deposit_batch_id),
    ADD CONSTRAINT fk_invoices_deposit_batch
        FOREIGN KEY (deposit_batch_id) REFERENCES deposit_batches (id);

ALTER TABLE deposit_batches
    ADD UNIQUE KEY uq_deposit_batch_invoice (invoice_id),
    ADD UNIQUE KEY uq_deposit_batch_payment_intent (payment_intent_id),
    ADD CONSTRAINT fk_deposit_batch_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoices (id),
    ADD CONSTRAINT fk_deposit_batch_payment_intent
        FOREIGN KEY (payment_intent_id) REFERENCES payment_intents (id);

ALTER TABLE lease_contracts
    ADD UNIQUE KEY uq_lease_contracts_deposit_agreement (deposit_agreement_id);
