ALTER TABLE hdbhms.change_requests
    MODIFY COLUMN request_type ENUM (
        'METER_READING_CORRECTION',
        'INVOICE_ADJUSTMENT',
        'RENT_PRICE_ADJUSTMENT',
        'DEPOSIT_REFUND_REQUEST',
        'ROOM_TRANSFER',
        'MOVE_OUT',
        'COMPLAINT',
        'PERMISSION_ACCESS',
        'TENANT_PROFILE_ACCESS',
        'ADD_CO_OCCUPANT',
        'EXPENSE_APPROVAL'
    ) NOT NULL;

ALTER TABLE hdbhms.change_requests
    MODIFY COLUMN target_type ENUM (
        'METER_READING',
        'INVOICE',
        'CONTRACT',
        'DEPOSIT',
        'TENANT_PROFILE',
        'IDENTITY_DOCUMENT',
        'REPORT',
        'FILE',
        'OPERATING_EXPENSE',
        'OTHER'
    ) NOT NULL;

ALTER TABLE hdbhms.operating_expenses
    MODIFY COLUMN status ENUM (
        'DRAFT',
        'PENDING_APPROVAL',
        'APPROVED',
        'READY_FOR_PAYMENT',
        'REJECTED',
        'PAID',
        'CANCELLED'
    ) DEFAULT 'DRAFT' NOT NULL;

CREATE TABLE IF NOT EXISTS hdbhms.expense_approval_requests
(
    expense_approval_request_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    operating_expense_id        BIGINT UNSIGNED                          NOT NULL,
    change_request_id           BIGINT UNSIGNED                          NOT NULL,
    reason                      TEXT                                     NOT NULL,
    vendor_name                 VARCHAR(255)                             NULL,
    expected_payment_date       DATE                                     NULL,
    created_at                  DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    updated_at                  DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
    KEY idx_expense_approval_expected_date (expected_payment_date),
    CONSTRAINT uq_expense_approval_expense
        UNIQUE (operating_expense_id),
    CONSTRAINT uq_expense_approval_change_request
        UNIQUE (change_request_id),
    CONSTRAINT fk_expense_approval_expense
        FOREIGN KEY (operating_expense_id) REFERENCES hdbhms.operating_expenses (operating_expense_id),
    CONSTRAINT fk_expense_approval_change_request
        FOREIGN KEY (change_request_id) REFERENCES hdbhms.change_requests (change_request_id)
);

CREATE TABLE IF NOT EXISTS hdbhms.expense_attachments
(
    expense_attachment_id BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    operating_expense_id  BIGINT UNSIGNED                                               NOT NULL,
    file_id               BIGINT UNSIGNED                                               NOT NULL,
    attachment_type       ENUM ('QUOTATION', 'DAMAGE_PHOTO', 'OTHER') DEFAULT 'OTHER'   NOT NULL,
    created_at            DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                      NOT NULL,
    KEY idx_expense_attachment_expense (operating_expense_id),
    CONSTRAINT fk_expense_attachment_expense
        FOREIGN KEY (operating_expense_id) REFERENCES hdbhms.operating_expenses (operating_expense_id),
    CONSTRAINT fk_expense_attachment_file
        FOREIGN KEY (file_id) REFERENCES hdbhms.file_metadata (file_metadata_id)
);

CREATE TABLE IF NOT EXISTS hdbhms.expense_payments
(
    expense_payment_id   BIGINT UNSIGNED AUTO_INCREMENT
        PRIMARY KEY,
    operating_expense_id BIGINT UNSIGNED                                             NOT NULL,
    payment_date         DATE                                                        NOT NULL,
    payment_method       ENUM ('CASH', 'BANK_TRANSFER', 'OTHER') DEFAULT 'CASH'      NOT NULL,
    payment_reference    VARCHAR(100)                                                NULL,
    receipt_file_id      BIGINT UNSIGNED                                             NULL,
    paid_by_user_id      BIGINT UNSIGNED                                             NOT NULL,
    paid_at              DATETIME(6)                                                 NOT NULL,
    note                 TEXT                                                        NULL,
    created_at           DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)                    NOT NULL,
    KEY idx_expense_payment_date (payment_date),
    CONSTRAINT uq_expense_payment_expense
        UNIQUE (operating_expense_id),
    CONSTRAINT fk_expense_payment_expense
        FOREIGN KEY (operating_expense_id) REFERENCES hdbhms.operating_expenses (operating_expense_id),
    CONSTRAINT fk_expense_payment_receipt
        FOREIGN KEY (receipt_file_id) REFERENCES hdbhms.file_metadata (file_metadata_id),
    CONSTRAINT fk_expense_payment_paid_by
        FOREIGN KEY (paid_by_user_id) REFERENCES hdbhms.users (user_id)
);

INSERT IGNORE INTO hdbhms.expense_payments
    (operating_expense_id, payment_date, payment_method, payment_reference, receipt_file_id, paid_by_user_id, paid_at, note, created_at)
SELECT oe.operating_expense_id,
       oe.expense_date,
       'OTHER',
       'LEGACY_OPERATING_EXPENSE',
       oe.receipt_file_id,
       oe.paid_by_user_id,
       COALESCE(oe.approved_at, oe.created_at),
       'Dữ liệu chi đã tồn tại trước luồng phê duyệt chi MVP',
       COALESCE(oe.approved_at, oe.created_at)
FROM hdbhms.operating_expenses oe
WHERE oe.status = 'PAID'
  AND oe.paid_by_user_id IS NOT NULL;
