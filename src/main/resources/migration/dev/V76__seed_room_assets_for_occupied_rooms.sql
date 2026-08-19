SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Existing demo data used the removed holder-replacement liquidation branch.
UPDATE hdbhms.change_requests
SET title = 'Thanh lý hợp đồng phòng 507',
    description = 'Yêu cầu thanh lý theo quy trình trả phòng toàn bộ người ở.',
    request_payload = JSON_SET(
        JSON_REMOVE(
            request_payload,
            '$.liquidationMode',
            '$.leavingProfileIds',
            '$.stayingProfileIds',
            '$.replacementPrimaryTenantProfileId',
            '$.requiresReplacementContract',
            '$.roomWillRemainOccupied'
        ),
        '$.reason', 'Khách không tiếp tục thuê phòng.'
    )
WHERE request_code = 'TLHD_P507_30_07_2026'
  AND request_type = 'CONTRACT_LIQUIDATION';

-- Keep the room amenities screen useful even when a contract has no handover record yet.
INSERT INTO hdbhms.room_assets
    (room_id, asset_name, asset_category, quantity, current_condition, description,
     image_file_id, created_at, updated_at, deleted_at)
SELECT DISTINCT
    r.room_id,
    assets.asset_name,
    assets.asset_category,
    assets.quantity,
    'GOOD',
    CONCAT('Tài sản mặc định của phòng ', r.room_code),
    NULL,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    NULL
FROM hdbhms.rooms r
CROSS JOIN (
    SELECT 'Điều hòa' AS asset_name, 'APPLIANCE' AS asset_category, 1 AS quantity
    UNION ALL SELECT 'Remote điều hòa', 'ACCESSORY', 1
    UNION ALL SELECT 'Bình nóng lạnh', 'APPLIANCE', 1
    UNION ALL SELECT 'Giường', 'FURNITURE', 1
) assets
WHERE r.deleted_at IS NULL
  AND (
      r.current_status = 'OCCUPIED'
      OR EXISTS (
          SELECT 1
          FROM hdbhms.lease_contracts lc
          JOIN hdbhms.contract_occupants co
            ON co.contract_id = lc.lease_contract_id
           AND co.status = 'ACTIVE'
          WHERE lc.room_id = r.room_id
            AND lc.deleted_at IS NULL
            AND lc.status IN ('ACTIVE', 'EXPIRING_SOON', 'EXPIRED', 'TERMINATION_PENDING')
      )
  )
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.room_assets existing_asset
      WHERE existing_asset.room_id = r.room_id
        AND existing_asset.asset_name = assets.asset_name
        AND existing_asset.deleted_at IS NULL
  );
