INSERT INTO notification_templates (template_key, channel, title_template, body_template, status)
VALUES
('ROOM_TRANSFER_HOLDER_NOMINATION_REQUESTED', 'PUSH', 'Bạn được đề cử làm người đại diện phòng', CONCAT('Yêu cầu [[', '$', '{requestCode}', ']] cần bạn xác nhận làm người đại diện mới của [[', '$', '{oldRoomName}', ']] sau khi người hiện tại chuyển đi. Vui lòng phản hồi để quản lý tiếp tục xử lý.'), 'ACTIVE'),
('ROOM_TRANSFER_HOLDER_NOMINATION_REQUESTED', 'WEB', 'Bạn được đề cử làm người đại diện phòng', CONCAT('Yêu cầu [[', '$', '{requestCode}', ']] cần bạn xác nhận làm người đại diện mới của [[', '$', '{oldRoomName}', ']] sau khi người hiện tại chuyển đi. Vui lòng phản hồi để quản lý tiếp tục xử lý.'), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    title_template = VALUES(title_template),
    body_template = VALUES(body_template),
    status = VALUES(status);
