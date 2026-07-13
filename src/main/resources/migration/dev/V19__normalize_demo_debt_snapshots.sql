DELIMITER //

CREATE PROCEDURE hdbhms.validate_v19_demo_debt_prerequisites()
BEGIN
    IF (SELECT COUNT(*) FROM hdbhms.properties WHERE property_code = 'HAI_DANG_1') <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V19 requires exactly one HAI_DANG_1 property';
    END IF;

    IF (
        SELECT COUNT(*)
        FROM hdbhms.rooms r
        JOIN hdbhms.properties p ON p.property_id = r.property_id
        WHERE p.property_code = 'HAI_DANG_1'
          AND r.room_code IN ('401','402','403','404','405','406','407','408','501','502','503','504','505','506','507')
          AND r.deleted_at IS NULL
    ) <> 15 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V19 requires the 15 real rooms 401-408 and 501-507';
    END IF;

    IF (
        SELECT COUNT(*)
        FROM hdbhms.debt_snapshots ds
        JOIN hdbhms.lease_contracts c ON c.lease_contract_id = ds.contract_id
        WHERE c.contract_code IN ('DEMO-LEASE-406-EXPIRING','DEMO-LEASE-501-ACTIVE','DEMO-LEASE-503-ACTIVE')
    ) <> 3 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V19 requires exactly three V18 demo debt snapshots';
    END IF;
END//

DELIMITER ;

CALL hdbhms.validate_v19_demo_debt_prerequisites();
DROP PROCEDURE hdbhms.validate_v19_demo_debt_prerequisites;

UPDATE hdbhms.debt_snapshots ds
JOIN hdbhms.lease_contracts c ON c.lease_contract_id = ds.contract_id
JOIN (
    SELECT
        source.debt_snapshot_id,
        COALESCE(SUM(CASE WHEN i.invoice_type = 'RENT' THEN i.remaining_amount ELSE 0 END), 0) AS rent_debt,
        COALESCE(SUM(CASE WHEN i.invoice_type = 'UTILITY' THEN i.remaining_amount ELSE 0 END), 0) AS utility_debt,
        COALESCE(SUM(CASE WHEN i.invoice_type NOT IN ('RENT','UTILITY','DEPOSIT') THEN i.remaining_amount ELSE 0 END), 0) AS other_debt,
        COUNT(DISTINCT CASE WHEN i.invoice_type = 'RENT' THEN i.billing_period END) AS rent_months,
        COUNT(DISTINCT CASE WHEN i.invoice_type = 'UTILITY' THEN i.billing_period END) AS utility_months
    FROM hdbhms.debt_snapshots source
    JOIN hdbhms.lease_contracts source_contract ON source_contract.lease_contract_id = source.contract_id
    LEFT JOIN hdbhms.invoices i
        ON i.lease_contract_id = source.contract_id
       AND i.remaining_amount > 0
       AND i.status IN ('ISSUED','PARTIALLY_PAID','OVERDUE')
       AND i.due_date < DATE_ADD(source.snapshot_date, INTERVAL 1 DAY)
    WHERE source_contract.contract_code IN ('DEMO-LEASE-406-EXPIRING','DEMO-LEASE-501-ACTIVE','DEMO-LEASE-503-ACTIVE')
    GROUP BY source.debt_snapshot_id
) calculated ON calculated.debt_snapshot_id = ds.debt_snapshot_id
SET
    ds.rent_debt_amount = calculated.rent_debt,
    ds.utility_debt_amount = calculated.utility_debt,
    ds.other_debt_amount = calculated.other_debt,
    ds.rent_debt_months = calculated.rent_months,
    ds.utility_debt_months = calculated.utility_months,
    ds.mixed_debt_amount = calculated.rent_debt + calculated.utility_debt + calculated.other_debt,
    ds.debt_limit_amount = c.monthly_rent * GREATEST(c.payment_cycle_months, 1) * 2 DIV 3,
    ds.is_over_limit = calculated.rent_months >= 3
        OR calculated.utility_months >= 3
        OR calculated.rent_debt + calculated.utility_debt + calculated.other_debt
            > c.monthly_rent * GREATEST(c.payment_cycle_months, 1) * 2 DIV 3
WHERE c.contract_code IN ('DEMO-LEASE-406-EXPIRING','DEMO-LEASE-501-ACTIVE','DEMO-LEASE-503-ACTIVE');
