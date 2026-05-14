-- ============================================================================
-- DATABASE MIGRATION - CẬP NHẬT BẢNG ITEMS VỚI HỆ THỐNG DUYỆT BIỂN SỐ
-- ============================================================================
--
-- ⚠️ CHỈ CHẠY SCRIPT NÀY NẾU BẢN CÓ DATABASE ĐANG CHẠY
--
-- Nếu bạn chạy lần đầu (database trống):
--   → Bỏ qua file này, app sẽ tự tạo bảng mới
--
-- Nếu bạn đã chạy app trước đó và database đã có dữ liệu:
--   → Chạy script này để thêm các trường cần thiết
--
-- ============================================================================

-- Bước 1: Kiểm tra bảng items đã có cột approval_status chưa
-- Nếu chưa có, thêm các cột mới
ALTER TABLE items ADD COLUMN IF NOT EXISTS approval_status VARCHAR(20) DEFAULT 'PENDING' AFTER status;
ALTER TABLE items ADD COLUMN IF NOT EXISTS seller_name VARCHAR(100) AFTER approval_status;
ALTER TABLE items ADD COLUMN IF NOT EXISTS seller_phone VARCHAR(15) AFTER seller_name;
ALTER TABLE items ADD COLUMN IF NOT EXISTS seller_email VARCHAR(100) AFTER seller_phone;
ALTER TABLE items ADD COLUMN IF NOT EXISTS seller_address TEXT AFTER seller_email;

-- Bước 2: Cập nhật dữ liệu cũ (nếu có)
-- Tất cả biển số cũ sẽ được đánh dấu là APPROVED (vì chúng đã được duyệt)
UPDATE items SET approval_status = 'APPROVED' WHERE approval_status IS NULL OR approval_status = '';

-- Bước 3: Kiểm tra kết quả
SELECT id, plate_number, status, approval_status, seller_name FROM items LIMIT 10;

-- ============================================================================
-- XONG! Database đã được cập nhật thành công
-- ============================================================================

