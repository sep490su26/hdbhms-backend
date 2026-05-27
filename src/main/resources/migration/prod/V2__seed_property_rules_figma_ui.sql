-- =========================================================
-- V5 - Seed full boarding-house rules for mobile "Noi quy nha tro" screen
-- Purpose: store rule content in DB while Flutter only renders the Figma-like UI.
-- Safe for Flyway: no DROP DATABASE / CREATE DATABASE / USE.
-- =========================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- Add display metadata so the mobile app can group rules exactly like the Figma UI.
ALTER TABLE property_rules
  ADD COLUMN  rule_category ENUM('GENERAL','SECURITY','HYGIENE','UTILITY','FINE') NOT NULL DEFAULT 'GENERAL' AFTER rule_code,
  ADD COLUMN  icon_key VARCHAR(50) NULL AFTER rule_category,
  ADD COLUMN fine_unit VARCHAR(50) NULL AFTER default_fine_amount,
  ADD COLUMN  is_highlight BOOLEAN NOT NULL DEFAULT FALSE AFTER fine_unit,
  ADD COLUMN  display_note VARCHAR(1000) NULL AFTER is_highlight;

-- Pick the seed property. If your property_code is different, this fallback takes the first property.
SET @property_id := (
  SELECT id FROM properties
  WHERE property_code = 'HD1-SEED'
  ORDER BY id
  LIMIT 1
);
SET @property_id := COALESCE(@property_id, (SELECT id FROM properties ORDER BY id LIMIT 1));

-- Keep the Figma header date stable via updated_at. The app can render max(updated_at) as "CẬP NHẬT: 20/10/2023".
SET @rules_updated_at := '2023-10-20 08:00:00.000000';

INSERT INTO property_rules
(property_id, rule_code, rule_category, icon_key, title, description, default_fine_amount, fine_unit, is_highlight, display_note, sort_order, status, created_at, updated_at)
VALUES
-- Quy định chung
(@property_id, 'GENERAL_001', 'GENERAL', 'info',
 'Mỗi phòng lưu trú đúng số lượng người đã đăng ký',
 'Mỗi phòng lưu trú đúng số lượng người đã đăng ký. Mọi thay đổi về số lượng người phải báo cho chủ trọ hoặc quản lí.',
 NULL, NULL, FALSE, NULL, 1, 'ACTIVE', @rules_updated_at, @rules_updated_at),
(@property_id, 'GENERAL_002', 'GENERAL', 'info',
 'Không kinh doanh, tàng trữ hàng cấm',
 'Không kinh doanh, tàng trữ hàng cấm, các chất dễ cháy nổ trong khuôn viên nhà trọ.',
 NULL, NULL, FALSE, NULL, 2, 'ACTIVE', @rules_updated_at, @rules_updated_at),
(@property_id, 'GENERAL_003', 'GENERAL', 'info',
 'Giữ gìn tài sản chung',
 'Giữ gìn tài sản chung và có ý thức bảo vệ cơ sở vật chất của tòa nhà.',
 NULL, NULL, FALSE, NULL, 3, 'ACTIVE', @rules_updated_at, @rules_updated_at),

-- An ninh
(@property_id, 'SECURITY_004', 'SECURITY', 'shield',
 'Khóa cửa cẩn thận khi ra vào',
 'Khóa cửa cẩn thận khi ra vào. Không cung cấp mật mã hoặc chìa khóa cho người lạ.',
 NULL, NULL, FALSE, NULL, 4, 'ACTIVE', @rules_updated_at, @rules_updated_at),
(@property_id, 'SECURITY_006', 'SECURITY', 'shield',
 'Khách đến thăm phải báo chủ trọ',
 'Khách đến thăm phải báo với chủ trọ và được chủ trọ đồng ý.',
 NULL, NULL, FALSE, NULL, 6, 'ACTIVE', @rules_updated_at, @rules_updated_at),

-- Vệ sinh
(@property_id, 'HYGIENE_007', 'HYGIENE', 'cleaning',
 'Để rác đúng nơi quy định',
 'Để rác đúng nơi quy định.',
 NULL, NULL, FALSE, NULL, 7, 'ACTIVE', @rules_updated_at, @rules_updated_at),
(@property_id, 'HYGIENE_008', 'HYGIENE', 'cleaning',
 'Không để đồ cá nhân ở hành lang chung',
 'Tuyệt đối không để giày dép, đồ đạc cá nhân ở hành lang chung.',
 NULL, NULL, FALSE, NULL, 8, 'ACTIVE', @rules_updated_at, @rules_updated_at),

-- Tiện ích
(@property_id, 'UTILITY_010', 'UTILITY', 'utility',
 'Sử dụng điện nước tiết kiệm',
 'Sử dụng điện, nước tiết kiệm. Tắt các thiết bị khi ra khỏi phòng.',
 NULL, NULL, FALSE, NULL, 10, 'ACTIVE', @rules_updated_at, @rules_updated_at),
(@property_id, 'UTILITY_011', 'UTILITY', 'utility',
 'Khu vực giặt sấy chung',
 'Khu vực giặt sấy chung: Vui lòng lấy đồ ngay sau khi hoàn tất chu trình.',
 NULL, NULL, FALSE, NULL, 11, 'ACTIVE', @rules_updated_at, @rules_updated_at),

-- Các khoản phạt
(@property_id, 'WIFI_RESET', 'FINE', 'wifi_off',
 'Reset Wi-Fi',
 'Không tự ý reset modem hoặc đổi mật khẩu Wi-Fi khi chưa được phép.',
 200000, 'lần', TRUE, NULL, 100, 'ACTIVE', @rules_updated_at, @rules_updated_at),
(@property_id, 'FINE_UNAUTHORIZED_REPAIR', 'FINE', 'repair_forbidden',
 'Tự ý sửa chữa không phép',
 'Theo thực tế hư hỏng + Phạt vi phạm hành chính tương ứng.',
 NULL, NULL, TRUE, 'Theo thực tế hư hỏng + Phạt vi phạm hành chính tương ứng.', 101, 'ACTIVE', @rules_updated_at, @rules_updated_at)
ON DUPLICATE KEY UPDATE
  rule_category = VALUES(rule_category),
  icon_key = VALUES(icon_key),
  title = VALUES(title),
  description = VALUES(description),
  default_fine_amount = VALUES(default_fine_amount),
  fine_unit = VALUES(fine_unit),
  is_highlight = VALUES(is_highlight),
  display_note = VALUES(display_note),
  sort_order = VALUES(sort_order),
  status = VALUES(status),
  updated_at = VALUES(updated_at);

-- If the old seed already inserted WIFI_RESET, this guarantees it is displayed as a highlighted fine.
UPDATE property_rules
SET rule_category = 'FINE',
    icon_key = 'wifi_off',
    title = 'Reset Wi-Fi',
    description = 'Không tự ý reset modem hoặc đổi mật khẩu Wi-Fi khi chưa được phép.',
    default_fine_amount = 200000,
    fine_unit = 'lần',
    is_highlight = TRUE,
    sort_order = 100,
    status = 'ACTIVE',
    updated_at = @rules_updated_at
WHERE property_id = @property_id AND rule_code = 'WIFI_RESET';

SET FOREIGN_KEY_CHECKS = 1;
