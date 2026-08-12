INSERT INTO notification_templates (template_key, channel, title_template, body_template, status)
VALUES
('ROOM_TRANSFER_HOLDER_NOMINATION_REQUESTED', 'PUSH', 'Xác nhận người đại diện mới', CONCAT('Bạn được đề cử làm người đại diện mới cho ', 'phòng [[', '$', '{oldRoomName}', ']]. Mã yêu cầu: [[', '$', '{requestCode}', ']].'), 'ACTIVE'),
('ROOM_TRANSFER_HOLDER_NOMINATION_REQUESTED', 'WEB', 'Xác nhận người đại diện mới', CONCAT('Bạn được đề cử làm người đại diện mới cho ', 'phòng [[', '$', '{oldRoomName}', ']]. Mã yêu cầu: [[', '$', '{requestCode}', ']].'), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    title_template = VALUES(title_template),
    body_template = VALUES(body_template),
    status = VALUES(status);
