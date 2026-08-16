SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Align the maintenance tickets seeded by V61 with the runtime code format.
-- Keep the repair idempotent so it is safe for an already-seeded dev database.
UPDATE hdbhms.maintenance_tickets
SET ticket_code = CASE ticket_code
    WHEN '#SC-0101' THEN 'SC_P101_13_08_2026'
    WHEN '#SC-0102' THEN 'SC_P102_12_08_2026'
    WHEN '#SC-0103' THEN 'SC_P103_10_08_2026'
    WHEN '#SC-0104' THEN 'SC_P104_08_08_2026'
    WHEN '#SC-0105' THEN 'SC_P105_05_08_2026'
    WHEN '#SC-0106' THEN 'SC_P106_04_08_2026'
    WHEN '#SC-0201' THEN 'SC_P201_03_08_2026'
    WHEN '#SC-0202' THEN 'SC_P202_01_08_2026'
    WHEN '#SC-KVC-01' THEN 'SC_COMMON_07_08_2026'
    WHEN '#SC-KVC-02' THEN 'SC_COMMON_09_08_2026'
    WHEN '#SC-NB-01' THEN 'SC_COMMON_11_08_2026'
    WHEN '#SC-NB-02' THEN 'SC_COMMON_28_07_2026'
    WHEN '#SC-NB-03' THEN 'SC_COMMON_14_08_2026'
    ELSE ticket_code
END
WHERE ticket_code IN (
    '#SC-0101', '#SC-0102', '#SC-0103', '#SC-0104', '#SC-0105', '#SC-0106',
    '#SC-0201', '#SC-0202', '#SC-KVC-01', '#SC-KVC-02',
    '#SC-NB-01', '#SC-NB-02', '#SC-NB-03'
);
