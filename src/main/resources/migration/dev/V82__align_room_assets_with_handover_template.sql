SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Add every standard handover asset to occupied rooms. Existing legacy asset
-- names remain active so historical handover references are not broken; the
-- frontend normalizes their aliases into the canonical template rows.
INSERT INTO hdbhms.room_assets
    (room_id, asset_name, asset_category, quantity, current_condition, description,
     image_file_id, created_at, updated_at, deleted_at)
SELECT DISTINCT
    r.room_id,
    assets.asset_name,
    assets.asset_category,
    assets.quantity,
    'GOOD',
    CASE
        WHEN assets.asset_description <> '' THEN assets.asset_description
        ELSE CONCAT('Tài sản mặc định của phòng ', r.room_code)
    END,
    NULL,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    NULL
FROM hdbhms.rooms r
CROSS JOIN (
    SELECT 'Điều hòa + Remote' AS asset_name, 'Thiết bị điện tử' AS asset_category, 1 AS quantity,
           '' AS asset_description
    UNION ALL SELECT 'Thiết bị vệ sinh + phòng tắm', 'Thiết bị vệ sinh', 1,
                     'Xí, vòi xịt, vòi sen, lavabo, gương, phụ kiện'
    UNION ALL SELECT 'Bình nóng lạnh', 'Thiết bị điện tử', 1, ''
    UNION ALL SELECT 'Tủ quần áo 3 buồng', 'Nội thất', 1, ''
    UNION ALL SELECT 'Bàn học', 'Nội thất', 1, ''
    UNION ALL SELECT 'Giường đôi/tầng + Dát giường', 'Nội thất', 1, ''
    UNION ALL SELECT 'Cửa đi + cửa sổ', 'Cơ sở hạ tầng', 1, ''
    UNION ALL SELECT 'Modem Internet', 'Thiết bị điện tử', 1, ''
    UNION ALL SELECT 'Hệ thống điện: công tắc, ổ cắm, bóng điện', 'Cơ sở hạ tầng', 1, ''
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
