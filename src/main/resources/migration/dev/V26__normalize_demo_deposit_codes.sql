-- Keep the applied V19 seed immutable and normalize only its demo deposit identifiers.
-- rooms.room_code is stored as 401/501; P is the business-facing room prefix.
UPDATE hdbhms.deposit_agreements da
JOIN hdbhms.rooms r ON r.room_id = da.room_id
JOIN hdbhms.properties p ON p.property_id = r.property_id
SET da.deposit_code = CONCAT(
    'HDC_P',
    r.room_code,
    '_',
    DATE_FORMAT(da.created_at, '%d.%m.%Y')
)
WHERE p.property_code = 'HAI_DANG_1'
  AND r.room_code IN (
      '401', '402', '403', '404', '405', '406', '407', '408',
      '501', '502', '503', '504', '505', '506', '507'
  )
  AND da.deposit_code LIKE 'DEMO-DEP-%';
