-- ===================================================================
-- DATABASE MIGRATION: Thêm trường thời gian và người tham gia
-- ===================================================================

-- Cập nhật bảng items với các trường mới cho hệ thống thời gian đấu giá
ALTER TABLE items
ADD COLUMN IF NOT EXISTS registration_duration INT DEFAULT 1440 COMMENT 'Thời gian đăng ký tham gia (phút)',
ADD COLUMN IF NOT EXISTS registration_start_time DATETIME COMMENT 'Khi bắt đầu giai đoạn đăng ký',
ADD COLUMN IF NOT EXISTS registration_end_time DATETIME COMMENT 'Khi kết thúc giai đoạn đăng ký',
ADD COLUMN IF NOT EXISTS auction_start_time DATETIME COMMENT 'Khi bắt đầu giai đoạn đấu giá',
ADD COLUMN IF NOT EXISTS auction_end_time DATETIME COMMENT 'Khi kết thúc giai đoạn đấu giá',
ADD COLUMN IF NOT EXISTS participant_count INT DEFAULT 0 COMMENT 'Số lượng người tham gia đấu giá',
ADD COLUMN IF NOT EXISTS auction_status VARCHAR(20) DEFAULT 'WAITING' COMMENT 'Trạng thái phiên: WAITING, REGISTRATION, AUCTION, FINISHED';

-- Thêm index để tối ưu hiệu suất truy vấn
ALTER TABLE items
ADD INDEX IF NOT EXISTS idx_auction_status (auction_status),
ADD INDEX IF NOT EXISTS idx_approval_status (approval_status),
ADD INDEX IF NOT EXISTS idx_auction_end_time (auction_end_time);

-- ===================================================================
-- SEED TÀI KHOẢN ADMIN MẶC ĐỊNH (nếu chưa có)
-- ===================================================================
-- Chạy query này nếu bảng users đã được tạo

INSERT IGNORE INTO users
(username, password, email, full_name, user_type, phone, address)
VALUES
('vpa_admin', 'Admin@2024!', 'admin@vpa.vn', 'QUẢN TRỊ VIÊN HỆ THỐNG', 'Admin', '1900636380', 'Hà Nội');

-- ===================================================================
-- KIỂM TRA DỮ LIỆU
-- ===================================================================

-- Kiểm tra tài khoản admin
SELECT id, username, user_type, full_name FROM users WHERE username = 'vpa_admin';

-- Kiểm tra cấu trúc bảng items
DESCRIBE items;

-- Lấy danh sách biển số đang chờ duyệt
SELECT plate_number, start_price, seller_name, approval_status
FROM items
WHERE approval_status = 'PENDING';

-- Lấy danh sách biển số đã duyệt chưa kết thúc
SELECT plate_number, start_price, current_price, participant_count, auction_status
FROM items
WHERE approval_status = 'APPROVED' AND auction_status != 'FINISHED';

-- Lấy danh sách biển số đã kết thúc phiên
SELECT plate_number, start_price, current_price, participant_count, auction_end_time
FROM items
WHERE auction_status = 'FINISHED'
ORDER BY auction_end_time DESC;

