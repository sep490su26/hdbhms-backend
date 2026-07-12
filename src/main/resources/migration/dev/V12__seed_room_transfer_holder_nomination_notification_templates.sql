INSERT INTO notification_templates (template_key, channel, title_template, body_template, status)
VALUES
('ROOM_TRANSFER_HOLDER_NOMINATION_REQUESTED', 'PUSH', 'Xac nhan holder moi', CONCAT('Ban duoc de cu lam holder moi cho phong [[', '$', '{oldRoomName}', ']]. Ma yeu cau: [[', '$', '{requestCode}', ']].'), 'ACTIVE'),
('ROOM_TRANSFER_HOLDER_NOMINATION_REQUESTED', 'WEB', 'Xac nhan holder moi', CONCAT('Ban duoc de cu lam holder moi cho phong [[', '$', '{oldRoomName}', ']]. Ma yeu cau: [[', '$', '{requestCode}', ']].'), 'ACTIVE')
ON DUPLICATE KEY UPDATE
    title_template = VALUES(title_template),
    body_template = VALUES(body_template),
    status = VALUES(status);
