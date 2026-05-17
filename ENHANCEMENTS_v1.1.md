# 🚀 NÂNG CẤP HỆ THỐNG ĐẤU GIÁ - BẢN CƠ BẢN

## ✅ NHỮNG CẢI TIẾN ĐÃ THỰC HIỆN

### 1️⃣ LỰA CHỌN ƯU TIÊN CAO: Auto-Extend Timer (Core Feature)
**Vị trí:** `Auction.java` + `AuctionManager.java`

**Tính năng:**
- Khi có bid được đặt trong 2 phút cuối, tự động kéo dài thêm 2 phút
- Theo chuẩn thực tế của các sàn đấu giá (ngăn "sniping")
- Hệ thống theo dõi thời gian bid cuối cùng và tự động extend endTime

**Phương thức chính:**
```java
auction.checkAndAutoExtend();  // Gọi mỗi khi có bid mới
auction.getTimeRemainingSeconds();  // Lấy thời gian còn lại
auction.isExpired();  // Kiểm tra phiên hết hạn
```

---

### 2️⃣ VALIDATION INPUT: Áp Dụng PlateValidator (Ưu tiên thấp)
**Vị trí:** `RegisterSellController.java`

**Lợi ích:**
- Kiểm tra định dạng biển số trước khi gửi (29A-12345, 30B.54321, etc.)
- Sử dụng regex đã có sẵn: `PlateValidator.isValidPlate(plate)`
- Giảm dữ liệu không hợp lệ trong database

**Thay đổi:**
- Thêm validation vào `handleSubmit()` trước khi gọi ProductService
- Thông báo lỗi cụ thể nếu định dạng sai

---

### 3️⃣ EMAIL NOTIFICATION SERVICE: Gửi Thông Báo (Ưu tiên trung)
**Vị trí:** `EmailService.java` (file mới)

**Chức năng:**
- Gửi email khi biển số được duyệt
- Gửi email khi bị outbid
- Gửi email khi thắng cuộc

**Cách dùng:**
```java
EmailService.sendPlateApprovedNotification(email, plate, startingPrice);
EmailService.sendOutbidNotification(email, plate, newBidAmount);
EmailService.sendWinnerNotification(email, plate, finalBidAmount);
```

**Lưu ý:** Để chạy thực, cần:
1. Thêm dependency JavaMail vào pom.xml:
```xml
<dependency>
    <groupId>javax.mail</groupId>
    <artifactId>mail</artifactId>
    <version>1.4.7</version>
</dependency>
```
2. Cấu hình email và app password trong `EmailService.java`

---

### 4️⃣ WATCHLIST MODEL: Theo Dõi Biển Số (Ưu tiên trung)
**Vị trí:** `Watchlist.java` (file mới)

**Tính năng:**
- Người dùng có thể "theo dõi" biển số chưa mở đấu giá
- Tự động nhận thông báo khi biển số vào sàn

**Cấu trúc:**
```java
Watchlist w = new Watchlist(userId, plateNumber);
w.getPlateNumber();  // Biển số được theo dõi
w.getAddedDate();    // Ngày thêm vào
w.isNotified();      // Đã thông báo chưa
```

**Mở rộng trong tương lai:**
- Kết nối với WatchlistDAO để lưu vào database
- Thêm UI button để add/remove watchlist trong Dashboard
- Trigger email từ EmailService khi biển số được duyệt

---

### 5️⃣ CẬP NHẬT AUCTIONMANAGER
**Vị trí:** `AuctionManager.java`

**Thay đổi:**
```java
// Khi có bid mới, cập nhật lastBidTime và check auto-extend
auction.setLastBidTime(System.currentTimeMillis());
auction.checkAndAutoExtend();
```

---

## 📋 NHỮNG TÍNH NĂNG CHƯA IMPLEMENT (Để phát triển tiếp)

### ⏸️ Ưu Tiên CAO - WebSocket Real-time
**Tại sao?** AuctionServer có sẵn nhưng chưa kết nối với UI
**Cách implement:**
1. Thêm WebSocket library (spring-websocket hoặc standalone)
2. Thay polling 5 giây bằng push thực sự từ server
3. Cập nhật giá tức thì mà không lag

### 📈 Ưu Tiên TRUNG - Biểu Đồ Giá (Line Chart)
**Tại sao?** Trực quan hơn danh sách text, dễ thấy xu hướng
**Cách implement:**
1. Thêm ThemazeChart library vào pom.xml
2. Lấy dữ liệu BidTransaction từ AuctionRoomController
3. Vẽ XYChart trong AuctionRoom.fxml

### 🔔 Ưu Tiên TRUNG - Notification Toast/Badge UI
**Tại sao?** UX tốt hơn khi có người outbid
**Cách implement:**
1. Thêm ControlsFX library (NotificationPane)
2. Show toast khi áp dụng polling

### 📄 Ưu Tiên THẤP - Phân Trang Dashboard
**Tại sao?** Nếu có 1000 auctions, page load sẽ chậm
**Cách implement:**
1. Thêm Pagination control trong Dashboard.fxml
2. Load 10-20 auctions mỗi trang
3. Sử dụng ItemDAO.getPaginatedAuctions(page, size)

### 🎨 Ưu Tiên THẤP - Dark/Light Mode
**Tại sao?** Thân thiện hơn, dễ áp dụng CSS variables
**Cách implement:**
1. Tạo two CSS files: dark.css & light.css
2. Toggle trong Settings
3. Thay thế hardcode #1a2230 bằng CSS variables

---

## 🔧 HƯỚNG DẪN CÀI ĐẶT

### Bước 1: Cập nhật pom.xml (nếu muốn dùng Email)
```xml
<dependency>
    <groupId>javax.mail</groupId>
    <artifactId>mail</artifactId>
    <version>1.4.7</version>
</dependency>

<dependency>
    <groupId>org.mariadb.jdbc</groupId>
    <artifactId>mariadb-java-client</artifactId>
    <version>3.0.8</version>
</dependency>
```

### Bước 2: Tạo Bảng Watchlist trong Database
```sql
CREATE TABLE watchlist (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    plate_number VARCHAR(20) NOT NULL,
    added_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notified BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE(user_id, plate_number)
);
```

### Bước 3: Cấu Hình Email (nếu dùng)
Trong `EmailService.java`, thay đổi:
```java
private static final String SENDER_EMAIL = "your-email@gmail.com";
private static final String APP_PASSWORD = "your-app-password";
```

---

## 🎯 TÍNH NĂNG TIẾP THEO ĐỀ NGHỊ

1. **Socket/WebSocket Real-time** - Thay polling 5s bằng push ngay lập tức
2. **Line Chart Giá** - Hiển thị xu hướng giá trực quan
3. **Notification Toast** - Thông báo tức thì trên UI khi outbid
4. **Pagination** - Chia trang dashboard cho 1000+ auctions
5. **Dark Mode** - Toggle giao diện tối/sáng
6. **Admin Dashboard** - Quản lý watchlist, email logs, bids history

---

**Ngày cập nhật:** 17/05/2026
**Phiên bản:** v1.1 (Basic Enhancement)

