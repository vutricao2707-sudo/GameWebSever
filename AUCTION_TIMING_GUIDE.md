# 🎯 HƯỚNG DẪN HOÀN CHỈNH: HỆ THỐNG THỜI GIAN ĐẤU GIÁ

## 📋 TÓUM TẮT CẬP NHẬT

Hệ thống đã được cập nhật với các tính năng sau:

### ✅ 1. TÀI KHOẢN ADMIN MẶC ĐỊNH
- **Username:** `vpa_admin`
- **Password:** `Admin@2024!`
- **Không thể tạo qua giao diện, chỉ tồn tại trong Database**
- Có quyền duyệt biển số và đặt thời gian đăng ký

### ✅ 2. KIỂM TRA TRÙNG
- **Tài khoản trùng** - Khi đăng ký, hệ thống kiểm tra username đã tồn tại chưa
- **Biển số trùng** - Khi đăng ký bán, biển số không được phép trùng với server
- **Tài khoản cấm** - Không thể tạo tài khoản: `admin`, `vpa_admin`, `root`, `system`

### ✅ 3. COOLDOWN 1 PHÚT
- Sau khi đặt giá, user phải **chờ 1 phút** mới được đặt tiếp
- Tránh spam đặt giá trong phòng đấu giá

### ✅ 4. GIÁ HIỆN TẠI = GIÁ KHỞI ĐIỂM
- Giá hay bước giá **bắt đầu từ giá người bán đăng** (start_price)
- Bước nhảy cố định: **5 triệu đ**
- Hệ số nhân (1x, 2x, 3x...) được chọn bởi user

### ✅ 5. THỜI GIAN ĐẤU GIÁ
- **Giai đoạn 1 - Đăng ký:** Do admin chỉ định khi duyệt (mặc định 60 phút)
  - Bắt đầu: Khi admin bấm duyệt
  - Kết thúc: Sau X phút
  
- **Giai đoạn 2 - Đấu giá:** Cố định 60 phút
  - Bắt đầu: Khi giai đoạn đăng ký hết
  - Kết thúc: Sau 60 phút

### ✅ 6. PERSISTENCE (GIỮ DỮ LIỆU)
- ✔️ Tắt/mở app → Biển số vẫn hiển thị
- ✔️ Giá hiện tại được lưu vào database
- ✔️ Số người tham gia được cập nhật liên tục
- ✔️ Trạng thái phiên được lưu (`WAITING`, `REGISTRATION`, `AUCTION`, `FINISHED`)

### ✅ 7. BIỂN SỐ HẾT PHIÊN
- Sau khi phiên đấu giá kết thúc (60 phút), biển số:
  - ❌ **Không hiển thị** ở trang Dashboard
  - ✔️ Vẫn lưu trữ trong Database với trạng thái `FINISHED`
  - ✔️ Có thể xem được ở trang "Lịch sử" hoặc "Kết quả"

### ✅ 8. SỐ NGƯỜI THAM GIA
- Cộng dồn số user đăng ký tham gia phiên đấu giá
- Hiển thị **liên tục** trên giao diện phòng đấu giá: `👥 N người tham gia`
- Cập nhật mỗi khi có user đăng ký

---

## 🔧 HƯỚNG DẪN THỰC HIỆN QUICK START

### BƯỚC 1: Kiểm tra Database Schema
Database đã được cập nhật với các trường mới:

```sql
-- Các trường mới trong bảng items:
- registration_duration (INT) - Thời gian đăng ký (phút)
- registration_start_time (DATETIME) - Khi nào bắt đầu đăng ký
- registration_end_time (DATETIME) - Khi nào kết thúc đăng ký
- auction_start_time (DATETIME) - Khi nào bắt đầu đấu giá
- auction_end_time (DATETIME) - Khi nào kết thúc đấu giá
- participant_count (INT) - Số người tham gia
- auction_status (VARCHAR) - Trạng thái ('WAITING', 'REGISTRATION', 'AUCTION', 'FINISHED')
```

### BƯỚC 2: Cập nhật Migration Script

Chạy script này trên Database để thêm trường mới:

```sql
-- Nếu bảng items đã tồn tại, thêm các trường thiếu
ALTER TABLE items 
ADD COLUMN IF NOT EXISTS registration_duration INT DEFAULT 1440,
ADD COLUMN IF NOT EXISTS registration_start_time DATETIME,
ADD COLUMN IF NOT EXISTS registration_end_time DATETIME,
ADD COLUMN IF NOT EXISTS auction_start_time DATETIME,
ADD COLUMN IF NOT EXISTS auction_end_time DATETIME,
ADD COLUMN IF NOT EXISTS participant_count INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS auction_status VARCHAR(20) DEFAULT 'WAITING';
```

### BƯỚC 3: Các File Java Đã Cập Nhật

**Chỉ cần copy các file này từ workspace:**

1. ✅ `DatabaseInitializer.java` - Schema mới
2. ✅ `ItemDAO.java` - Thêm 7 hàm mới
3. ✅ `AuctionRoomController.java` - Thêm cooldown + số người tham gia
4. ✅ `AdminController.java` - Hỏi thời gian đăng ký
5. ✅ `ProductService.java` - Loại bỏ item hết phiên
6. ✅ `AuctionRoom.fxml` - Thêm label số người

---

## 🎮 QUY TRÌNH HOẠT ĐỘNG

### 1️⃣ NGƯỜI BÁN ĐĂNG KÝ BÁN (RegisterSell)
```
📝 Người bán nhập: Biển số, Giá khởi điểm, Loại xe...
    ↓
🔍 Kiểm tra: Biển số đã tồn tại chưa?
    ↓
💾 Lưu vào DB: items (approval_status = 'PENDING')
    ↓
👮 Admin chờ duyệt
```

### 2️⃣ ADMIN DUYỆT BIỂN SỐ (Admin Dashboard)
```
📋 Admin xem danh sách chờ duyệt
    ↓
🔧 Admin bấm "✔" (Duyệt)
    ↓
⏱️ Hiển thị dialog: "Nhập thời gian đăng ký (phút)"
    ↓
💾 Cập nhật DB:
   - approval_status = 'APPROVED'
   - auction_status = 'REGISTRATION'
   - registration_start_time = NOW()
   - registration_end_time = NOW() + X phút
   - auction_start_time = registration_end_time
   - auction_end_time = registration_end_time + 60 phút
```

### 3️⃣ GIAI ĐOẠN ĐĂNG KÝ (Waiting Period)
```
⏰ Thời gian: X phút (do admin đặt)
📱 User xem Dashboard → Biển số hiển thị
🔗 User bấm "Đăng ký tham gia"
    ↓
👥 Cập nhật: participant_count += 1
```

### 4️⃣ GIAI ĐOẠN ĐẤU GIÁ (Auction Period)
```
⏰ Thời gian: 60 phút (cố định)
🎮 User vào phòng auctions (AuctionRoom)
💰 Bấm "ĐẶT GIÁ" → Tăng giá + Cooldown 1 phút
📊 Cập nhật: current_price, bid_history
⏳ Đếm ngược: 60:00 → 00:00
```

### 5️⃣ KẾT THÚC PHIÊN
```
⏰ Hết 60 phút
    ↓
🏁 Cập nhật DB:
   - auction_status = 'FINISHED'
   - status = 'INACTIVE'
    ↓
❌ Biển số KHÔNG hiển thị ở Dashboard nữa
✅ Vẫn lưu trong DB để xem lịch sử/kết quả
```

---

## 📊 BẢNG DỮ LIỆU TRƯỜNG MỚI

| Trường | Kiểu | Ý nghĩa | Ví dụ |
|--------|------|---------|-------|
| `registration_duration` | INT | Thời gian đăng ký (phút) | 60 |
| `registration_start_time` | DATETIME | Khi bắt đầu giai đoạn đăng ký | 2026-05-14 10:00:00 |
| `registration_end_time` | DATETIME | Khi hết đăng ký | 2026-05-14 11:00:00 |
| `auction_start_time` | DATETIME | Khi bắt đầu đấu giá | 2026-05-14 11:00:00 |
| `auction_end_time` | DATETIME | Khi hết đấu giá | 2026-05-14 12:00:00 |
| `participant_count` | INT | Số người đăng ký tham gia | 15 |
| `auction_status` | VARCHAR(20) | Trạng thái phiên | REGISTRATION / AUCTION / FINISHED |

---

## 🔐 TÀI KHOẢN ADMIN MẶC ĐỊNH

```
=== CREDENTIALS ===
📧 Username: vpa_admin
🔑 Password: Admin@2024!
🏢 Tên: QUẢN TRỊ VIÊN HỆ THỐNG
📱 SĐT: 1900636380
🏠 Địa chỉ: Hà Nội

=== HẠNG CHẾ ===
❌ KHÔNG thể tạo tài khoản admin qua giao diện đăng ký
❌ KHÔNG thể xóa tài khoản này
✔️ CHỈ có thể sửa mật khẩu trong DB trực tiếp nếu cần
```

---

## 🛑 NHỮNG THAY ĐỔI CẤM MẫU

### ✅ ĐÃ XÓA (Dữ liệu mẫu)

```java
// CŨ - KHÔNG CÒN DÙNG:
- BienzSoManageFrame.java - Không dùng
- Dữ liệu hardcode mẫu ở DataStore
- Lịch sử mẫu ở AuctionRoomController (addHistoryRow)

// MỚI:
- Toàn bộ dữ liệu từ Database
- Persistence qua mỗi lần đăng nhập
- Thời gian thực tế từ DB, không hardcode
```

---

## ⚠️ LƯU Ý QUAN TRỌNG

### 1. Database Initialization
- Khi app khởi động, `DatabaseInitializer.initDatabase()` chạy tự động
- Nó tạo bảng + Seed tài khoản admin một lần duy nhất

### 2. INSERT IGNORE
```sql
INSERT IGNORE INTO users ... ('vpa_admin', 'Admin@2024!', ...)
```
- Nếu admin đã tồn tại → Bỏ qua (INSERT IGNORE)
- Nếu admin chưa tồn tại → Tạo mới

### 3. Cooldown Logic
- Mỗi lần bấm "ĐẶT GIÁ" → Lưu `lastBidTime = System.currentTimeMillis()`
- Lần sau bấm → Check: `(currentTime - lastBidTime) < 60 giây`?
- Nếu đúng → Alert "Chờ X giây nữa mới được đặt giá"

### 4. Biển Số Hết Phiên
- ProductService sử dụng `getAllApprovedActiveItems()` (mới)
- Sẽ **loại bỏ** `auction_status = 'FINISHED'`
- Dashboard chỉ thấy biển số chưa kết thúc

---

## 🔗 LIÊN KẾT CÁC HÀM MỚI

### ItemDAO.java
```java
✅ startRegistrationPhase(plate, minutes)      - Bắt đầu giai đoạn đăng ký
✅ incrementParticipantCount(plate)             - Tăng số người tham gia
✅ getParticipantCount(plate)                   - Lấy số người tham gia
✅ getAuctionTimeRemaining(plate)               - Lấy thời gian còn lại
✅ finishAuction(plate)                         - Kết thúc phiên đấu giá
✅ getAllApprovedActiveItems()                  - Lấy CHỈ những biển chưa kết thúc
```

### AuctionRoomController.java
```java
✅ lastBidTime (biến)                           - Lưu thời điểm bấm lần cuối
✅ BID_COOLDOWN_SECONDS = 60L                   - Cooldown 1 phút
✅ updateParticipantCount()                     - Cập nhật số người hiển thị
✅ handlePlaceBid() → Thêm logic cooldown
```

### AdminController.java
```java
✅ handleApprove() → Thêm dialog hỏi thời gian
✅ ItemDAO.startRegistrationPhase() được gọi
```

---

## 📝 TESTING CHECKLIST

- [ ] Đăng nhập bằng `vpa_admin` / `Admin@2024!`
- [ ] Không thể tạo tài khoản `admin` (bị cấm)
- [ ] Không thể tạo tài khoản trùng username
- [ ] Không thể đăng ký bán biển số trùng
- [ ] Admin duyệt biển → Hỏi thời gian đăng ký
- [ ] Giai đoạn đăng ký chạy đúng giờ
- [ ] Giai đoạn đấu giá chạy 60 phút (cố định)
- [ ] Cooldown 1 phút sau mỗi lần đặt giá
- [ ] Số người tham gia cập nhật liên tục
- [ ] Biển số hết phiên không hiển thị ở Dashboard
- [ ] Tắt/mở app → Dữ liệu vẫn giữ nguyên

---

## 🎉 HOÀN THÀNH!

Tất cả các tính năng đã được cập nhật và sẵn sàng sử dụng.

**Bắt đầu bằng:** Đăng nhập với `vpa_admin` / `Admin@2024!`

