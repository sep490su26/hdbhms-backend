-- The tenant confirmation step is no longer part of the room-transfer flow.
-- Keep the enum value for read compatibility, but move existing rows forward.

UPDATE hdbhms.room_transfer_requests request
SET request.status = 'WAITING_PAYMENT',
    request.updated_at = CURRENT_TIMESTAMP(6)
WHERE request.status IN (
    'WAITING_HOLDER_RESPONSE',
    'WAITING_TENANT_CONFIRMATION',
    'WAITING_TARGET_HOLDER_APPROVAL'
)
  AND EXISTS (
      SELECT 1
      FROM hdbhms.transfer_settlements settlement
      JOIN hdbhms.invoices invoice
        ON invoice.invoice_id = settlement.transfer_difference_invoice_id
      WHERE settlement.transfer_request_id = request.room_transfer_request_id
        AND settlement.transfer_difference_invoice_id IS NOT NULL
        AND invoice.status <> 'VOIDED'
  );

UPDATE hdbhms.room_transfer_requests request
SET request.status = 'WAITING_SIGNING',
    request.updated_at = CURRENT_TIMESTAMP(6)
WHERE request.status IN (
    'WAITING_HOLDER_RESPONSE',
    'WAITING_TENANT_CONFIRMATION',
    'WAITING_TARGET_HOLDER_APPROVAL'
)
  AND request.new_contract_id IS NOT NULL;

UPDATE hdbhms.room_transfer_requests
SET status = 'MANAGER_APPROVED',
    updated_at = CURRENT_TIMESTAMP(6)
WHERE status IN (
    'WAITING_HOLDER_RESPONSE',
    'WAITING_TENANT_CONFIRMATION',
    'WAITING_TARGET_HOLDER_APPROVAL'
);
