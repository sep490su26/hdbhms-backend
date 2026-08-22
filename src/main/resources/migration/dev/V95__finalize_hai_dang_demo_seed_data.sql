SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Finalize the dev fixtures after the older lifecycle overlays have run. This
-- migration keeps user-facing seed data Vietnamese and aligns the dates used
-- by contracts, rooms, invoices, and expiry reminders.
SET @hdd1_property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @hdd1_seed_now := '2026-08-22 09:00:00';
SET @hdd1_manager_id := (
    SELECT staff_user_id
    FROM hdbhms.property_staff_assignments
    WHERE property_id = @hdd1_property_id
      AND assigned_role = 'MANAGER'
      AND assignment_status = 'ACTIVE'
      AND ended_at IS NULL
    ORDER BY is_primary DESC, property_staff_assignment_id
    LIMIT 1
);
SET @hdd1_khai_user_id := (
    SELECT user_id
    FROM hdbhms.users
    WHERE deleted_at IS NULL
      AND (
          email IN (
              'nguyen.van.khai@haidang1.local',
              'nguyenvankhai95@gmail.com'
          )
          OR phone IN ('0901309001', '0918526407')
      )
    ORDER BY email = 'nguyen.van.khai@haidang1.local' DESC, user_id
    LIMIT 1
);
SET @hdd1_khai_tenant_id := (
    SELECT tenant_id
    FROM hdbhms.tenants
    WHERE user_id = @hdd1_khai_user_id
      AND property_id = @hdd1_property_id
      AND deleted_at IS NULL
    LIMIT 1
);
SET @hdd1_khai_profile_id := (
    SELECT person_profile_id
    FROM hdbhms.person_profiles
    WHERE user_id = @hdd1_khai_user_id
      AND deleted_at IS NULL
    LIMIT 1
);

-- Keep the demo account and profile consistent with the account used by the
-- three expiry fixtures and the additional Khai room memberships.
UPDATE hdbhms.users
SET email = 'nguyen.van.khai@haidang1.local',
    phone = '0918526407',
    status = 'ACTIVE',
    deleted_at = NULL,
    updated_at = @hdd1_seed_now
WHERE user_id = @hdd1_khai_user_id;

UPDATE hdbhms.person_profiles
SET full_name = 'Nguyễn Văn Khải',
    email = 'nguyen.van.khai@haidang1.local',
    phone = '0918526407',
    updated_at = @hdd1_seed_now
WHERE person_profile_id = @hdd1_khai_profile_id;

UPDATE hdbhms.tenant_account_provisionings
SET user_id = @hdd1_khai_user_id,
    recipient_email = 'nguyen.van.khai@haidang1.local',
    status = 'ACTIVE',
    updated_at = @hdd1_seed_now
WHERE tenant_profile_id = @hdd1_khai_profile_id;

-- These profiles were copied from the workbook and historically shared the
-- Khai phone number. Give them stable, distinct contact numbers.
UPDATE hdbhms.person_profiles
SET phone = CASE email
    WHEN 'do.minh.tam.302@haidang1.local' THEN '0984127365'
    WHEN 'le.quoc.viet.303@haidang1.local' THEN '0976241830'
    ELSE phone
END,
    updated_at = @hdd1_seed_now
WHERE email IN (
    'do.minh.tam.302@haidang1.local',
    'le.quoc.viet.303@haidang1.local'
);

UPDATE hdbhms.users user_account
JOIN hdbhms.person_profiles profile
  ON profile.user_id = user_account.user_id
SET user_account.phone = profile.phone,
    user_account.updated_at = @hdd1_seed_now
WHERE profile.email IN (
    'do.minh.tam.302@haidang1.local',
    'le.quoc.viet.303@haidang1.local'
);

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_khai_added_contracts;
CREATE TEMPORARY TABLE tmp_hdd1_khai_added_contracts
(
    room_code VARCHAR(10) NOT NULL PRIMARY KEY,
    contract_id BIGINT UNSIGNED NOT NULL
);

INSERT INTO tmp_hdd1_khai_added_contracts (room_code, contract_id)
SELECT room_code, contract_id
FROM (
    SELECT
        room.room_code,
        contract.lease_contract_id AS contract_id,
        ROW_NUMBER() OVER (
            PARTITION BY room.room_code
            ORDER BY CASE contract.status
                WHEN 'ACTIVE' THEN 0
                WHEN 'EXPIRING_SOON' THEN 1
                WHEN 'TERMINATION_PENDING' THEN 2
                WHEN 'SIGNED' THEN 3
                WHEN 'CONFIRMED' THEN 4
                ELSE 5
            END,
            contract.start_date DESC,
            contract.lease_contract_id DESC
        ) AS row_rank
    FROM hdbhms.rooms room
    JOIN hdbhms.lease_contracts contract
      ON contract.room_id = room.room_id
     AND contract.deleted_at IS NULL
    WHERE room.property_id = @hdd1_property_id
      AND room.room_code IN ('401', '402', '408')
      AND contract.status IN (
          'ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING',
          'SIGNED', 'CONFIRMED'
      )
) selected_contract
WHERE row_rank = 1;

-- Move the previous primary occupants out before assigning the same current
-- contract to Khai. Historical occupants remain available for audit screens.
UPDATE hdbhms.contract_occupants occupant
JOIN tmp_hdd1_khai_added_contracts selected_contract
  ON selected_contract.contract_id = occupant.contract_id
SET occupant.status = 'MOVED_OUT',
    occupant.move_out_date = DATE(@hdd1_seed_now),
    occupant.disabled_reason = 'Đã thay người đứng tên hiện tại trong dữ liệu mẫu.',
    occupant.disabled_by = @hdd1_manager_id,
    occupant.disabled_at = @hdd1_seed_now
WHERE occupant.occupant_role = 'PRIMARY'
  AND occupant.status = 'ACTIVE'
  AND occupant.tenant_profile_id <> @hdd1_khai_profile_id;

UPDATE hdbhms.lease_contracts contract
JOIN tmp_hdd1_khai_added_contracts selected_contract
  ON selected_contract.contract_id = contract.lease_contract_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
SET contract.primary_tenant_profile_id = @hdd1_khai_profile_id,
    contract.status = CASE room.room_code
        WHEN '402' THEN 'EXPIRING_SOON'
        ELSE 'ACTIVE'
    END,
    contract.start_date = CASE room.room_code
        WHEN '402' THEN '2026-01-01'
        ELSE '2026-07-01'
    END,
    contract.rent_start_date = CASE room.room_code
        WHEN '402' THEN '2026-01-01'
        ELSE '2026-07-01'
    END,
    contract.end_date = CASE room.room_code
        WHEN '402' THEN '2026-09-15'
        ELSE '2026-12-31'
    END,
    contract.tenant_intention = NULL,
    contract.expected_vacant_date = NULL,
    contract.intention_recorded_at = NULL,
    contract.signed_at = '2026-06-30 10:00:00',
    contract.created_at = '2026-06-25 09:00:00',
    contract.updated_at = @hdd1_seed_now
WHERE contract.deleted_at IS NULL;

UPDATE hdbhms.contract_occupants occupant
JOIN tmp_hdd1_khai_added_contracts selected_contract
  ON selected_contract.contract_id = occupant.contract_id
SET occupant.tenant_id = @hdd1_khai_tenant_id,
    occupant.tenant_profile_id = @hdd1_khai_profile_id,
    occupant.occupant_role = 'PRIMARY',
    occupant.move_in_date = CASE
        WHEN selected_contract.room_code = '402' THEN '2026-01-01'
        ELSE '2026-07-01'
    END,
    occupant.move_out_date = NULL,
    occupant.status = 'ACTIVE',
    occupant.disabled_reason = NULL,
    occupant.disabled_by = NULL,
    occupant.disabled_at = NULL
WHERE occupant.tenant_profile_id = @hdd1_khai_profile_id;

INSERT INTO hdbhms.contract_occupants
    (contract_id, tenant_id, tenant_profile_id, occupant_role, move_in_date,
     move_out_date, status, disabled_reason, disabled_by, disabled_at, created_at)
SELECT selected_contract.contract_id,
       @hdd1_khai_tenant_id,
       @hdd1_khai_profile_id,
       'PRIMARY',
       CASE
           WHEN selected_contract.room_code = '402' THEN '2026-01-01'
           ELSE '2026-07-01'
       END,
       NULL,
       'ACTIVE',
       NULL,
       NULL,
       NULL,
       '2026-06-25 09:00:00'
FROM tmp_hdd1_khai_added_contracts selected_contract
WHERE NOT EXISTS (
    SELECT 1
    FROM hdbhms.contract_occupants existing_occupant
    WHERE existing_occupant.contract_id = selected_contract.contract_id
      AND existing_occupant.tenant_profile_id = @hdd1_khai_profile_id
);

UPDATE hdbhms.rooms room
JOIN tmp_hdd1_khai_added_contracts selected_contract
  ON selected_contract.room_code = room.room_code
SET room.current_status = CASE room.room_code
        WHEN '402' THEN 'SOON_VACANT'
        ELSE 'OCCUPIED'
    END,
    room.public_note = CASE room.room_code
        WHEN '402' THEN 'Hợp đồng của Nguyễn Văn Khải hết hạn ngày 15/09/2026.'
        ELSE CONCAT('Phòng đang được Nguyễn Văn Khải thuê.')
    END,
    room.internal_note = CASE room.room_code
        WHEN '402' THEN 'Hợp đồng sắp hết hạn, chưa ghi nhận lựa chọn gia hạn, chuyển phòng hoặc chuyển đi.'
        ELSE 'Hợp đồng đang hiệu lực; dữ liệu dùng cho tài khoản có nhiều phòng.'
    END,
    room.updated_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND room.deleted_at IS NULL;

-- Normalize current contract codes to the same format as generated contract
-- files: HDT_P{room}_{ngay_bat_dau}.
UPDATE hdbhms.lease_contracts contract
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
SET contract.contract_code = CONCAT(
        'HDT_P', room.room_code, '_', DATE_FORMAT(contract.start_date, '%d_%m_%Y')
    ),
    contract.updated_at = @hdd1_seed_now
WHERE room.property_id = @hdd1_property_id
  AND contract.deleted_at IS NULL
  AND contract.status IN (
      'ACTIVE', 'EXPIRING_SOON', 'TERMINATION_PENDING',
      'SIGNED', 'CONFIRMED', 'PENDING_SIGNATURE'
  )
  AND (
      contract.contract_code NOT LIKE 'HDT_P%'
      OR contract.contract_code LIKE '%2025'
  );

-- Replace English seed notes and audit reasons left by older overlays.
UPDATE hdbhms.rooms
SET public_note = CASE room_code
        WHEN '101' THEN 'Phòng trống, có thể tiếp nhận đặt phòng.'
        WHEN '102' THEN 'Phòng trống, dùng cho trường hợp kiểm tra tệp đặt phòng.'
        WHEN '304' THEN 'Phòng trống, dùng cho luồng đặt phòng thành công.'
        WHEN '403' THEN 'Phòng trống, dùng cho trường hợp thanh toán tiền cọc thất bại.'
        WHEN '404' THEN 'Phòng trống, dùng cho trường hợp thanh toán tiền cọc quá thời hạn.'
        WHEN '405' THEN 'Phòng trống, dùng cho trường hợp có người đặt đồng thời.'
        WHEN '407' THEN 'Phòng trống, dùng cho trường hợp thiếu thông tin đặt phòng.'
        WHEN '302' THEN 'Hợp đồng của Nguyễn Văn Khải hết hạn ngày 31/10/2026.'
        WHEN '303' THEN 'Hợp đồng của Nguyễn Văn Khải hết hạn ngày 01/09/2026.'
        ELSE public_note
    END,
    internal_note = CASE room_code
        WHEN '101' THEN 'Phòng trống dùng làm mục tiêu chuyển sang phòng có giá cao hơn.'
        WHEN '102' THEN 'Phòng trống dùng cho trường hợp tải tệp không hợp lệ.'
        WHEN '304' THEN 'Phòng trống dùng cho luồng đặt phòng và kích hoạt hợp đồng thành công.'
        WHEN '403' THEN 'Phòng trống dùng cho trường hợp thanh toán tiền cọc thất bại.'
        WHEN '404' THEN 'Phòng trống dùng cho trường hợp phiên thanh toán tiền cọc hết hạn.'
        WHEN '405' THEN 'Phòng trống dùng cho trường hợp tranh chấp đặt phòng đồng thời.'
        WHEN '407' THEN 'Phòng trống dùng cho trường hợp thông tin đặt phòng chưa đầy đủ.'
        WHEN '302' THEN 'Còn công nợ; dùng cho trường hợp không đủ điều kiện thực hiện thanh lý hoặc chuyển phòng.'
        WHEN '303' THEN 'Hợp đồng sắp hết hạn; dùng cho luồng gia hạn và kiểm tra thời hạn.'
        WHEN '301' THEN 'Hợp đồng đang hiệu lực; không thuộc nhóm hợp đồng sắp hết hạn.'
        ELSE internal_note
    END,
    updated_at = @hdd1_seed_now
WHERE property_id = @hdd1_property_id
  AND deleted_at IS NULL;

UPDATE hdbhms.contract_occupants
SET disabled_reason = 'Phòng đã được giải phóng trong dữ liệu mẫu tháng 08/2026.'
WHERE disabled_reason IN (
    'Room released in the August seed.',
    'August seed room release completed.'
);

UPDATE hdbhms.room_status_history
SET reason = 'Đã giải phóng phòng trong dữ liệu mẫu tháng 08/2026.'
WHERE reason = 'August seed room release completed.';

-- Keep utility invoice descriptions readable in exports and detail dialogs.
UPDATE hdbhms.invoice_lines line
JOIN hdbhms.invoices invoice
  ON invoice.invoice_id = line.invoice_id
JOIN hdbhms.rooms room
  ON room.room_id = invoice.room_id
SET line.description = CASE line.line_type
    WHEN 'ELECTRICITY' THEN CONCAT('Tiền điện phòng ', room.room_code, ' kỳ ', COALESCE(invoice.billing_period, '07/2026'))
    WHEN 'WATER' THEN CONCAT('Tiền nước phòng ', room.room_code, ' kỳ ', COALESCE(invoice.billing_period, '07/2026'))
    WHEN 'SERVICE_FEE' THEN CONCAT('Phí dịch vụ phòng ', room.room_code, ' kỳ ', COALESCE(invoice.billing_period, '07/2026'))
    ELSE line.description
END
WHERE invoice.property_id = @hdd1_property_id
  AND invoice.status = 'DRAFT'
  AND line.line_type IN ('ELECTRICITY', 'WATER', 'SERVICE_FEE');

-- Repair the expiry messages already materialized in the outbox. Templates and
-- outbox rows must use the same Vietnamese wording and the contract's date.
UPDATE hdbhms.notification_outbox notification
JOIN hdbhms.lease_contracts contract
  ON notification.target_type = 'CONTRACT'
 AND notification.target_id = contract.lease_contract_id
JOIN hdbhms.rooms room
  ON room.room_id = contract.room_id
SET notification.title = CASE notification.event_type
        WHEN 'CONTRACT_EXPIRING_SOON_REVIEW' THEN CONCAT('Hợp đồng phòng ', room.room_code, ' sắp hết hạn')
        WHEN 'LEASE_EXPIRY_REMINDER_FIRST' THEN CONCAT('Hợp đồng ', contract.contract_code, ' sắp hết hạn')
        WHEN 'LEASE_EXPIRY_REMINDER_SECOND' THEN CONCAT('Bạn chưa phản hồi về hợp đồng ', contract.contract_code)
        WHEN 'LEASE_EXPIRY_REMINDER_FINAL' THEN CONCAT('Nhắc lần cuối về hợp đồng ', contract.contract_code)
        ELSE notification.title
    END,
    notification.body = CASE notification.event_type
        WHEN 'CONTRACT_EXPIRING_SOON_REVIEW' THEN CONCAT(
            'Hợp đồng phòng ', room.room_code, ' sẽ hết hạn vào ngày ', DATE_FORMAT(contract.end_date, '%d/%m/%Y'),
            '. Vui lòng theo dõi và xử lý khi người thuê phản hồi.'
        )
        WHEN 'LEASE_EXPIRY_REMINDER_FIRST' THEN CONCAT(
            'Phòng ', room.room_code, ' sẽ hết hạn hợp đồng vào ngày ', DATE_FORMAT(contract.end_date, '%d/%m/%Y'),
            '. Vui lòng chọn gia hạn, chuyển phòng hoặc chuyển đi.'
        )
        WHEN 'LEASE_EXPIRY_REMINDER_SECOND' THEN CONCAT(
            'Bạn chưa phản hồi về hợp đồng phòng ', room.room_code,
            '. Vui lòng chọn ý định trước ngày ', DATE_FORMAT(contract.end_date, '%d/%m/%Y'), '.'
        )
        WHEN 'LEASE_EXPIRY_REMINDER_FINAL' THEN CONCAT(
            'Hợp đồng phòng ', room.room_code, ' sắp hết hạn vào ngày ', DATE_FORMAT(contract.end_date, '%d/%m/%Y'),
            '. Vui lòng phản hồi để tránh chậm xử lý bàn giao.'
        )
        ELSE notification.body
    END
WHERE room.property_id = @hdd1_property_id
  AND notification.event_type IN (
      'CONTRACT_EXPIRING_SOON_REVIEW',
      'LEASE_EXPIRY_REMINDER_FIRST',
      'LEASE_EXPIRY_REMINDER_SECOND',
      'LEASE_EXPIRY_REMINDER_FINAL'
  );

INSERT INTO hdbhms.notification_templates
    (template_key, channel, title_template, body_template, status)
VALUES
    ('CONTRACT_EXPIRING_SOON_REVIEW', 'PUSH',
     CONCAT('Hợp đồng phòng ', '[[', '$', '{roomCode}]]', ' sắp hết hạn'),
     CONCAT('Hợp đồng ', '[[', '$', '{contractCode}]]', ' của phòng ', '[[', '$', '{roomCode}]]',
            ' sẽ hết hạn vào ngày ', '[[', '$', '{endDate}]]', '.'), 'ACTIVE'),
    ('CONTRACT_EXPIRING_SOON_REVIEW', 'WEB',
     CONCAT('Hợp đồng phòng ', '[[', '$', '{roomCode}]]', ' sắp hết hạn'),
     CONCAT('Hợp đồng ', '[[', '$', '{contractCode}]]', ' của phòng ', '[[', '$', '{roomCode}]]',
            ' sẽ hết hạn vào ngày ', '[[', '$', '{endDate}]]', '.'), 'ACTIVE'),
    ('LEASE_EXPIRY_REMINDER_FIRST', 'PUSH',
     CONCAT('Hợp đồng ', '[[', '$', '{contractCode}]]', ' sắp hết hạn'),
     CONCAT('Phòng ', '[[', '$', '{roomName}]]', ' sẽ hết hạn hợp đồng vào ngày ', '[[', '$', '{endDate}]]',
            '. Vui lòng chọn gia hạn, chuyển phòng hoặc chuyển đi.'), 'ACTIVE'),
    ('LEASE_EXPIRY_REMINDER_SECOND', 'PUSH',
     CONCAT('Bạn chưa phản hồi về hợp đồng ', '[[', '$', '{contractCode}]]'),
     CONCAT('Vui lòng chọn ý định cho phòng ', '[[', '$', '{roomName}]]',
            ' trước ngày ', '[[', '$', '{endDate}]]', '.'), 'ACTIVE'),
    ('LEASE_EXPIRY_REMINDER_FINAL', 'PUSH',
     CONCAT('Nhắc lần cuối về hợp đồng ', '[[', '$', '{contractCode}]]'),
     CONCAT('Hợp đồng phòng ', '[[', '$', '{roomName}]]', ' sắp hết hạn vào ngày ', '[[', '$', '{endDate}]]',
            '. Vui lòng phản hồi để tránh chậm xử lý bàn giao.'), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    title_template = VALUES(title_template),
    body_template = VALUES(body_template),
    status = 'ACTIVE';

DROP TEMPORARY TABLE IF EXISTS tmp_hdd1_khai_added_contracts;
