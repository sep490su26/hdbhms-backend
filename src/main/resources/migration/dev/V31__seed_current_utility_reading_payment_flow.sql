SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS hdbhms.seed_current_utility_reading_payment_flow_v31;

DELIMITER //

CREATE PROCEDURE hdbhms.seed_current_utility_reading_payment_flow_v31()
BEGIN
    SET @property_id := (SELECT property_id FROM hdbhms.properties WHERE property_code = 'HAI_DANG_1' LIMIT 1);
    SET @manager_id := (SELECT user_id FROM hdbhms.users WHERE email = 'demo.manager@hdbhms.local' AND deleted_at IS NULL LIMIT 1);
    SET @utility_account := (
        SELECT collection_account_id
        FROM hdbhms.collection_accounts
        WHERE property_id = @property_id
          AND account_number = 'DEMO-UTILITY-001'
          AND status = 'ACTIVE'
        LIMIT 1
    );

    SET @period_for_reading := DATE_FORMAT(CURDATE(), '%m-%Y');
    SET @period_for_invoice := DATE_FORMAT(CURDATE(), '%Y-%m');
    SET @period_start := STR_TO_DATE(CONCAT(@period_for_invoice, '-01'), '%Y-%m-%d');
    SET @now := NOW(6);
    SET @due_at := DATE_ADD(@now, INTERVAL 7 DAY);

    SET @r405 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '405' AND deleted_at IS NULL LIMIT 1);
    SET @r506 := (SELECT room_id FROM hdbhms.rooms WHERE property_id = @property_id AND room_code = '506' AND deleted_at IS NULL LIMIT 1);
    SET @c405 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'DEMO-LEASE-405-ACTIVE' AND deleted_at IS NULL LIMIT 1);
    SET @c506 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'DEMO-LEASE-506-RENEWED' AND deleted_at IS NULL LIMIT 1);
    SET @u405 := (SELECT user_id FROM hdbhms.users WHERE email = 'demo.tenant405@hdbhms.local' AND deleted_at IS NULL LIMIT 1);
    SET @u506 := (SELECT user_id FROM hdbhms.users WHERE email = 'demo.tenant506@hdbhms.local' AND deleted_at IS NULL LIMIT 1);

    IF @property_id IS NOT NULL
       AND @manager_id IS NOT NULL
       AND @utility_account IS NOT NULL
       AND @r405 IS NOT NULL
       AND @r506 IS NOT NULL
       AND @c405 IS NOT NULL
       AND @c506 IS NOT NULL
       AND @u405 IS NOT NULL
       AND @u506 IS NOT NULL
       AND NOT EXISTS (
           SELECT 1
           FROM hdbhms.utility_billing_runs
           WHERE property_id = @property_id
             AND billing_period = @period_for_invoice
             AND invoice_reason = 'MONTHLY'
       )
       AND NOT EXISTS (
           SELECT 1
           FROM hdbhms.invoices
           WHERE lease_contract_id IN (@c405, @c506)
             AND billing_period = @period_for_invoice
             AND invoice_type = 'UTILITY'
             AND status <> 'VOIDED'
       )
    THEN
        INSERT INTO hdbhms.meters (room_id, meter_type, meter_code, status, installed_at, created_at)
        SELECT @r405, d.meter_type, CONCAT('DEMO-', d.prefix, '-405'), 'ACTIVE', CURDATE(), @now
        FROM (
            SELECT 'ELECTRICITY' AS meter_type, 'E' AS prefix
            UNION ALL
            SELECT 'WATER', 'W'
        ) d
        WHERE NOT EXISTS (
            SELECT 1
            FROM hdbhms.meters m
            WHERE m.room_id = @r405
              AND m.meter_type = d.meter_type
              AND m.status = 'ACTIVE'
        );

        INSERT INTO hdbhms.meters (room_id, meter_type, meter_code, status, installed_at, created_at)
        SELECT @r506, d.meter_type, CONCAT('DEMO-', d.prefix, '-506'), 'ACTIVE', CURDATE(), @now
        FROM (
            SELECT 'ELECTRICITY' AS meter_type, 'E' AS prefix
            UNION ALL
            SELECT 'WATER', 'W'
        ) d
        WHERE NOT EXISTS (
            SELECT 1
            FROM hdbhms.meters m
            WHERE m.room_id = @r506
              AND m.meter_type = d.meter_type
              AND m.status = 'ACTIVE'
        );

        SET @m405e := (SELECT meter_id FROM hdbhms.meters WHERE room_id = @r405 AND meter_type = 'ELECTRICITY' AND status = 'ACTIVE' LIMIT 1);
        SET @m405w := (SELECT meter_id FROM hdbhms.meters WHERE room_id = @r405 AND meter_type = 'WATER' AND status = 'ACTIVE' LIMIT 1);
        SET @m506e := (SELECT meter_id FROM hdbhms.meters WHERE room_id = @r506 AND meter_type = 'ELECTRICITY' AND status = 'ACTIVE' LIMIT 1);
        SET @m506w := (SELECT meter_id FROM hdbhms.meters WHERE room_id = @r506 AND meter_type = 'WATER' AND status = 'ACTIVE' LIMIT 1);

        INSERT INTO hdbhms.meter_reading_batches
            (property_id, reading_period, status, imported_file_id, created_by, confirmed_by, confirmed_at, created_at, total_rooms, completed_rooms, anomaly_count)
        SELECT @property_id, @period_for_reading, 'CONFIRMED', NULL, @manager_id, @manager_id, @now, @now, 2, 2, 0
        WHERE NOT EXISTS (
            SELECT 1
            FROM hdbhms.meter_reading_batches
            WHERE property_id = @property_id
              AND reading_period = @period_for_reading
        );

        SET @batch_id := (
            SELECT meter_reading_batch_id
            FROM hdbhms.meter_reading_batches
            WHERE property_id = @property_id
              AND reading_period = @period_for_reading
            ORDER BY meter_reading_batch_id DESC
            LIMIT 1
        );

        SET @e405_prev := COALESCE((
            SELECT current_value
            FROM hdbhms.meter_readings
            WHERE meter_id = @m405e
              AND status = 'CONFIRMED'
              AND reading_date < @period_start
            ORDER BY reading_date DESC, meter_reading_id DESC
            LIMIT 1
        ), 4147.000);
        SET @w405_prev := COALESCE((
            SELECT current_value
            FROM hdbhms.meter_readings
            WHERE meter_id = @m405w
              AND status = 'CONFIRMED'
              AND reading_date < @period_start
            ORDER BY reading_date DESC, meter_reading_id DESC
            LIMIT 1
        ), 426.000);
        SET @e506_prev := COALESCE((
            SELECT current_value
            FROM hdbhms.meter_readings
            WHERE meter_id = @m506e
              AND status = 'CONFIRMED'
              AND reading_date < @period_start
            ORDER BY reading_date DESC, meter_reading_id DESC
            LIMIT 1
        ), 5157.000);
        SET @w506_prev := COALESCE((
            SELECT current_value
            FROM hdbhms.meter_readings
            WHERE meter_id = @m506w
              AND status = 'CONFIRMED'
              AND reading_date < @period_start
            ORDER BY reading_date DESC, meter_reading_id DESC
            LIMIT 1
        ), 527.000);

        SET @e405_usage := 42.000;
        SET @w405_usage := 9.000;
        SET @e506_usage := 36.000;
        SET @w506_usage := 7.000;

        INSERT INTO hdbhms.meter_readings
            (batch_id, meter_id, room_id, reading_period, revision_no, previous_value, current_value, reading_date, photo_file_id, status, void_reason, created_by, created_at, purpose, source, review_status, review_count)
        SELECT @batch_id, x.meter_id, x.room_id, @period_for_reading, 1, x.previous_value, x.current_value, CURDATE(), NULL, 'CONFIRMED', NULL, @manager_id, @now, 'MONTHLY', 'MANUAL', 'NONE', 0
        FROM (
            SELECT @m405e AS meter_id, @r405 AS room_id, @e405_prev AS previous_value, @e405_prev + @e405_usage AS current_value
            UNION ALL
            SELECT @m405w, @r405, @w405_prev, @w405_prev + @w405_usage
            UNION ALL
            SELECT @m506e, @r506, @e506_prev, @e506_prev + @e506_usage
            UNION ALL
            SELECT @m506w, @r506, @w506_prev, @w506_prev + @w506_usage
        ) x
        WHERE x.meter_id IS NOT NULL
          AND NOT EXISTS (
              SELECT 1
              FROM hdbhms.meter_readings mr
              WHERE mr.meter_id = x.meter_id
                AND mr.reading_period = @period_for_reading
                AND mr.status <> 'VOIDED'
          );

        SET @mr405e := (SELECT meter_reading_id FROM hdbhms.meter_readings WHERE meter_id = @m405e AND reading_period = @period_for_reading AND status = 'CONFIRMED' LIMIT 1);
        SET @mr405w := (SELECT meter_reading_id FROM hdbhms.meter_readings WHERE meter_id = @m405w AND reading_period = @period_for_reading AND status = 'CONFIRMED' LIMIT 1);
        SET @mr506e := (SELECT meter_reading_id FROM hdbhms.meter_readings WHERE meter_id = @m506e AND reading_period = @period_for_reading AND status = 'CONFIRMED' LIMIT 1);
        SET @mr506w := (SELECT meter_reading_id FROM hdbhms.meter_readings WHERE meter_id = @m506w AND reading_period = @period_for_reading AND status = 'CONFIRMED' LIMIT 1);

        IF @mr405e IS NOT NULL
           AND @mr405w IS NOT NULL
           AND @mr506e IS NOT NULL
           AND @mr506w IS NOT NULL
        THEN
            SET @electricity_price := COALESCE((
                SELECT unit_price
                FROM hdbhms.utility_tariffs
                WHERE property_id = @property_id
                  AND utility_type = 'ELECTRICITY'
                  AND effective_from <= CURDATE()
                  AND (effective_to IS NULL OR effective_to >= CURDATE())
                ORDER BY effective_from DESC, utility_tariff_id DESC
                LIMIT 1
            ), 3500);
            SET @water_price := COALESCE((
                SELECT unit_price
                FROM hdbhms.utility_tariffs
                WHERE property_id = @property_id
                  AND utility_type = 'WATER'
                  AND effective_from <= CURDATE()
                  AND (effective_to IS NULL OR effective_to >= CURDATE())
                ORDER BY effective_from DESC, utility_tariff_id DESC
                LIMIT 1
            ), 20000);
            SET @water_free := COALESCE((
                SELECT free_allowance
                FROM hdbhms.utility_tariffs
                WHERE property_id = @property_id
                  AND utility_type = 'WATER'
                  AND effective_from <= CURDATE()
                  AND (effective_to IS NULL OR effective_to >= CURDATE())
                ORDER BY effective_from DESC, utility_tariff_id DESC
                LIMIT 1
            ), 6);

            SET @e405_qty := CEIL(@e405_usage);
            SET @w405_qty := GREATEST(CEIL(@w405_usage - @water_free), 0);
            SET @e506_qty := CEIL(@e506_usage);
            SET @w506_qty := GREATEST(CEIL(@w506_usage - @water_free), 0);
            SET @e405_amount := @e405_qty * @electricity_price;
            SET @w405_amount := @w405_qty * @water_price;
            SET @e506_amount := @e506_qty * @electricity_price;
            SET @w506_amount := @w506_qty * @water_price;
            SET @total405 := @e405_amount + @w405_amount;
            SET @total506 := @e506_amount + @w506_amount;

            INSERT INTO hdbhms.utility_billing_runs
                (property_id, billing_period, invoice_reason, status, total_rooms, ready_count, warning_count, skipped_count, generated_invoice_count, subtotal_amount, discount_amount, total_amount, created_by, generated_by, generated_at, created_at, updated_at)
            VALUES
                (@property_id, @period_for_invoice, 'MONTHLY', 'INVOICES_CREATED', 2, 0, 0, 0, 2, @total405 + @total506, 0, @total405 + @total506, @manager_id, @manager_id, @now, @now, @now);

            SET @run_id := LAST_INSERT_ID();

            INSERT INTO hdbhms.invoices
                (invoice_code, property_id, room_id, lease_contract_id, deposit_agreement_id, deposit_batch_id, invoice_type, revision_no, billing_period, invoice_reason, issue_date, due_date, status, subtotal_amount, discount_amount, total_amount, paid_amount, remaining_amount, collection_account_id, created_by, issued_at, voided_at, void_reason, created_at, updated_at, version)
            VALUES
                (CONCAT('DEMO-INV-405-', @period_for_invoice, '-UTILITY-CURRENT'), @property_id, @r405, @c405, NULL, NULL, 'UTILITY', 1, @period_for_invoice, 'MONTHLY', @now, @due_at, 'ISSUED', @total405, 0, @total405, 0, @total405, @utility_account, @manager_id, @now, NULL, NULL, @now, @now, 0),
                (CONCAT('DEMO-INV-506-', @period_for_invoice, '-UTILITY-PAID'), @property_id, @r506, @c506, NULL, NULL, 'UTILITY', 1, @period_for_invoice, 'MONTHLY', @now, @due_at, 'PAID', @total506, 0, @total506, @total506, 0, @utility_account, @manager_id, @now, NULL, NULL, @now, @now, 0);

            SET @inv405 := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code = CONCAT('DEMO-INV-405-', @period_for_invoice, '-UTILITY-CURRENT') LIMIT 1);
            SET @inv506 := (SELECT invoice_id FROM hdbhms.invoices WHERE invoice_code = CONCAT('DEMO-INV-506-', @period_for_invoice, '-UTILITY-PAID') LIMIT 1);

            INSERT INTO hdbhms.utility_billing_run_items
                (run_id, room_id, lease_contract_id, electricity_reading_id, water_reading_id, electricity_previous, electricity_current, electricity_usage, electricity_quantity, electricity_unit_price, electricity_amount, water_previous, water_current, water_usage, water_quantity, water_unit_price, water_amount, subtotal_amount, discount_amount, total_amount, warning_message, adjustment_reason, status, invoice_id)
            VALUES
                (@run_id, @r405, @c405, @mr405e, @mr405w, @e405_prev, @e405_prev + @e405_usage, @e405_usage, @e405_qty, @electricity_price, @e405_amount, @w405_prev, @w405_prev + @w405_usage, @w405_usage, @w405_qty, @water_price, @w405_amount, @total405, 0, @total405, NULL, NULL, 'INVOICED', @inv405),
                (@run_id, @r506, @c506, @mr506e, @mr506w, @e506_prev, @e506_prev + @e506_usage, @e506_usage, @e506_qty, @electricity_price, @e506_amount, @w506_prev, @w506_prev + @w506_usage, @w506_usage, @w506_qty, @water_price, @w506_amount, @total506, 0, @total506, NULL, NULL, 'INVOICED', @inv506);

            SET @item405 := (SELECT utility_billing_run_item_id FROM hdbhms.utility_billing_run_items WHERE run_id = @run_id AND room_id = @r405 LIMIT 1);
            SET @item506 := (SELECT utility_billing_run_item_id FROM hdbhms.utility_billing_run_items WHERE run_id = @run_id AND room_id = @r506 LIMIT 1);

            INSERT INTO hdbhms.invoice_lines
                (invoice_id, line_type, description, quantity, unit_price, meter_reading_id, source_type, source_id, collection_account_id, created_at)
            VALUES
                (@inv405, 'ELECTRICITY', CONCAT('Electricity ', @period_for_invoice, ': ', @e405_prev, ' -> ', @e405_prev + @e405_usage), @e405_qty, @electricity_price, @mr405e, 'UTILITY_BILLING_RUN_ITEM', @item405, @utility_account, @now),
                (@inv405, 'WATER', CONCAT('Water ', @period_for_invoice, ': ', @w405_prev, ' -> ', @w405_prev + @w405_usage), @w405_qty, @water_price, @mr405w, 'UTILITY_BILLING_RUN_ITEM', @item405, @utility_account, @now),
                (@inv506, 'ELECTRICITY', CONCAT('Electricity ', @period_for_invoice, ': ', @e506_prev, ' -> ', @e506_prev + @e506_usage), @e506_qty, @electricity_price, @mr506e, 'UTILITY_BILLING_RUN_ITEM', @item506, @utility_account, @now),
                (@inv506, 'WATER', CONCAT('Water ', @period_for_invoice, ': ', @w506_prev, ' -> ', @w506_prev + @w506_usage), @w506_qty, @water_price, @mr506w, 'UTILITY_BILLING_RUN_ITEM', @item506, @utility_account, @now);

            INSERT INTO hdbhms.payment_intents
                (invoice_id, deposit_agreement_id, deposit_batch_id, invoice_payment_group_id, amount, provider, collection_account_id, payment_content, provider_order_code, qr_payload, status, expires_at, created_at)
            VALUES
                (@inv405, NULL, NULL, NULL, @total405, 'PAYOS', @utility_account, CONCAT('DEMO UTL 405 ', @period_for_invoice), CONCAT('DEMO-ORDER-405-', REPLACE(@period_for_invoice, '-', ''), '-UTILITY'), JSON_OBJECT(
                    'checkoutUrl', 'http://localhost:3000/demo-payments/utility-405',
                    'qrCode', CONCAT('DEMO-QR-UTILITY-405-', @period_for_invoice),
                    'providerOrderCode', CONCAT('DEMO-ORDER-405-', REPLACE(@period_for_invoice, '-', ''), '-UTILITY'),
                    'paymentLinkId', CONCAT('DEMO-LINK-405-', REPLACE(@period_for_invoice, '-', '')),
                    'bankBin', '970436',
                    'bankShortName', 'VCB',
                    'accountNumber', 'DEMO-UTILITY-001',
                    'accountName', 'HDBHMS DEMO',
                    'transferDescription', CONCAT('DEMO UTL 405 ', @period_for_invoice)
                ), 'PENDING', @due_at, @now),
                (@inv506, NULL, NULL, NULL, @total506, 'PAYOS', @utility_account, CONCAT('DEMO UTL 506 ', @period_for_invoice), CONCAT('DEMO-ORDER-506-', REPLACE(@period_for_invoice, '-', ''), '-UTILITY'), JSON_OBJECT(
                    'providerOrderCode', CONCAT('DEMO-ORDER-506-', REPLACE(@period_for_invoice, '-', ''), '-UTILITY'),
                    'paymentLinkId', CONCAT('DEMO-LINK-506-', REPLACE(@period_for_invoice, '-', '')),
                    'transferDescription', CONCAT('DEMO UTL 506 ', @period_for_invoice)
                ), 'SUCCEEDED', @due_at, @now);

            SET @pi405 := (SELECT payment_intent_id FROM hdbhms.payment_intents WHERE provider_order_code = CONCAT('DEMO-ORDER-405-', REPLACE(@period_for_invoice, '-', ''), '-UTILITY') LIMIT 1);
            SET @pi506 := (SELECT payment_intent_id FROM hdbhms.payment_intents WHERE provider_order_code = CONCAT('DEMO-ORDER-506-', REPLACE(@period_for_invoice, '-', ''), '-UTILITY') LIMIT 1);

            INSERT INTO hdbhms.invoice_payment_groups
                (invoice_id, collection_account_id, group_type, amount, payment_intent_id, status, created_at)
            VALUES
                (@inv405, @utility_account, 'UTILITY', @total405, @pi405, 'PENDING', @now),
                (@inv506, @utility_account, 'UTILITY', @total506, @pi506, 'PAID', @now);

            SET @ipg405 := (SELECT invoice_payment_group_id FROM hdbhms.invoice_payment_groups WHERE invoice_id = @inv405 AND collection_account_id = @utility_account AND group_type = 'UTILITY' LIMIT 1);
            SET @ipg506 := (SELECT invoice_payment_group_id FROM hdbhms.invoice_payment_groups WHERE invoice_id = @inv506 AND collection_account_id = @utility_account AND group_type = 'UTILITY' LIMIT 1);

            UPDATE hdbhms.payment_intents
            SET invoice_payment_group_id = CASE
                    WHEN payment_intent_id = @pi405 THEN @ipg405
                    WHEN payment_intent_id = @pi506 THEN @ipg506
                    ELSE invoice_payment_group_id
                END
            WHERE payment_intent_id IN (@pi405, @pi506);

            INSERT INTO hdbhms.payment_transactions
                (provider, provider_transaction_id, collection_account_id, amount, transaction_time, payer_name, payer_account, content, status, raw_payload, confirmed_by, confirmed_at, created_at)
            VALUES
                ('PAYOS', CONCAT('DEMO-TXN-506-', REPLACE(@period_for_invoice, '-', ''), '-UTILITY'), @utility_account, @total506, @now, 'Tran Thu Ha', 'DEMO-PAYER-506', CONCAT('DEMO UTL 506 ', @period_for_invoice), 'MATCHED', CAST(JSON_OBJECT('source', 'seed-v31', 'invoiceCode', CONCAT('DEMO-INV-506-', @period_for_invoice, '-UTILITY-PAID')) AS BINARY), @manager_id, @now, @now);

            SET @tx506 := (
                SELECT payment_transaction_id
                FROM hdbhms.payment_transactions
                WHERE provider = 'PAYOS'
                  AND provider_transaction_id = CONCAT('DEMO-TXN-506-', REPLACE(@period_for_invoice, '-', ''), '-UTILITY')
                LIMIT 1
            );

            INSERT INTO hdbhms.payment_allocations
                (payment_transaction_id, invoice_id, amount, allocated_by, allocated_at)
            VALUES
                (@tx506, @inv506, @total506, @manager_id, @now);

            INSERT INTO hdbhms.room_utility_baselines
                (room_id, meter_id, last_billed_reading, last_invoice_id, created_at, updated_at)
            VALUES
                (@r405, @m405e, @e405_prev + @e405_usage, @inv405, @now, @now),
                (@r405, @m405w, @w405_prev + @w405_usage, @inv405, @now, @now),
                (@r506, @m506e, @e506_prev + @e506_usage, @inv506, @now, @now),
                (@r506, @m506w, @w506_prev + @w506_usage, @inv506, @now, @now)
            ON DUPLICATE KEY UPDATE
                room_id = VALUES(room_id),
                last_billed_reading = VALUES(last_billed_reading),
                last_invoice_id = VALUES(last_invoice_id),
                updated_at = VALUES(updated_at);

            INSERT INTO hdbhms.notification_outbox
                (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status, retry_count, max_retries, last_error, scheduled_at, sent_at, created_at, is_read, read_at, next_retry_at)
            VALUES
                ('INVOICE_ISSUED', 'INVOICE', @inv405, @u405, 'PUSH', 'New utility invoice', CONCAT('Room 405 utility invoice ', @period_for_invoice, ' is ready for payment.'), JSON_OBJECT('roomCode', '405', 'invoiceId', @inv405, 'paymentIntentId', @pi405, 'amount', @total405), 'SENT', 0, 3, NULL, @now, @now, @now, FALSE, NULL, NULL),
                ('PAYMENT_CONFIRMED', 'INVOICE', @inv506, @u506, 'PUSH', 'Utility payment confirmed', CONCAT('Room 506 utility invoice ', @period_for_invoice, ' has been paid.'), JSON_OBJECT('roomCode', '506', 'invoiceId', @inv506, 'paymentIntentId', @pi506, 'amount', @total506), 'SENT', 0, 3, NULL, @now, @now, @now, TRUE, @now, NULL);
        END IF;
    END IF;
END//

DELIMITER ;

CALL hdbhms.seed_current_utility_reading_payment_flow_v31();

DROP PROCEDURE IF EXISTS hdbhms.seed_current_utility_reading_payment_flow_v31;
