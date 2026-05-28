# PlateAuction
## 1. Bài toán và phạm vi hệ thống

Hệ thống đấu giá biển số xe trực tuyến giúp người dùng đăng ký, tham gia đấu giá và quản lý phiên đấu giá một cách minh bạch.

## 2. Công nghệ sử dụng, môi trường chạy và yêu cầu cài đặt

### Công nghệ sử dụng
- **Ngôn ngữ lập trình:** Java 21
- **Framework giao diện:** JavaFX 21.0.2
- **Quản lý dự án:** Maven 4.0.0
- **Cơ sở dữ liệu:** MySQL 8.x
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

