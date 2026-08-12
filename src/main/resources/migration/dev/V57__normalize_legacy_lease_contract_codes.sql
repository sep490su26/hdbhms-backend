-- Keep contracts created before the filename-compatible code format aligned
-- with the same HDT_P{room}_{dd_MM_yyyy} convention used by PDF exports.
CREATE TEMPORARY TABLE tmp_legacy_contract_code_map
(
    old_code VARCHAR(80) NOT NULL PRIMARY KEY,
    new_code VARCHAR(80) NOT NULL UNIQUE
);

INSERT INTO tmp_legacy_contract_code_map (old_code, new_code)
SELECT contract.contract_code,
       CONCAT(
           'HDT_P',
           room.room_code,
           '_',
           DATE_FORMAT(contract.start_date, '%d_%m_%Y')
       )
FROM hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
WHERE contract.contract_code LIKE 'HD-2026-H%'
  AND contract.start_date IS NOT NULL
  AND contract.deleted_at IS NULL;

UPDATE hdbhms.lease_contracts contract
JOIN tmp_legacy_contract_code_map code_map
  ON code_map.old_code = contract.contract_code
SET contract.contract_code = code_map.new_code,
    contract.updated_at = CURRENT_TIMESTAMP;

UPDATE hdbhms.manager_tasks task
JOIN tmp_legacy_contract_code_map code_map
  ON LOCATE(code_map.old_code, task.title) > 0
  OR LOCATE(code_map.old_code, task.description) > 0
  OR LOCATE(code_map.old_code, task.idempotency_key) > 0
SET task.title = REPLACE(task.title, code_map.old_code, code_map.new_code),
    task.description = REPLACE(task.description, code_map.old_code, code_map.new_code),
    task.idempotency_key = REPLACE(task.idempotency_key, code_map.old_code, code_map.new_code);

UPDATE hdbhms.notification_outbox notification
JOIN tmp_legacy_contract_code_map code_map
  ON LOCATE(code_map.old_code, notification.title) > 0
  OR LOCATE(code_map.old_code, notification.body) > 0
  OR LOCATE(code_map.old_code, notification.payload) > 0
SET notification.title = REPLACE(notification.title, code_map.old_code, code_map.new_code),
    notification.body = REPLACE(notification.body, code_map.old_code, code_map.new_code),
    notification.payload = REPLACE(notification.payload, code_map.old_code, code_map.new_code);

UPDATE hdbhms.change_requests request
JOIN tmp_legacy_contract_code_map code_map
  ON LOCATE(code_map.old_code, request.title) > 0
  OR LOCATE(code_map.old_code, request.description) > 0
  OR LOCATE(code_map.old_code, request.request_payload) > 0
SET request.title = REPLACE(request.title, code_map.old_code, code_map.new_code),
    request.description = REPLACE(request.description, code_map.old_code, code_map.new_code),
    request.request_payload = REPLACE(request.request_payload, code_map.old_code, code_map.new_code);

DROP TEMPORARY TABLE IF EXISTS tmp_legacy_contract_code_map;
