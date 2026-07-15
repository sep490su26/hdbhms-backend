CREATE TABLE IF NOT EXISTS hdbhms.room_deposit_failures
(
    room_deposit_failure_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    room_id                 BIGINT UNSIGNED NOT NULL,
    room_hold_id            BIGINT UNSIGNED NULL,
    payment_intent_id       BIGINT UNSIGNED NULL,
    reason                  VARCHAR(50)  NOT NULL,
    occurred_at             DATETIME(6)  NOT NULL,
    created_at              DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_room_deposit_failure_hold UNIQUE (room_hold_id),
    CONSTRAINT fk_room_deposit_failures_room
        FOREIGN KEY (room_id) REFERENCES hdbhms.rooms (room_id),
    CONSTRAINT fk_room_deposit_failures_room_hold
        FOREIGN KEY (room_hold_id) REFERENCES hdbhms.room_holds (room_hold_id),
    CONSTRAINT fk_room_deposit_failures_payment_intent
        FOREIGN KEY (payment_intent_id) REFERENCES hdbhms.payment_intents (payment_intent_id),
    INDEX idx_room_deposit_failures_room_time (room_id, occurred_at),
    INDEX idx_room_deposit_failures_payment_intent (payment_intent_id)
);
