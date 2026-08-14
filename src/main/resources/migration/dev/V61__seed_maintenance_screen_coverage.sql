SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

SET @property_id := (
    SELECT property_id
    FROM hdbhms.properties
    WHERE property_code = 'HAI_DANG_1'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @manager_id := (
    SELECT user_id
    FROM hdbhms.users
    WHERE email = 'tranthuhuong90@gmail.com'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @manager_tenant_id := (
    SELECT tenant_id
    FROM hdbhms.tenants
    WHERE user_id = @manager_id
      AND property_id = @property_id
      AND deleted_at IS NULL
    LIMIT 1
);

SET @c101 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HDT_P101_01_07_2026' AND deleted_at IS NULL LIMIT 1);
SET @c102 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HDT_P102_01_07_2026' AND deleted_at IS NULL LIMIT 1);
SET @c103 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HDT_P103_01_07_2026' AND deleted_at IS NULL LIMIT 1);
SET @c104 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HDT_P104_01_07_2026' AND deleted_at IS NULL LIMIT 1);
SET @c105 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HDT_P105_01_07_2026' AND deleted_at IS NULL LIMIT 1);
SET @c106 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HDT_P106_01_07_2026' AND deleted_at IS NULL LIMIT 1);
SET @c201 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HDT_P201_01_07_2026' AND deleted_at IS NULL LIMIT 1);
SET @c202 := (SELECT lease_contract_id FROM hdbhms.lease_contracts WHERE contract_code = 'HDT_P202_01_07_2026' AND deleted_at IS NULL LIMIT 1);

SET @r101 := (SELECT room_id FROM hdbhms.lease_contracts WHERE lease_contract_id = @c101);
SET @r102 := (SELECT room_id FROM hdbhms.lease_contracts WHERE lease_contract_id = @c102);
SET @r103 := (SELECT room_id FROM hdbhms.lease_contracts WHERE lease_contract_id = @c103);
SET @r104 := (SELECT room_id FROM hdbhms.lease_contracts WHERE lease_contract_id = @c104);
SET @r105 := (SELECT room_id FROM hdbhms.lease_contracts WHERE lease_contract_id = @c105);
SET @r106 := (SELECT room_id FROM hdbhms.lease_contracts WHERE lease_contract_id = @c106);
SET @r201 := (SELECT room_id FROM hdbhms.lease_contracts WHERE lease_contract_id = @c201);
SET @r202 := (SELECT room_id FROM hdbhms.lease_contracts WHERE lease_contract_id = @c202);

SET @u101 := (SELECT profile.user_id FROM hdbhms.lease_contracts contract JOIN hdbhms.person_profiles profile ON profile.person_profile_id = contract.primary_tenant_profile_id WHERE contract.lease_contract_id = @c101);
SET @u102 := (SELECT profile.user_id FROM hdbhms.lease_contracts contract JOIN hdbhms.person_profiles profile ON profile.person_profile_id = contract.primary_tenant_profile_id WHERE contract.lease_contract_id = @c102);
SET @u103 := (SELECT profile.user_id FROM hdbhms.lease_contracts contract JOIN hdbhms.person_profiles profile ON profile.person_profile_id = contract.primary_tenant_profile_id WHERE contract.lease_contract_id = @c103);
SET @u104 := (SELECT profile.user_id FROM hdbhms.lease_contracts contract JOIN hdbhms.person_profiles profile ON profile.person_profile_id = contract.primary_tenant_profile_id WHERE contract.lease_contract_id = @c104);
SET @u105 := (SELECT profile.user_id FROM hdbhms.lease_contracts contract JOIN hdbhms.person_profiles profile ON profile.person_profile_id = contract.primary_tenant_profile_id WHERE contract.lease_contract_id = @c105);
SET @u106 := (SELECT profile.user_id FROM hdbhms.lease_contracts contract JOIN hdbhms.person_profiles profile ON profile.person_profile_id = contract.primary_tenant_profile_id WHERE contract.lease_contract_id = @c106);
SET @u201 := (SELECT profile.user_id FROM hdbhms.lease_contracts contract JOIN hdbhms.person_profiles profile ON profile.person_profile_id = contract.primary_tenant_profile_id WHERE contract.lease_contract_id = @c201);
SET @u202 := (SELECT profile.user_id FROM hdbhms.lease_contracts contract JOIN hdbhms.person_profiles profile ON profile.person_profile_id = contract.primary_tenant_profile_id WHERE contract.lease_contract_id = @c202);

SET @t101 := (SELECT tenant_id FROM hdbhms.tenants WHERE user_id = @u101 AND property_id = @property_id AND deleted_at IS NULL LIMIT 1);
SET @t103 := (SELECT tenant_id FROM hdbhms.tenants WHERE user_id = @u103 AND property_id = @property_id AND deleted_at IS NULL LIMIT 1);
SET @t104 := (SELECT tenant_id FROM hdbhms.tenants WHERE user_id = @u104 AND property_id = @property_id AND deleted_at IS NULL LIMIT 1);
SET @t105 := (SELECT tenant_id FROM hdbhms.tenants WHERE user_id = @u105 AND property_id = @property_id AND deleted_at IS NULL LIMIT 1);

SET @image_normal_1 := (SELECT file_metadata_id FROM hdbhms.file_metadata WHERE storage_key = 'room-samples/normal/1.png' AND deleted_at IS NULL LIMIT 1);
SET @image_normal_2 := (SELECT file_metadata_id FROM hdbhms.file_metadata WHERE storage_key = 'room-samples/normal/2.png' AND deleted_at IS NULL LIMIT 1);
SET @image_normal_3 := (SELECT file_metadata_id FROM hdbhms.file_metadata WHERE storage_key = 'room-samples/normal/3.png' AND deleted_at IS NULL LIMIT 1);
SET @image_premium_1 := (SELECT file_metadata_id FROM hdbhms.file_metadata WHERE storage_key = 'room-samples/premium/1.png' AND deleted_at IS NULL LIMIT 1);
SET @image_premium_2 := (SELECT file_metadata_id FROM hdbhms.file_metadata WHERE storage_key = 'room-samples/premium/2.png' AND deleted_at IS NULL LIMIT 1);
SET @image_premium_3 := (SELECT file_metadata_id FROM hdbhms.file_metadata WHERE storage_key = 'room-samples/premium/3.png' AND deleted_at IS NULL LIMIT 1);
SET @image_premium_4 := (SELECT file_metadata_id FROM hdbhms.file_metadata WHERE storage_key = 'room-samples/premium/4.png' AND deleted_at IS NULL LIMIT 1);

-- Eight room tickets are visible from both web and the corresponding tenant's
-- mobile account because room_id and contract_id reference an active lease.
INSERT INTO hdbhms.maintenance_tickets
    (ticket_code, property_id, room_id, contract_id, created_by, ticket_scope,
     category, title, description, repair_requested, status, rejection_reason,
     assigned_to, worker_name, external_repairman_name, external_repairman_phone,
     external_repair_provider, external_repair_note, repairman_phone, repair_items,
     completed_at, created_at, updated_at)
VALUES
    ('#SC-0101', @property_id, @r101, @c101, @u101, 'TENANT_ROOM', 'ELECTRICITY',
     'Ổ cắm điện chập chờn', 'Ổ cắm cạnh bàn học phát tia lửa nhỏ khi cắm quạt, cần kiểm tra sớm.', TRUE,
     'PENDING_ACCEPTANCE', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
     NULL, '2026-08-13 19:20:00', '2026-08-13 19:20:00'),
    ('#SC-0102', @property_id, @r102, @c102, @u102, 'TENANT_ROOM', 'WATER',
     'Áp lực nước yếu giờ cao điểm', 'Nước vòi sen yếu từ 19 giờ đến 21 giờ nhưng chưa cần thay thiết bị.', FALSE,
     'ACCEPTED', NULL, @manager_id, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
     NULL, '2026-08-12 20:05:00', '2026-08-13 08:15:00'),
    ('#SC-0103', @property_id, @r103, @c103, @u103, 'TENANT_ROOM', 'AIR_CONDITIONER',
     'Máy lạnh không đủ mát', 'Máy lạnh vẫn chạy nhưng phòng không mát, có tiếng rung ở dàn lạnh.', TRUE,
     'IN_PROGRESS', NULL, @manager_id, 'Nguyễn Thành Công', 'Nguyễn Thành Công', '0909123456',
     'Điện lạnh Thành Công', 'Kỹ thuật viên đã đến kiểm tra tại phòng.', '0909123456',
     '[ROOT_CAUSE]Dàn lạnh bám bụi và quạt bị lệch trục.\n[REPAIR_ITEMS]Vệ sinh dàn lạnh, cân chỉnh quạt và kiểm tra gas.',
     NULL, '2026-08-10 21:10:00', '2026-08-13 14:30:00'),
    ('#SC-0104', @property_id, @r104, @c104, @u104, 'TENANT_ROOM', 'DOOR_LOCK',
     'Khóa cửa phòng bị kẹt', 'Chìa khóa khó xoay, tay nắm cửa bị rơ và cần thay lõi khóa.', TRUE,
     'WAITING_CONFIRMATION', NULL, @manager_id, 'Trần Quốc Bảo', NULL, NULL, NULL, NULL, '0912345680',
     '[ROOT_CAUSE]Lõi khóa bị mòn sau thời gian dài sử dụng.\n[REPAIR_ITEMS]Thay lõi khóa mới và siết lại tay nắm cửa.',
     '2026-08-12 16:20:00', '2026-08-08 07:45:00', '2026-08-12 16:20:00'),
    ('#SC-0105', @property_id, @r105, @c105, @u105, 'TENANT_ROOM', 'INTERNET',
     'Mạng Internet mất kết nối', 'Bộ phát wifi trong phòng mất kết nối nhiều lần trong ngày.', TRUE,
     'COMPLETED', NULL, @manager_id, 'Đội kỹ thuật Viettel', 'Lê Minh Tuấn', '0987654321',
     'Viettel Hà Nội', 'Đã thay đầu nối cáp và cấu hình lại bộ phát.', '0987654321',
     '[ROOT_CAUSE]Đầu nối cáp quang lỏng và bộ phát bị treo.\n[REPAIR_ITEMS]Thay đầu nối, khởi động và cấu hình lại bộ phát wifi.',
     '2026-08-06 11:00:00', '2026-08-05 09:10:00', '2026-08-06 18:30:00'),
    ('#SC-0106', @property_id, @r106, @c106, @u106, 'TENANT_ROOM', 'FURNITURE',
     'Đề nghị đổi bàn làm việc', 'Bàn vẫn sử dụng bình thường nhưng khách muốn đổi sang mẫu lớn hơn.', FALSE,
     'REJECTED', 'Nội thất hiện tại còn sử dụng tốt; nhu cầu đổi mẫu không thuộc phạm vi bảo trì.',
     @manager_id, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
     NULL, '2026-08-04 10:00:00', '2026-08-04 15:20:00'),
    ('#SC-0201', @property_id, @r201, @c201, @u201, 'TENANT_ROOM', 'SANITARY',
     'Mùi thoát sàn nhà vệ sinh', 'Khách báo mùi thoát sàn nhưng sau đó tự vệ sinh và xin hủy yêu cầu.', TRUE,
     'CANCELLED', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
     NULL, '2026-08-03 08:40:00', '2026-08-03 10:10:00'),
    ('#SC-0202', @property_id, @r202, @c202, @u202, 'TENANT_ROOM', 'OTHER',
     'Kính cửa sổ bị nứt', 'Kính cửa sổ nứt do va chạm trong quá trình sử dụng, đã thay kính mới.', TRUE,
     'COMPLETED', NULL, @manager_id, 'Phạm Văn Hải', NULL, NULL, NULL, NULL, '0934567890',
     '[ROOT_CAUSE]Kính bị va đập từ phía trong phòng.\n[REPAIR_ITEMS]Tháo kính vỡ, thay kính mới và bơm keo chống thấm.',
     '2026-08-04 17:00:00', '2026-08-01 18:30:00', '2026-08-05 09:00:00'),

    -- Two common-area tickets cover the special violation presentation and
    -- the common-area scope without leaking into the tenant mobile list.
    ('#SC-KVC-01', @property_id, NULL, NULL, @manager_id, 'COMMON_AREA', 'RULE_VIOLATION',
     'Tự ý đổi mật khẩu wifi tầng 2', 'Đã xác minh thiết bị phòng 207 tự ý đổi mật khẩu bộ phát wifi dùng chung.', FALSE,
     'COMPLETED', NULL, @manager_id, 'Trần Thu Hương', NULL, NULL, NULL, NULL, '0977000002',
     '[ROOT_CAUSE]Khách thuê tự ý truy cập trang quản trị modem.\n[REPAIR_ITEMS]Khôi phục cấu hình và đổi mật khẩu quản trị thiết bị.',
     '2026-08-07 14:00:00', '2026-08-07 08:00:00', '2026-08-07 14:20:00'),
    ('#SC-KVC-02', @property_id, NULL, NULL, @manager_id, 'COMMON_AREA', 'CLEANING',
     'Vệ sinh hành lang tầng 3', 'Hành lang tầng 3 có vết bẩn sau khi vận chuyển đồ, cần vệ sinh bổ sung.', TRUE,
     'ACCEPTED', NULL, @manager_id, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
     NULL, '2026-08-09 16:10:00', '2026-08-10 07:30:00'),

    -- Three internal tickets cover pending, processing and completed internal
    -- maintenance, including one landlord-paid operating cost.
    ('#SC-NB-01', @property_id, NULL, NULL, @manager_id, 'PROPERTY_OPERATION', 'SECURITY',
     'Kiểm tra camera cổng chính', 'Camera cổng chính chập chờn vào ban đêm, đội kỹ thuật nội bộ đang kiểm tra nguồn.', TRUE,
     'IN_PROGRESS', NULL, @manager_id, 'Đội kỹ thuật nội bộ', NULL, NULL, NULL, NULL, '0977000002',
     '[ROOT_CAUSE]Nguồn cấp camera không ổn định.\n[REPAIR_ITEMS]Đo nguồn, thay đầu nối và theo dõi tín hiệu ban đêm.',
     NULL, '2026-08-11 06:30:00', '2026-08-13 20:00:00'),
    ('#SC-NB-02', @property_id, NULL, NULL, @manager_id, 'PROPERTY_OPERATION', 'COMMON_EQUIPMENT',
     'Bảo dưỡng máy bơm nước', 'Bảo dưỡng định kỳ máy bơm và tủ điều khiển cấp nước toàn cơ sở.', TRUE,
     'COMPLETED', NULL, @manager_id, 'Đội kỹ thuật nội bộ', NULL, NULL, NULL, NULL, '0977000002',
     '[ROOT_CAUSE]Thiết bị đến kỳ bảo dưỡng định kỳ.\n[REPAIR_ITEMS]Tra dầu, vệ sinh tủ điện và thay phớt máy bơm.',
     '2026-08-02 10:30:00', '2026-07-28 08:00:00', '2026-08-02 10:30:00'),
    ('#SC-NB-03', @property_id, NULL, NULL, @manager_id, 'PROPERTY_OPERATION', 'PAINTING',
     'Sơn lại biển số phòng tầng 5', 'Biển số một số phòng tầng 5 bị phai màu, cần sơn lại trong đợt bảo trì tới.', TRUE,
     'PENDING_ACCEPTANCE', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
     NULL, '2026-08-14 08:15:00', '2026-08-14 08:15:00');

SET @mt101 := (SELECT maintenance_ticket_id FROM hdbhms.maintenance_tickets WHERE ticket_code = '#SC-0101');
SET @mt102 := (SELECT maintenance_ticket_id FROM hdbhms.maintenance_tickets WHERE ticket_code = '#SC-0102');
SET @mt103 := (SELECT maintenance_ticket_id FROM hdbhms.maintenance_tickets WHERE ticket_code = '#SC-0103');
SET @mt104 := (SELECT maintenance_ticket_id FROM hdbhms.maintenance_tickets WHERE ticket_code = '#SC-0104');
SET @mt105 := (SELECT maintenance_ticket_id FROM hdbhms.maintenance_tickets WHERE ticket_code = '#SC-0105');
SET @mt106 := (SELECT maintenance_ticket_id FROM hdbhms.maintenance_tickets WHERE ticket_code = '#SC-0106');
SET @mt201 := (SELECT maintenance_ticket_id FROM hdbhms.maintenance_tickets WHERE ticket_code = '#SC-0201');
SET @mt202 := (SELECT maintenance_ticket_id FROM hdbhms.maintenance_tickets WHERE ticket_code = '#SC-0202');
SET @mt_kvc1 := (SELECT maintenance_ticket_id FROM hdbhms.maintenance_tickets WHERE ticket_code = '#SC-KVC-01');
SET @mt_kvc2 := (SELECT maintenance_ticket_id FROM hdbhms.maintenance_tickets WHERE ticket_code = '#SC-KVC-02');
SET @mt_nb1 := (SELECT maintenance_ticket_id FROM hdbhms.maintenance_tickets WHERE ticket_code = '#SC-NB-01');
SET @mt_nb2 := (SELECT maintenance_ticket_id FROM hdbhms.maintenance_tickets WHERE ticket_code = '#SC-NB-02');
SET @mt_nb3 := (SELECT maintenance_ticket_id FROM hdbhms.maintenance_tickets WHERE ticket_code = '#SC-NB-03');

INSERT INTO hdbhms.maintenance_ticket_attachments
    (ticket_id, file_id, attachment_phase, sort_order, created_by, created_by_user_id, created_at)
VALUES
    (@mt101, @image_normal_1, 'BEFORE', 0, @t101, @u101, '2026-08-13 19:21:00'),
    (@mt103, @image_normal_2, 'BEFORE', 0, @t103, @u103, '2026-08-10 21:11:00'),
    (@mt104, @image_normal_3, 'BEFORE', 0, @t104, @u104, '2026-08-08 07:46:00'),
    (@mt104, @image_premium_1, 'AFTER', 0, @manager_tenant_id, @manager_id, '2026-08-12 16:20:00'),
    (@mt105, @image_premium_2, 'BEFORE', 0, @t105, @u105, '2026-08-05 09:11:00'),
    (@mt105, @image_premium_3, 'AFTER', 0, @manager_tenant_id, @manager_id, '2026-08-06 11:00:00'),
    (@mt_nb2, @image_premium_4, 'BEFORE', 0, @manager_tenant_id, @manager_id, '2026-07-28 08:01:00'),
    (@mt_nb2, @image_normal_1, 'AFTER', 0, @manager_tenant_id, @manager_id, '2026-08-02 10:30:00');

INSERT INTO hdbhms.maintenance_costs
    (ticket_id, cost_type, description, amount, paid_by, cost_responsibility,
     charge_invoice_id, receipt_file_id, created_by, created_at)
VALUES
    (@mt104, 'MATERIAL', 'Thay lõi khóa và căn chỉnh tay nắm cửa.', 250000, 'LANDLORD', 'OWNER', NULL, NULL, @manager_id, '2026-08-12 16:20:00'),
    (@mt105, 'LABOR', 'Phí kỹ thuật kiểm tra và cấu hình lại thiết bị mạng.', 180000, 'LANDLORD', 'OWNER', NULL, NULL, @manager_id, '2026-08-06 11:00:00'),
    (@mt202, 'TENANT_COMPENSATION', 'Chi phí thay kính do va đập từ phía trong phòng.', 450000, 'TENANT', 'TENANT', NULL, NULL, @manager_id, '2026-08-04 17:00:00'),
    (@mt_nb2, 'COMMON_OPERATING', 'Bảo dưỡng máy bơm và thay phớt định kỳ.', 1200000, 'LANDLORD', 'OWNER', NULL, NULL, @manager_id, '2026-08-02 10:30:00');

INSERT INTO hdbhms.maintenance_reviews
    (ticket_id, reviewer_user_id, rating, comment, created_at)
VALUES
    (@mt105, @u105, 5, 'Mạng đã ổn định, kỹ thuật viên xử lý nhanh và hướng dẫn rõ ràng.', '2026-08-06 18:30:00'),
    (@mt202, @u202, 4, 'Kính đã được thay chắc chắn, thời gian chờ hơi lâu.', '2026-08-05 09:00:00');

-- Every ticket has a creation event. Additional rows below represent the
-- shortest valid lifecycle needed to reach each seeded status.
INSERT INTO hdbhms.maintenance_ticket_events
    (ticket_id, from_status, to_status, action, note, created_by, created_at)
VALUES
    (@mt101, NULL, 'PENDING_ACCEPTANCE', 'CREATE', 'Khách thuê phòng 101 tạo phiếu từ mobile.', @u101, '2026-08-13 19:20:00'),
    (@mt102, NULL, 'PENDING_ACCEPTANCE', 'CREATE', 'Khách thuê phòng 102 chỉ báo tình trạng nước yếu.', @u102, '2026-08-12 20:05:00'),
    (@mt103, NULL, 'PENDING_ACCEPTANCE', 'CREATE', 'Khách thuê phòng 103 báo máy lạnh không đủ mát.', @u103, '2026-08-10 21:10:00'),
    (@mt104, NULL, 'PENDING_ACCEPTANCE', 'CREATE', 'Khách thuê phòng 104 báo khóa cửa bị kẹt.', @u104, '2026-08-08 07:45:00'),
    (@mt105, NULL, 'PENDING_ACCEPTANCE', 'CREATE', 'Khách thuê phòng 105 báo mất kết nối Internet.', @u105, '2026-08-05 09:10:00'),
    (@mt106, NULL, 'PENDING_ACCEPTANCE', 'CREATE', 'Khách thuê phòng 106 đề nghị đổi bàn.', @u106, '2026-08-04 10:00:00'),
    (@mt201, NULL, 'PENDING_ACCEPTANCE', 'CREATE', 'Khách thuê phòng 201 báo mùi thoát sàn.', @u201, '2026-08-03 08:40:00'),
    (@mt202, NULL, 'PENDING_ACCEPTANCE', 'CREATE', 'Khách thuê phòng 202 báo kính cửa sổ bị nứt.', @u202, '2026-08-01 18:30:00'),
    (@mt_kvc1, NULL, 'PENDING_ACCEPTANCE', 'CREATE', 'Quản lý ghi nhận vi phạm nội quy wifi.', @manager_id, '2026-08-07 08:00:00'),
    (@mt_kvc2, NULL, 'PENDING_ACCEPTANCE', 'CREATE', 'Quản lý tạo phiếu vệ sinh khu vực chung.', @manager_id, '2026-08-09 16:10:00'),
    (@mt_nb1, NULL, 'PENDING_ACCEPTANCE', 'CREATE', 'Tạo phiếu bảo trì nội bộ camera cổng.', @manager_id, '2026-08-11 06:30:00'),
    (@mt_nb2, NULL, 'PENDING_ACCEPTANCE', 'CREATE', 'Tạo phiếu bảo trì định kỳ máy bơm.', @manager_id, '2026-07-28 08:00:00'),
    (@mt_nb3, NULL, 'PENDING_ACCEPTANCE', 'CREATE', 'Tạo kế hoạch sơn lại biển số phòng.', @manager_id, '2026-08-14 08:15:00'),

    (@mt102, 'PENDING_ACCEPTANCE', 'ACCEPTED', 'ACCEPT', 'Quản lý đã tiếp nhận và theo dõi áp lực nước.', @manager_id, '2026-08-13 08:15:00'),
    (@mt103, 'PENDING_ACCEPTANCE', 'ACCEPTED', 'ACCEPT', 'Quản lý đã tiếp nhận phiếu máy lạnh.', @manager_id, '2026-08-11 08:00:00'),
    (@mt103, 'ACCEPTED', 'IN_PROGRESS', 'START_PROGRESS', 'Kỹ thuật viên đang vệ sinh và kiểm tra máy lạnh.', @manager_id, '2026-08-13 14:30:00'),
    (@mt104, 'PENDING_ACCEPTANCE', 'ACCEPTED', 'ACCEPT', 'Quản lý đã tiếp nhận phiếu khóa cửa.', @manager_id, '2026-08-08 08:10:00'),
    (@mt104, 'ACCEPTED', 'IN_PROGRESS', 'START_PROGRESS', 'Thợ khóa bắt đầu thay lõi khóa.', @manager_id, '2026-08-12 15:00:00'),
    (@mt104, 'IN_PROGRESS', 'WAITING_CONFIRMATION', 'REQUEST_CONFIRMATION', 'Đã thay khóa, chờ khách thuê xác nhận trên mobile.', @manager_id, '2026-08-12 16:20:00'),
    (@mt105, 'PENDING_ACCEPTANCE', 'ACCEPTED', 'ACCEPT', 'Quản lý đã tiếp nhận phiếu Internet.', @manager_id, '2026-08-05 09:30:00'),
    (@mt105, 'ACCEPTED', 'IN_PROGRESS', 'START_PROGRESS', 'Kỹ thuật viên bắt đầu kiểm tra đường truyền.', @manager_id, '2026-08-06 09:00:00'),
    (@mt105, 'IN_PROGRESS', 'WAITING_CONFIRMATION', 'REQUEST_CONFIRMATION', 'Đã khôi phục kết nối, chờ khách xác nhận.', @manager_id, '2026-08-06 11:00:00'),
    (@mt105, 'WAITING_CONFIRMATION', 'COMPLETED', 'CONFIRM_COMPLETED', 'Khách thuê xác nhận mạng đã ổn định.', @u105, '2026-08-06 18:20:00'),
    (@mt105, 'COMPLETED', 'COMPLETED', 'REVIEW', 'Khách thuê đánh giá 5 sao.', @u105, '2026-08-06 18:30:00'),
    (@mt106, 'PENDING_ACCEPTANCE', 'REJECTED', 'REJECT', 'Nội thất hiện tại còn sử dụng tốt.', @manager_id, '2026-08-04 15:20:00'),
    (@mt201, 'PENDING_ACCEPTANCE', 'CANCELLED', 'REJECT', 'Khách thuê xin hủy vì đã tự vệ sinh thoát sàn.', @u201, '2026-08-03 10:10:00'),
    (@mt202, 'PENDING_ACCEPTANCE', 'ACCEPTED', 'ACCEPT', 'Quản lý đã tiếp nhận phiếu thay kính.', @manager_id, '2026-08-02 08:00:00'),
    (@mt202, 'ACCEPTED', 'IN_PROGRESS', 'START_PROGRESS', 'Thợ kính bắt đầu đo và thay kính.', @manager_id, '2026-08-04 14:00:00'),
    (@mt202, 'IN_PROGRESS', 'WAITING_CONFIRMATION', 'REQUEST_CONFIRMATION', 'Đã thay kính, chờ khách xác nhận.', @manager_id, '2026-08-04 17:00:00'),
    (@mt202, 'WAITING_CONFIRMATION', 'COMPLETED', 'CONFIRM_COMPLETED', 'Khách thuê xác nhận đã hoàn tất.', @u202, '2026-08-05 08:50:00'),
    (@mt202, 'COMPLETED', 'COMPLETED', 'REVIEW', 'Khách thuê đánh giá 4 sao.', @u202, '2026-08-05 09:00:00'),
    (@mt_kvc1, 'PENDING_ACCEPTANCE', 'ACCEPTED', 'ACCEPT', 'Quản lý tiếp nhận biên bản vi phạm.', @manager_id, '2026-08-07 08:20:00'),
    (@mt_kvc1, 'ACCEPTED', 'IN_PROGRESS', 'START_PROGRESS', 'Khôi phục cấu hình bộ phát wifi dùng chung.', @manager_id, '2026-08-07 09:00:00'),
    (@mt_kvc1, 'IN_PROGRESS', 'WAITING_CONFIRMATION', 'REQUEST_CONFIRMATION', 'Đã xử lý cấu hình và ghi nhận vi phạm.', @manager_id, '2026-08-07 13:45:00'),
    (@mt_kvc1, 'WAITING_CONFIRMATION', 'COMPLETED', 'CONFIRM_COMPLETED', 'Chủ trọ xác nhận hoàn tất xử lý.', @manager_id, '2026-08-07 14:00:00'),
    (@mt_kvc2, 'PENDING_ACCEPTANCE', 'ACCEPTED', 'ACCEPT', 'Đã giao ca vệ sinh xử lý hành lang tầng 3.', @manager_id, '2026-08-10 07:30:00'),
    (@mt_nb1, 'PENDING_ACCEPTANCE', 'ACCEPTED', 'ACCEPT', 'Đã phân công đội kỹ thuật nội bộ.', @manager_id, '2026-08-11 07:00:00'),
    (@mt_nb1, 'ACCEPTED', 'IN_PROGRESS', 'START_PROGRESS', 'Đang kiểm tra nguồn và tín hiệu camera.', @manager_id, '2026-08-13 20:00:00'),
    (@mt_nb2, 'PENDING_ACCEPTANCE', 'ACCEPTED', 'ACCEPT', 'Đã duyệt kế hoạch bảo dưỡng máy bơm.', @manager_id, '2026-07-28 09:00:00'),
    (@mt_nb2, 'ACCEPTED', 'IN_PROGRESS', 'START_PROGRESS', 'Đội kỹ thuật bắt đầu bảo dưỡng.', @manager_id, '2026-08-02 08:00:00'),
    (@mt_nb2, 'IN_PROGRESS', 'WAITING_CONFIRMATION', 'REQUEST_CONFIRMATION', 'Đã chạy thử máy bơm sau bảo dưỡng.', @manager_id, '2026-08-02 10:15:00'),
    (@mt_nb2, 'WAITING_CONFIRMATION', 'COMPLETED', 'CONFIRM_COMPLETED', 'Chủ trọ xác nhận hệ thống vận hành ổn định.', @manager_id, '2026-08-02 10:30:00');
