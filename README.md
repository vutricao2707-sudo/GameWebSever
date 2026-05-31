# PlateAuction
## 1. Bài toán và phạm vi hệ thống

Hệ thống đấu giá biển số xe trực tuyến giúp người dùng đăng ký, tham gia đấu giá và quản lý phiên đấu giá một cách minh bạch.

## 2. Công nghệ sử dụng, môi trường chạy và yêu cầu cài đặt

### Công nghệ sử dụng
- **Ngôn ngữ lập trình:** Java 21
- **Framework giao diện:** JavaFX 21.0.2
- **Quản lý dự án:** Maven 4.0.0
- **Cơ sở dữ liệu:** MySQL 8.x, Railway 
- **Kết nối cơ sở dữ liệu:** MySQL Connector/J 8.3.0, HikariCP 5.0.1 (Connection Pooling)
- **Giao tiếp real-time:** Java-WebSocket 1.5.4
- **Xử lý JSON:** org.json 20240303
- **Logging:** SLF4J 2.0.9
- **Testing:** JUnit 5 5.12.2

### Yêu cầu hệ thống
- **JDK:** 21 hoặc phiên bản cao hơn
- **MySQL:** Phiên bản 8.0 trở lên
- **Hệ điều hành:** Windows, Linux, macOS
- **RAM tối thiểu:** 2GB
- **Dung lượng ổ cứng:** 500MB

### Yêu cầu cài đặt

#### 1. Cài đặt JDK 21
- Tải và cài đặt JDK 21 từ [Oracle](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
- Thiết lập biến môi trường `JAVA_HOME`

#### 2. Cài đặt MySQL
- Tải và cài đặt MySQL Server từ [mysql.com](https://dev.mysql.com/downloads/mysql/)
- Khởi động dịch vụ MySQL

#### 3. Cấu hình cơ sở dữ liệu
- Tạo file `database.properties` từ `database.properties.example`
- Điền thông tin kết nối:
- db.host=localhost db.port=3306 db.name=auction_system db.user=your_username db.password=your_password

- Chạy script migration:
  ```sql
  SOURCE DATABASE_MIGRATION.sql;
  SOURCE MIGRATION_TIMING_SYSTEM.sql;
## 3. Cấu trúc dự án

 ## Cấu trúc dự án (Project Structure)

Dự án `PlateAuction` được tổ chức theo mô hình chuẩn của Maven, kết hợp với kiến trúc phân lớp để đảm bảo code dễ bảo trì và mở rộng.

```text
PlateAuction/
├── src/
│   ├── main/
│   │   ├── java/com/auction/
│   │   │   ├── controller/   # Xử lý sự kiện giao diện (JavaFX Controllers)
│   │   │   ├── database/     # Quản lý kết nối và các câu lệnh truy vấn CSDL
│   │   │   ├── dto/          # Data Transfer Objects - Gói dữ liệu luân chuyển giữa các lớp
│   │   │   ├── exception/    # Các lớp xử lý ngoại lệ (Custom Exceptions) tùy chỉnh
│   │   │   ├── main/         # Chứa class Main - Điểm khởi chạy của ứng dụng
│   │   │   ├── model/        # Các Entity / Model đại diện cho các bảng trong CSDL
│   │   │   ├── server/       # Khởi tạo và xử lý kết nối server (WebSocket)
│   │   │   ├── service/      # Chứa Business Logic (xử lý nghiệp vụ chính)
│   │   │   ├── ui/           # Các component, helper hỗ trợ render giao diện
│   │   │   └── util/         # Các hàm tiện ích (Helpers, Constants, Formatters...)
│   │   └── resources/
│   │       ├── fxml/         # Các file thiết kế giao diện FXML
│   │       ├── image/        # Chứa tài nguyên hình ảnh, icon, logo
│   │       ├── database.properties # File cấu hình kết nối Database
│   │       └── hikari.properties   # File cấu hình Connection Pool (HikariCP)
│   └── test/
│       └── java/com/auction/ # Thư mục chứa các Unit Tests
├── DATABASE_MIGRATION.sql    # Script SQL khởi tạo schema và dữ liệu ban đầu
├── MIGRATION_TIMING_SYSTEM.sql # Script SQL setup các event/trigger cho hệ thống timing
├── pom.xml                   # File cấu hình quản lý dependencies và build của Maven
└── README.md                 # Tài liệu hướng dẫn và giới thiệu dự án

```

##  4. Hướng Dẫn Chạy Dự Án

### Bước 1: Tải Mã Nguồn Về Máy

1. Mở thư mục bất kỳ muốn lưu dự án.
2. Click chuột phải vào khoảng trống, chọn **"Open Git Bash here"**.
3. Chạy lệnh sau để tải code về:

```bash
git clone https://github.com/r0naldaT/PlateAuction.git
```

4. Di chuyển vào thư mục dự án:

```bash
cd PlateAuction
```

---

### Bước 2: Biên Dịch Dự Án

Tại cửa sổ Git Bash, chạy lệnh sau để Maven tải thư viện và biên dịch code:

```bash
mvn clean compile
```

> ✅ Chờ đến khi thấy dòng **BUILD SUCCESS** màu xanh lá là thành công.

---

### Bước 3: Khởi Động Máy Chủ (Server)

> ⚠️ Vì đây là hệ thống Real-time, cần **bật Server trước**.

Tại cửa sổ Git Bash hiện tại, chạy lệnh:

```bash
mvn exec:java -Dexec.mainClass="com.auction.server.AuctionServer"
```

> Khi thấy dòng `✅ WebSocket Server khởi tạo trên port...` — **giữ nguyên cửa sổ này, không được tắt**.

---

### Bước 4: Khởi Động Giao Diện (Client)

1. Quay lại thư mục dự án (**không tắt** cửa sổ Git Bash cũ).
2. Click chuột phải, chọn **"Open Git Bash here"** để mở thêm một cửa sổ Git Bash **thứ 2**.
3. Tại cửa sổ thứ 2, chạy lệnh:

```bash
mvn javafx:run
```
# 5. Danh Sách Chức Năng Đã Hoàn Thành

---

## 1. Xác Thực & Quản Lý Tài Khoản

- [x] Đăng ký tài khoản mới với đầy đủ thông tin (họ tên, email, SĐT, mật khẩu)
- [x] Kiểm tra tên đăng nhập trùng lặp và tên đăng nhập bị cấm (reserved username)
- [x] Xác thực định dạng email và độ dài tối thiểu của mật khẩu (≥ 6 ký tự)
- [x] Xác nhận mật khẩu (confirm password)
- [x] Mã hoá mật khẩu bằng SHA-256 trước khi lưu vào Database
- [x] Đăng nhập và kiểm tra thông tin từ Database
- [x] Phân quyền tự động: **Admin** → trang quản trị, **User** → trang Dashboard
- [x] Quản lý phiên đăng nhập (Session) bằng `SessionManager` Singleton
- [x] Đăng xuất và dọn dẹp Session, giải phóng tài nguyên

---

## 2. Dashboard Người Dùng

- [x] Hiển thị danh sách biển số đấu giá dạng thẻ (card), lọc theo loại xe: **Ô tô / Xe máy**
- [x] Mỗi thẻ hiển thị: biển số, tỉnh/thành, số phiên, loại xe (badge), giá, trạng thái
- [x] Hiển thị popup **chi tiết biển số**: họ tên, SĐT, email, địa chỉ người bán
- [x] Đồng hồ đếm ngược real-time trên mỗi thẻ (đồng bộ từ server qua `AuctionSyncManager`)
- [x] Tự động làm mới danh sách mỗi **10 giây** (auto-refresh)
- [x] Cập nhật trạng thái nút đấu giá theo thời gian thực:
  - `ĐĂNG KÝ ĐẤU GIÁ` (xanh) → chờ khai mạc
  - `VÀO ĐẤU GIÁ NGAY` (vàng) → đang diễn ra
  - `ĐÃ KẾT THÚC` (xám) → phiên đóng
- [x] Chặn truy cập phòng đấu giá khi phiên chưa mở hoặc đã kết thúc (kiểm tra lại DB khi click)
- [x] Điều hướng đến: Lịch sử đấu giá, Kết quả, Đăng ký bán

---

## 3. Đăng Ký Bán Biển Số

- [x] Form đăng ký với đầy đủ thông tin: họ tên, SĐT, email, địa chỉ, loại xe, biển số, tỉnh, giá khởi điểm
- [x] Validate định dạng biển số theo chuẩn Việt Nam (regex: `29A-12345`, `51B1-12345`, ...)
- [x] Kiểm tra biển số trùng lặp trong hệ thống
- [x] Gửi yêu cầu với trạng thái `PENDING` — chờ Admin duyệt
- [x] Hiển thị thông báo thành công / lỗi sau khi gửi

---

## 4. Phòng Đấu Giá Real-time

- [x] Kết nối WebSocket tới server và tự động **reconnect** với exponential backoff (tối đa 30 giây)
- [x] Hiển thị đồng hồ đếm ngược (phút : giây), đổi màu khi còn ≤ 60 giây
- [x] Hiển thị giá hiện tại và cập nhật real-time khi có người trả giá mới
- [x] Điều chỉnh bước nhảy giá (step multiplier tăng/giảm)
- [x] Đặt giá thầu: lưu vào DB và broadcast tới tất cả người tham gia qua WebSocket
- [x] Hiển thị lịch sử đặt giá trực tiếp (live feed) trong phòng
- [x] Biểu đồ đường (Line Chart) theo dõi biến động giá theo thời gian
- [x] Hiển thị số lượng người tham gia real-time
- [x] Tải lại lịch sử đặt giá từ DB khi vào phòng (restore bid history)
- [x] Xử lý kết thúc phiên: xác định người thắng, hiển thị popup kết quả (thắng / thua)
- [x] Ngăn đặt giá thấp hơn giá hiện tại

---

## 5. Lịch Sử & Kết Quả Đấu Giá

- [x] Xem **lịch sử các lần đặt giá** của tài khoản hiện tại
- [x] Xem **kết quả các phiên đấu giá** đã kết thúc (người thắng, giá cuối)

---

## 6. Trang Quản Trị Admin

### 6.1 Duyệt Biển Số (Pending)
- [x] Xem danh sách biển số đang chờ duyệt (`PENDING`)
- [x] Duyệt biển số → chuyển trạng thái sang `APPROVED`, lên sàn đấu giá
- [x] Từ chối biển số → xoá khỏi hệ thống kèm xác nhận

### 6.2 Giám Sát Phiên Đấu Giá (Monitoring)
- [x] Theo dõi toàn bộ phiên đang diễn ra theo thời gian thực
- [x] Xem đếm ngược thời gian, giá hiện tại của từng phiên

### 6.3 Quản Lý Người Dùng (Users)
- [x] Xem danh sách toàn bộ tài khoản trong hệ thống
- [x] Xoá tài khoản người dùng

---

## 7. Hạ Tầng & Kỹ Thuật

- [x] **WebSocket Server** (Java-WebSocket) hỗ trợ nhiều client đồng thời, tự động ping giữ kết nối mỗi 30 giây
- [x] **Connection Pool** HikariCP (tối đa 5 luồng, tự dọn dẹp, chống rò rỉ kết nối)
- [x] **AuctionSyncManager**: đồng bộ thời gian đếm ngược từ server mỗi giây cho tất cả màn hình (observer pattern)
- [x] **AuctionFinishService**: tự động quét và kết thúc phiên đấu giá hết giờ (5 giây/lần), broadcast kết quả
- [x] Tự động khởi tạo schema Database khi app khởi động (CREATE TABLE IF NOT EXISTS)
- [x] Kiểm tra sức khoẻ Database (`DatabaseHealthCheck`) trước khi hiển thị giao diện
- [x] Shutdown hook: đóng WebSocket Server an toàn khi tắt ứng dụng
- [x] UI Throttle: chống spam cập nhật giao diện quá nhanh
- [x] Cấu hình tách biệt qua `database.properties` (không hard-code thông tin nhạy cảm)
- [x] Deploy server lên **Railway** với port động qua biến môi trường `PORT`
