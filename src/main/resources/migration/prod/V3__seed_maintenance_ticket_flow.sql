-- =========================================================
-- V3__seed_maintenance_ticket_flow.sql
-- Seed thêm dữ liệu test cho mobile màn Báo cáo sự cố / Phiếu sự cố.
-- Run AFTER V0__init.sql + V1__seed_5_tenant_accounts_full_test_data.sql
-- Không sửa cấu trúc bảng; chỉ bổ sung dữ liệu demo cho các trạng thái ticket.
-- =========================================================

SET NAMES utf8mb4;

SET @OLD_FOREIGN_KEY_CHECKS := @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;
START TRANSACTION;

-- Base seed references
SET @property_id := (SELECT id FROM properties WHERE property_code='HD1-SEED' ORDER BY id DESC LIMIT 1);

SET @owner_user_id   := (SELECT id FROM users WHERE phone='0900000001' LIMIT 1);
SET @manager_user_id := (SELECT id FROM users WHERE phone='0900000002' LIMIT 1);

SET @u201 := (SELECT id FROM users WHERE phone='0912000201' LIMIT 1);
SET @u202 := (SELECT id FROM users WHERE phone='0912000202' LIMIT 1);
SET @u203 := (SELECT id FROM users WHERE phone='0912000203' LIMIT 1);
SET @u204 := (SELECT id FROM users WHERE phone='0912000204' LIMIT 1);
SET @u205 := (SELECT id FROM users WHERE phone='0912000205' LIMIT 1);

SET @room201 := (SELECT id FROM rooms WHERE property_id=@property_id AND room_code='201' LIMIT 1);
SET @room202 := (SELECT id FROM rooms WHERE property_id=@property_id AND room_code='202' LIMIT 1);
SET @room203 := (SELECT id FROM rooms WHERE property_id=@property_id AND room_code='203' LIMIT 1);
SET @room204 := (SELECT id FROM rooms WHERE property_id=@property_id AND room_code='204' LIMIT 1);
SET @room205 := (SELECT id FROM rooms WHERE property_id=@property_id AND room_code='205' LIMIT 1);

SET @contract201 := (SELECT id FROM lease_contracts WHERE contract_code='HD-2026-201' LIMIT 1);
SET @contract202 := (SELECT id FROM lease_contracts WHERE contract_code='HD-2026-202' LIMIT 1);
SET @contract203 := (SELECT id FROM lease_contracts WHERE contract_code='HD-2026-203' LIMIT 1);
SET @contract204 := (SELECT id FROM lease_contracts WHERE contract_code='HD-2026-204' LIMIT 1);
SET @contract205 := (SELECT id FROM lease_contracts WHERE contract_code='HD-2026-205' LIMIT 1);

-- ---------------------------------------------------------
-- 1) File đính kèm BEFORE/AFTER cho ticket.
-- Lưu ý: V0 hiện có FK maintenance_ticket_attachments.created_by -> tenants(id),
-- nên ở bảng attachments bên dưới để created_by=NULL để tránh sai FK.
-- ---------------------------------------------------------
INSERT INTO file_metadata (owner_user_id, storage_key, original_name, mime_type, size_bytes, sha256_checksum, category, is_sensitive)
VALUES
(@u201,'seed/tickets/SC-2026-201-001/before-leak-1.jpg','before-leak-1.jpg','image/jpeg',245000,REPEAT('a',64),'TICKET_ATTACHMENT',FALSE),
(@u201,'seed/tickets/SC-2026-201-001/before-leak-2.jpg','before-leak-2.jpg','image/jpeg',252000,REPEAT('b',64),'TICKET_ATTACHMENT',FALSE),
(@u201,'seed/tickets/SC-2026-201-001/before-leak-video.mp4','before-leak-video.mp4','video/mp4',2400000,REPEAT('c',64),'TICKET_ATTACHMENT',FALSE),

(@u202,'seed/tickets/SC-2026-202-001/before-electric-1.jpg','before-electric-1.jpg','image/jpeg',198000,REPEAT('d',64),'TICKET_ATTACHMENT',FALSE),

(@u203,'seed/tickets/SC-2026-203-001/before-water-1.jpg','before-water-1.jpg','image/jpeg',210000,REPEAT('e',64),'TICKET_ATTACHMENT',FALSE),
(@manager_user_id,'seed/tickets/SC-2026-203-001/after-water-1.jpg','after-water-1.jpg','image/jpeg',230000,REPEAT('f',64),'TICKET_ATTACHMENT',FALSE),

(@u205,'seed/tickets/SC-2026-205-001/before-door-1.jpg','before-door-1.jpg','image/jpeg',205000,REPEAT('1',64),'TICKET_ATTACHMENT',FALSE);

SET @file201_1 := (SELECT id FROM file_metadata WHERE storage_key='seed/tickets/SC-2026-201-001/before-leak-1.jpg' ORDER BY id DESC LIMIT 1);
SET @file201_2 := (SELECT id FROM file_metadata WHERE storage_key='seed/tickets/SC-2026-201-001/before-leak-2.jpg' ORDER BY id DESC LIMIT 1);
SET @file201_3 := (SELECT id FROM file_metadata WHERE storage_key='seed/tickets/SC-2026-201-001/before-leak-video.mp4' ORDER BY id DESC LIMIT 1);
SET @file202_1 := (SELECT id FROM file_metadata WHERE storage_key='seed/tickets/SC-2026-202-001/before-electric-1.jpg' ORDER BY id DESC LIMIT 1);
SET @file203_before := (SELECT id FROM file_metadata WHERE storage_key='seed/tickets/SC-2026-203-001/before-water-1.jpg' ORDER BY id DESC LIMIT 1);
SET @file203_after  := (SELECT id FROM file_metadata WHERE storage_key='seed/tickets/SC-2026-203-001/after-water-1.jpg' ORDER BY id DESC LIMIT 1);
SET @file205_1 := (SELECT id FROM file_metadata WHERE storage_key='seed/tickets/SC-2026-205-001/before-door-1.jpg' ORDER BY id DESC LIMIT 1);

-- ---------------------------------------------------------
-- 2) Ticket mới tạo từ màn Báo cáo sự cố: PENDING_ACCEPTANCE / Chờ tiếp nhận
-- Dùng để test: chọn loại sự cố, mô tả, đính kèm tối đa 3 file, gửi, sinh mã SC.
-- ---------------------------------------------------------
INSERT IGNORE INTO maintenance_tickets
(ticket_code, property_id, room_id, contract_id, created_by, ticket_scope, priority, category, title, description, status, assigned_to, created_at, updated_at)
VALUES
('SC-2026-201-001', @property_id, @room201, @contract201, @u201, 'TENANT_ROOM', 'HIGH', 'PLUMBING',
 'Vòi nước nhà vệ sinh bị rỉ',
 'Vòi nước phòng tắm chảy liên tục khi đã đóng van. Em đã đính kèm ảnh/video để quản lý kiểm tra.',
 'PENDING_ACCEPTANCE', NULL, '2026-05-18 08:15:00.000000', '2026-05-18 08:15:00.000000');
SET @ticket201 := (SELECT id FROM maintenance_tickets WHERE ticket_code='SC-2026-201-001' LIMIT 1);

INSERT INTO maintenance_ticket_attachments (ticket_id, file_id, attachment_phase, sort_order, created_by)
SELECT @ticket201, @file201_1, 'BEFORE', 0, NULL WHERE NOT EXISTS (
    SELECT 1 FROM maintenance_ticket_attachments WHERE ticket_id=@ticket201 AND file_id=@file201_1
);
INSERT INTO maintenance_ticket_attachments (ticket_id, file_id, attachment_phase, sort_order, created_by)
SELECT @ticket201, @file201_2, 'BEFORE', 1, NULL WHERE NOT EXISTS (
    SELECT 1 FROM maintenance_ticket_attachments WHERE ticket_id=@ticket201 AND file_id=@file201_2
);
INSERT INTO maintenance_ticket_attachments (ticket_id, file_id, attachment_phase, sort_order, created_by)
SELECT @ticket201, @file201_3, 'BEFORE', 2, NULL WHERE NOT EXISTS (
    SELECT 1 FROM maintenance_ticket_attachments WHERE ticket_id=@ticket201 AND file_id=@file201_3
);

INSERT INTO maintenance_ticket_events (ticket_id, from_status, to_status, note, created_by)
SELECT @ticket201, NULL, 'PENDING_ACCEPTANCE', 'Khách thuê tạo phiếu từ app mobile, đính kèm 3 file BEFORE.', @u201
WHERE NOT EXISTS (
    SELECT 1 FROM maintenance_ticket_events WHERE ticket_id=@ticket201 AND to_status='PENDING_ACCEPTANCE' AND note LIKE 'Khách thuê tạo phiếu%'
);

-- Notification gửi Owner + Manager khi ticket mới tạo
INSERT INTO notification_outbox (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status, scheduled_at)
SELECT 'TICKET_CREATED','MAINTENANCE_TICKET',@ticket201,@owner_user_id,'PUSH',
       'Có phiếu sự cố mới #SC-2026-201-001',
       'Phòng 201 báo vòi nước nhà vệ sinh bị rỉ. Vui lòng tiếp nhận trong 24 giờ.',
       JSON_OBJECT('ticket_code','SC-2026-201-001','room_code','201','status','PENDING_ACCEPTANCE'),
       'PENDING','2026-05-18 08:15:05.000000'
WHERE NOT EXISTS (
    SELECT 1 FROM notification_outbox WHERE event_type='TICKET_CREATED' AND target_type='MAINTENANCE_TICKET' AND target_id=@ticket201 AND recipient_user_id=@owner_user_id
);
INSERT INTO notification_outbox (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status, scheduled_at)
SELECT 'TICKET_CREATED','MAINTENANCE_TICKET',@ticket201,@manager_user_id,'PUSH',
       'Có phiếu sự cố mới #SC-2026-201-001',
       'Phòng 201 báo vòi nước nhà vệ sinh bị rỉ. Vui lòng tiếp nhận trong 24 giờ.',
       JSON_OBJECT('ticket_code','SC-2026-201-001','room_code','201','status','PENDING_ACCEPTANCE'),
       'PENDING','2026-05-18 08:15:05.000000'
WHERE NOT EXISTS (
    SELECT 1 FROM notification_outbox WHERE event_type='TICKET_CREATED' AND target_type='MAINTENANCE_TICKET' AND target_id=@ticket201 AND recipient_user_id=@manager_user_id
);

-- ---------------------------------------------------------
-- 3) Ticket đã tiếp nhận: ACCEPTED
-- ---------------------------------------------------------
INSERT IGNORE INTO maintenance_tickets
(ticket_code, property_id, room_id, contract_id, created_by, ticket_scope, priority, category, title, description, status, assigned_to, created_at, updated_at)
VALUES
('SC-2026-202-001', @property_id, @room202, @contract202, @u202, 'TENANT_ROOM', 'MEDIUM', 'ELECTRIC',
 'Ổ cắm gần bàn học bị lỏng',
 'Ổ cắm gần bàn học lỏng, cắm sạc laptop lúc được lúc không.',
 'ACCEPTED', @manager_user_id, '2026-05-17 19:30:00.000000', '2026-05-18 09:00:00.000000');
SET @ticket202 := (SELECT id FROM maintenance_tickets WHERE ticket_code='SC-2026-202-001' LIMIT 1);

INSERT INTO maintenance_ticket_attachments (ticket_id, file_id, attachment_phase, sort_order, created_by)
SELECT @ticket202, @file202_1, 'BEFORE', 0, NULL WHERE NOT EXISTS (
    SELECT 1 FROM maintenance_ticket_attachments WHERE ticket_id=@ticket202 AND file_id=@file202_1
);

INSERT INTO maintenance_ticket_events (ticket_id, from_status, to_status, note, created_by)
SELECT @ticket202, NULL, 'PENDING_ACCEPTANCE', 'Khách tạo ticket ổ cắm bị lỏng.', @u202
WHERE NOT EXISTS (SELECT 1 FROM maintenance_ticket_events WHERE ticket_id=@ticket202 AND to_status='PENDING_ACCEPTANCE');
INSERT INTO maintenance_ticket_events (ticket_id, from_status, to_status, note, created_by)
SELECT @ticket202, 'PENDING_ACCEPTANCE', 'ACCEPTED', 'Quản lý đã tiếp nhận, hẹn kiểm tra trong ngày.', @manager_user_id
WHERE NOT EXISTS (SELECT 1 FROM maintenance_ticket_events WHERE ticket_id=@ticket202 AND to_status='ACCEPTED');

-- ---------------------------------------------------------
-- 4) Ticket đang chờ khách xác nhận: WAITING_CONFIRMATION
-- Dùng để test màn "Xác nhận hoàn tất".
-- ---------------------------------------------------------
INSERT IGNORE INTO maintenance_tickets
(ticket_code, property_id, room_id, contract_id, created_by, ticket_scope, priority, category, title, description, status, assigned_to, worker_name, repair_items, completed_at, created_at, updated_at)
VALUES
('SC-2026-203-001', @property_id, @room203, @contract203, @u203, 'TENANT_ROOM', 'HIGH', 'PLUMBING',
 'Lavabo thoát nước chậm',
 'Lavabo bị nghẹt nhẹ, nước thoát rất chậm sau khi rửa mặt.',
 'WAITING_CONFIRMATION', @manager_user_id, 'Thợ Hùng - 0987654321', 'Thông tắc lavabo + vệ sinh siphon', '2026-05-18 10:45:00.000000',
 '2026-05-16 21:10:00.000000', '2026-05-18 10:45:00.000000');
SET @ticket203 := (SELECT id FROM maintenance_tickets WHERE ticket_code='SC-2026-203-001' LIMIT 1);

INSERT INTO maintenance_ticket_attachments (ticket_id, file_id, attachment_phase, sort_order, created_by)
SELECT @ticket203, @file203_before, 'BEFORE', 0, NULL WHERE NOT EXISTS (
    SELECT 1 FROM maintenance_ticket_attachments WHERE ticket_id=@ticket203 AND file_id=@file203_before
);
INSERT INTO maintenance_ticket_attachments (ticket_id, file_id, attachment_phase, sort_order, created_by)
SELECT @ticket203, @file203_after, 'AFTER', 0, NULL WHERE NOT EXISTS (
    SELECT 1 FROM maintenance_ticket_attachments WHERE ticket_id=@ticket203 AND file_id=@file203_after
);

INSERT INTO maintenance_ticket_events (ticket_id, from_status, to_status, note, created_by)
SELECT @ticket203, NULL, 'PENDING_ACCEPTANCE', 'Khách tạo ticket lavabo thoát nước chậm.', @u203
WHERE NOT EXISTS (SELECT 1 FROM maintenance_ticket_events WHERE ticket_id=@ticket203 AND to_status='PENDING_ACCEPTANCE');
INSERT INTO maintenance_ticket_events (ticket_id, from_status, to_status, note, created_by)
SELECT @ticket203, 'PENDING_ACCEPTANCE', 'ACCEPTED', 'Quản lý tiếp nhận và liên hệ thợ.', @manager_user_id
WHERE NOT EXISTS (SELECT 1 FROM maintenance_ticket_events WHERE ticket_id=@ticket203 AND to_status='ACCEPTED');
INSERT INTO maintenance_ticket_events (ticket_id, from_status, to_status, note, created_by)
SELECT @ticket203, 'ACCEPTED', 'IN_PROGRESS', 'Thợ đang xử lý thông tắc lavabo.', @manager_user_id
WHERE NOT EXISTS (SELECT 1 FROM maintenance_ticket_events WHERE ticket_id=@ticket203 AND to_status='IN_PROGRESS');
INSERT INTO maintenance_ticket_events (ticket_id, from_status, to_status, note, created_by)
SELECT @ticket203, 'IN_PROGRESS', 'WAITING_CONFIRMATION', 'Đã sửa xong, chờ khách xác nhận.', @manager_user_id
WHERE NOT EXISTS (SELECT 1 FROM maintenance_ticket_events WHERE ticket_id=@ticket203 AND to_status='WAITING_CONFIRMATION');

INSERT INTO maintenance_costs (ticket_id, cost_type, description, amount, paid_by, charge_invoice_id, receipt_file_id, created_by)
SELECT @ticket203, 'LABOR', 'Công thông tắc lavabo', 150000, 'LANDLORD', NULL, NULL, @manager_user_id
WHERE NOT EXISTS (
    SELECT 1 FROM maintenance_costs WHERE ticket_id=@ticket203 AND description='Công thông tắc lavabo'
);

INSERT INTO notification_outbox (event_type, target_type, target_id, recipient_user_id, channel, title, body, payload, status, scheduled_at)
SELECT 'TICKET_WAITING_CONFIRMATION','MAINTENANCE_TICKET',@ticket203,@u203,'PUSH',
       'Sự cố #SC-2026-203-001 đã sửa xong',
       'Lavabo phòng 203 đã xử lý xong. Vui lòng kiểm tra và xác nhận hoàn tất.',
       JSON_OBJECT('ticket_code','SC-2026-203-001','room_code','203','status','WAITING_CONFIRMATION'),
       'PENDING','2026-05-18 10:45:05.000000'
WHERE NOT EXISTS (
    SELECT 1 FROM notification_outbox WHERE event_type='TICKET_WAITING_CONFIRMATION' AND target_id=@ticket203 AND recipient_user_id=@u203
);

-- ---------------------------------------------------------
-- 5) Bổ sung review cho ticket room 204 đã có sẵn trong V1.
-- ---------------------------------------------------------
SET @ticket204 := (SELECT id FROM maintenance_tickets WHERE ticket_code='SC-2026-204-001' LIMIT 1);

INSERT INTO maintenance_reviews (ticket_id, reviewer_user_id, rating, comment)
SELECT @ticket204, @u204, 5, 'Sửa nhanh, thay remote dùng bình thường.'
WHERE @ticket204 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM maintenance_reviews WHERE ticket_id=@ticket204 AND reviewer_user_id=@u204);

INSERT INTO maintenance_ticket_events (ticket_id, from_status, to_status, note, created_by)
SELECT @ticket204, 'WAITING_CONFIRMATION', 'COMPLETED', 'Khách xác nhận hoàn tất và đánh giá 5 sao.', @u204
WHERE @ticket204 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM maintenance_ticket_events WHERE ticket_id=@ticket204 AND to_status='COMPLETED' AND note LIKE 'Khách xác nhận%');

-- ---------------------------------------------------------
-- 6) Ticket bị từ chối: REJECTED
-- Dùng để test tab/lọc trạng thái Từ chối.
-- ---------------------------------------------------------
INSERT IGNORE INTO maintenance_tickets
(ticket_code, property_id, room_id, contract_id, created_by, ticket_scope, priority, category, title, description, status, rejection_reason, assigned_to, created_at, updated_at)
VALUES
('SC-2026-205-001', @property_id, @room205, @contract205, @u205, 'TENANT_ROOM', 'LOW', 'DOOR_LOCK',
 'Cửa phòng đóng hơi kêu',
 'Cửa phòng đóng mở có tiếng kêu nhẹ, muốn quản lý kiểm tra.',
 'REJECTED', 'Đã kiểm tra trực tiếp, cửa hoạt động bình thường; chỉ cần tra dầu định kỳ.', @manager_user_id,
 '2026-05-15 20:20:00.000000', '2026-05-16 08:30:00.000000');
SET @ticket205 := (SELECT id FROM maintenance_tickets WHERE ticket_code='SC-2026-205-001' LIMIT 1);

INSERT INTO maintenance_ticket_attachments (ticket_id, file_id, attachment_phase, sort_order, created_by)
SELECT @ticket205, @file205_1, 'BEFORE', 0, NULL WHERE NOT EXISTS (
    SELECT 1 FROM maintenance_ticket_attachments WHERE ticket_id=@ticket205 AND file_id=@file205_1
);

INSERT INTO maintenance_ticket_events (ticket_id, from_status, to_status, note, created_by)
SELECT @ticket205, NULL, 'PENDING_ACCEPTANCE', 'Khách tạo ticket cửa phòng đóng hơi kêu.', @u205
WHERE NOT EXISTS (SELECT 1 FROM maintenance_ticket_events WHERE ticket_id=@ticket205 AND to_status='PENDING_ACCEPTANCE');
INSERT INTO maintenance_ticket_events (ticket_id, from_status, to_status, note, created_by)
SELECT @ticket205, 'PENDING_ACCEPTANCE', 'REJECTED', 'Từ chối: cửa hoạt động bình thường, tra dầu định kỳ.', @manager_user_id
WHERE NOT EXISTS (SELECT 1 FROM maintenance_ticket_events WHERE ticket_id=@ticket205 AND to_status='REJECTED');

COMMIT;
SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;
