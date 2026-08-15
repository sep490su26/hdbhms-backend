-- Existing requests must not remain blocked by the removed target-holder step.
UPDATE hdbhms.room_transfer_requests
SET status = 'MANAGER_APPROVED',
    target_holder_approved_by = NULL,
    target_holder_approved_at = NULL,
    target_holder_rejected_at = NULL,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE status = 'WAITING_TARGET_HOLDER_APPROVAL';
