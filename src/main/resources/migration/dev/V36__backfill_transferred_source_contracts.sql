UPDATE lease_contracts lc
JOIN room_transfer_requests rtr
  ON rtr.old_contract_id = lc.lease_contract_id
SET lc.status = 'TRANSFERRED',
    lc.updated_at = CURRENT_TIMESTAMP
WHERE lc.deleted_at IS NULL
  AND rtr.status IN ('EXECUTED', 'COMPLETED')
  AND lc.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING');

UPDATE lease_contracts renewal
JOIN room_transfer_requests rtr
  ON rtr.old_contract_id = renewal.previous_contract_id
JOIN lease_contracts old_source
  ON old_source.lease_contract_id = rtr.old_contract_id
SET renewal.status = 'TRANSFERRED',
    renewal.updated_at = CURRENT_TIMESTAMP
WHERE renewal.deleted_at IS NULL
  AND rtr.status IN ('EXECUTED', 'COMPLETED')
  AND renewal.room_id = old_source.room_id
  AND (rtr.new_contract_id IS NULL OR rtr.new_contract_id <> renewal.lease_contract_id)
  AND (rtr.replacement_old_contract_id IS NULL OR rtr.replacement_old_contract_id <> renewal.lease_contract_id)
  AND renewal.status IN ('ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING');

UPDATE contract_occupants co
JOIN lease_contracts renewal
  ON renewal.lease_contract_id = co.contract_id
JOIN room_transfer_requests rtr
  ON rtr.old_contract_id = renewal.previous_contract_id
JOIN lease_contracts old_source
  ON old_source.lease_contract_id = rtr.old_contract_id
SET co.status = 'MOVED_OUT',
    co.move_out_date = COALESCE(DATE(rtr.completed_at), DATE(rtr.executed_at), rtr.requested_transfer_date, CURRENT_DATE)
WHERE co.status = 'ACTIVE'
  AND renewal.deleted_at IS NULL
  AND renewal.status = 'TRANSFERRED'
  AND rtr.status IN ('EXECUTED', 'COMPLETED')
  AND renewal.room_id = old_source.room_id
  AND (rtr.new_contract_id IS NULL OR rtr.new_contract_id <> renewal.lease_contract_id)
  AND (rtr.replacement_old_contract_id IS NULL OR rtr.replacement_old_contract_id <> renewal.lease_contract_id);
