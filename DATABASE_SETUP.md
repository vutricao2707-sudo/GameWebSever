# 🗄️ HƯỚNG DẪN DATABASE - ĐƠN GIẢN & CƠ BẢN

## 📋 NỘI DUNG

- [Cấu trúc Database](#cấu-trúc-database)
- [Cách Setup](#cách-setup)
- [Cách Dùng](#cách-dùng)
- [Các Bảng Chi Tiết](#các-bảng-chi-tiết)

---

## 🗂️ Cấu Trúc Database

Dự án sử dụng **4 bảng chính**:

```
users          ← Thông tin người dùng (buyer, seller)
├─ id (khóa chính)
├─ username, password, email
├─ full_name, user_type (BUYER/SELLER)
└─ phone, address

items          ← Biển số xe (cái cần đấu giá)
├─ id
├─ plate_number (VD: 30K-888.88)
├─ start_price, current_price
└─ description, vehicle_type, status

auctions       ← Phiên đấu giá
├─ id
├─ item_id → items.id
├─ seller_id → users.id
├─ winner_id → users.id
└─ is_open, final_price

bids           ← Lịch sử đặt giá
├─ id
├─ auction_id → auctions.id
├─ bidder_id → users.id
├─ amount, bid_time
```

---

## 🚀 Cách Setup

### **Bước 1: Copy File Config**

File: `src/main/resources/database.properties.example`

```bash
1. Mở folder: src/main/resources/
2. Copy file: database.properties.example
3. Tạo file mới: database.properties (paste)
4. KHÔNG commit file này lên Git (có trong .gitignore)
```

### **Bước 2: Sửa Password MySQL**

File: `src/main/resources/database.properties`

```properties
db.url=jdbc:mysql://localhost:3306/auction_db
db.username=root
db.password=123456  ← ĐỔI THÀNH MẬT KHẨU CỦA BẠN
```

**Giải thích:**
- Mỗi người có file riêng
- Mỗi người có password khác
- Code không cứng password
- Git bỏ qua file này

### **Bước 3: Tạo Database**

Mở MySQL Workbench, chạy:

```sql
CREATE DATABASE auction_db;
```

### **Bước 4: Chạy App**

App sẽ tự động tạo 4 bảng. Kiểm tra console:

```
✅ Đã load config từ database.properties
✅ Bảng users sẵn sàng
✅ Bảng items sẵn sàng
✅ Bảng auctions sẵn sàng
✅ Bảng bids sẵn sàng

✅✅✅ Database khởi tạo thành công! ✅✅✅
```

**XONG!** 🎉

---

## 📖 Cách Dùng

### **Thêm User**

```java
UserDAO.addUser("nguyenduc", "password123", "duc@mail.com", 
                "Nguyễn Đức", "BUYER", "0912345678", "Hà Nội");
```

### **Lấy User theo Username**

```java
UserDAO.findUserByUsername("nguyenduc");
```

### **Lấy Tất Cả Users**

```java
UserDAO.getAllUsers();
```

### **Kiểm Tra Đăng Nhập**

```java
boolean logged = UserDAO.checkLogin("nguyenduc", "password123");
if (logged) {
    System.out.println("Đăng nhập thành công!");
} else {
    System.out.println("Sai mật khẩu!");
}
```

---

### **Thêm Item (Biển Số Xe)**

```java
ItemDAO.addItem("30K-888.88", 50000000, "Biển đẹp", "CAR", "ACTIVE");
```

### **Lấy Item theo Biển**

```java
ItemDAO.getItemByPlate("30K-888.88");
```

### **Cập Nhật Giá Item**

```java
ItemDAO.updateCurrentPrice("30K-888.88", 52500000);
```

---

### **Tạo Phiên Đấu Giá**

```java
AuctionDAO.createAuction(1, 2);  // item_id=1, seller_id=2
```

### **Lấy Phiên Đấu Giá Đang Mở**

```java
AuctionDAO.getOpenAuctions();
```

### **Đóng Phiên Đấu Giá**

```java
AuctionDAO.closeAuction(1, 3, 55000000);  // auction_id=1, winner_id=3, final_price=55M
```

---

### **Đặt Giá (Bid)**

```java
BidDAO.addBid(1, 3, 52000000);  // auction_id=1, bidder_id=3, amount=52M
```

### **Lấy Lịch Sử Đặt Giá Của Phiên**

```java
BidDAO.getBidsForAuction(1);  // Sắp xếp từ cao xuống thấp
```

### **Lấy Giá Cao Nhất**

```java
double highest = BidDAO.getHighestBidForAuction(1);
System.out.println("Giá cao nhất: " + highest);
```

### **Lấy Người Đặt Giá Cao Nhất**

```java
BidDAO.getHighestBidder(1);
```

### **Đếm Số Lần Đặt Giá**

```java
BidDAO.countBidsForAuction(1);
```

---

## 📊 Các Bảng Chi Tiết

### **Bảng: USERS**

| Cột | Kiểu | Ghi Chú |
|-----|------|--------|
| id | INT | Khóa chính, tự tăng |
| username | VARCHAR(50) | Không trùng (UNIQUE) |
| password | VARCHAR(255) | Mã hóa (hash) |
| email | VARCHAR(100) | Email người dùng |
| full_name | VARCHAR(100) | Tên đầy đủ |
| user_type | VARCHAR(20) | BUYER hoặc SELLER |
| phone | VARCHAR(15) | SĐT |
| address | TEXT | Địa chỉ |
| created_at | TIMESTAMP | Thời gian tạo (tự động) |

---

### **Bảng: ITEMS**

| Cột | Kiểu | Ghi Chú |
|-----|------|--------|
| id | INT | Khóa chính, tự tăng |
| plate_number | VARCHAR(20) | Biển số xe (UNIQUE) |
| start_price | DECIMAL(15,2) | Giá khởi điểm |
| current_price | DECIMAL(15,2) | Giá hiện tại |
| description | TEXT | Mô tả |
| vehicle_type | VARCHAR(50) | Loại xe (CAR, BIKE, v.v.) |
| status | VARCHAR(20) | ACTIVE, SOLD, v.v. |

---

### **Bảng: AUCTIONS**

| Cột | Kiểu | Ghi Chú |
|-----|------|--------|
| id | INT | Khóa chính, tự tăng |
| item_id | INT | Tham chiếu items.id (FOREIGN KEY) |
| seller_id | INT | Tham chiếu users.id (FOREIGN KEY) |
| winner_id | INT | Người thắng (NULL = chưa kết thúc) |
| is_open | BOOLEAN | TRUE = đang mở, FALSE = đã đóng |
| start_time | TIMESTAMP | Thời gian bắt đầu (tự động) |
| end_time | DATETIME | Thời gian kết thúc |
| final_price | DECIMAL(15,2) | Giá bán cuối cùng |

---

### **Bảng: BIDS**

| Cột | Kiểu | Ghi Chú |
|-----|------|--------|
| id | INT | Khóa chính, tự tăng |
| auction_id | INT | Tham chiếu auctions.id (FOREIGN KEY) |
| bidder_id | INT | Tham chiếu users.id (FOREIGN KEY) |
| amount | DECIMAL(15,2) | Số tiền đặt |
| bid_time | TIMESTAMP | Thời gian đặt (tự động) |

---

## ⚠️ Lưu Ý

### **Về File Config (database.properties)**

✅ **Mỗi người có file riêng**
- Bạn A: `database.properties` → password "123456"
- Bạn B: `database.properties` → password "abcxyz"
- Code giống nhau, chỉ config khác

✅ **Không commit lên Git**
- File `database.properties` trong `.gitignore`
- Commit `database.properties.example` làm mẫu
- Mỗi người copy `.example` → sửa password

✅ **Cách hoạt động**
- App chạy → ConfigLoader.java đọc `database.properties`
- Connection dùng password từ file
- Mỗi máy chạy code riêng → không lộ password

### **Về Database**

- **Tạo database 1 lần**: Chạy `CREATE DATABASE auction_db;` trong MySQL Workbench
- **App chạy lần đầu**: DatabaseInitializer sẽ tạo 4 bảng tự động
- **Lần 2 trở đi**: Bảng đã tồn tại, app không tạo lại

---

**Vậy đó! Simple, bảo mật, và dễ hiểu 😊**

