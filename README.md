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

```

## **Phần 4: Câu lệnh dòng lệnh để chạy chương trình**

```markdown
## 4. Câu lệnh dòng lệnh để chạy chương trình

### 4.1 Xây dựng dự án (Build)

Trước khi chạy, cần build dự án thành JAR file:

```bash
# Windows (PowerShell)
cd D:\Project\PlateAuction
mvn clean package

# Linux/macOS
cd ~/PlateAuction
mvn clean package
```

After successful build, JAR files sẽ được sinh ra tại: `target/`
- `Auction-System-1.0-SNAPSHOT.jar` (regular)
- `Auction-System-1.0-SNAPSHOT-standalone.jar` (standalone - recommended)

### 4.2 Chạy ứng dụng Client (JavaFX)

Khuyến nghị sử dụng **standalone JAR** vì nó bao gồm tất cả dependencies:

**Windows (PowerShell):**
```powershell
cd D:\Project\PlateAuction\target
chcp 65001
java -jar .\Auction-System-1.0-SNAPSHOT-standalone.jar
```

**Windows (Command Prompt):**
```cmd
cd D:\Project\PlateAuction\target
java -jar Auction-System-1.0-SNAPSHOT-standalone.jar
```

**Linux/macOS:**
```bash
cd ~/PlateAuction/target
java -jar ./Auction-System-1.0-SNAPSHOT-standalone.jar
```

### 4.3 Chạy với Maven (thay thế cho JAR)

Nếu không muốn sử dụng JAR, có thể chạy trực tiếp từ Maven:

**Windows:**
```powershell
mvn -f pom.xml javafx:run
```

**Linux/macOS:**
```bash
mvn -f pom.xml javafx:run
```

### 4.4 Ghi chú quan trọng

- 🔴 **Yêu cầu:** MySQL phải đang chạy trước khi khởi động ứng dụng
- 🔴 **Character Encoding:** Command `chcp 65001` (Windows) để hiển thị tiếng Việt đúng
- 🟢 Nếu gặp lỗi port, thay đổi `PORT` trong `application.properties`


## **Phần 5: Hướng dẫn chạy Server/Client theo thứ tự**

```markdown
## 5. Hướng dẫn chạy Server/Client theo thứ tự cụ thể

### 5.1 Kiến trúc Server/Client

Hệ thống sử dụng kiến trúc **WebSocket** cho real-time communication:
- **Server:** `AuctionWebSocketServer` (WebSocket Server)
- **Client:** `App.java` (JavaFX Client)

```
┌─────────────────┐              ┌──────────────────────┐
│  JavaFX Client  │  ←─────────→ │  WebSocket Server    │
│   (App.java)    │   WebSocket  │ (AuctionServer.java) │
│  Port: Default  │              │    Port: 8081        │
└─────────────────┘              └──────────────────────┘
         ↓
   ┌─────────────┐
   │   MySQL     │
   │  Database   │
   └─────────────┘


### 5.2 Bước chạy chi tiết

#### **Bước 1: Khởi động MySQL Database**

Đảm bảo MySQL Server đang chạy:

**Windows:**
```powershell
# Nếu đã cài MySQL thành Service
net start MySQL80

# Nếu chạy từ MySQL folder
cd "C:\Program Files\MySQL\MySQL Server 8.0\bin"
mysqld.exe
```

**Linux:**
```bash
sudo systemctl start mysql
# hoặc
sudo service mysql start
```

**macOS:**
```bash
brew services start mysql
```

#### **Bước 2: Cấu hình Database Connection (nếu chưa có)**

Tạo file `src/main/resources/database.properties`:
```properties
db.host=localhost
db.port=3306
db.database=auction_db
db.username=root
db.password=your_password
```

#### **Bước 3: Build Project**

```bash
# Windows PowerShell
mvn clean package -DskipTests

# Linux/macOS
mvn clean package -DskipTests
```

Wait cho đến khi message: `BUILD SUCCESS ✅`

#### **Bước 4: Khởi động WebSocket Server**

**Windows (PowerShell):**
```powershell
cd D:\Project\PlateAuction\target
java -cp .\Auction-System-1.0-SNAPSHOT-standalone.jar com.auction.server.AuctionServer
```

**Windows (Command Prompt):**
```cmd
cd D:\Project\PlateAuction\target
java -cp Auction-System-1.0-SNAPSHOT-standalone.jar com.auction.server.AuctionServer
```

**Linux/macOS:**
```bash
cd ~/PlateAuction/target
java -cp ./Auction-System-1.0-SNAPSHOT-standalone.jar com.auction.server.AuctionServer
```

✅ **Khi thành công, bạn sẽ thấy:**
```
⏳ Đang chuẩn bị khởi động hệ thống Đấu Giá (WebSocket)...
🚀 WebSocket Server khởi tạo trên port 8081
✅ Đã gửi lệnh khởi động WebSocket Server!
```

**⚠️ Không đóng terminal này! Server cần chạy liên tục**

#### **Bước 5: Khởi động Client JavaFX (trong terminal khác)**

**Mở terminal/cmd mới:**

**Windows (PowerShell):**
```powershell
cd D:\Project\PlateAuction\target
chcp 65001
java -jar .\Auction-System-1.0-SNAPSHOT-standalone.jar
```

**Windows (Command Prompt):**
```cmd
cd D:\Project\PlateAuction\target
java -jar Auction-System-1.0-SNAPSHOT-standalone.jar
```

**Linux/macOS:**
```bash
cd ~/PlateAuction/target
java -jar ./Auction-System-1.0-SNAPSHOT-standalone.jar
```

✅ **Khi thành công:**
- Giao diện JavaFX hiện lên
- Client tự động kết nối đến WebSocket Server
- Màn hình Login xuất hiện

### 5.3 Quy trình hoạt động

```
1️⃣  User mở Client App (JavaFX)
     ↓
2️⃣  App kiểm tra MySQL có sống không
     ↓
3️⃣  DatabaseInitializer tạo bảng nếu chưa có
     ↓
4️⃣  Load Login.fxml
     ↓
5️⃣  Client kết nối tới WebSocket Server (port 8081)
     ↓
6️⃣  User đăng nhập
     ↓
7️⃣  Dashboard được load, thực hiện real-time updates
```

### 5.4 Troubleshooting

| Lỗi | Nguyên nhân | Giải pháp |
|-----|-----------|---------|
| `Connection refused` | Server chưa chạy | Chạy bước 4 trước |
| `Port 8081 already in use` | Port bị chiếm | Đóng các chương trình khác hoặc thay đổi port |
| `Cannot connect to database` | MySQL chưa chạy | Kiểm tra MySQL Server đã bật hay chưa |
| `ClassNotFoundException` | JAR file không đúng | Build lại: `mvn clean package` |
| `Database not found` | Database chưa được tạo | Chạy `DATABASE_MIGRATION.sql` |

### 5.5 Tắt Server/Client đúng cách

**Client:**
- Click nút **X** hoặc **Exit** trên cửa sổ ứng dụng

**Server:**
- Chuyển sang terminal chạy Server và nhấn **Ctrl+C**
- Server sẽ:
  1. Gửi broadcast ngắt kết nối cho tất cả clients
  2. Dọn dẹp bộ nhớ và kết nối
  3. Tắt an toàn
