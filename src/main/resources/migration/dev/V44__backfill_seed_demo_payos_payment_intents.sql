INSERT INTO hdbhms.payment_intents
    (invoice_id,
     deposit_agreement_id,
     deposit_batch_id,
     invoice_payment_group_id,
     amount,
     provider,
     collection_account_id,
     payment_content,
     provider_order_code,
     qr_payload,
     status,
     expires_at,
     created_at)
SELECT i.invoice_id,
       NULL,
       NULL,
       NULL,
       i.remaining_amount,
       'PAYOS',
       i.collection_account_id,
       seed.payment_content,
       NULL,
       NULL,
       'PENDING',
       '2026-12-31 23:59:59.000000',
       NOW(6)
FROM hdbhms.invoices i
JOIN (
    SELECT 'HD_P302_01_06_2026_TP' AS invoice_code,
           'HD_P302_01_06_2026_TP' AS payment_content
    UNION ALL
    SELECT 'HD_P302_01_06_2026_DV',
           'HD_P302_01_06_2026_DV'
    UNION ALL
    SELECT 'HD_P403_30_07_2026_QT',
           'HD_P403_30_07_2026_QT'
    UNION ALL
    SELECT 'HD_P503_30_07_2026_QT',
           'HD_P503_30_07_2026_QT'
) seed
  ON seed.invoice_code = i.invoice_code
WHERE i.remaining_amount > 0
  AND i.status IN ('ISSUED', 'PARTIALLY_PAID', 'OVERDUE')
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.payment_intents existing
      WHERE existing.invoice_id = i.invoice_id
        AND existing.status = 'PENDING'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM hdbhms.payment_intents existing
      WHERE existing.payment_content = seed.payment_content
  );
