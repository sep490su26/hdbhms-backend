-- =========================================================
-- V33__rename_expected_move_out_to_expected_vacant.sql
-- Purpose:
--   Align SOON_VACANT MVP DB column naming.
--   Rename lease_contracts.expected_move_out_date to expected_vacant_date.
--
-- Context:
--   V32 originally added expected_move_out_date.
--   MVP spec now standardizes DB column name as expected_vacant_date.
--   API can still use expectedMoveOutDate and map it in service layer.
-- =========================================================

ALTER TABLE lease_contracts
  DROP INDEX idx_lc_room_intention_moveout;

ALTER TABLE lease_contracts
  CHANGE COLUMN expected_move_out_date expected_vacant_date DATE NULL;

ALTER TABLE lease_contracts
  ADD KEY idx_lc_room_intention_vacant (
    room_id,
    tenant_intention,
    expected_vacant_date
  );
