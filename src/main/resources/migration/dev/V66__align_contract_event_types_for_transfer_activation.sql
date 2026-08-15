-- Existing databases created before the transfer lifecycle changes can still
-- have the older contract_events.event_type ENUM.
ALTER TABLE contract_events
    MODIFY COLUMN event_type ENUM (
        'CREATED',
        'SIGNED',
        'RENEWED',
        'NOTICE_SENT',
        'INTENTION_RECORDED',
        'EXPIRED',
        'LIQUIDATED',
        'AUTO_TERMINATED',
        'PRICE_CHANGED',
        'OCCUPANT_CHANGED',
        'TRANSFERRED',
        'RENEWAL_AFTER_MOVE_OUT_INTENT',
        'LIQUIDATION_PROCESSING_STARTED'
    ) NOT NULL;
