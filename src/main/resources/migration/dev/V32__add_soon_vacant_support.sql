-- =========================================================
-- V32__add_soon_vacant_support.sql
-- Purpose:
--   Support SOON_VACANT MVP flow with structured tenant intention data.
--   SOON_VACANT is only a planned vacancy state, not liquidation.
--
-- Changes:
--   1) lease_contracts:
--      - tenant_intention
--      - expected_move_out_date
--      - intention_recorded_at
--   2) contract_events.event_type:
--      - add RENEWAL_AFTER_MOVE_OUT_INTENT
--   3) indexes for SOON_VACANT/intention queries
-- =========================================================

ALTER TABLE lease_contracts
  ADD COLUMN tenant_intention ENUM('UNDECIDED','RENEW','MOVE_OUT','TRANSFER') NULL AFTER status,
  ADD COLUMN expected_move_out_date DATE NULL AFTER tenant_intention,
  ADD COLUMN intention_recorded_at DATETIME(6) NULL AFTER expected_move_out_date,
  ADD KEY idx_lc_room_intention_moveout (
    room_id,
    tenant_intention,
    expected_move_out_date
  ),
  ADD KEY idx_lc_intention_recorded (
    tenant_intention,
    intention_recorded_at
  );

ALTER TABLE contract_events
  MODIFY COLUMN event_type ENUM(
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
    'RENEWAL_AFTER_MOVE_OUT_INTENT'
  ) NOT NULL;
