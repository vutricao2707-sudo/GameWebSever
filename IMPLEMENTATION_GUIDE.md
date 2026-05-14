# 🚀 HƯỚNG DẪN CỬC KỲ CHI TIẾT - HỆ睲Ệ DUYỆT BIỂN SỐ + DATABASE

## 📋 TỔNG QUAN

Hệ thống sẽ hoạt động như sau:

```
1️⃣  Người bán tạo biển số
        ↓
2️⃣  Biển số lưu vào DATABASE với status = "PENDING" (chờ duyệt)
        ↓
3️⃣  Admin xem danh sách chờ duyệt ở trang Admin
        ↓
4️⃣  Admin bấm nút ✔ để duyệt (APPROVED) hoặc ✖ để từ chối (REJECTED)
        ↓
5️⃣  Biển số đã duyệt (APPROVED) hiển thị ở Dashboard
        ↓
6️⃣  Biển số vẫn hiển thị kse người dùng đăng nhập lại (vì nó lưu trong DATABASE)
```

---

## 🛠️ BƯỚC 1: CẬP NHẬT CODE JAVA

Tôi đã cập nhật 4 file chính. Bạn copy-paste toàn bộ code từ 4 file này:

### **File 1️⃣ : ItemDAO.java**
📍 Tuyệt đối: `src/main/java/com/auction/database/ItemDAO.java`

**Chức năng:**
- `addItemFromSeller()` - Thêm biển số mới từ người bán (status = PENDING)
- `getPendingItems()` - Lấy danh sách biển số chờ duyệt
- `approveItem()` - Duyệt biển số
- `rejectItem()` - Từ chối biển số
- `plateExists()` - Kiểm tra biển số đã tồn tại
- `getAllApprovedItems()` - Lấy tất cả biển số đã duyệt

```java
package com.auction.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ItemDAO - Quản lý biển số xe (items)
 */
public class ItemDAO {
    
    // Thêm item (biển số xe) mới
    public static void addItem(String plateNumber, double startPrice, String description, 
                               String vehicleType, String status) {
        String sql = "INSERT INTO items (plate_number, start_price, current_price, description, vehicle_type, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, plateNumber);
            pstmt.setDouble(2, startPrice);
            pstmt.setDouble(3, startPrice);  // current_price = start_price lúc đầu
            pstmt.setString(4, description);
            pstmt.setString(5, vehicleType);
            pstmt.setString(6, status);
            
            pstmt.executeUpdate();
            System.out.println("✅ Thêm item biển '" + plateNumber + "' thành công");
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }
    
    // Thêm item mới từ người bán (với thông tin seller)
    public static void addItemFromSeller(String plateNumber, double startPrice, String description, 
                                         String vehicleType, String sellerName, String sellerPhone,
                                         String sellerEmail, String sellerAddress) {
        String sql = "INSERT INTO items (plate_number, start_price, current_price, description, vehicle_type, status, " +
                     "approval_status, seller_name, seller_phone, seller_email, seller_address) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, plateNumber);
            pstmt.setDouble(2, startPrice);
            pstmt.setDouble(3, startPrice);  // current_price = start_price lúc đầu
            pstmt.setString(4, description);
            pstmt.setString(5, vehicleType);
            pstmt.setString(6, "ACTIVE");  // status = ACTIVE
            pstmt.setString(7, "PENDING"); // approval_status = PENDING (chờ duyệt)
            pstmt.setString(8, sellerName);
            pstmt.setString(9, sellerPhone);
            pstmt.setString(10, sellerEmail);
            pstmt.setString(11, sellerAddress);
            
            pstmt.executeUpdate();
            System.out.println("✅ Thêm item (chờ duyệt) biển '" + plateNumber + "' thành công");
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }

    
    // Lấy item theo plate_number - trả về ResultSet
    public static ResultSet getItemByPlate(String plateNumber) {
        String sql = "SELECT * FROM items WHERE plate_number = ?";
        
        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, plateNumber);
            return pstmt.executeQuery();
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
            return null;
        }
    }
    
    // Lấy tất cả items - trả về List
    public static List<String[]> getAllApprovedItems() {
        List<String[]> result = new ArrayList<>();
        String sql = "SELECT id, plate_number, start_price, current_price, description, vehicle_type, status, " +
                     "seller_name, seller_phone, seller_email FROM items " +
                     "WHERE approval_status = 'APPROVED' AND status = 'ACTIVE'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String[] item = {
                    String.valueOf(rs.getInt("id")),
                    rs.getString("plate_number"),
                    String.valueOf(rs.getDouble("start_price")),
                    String.valueOf(rs.getDouble("current_price")),
                    rs.getString("description"),
                    rs.getString("vehicle_type"),
                    rs.getString("status"),
                    rs.getString("seller_name"),
                    rs.getString("seller_phone"),
                    rs.getString("seller_email")
                };
                result.add(item);
            }
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
        
        return result;
    }
    
    // Lấy tất cả items chờ duyệt (PENDING)
    public static List<String[]> getPendingItems() {
        List<String[]> result = new ArrayList<>();
        String sql = "SELECT id, plate_number, start_price, vehicle_type, seller_name, " +
                     "seller_phone, seller_email, seller_address, approval_status FROM items " +
                     "WHERE approval_status = 'PENDING'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String[] item = {
                    String.valueOf(rs.getInt("id")),
                    rs.getString("plate_number"),
                    String.valueOf(rs.getDouble("start_price")),
                    rs.getString("vehicle_type"),
                    rs.getString("seller_name"),
                    rs.getString("seller_phone"),
                    rs.getString("seller_email"),
                    rs.getString("seller_address"),
                    rs.getString("approval_status")
                };
                result.add(item);
            }
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
        
        return result;
    }
    
    // Duyệt biển số (thay đổi từ PENDING thành APPROVED)
    public static boolean approveItem(String plateNumber) {
        String sql = "UPDATE items SET approval_status = 'APPROVED' WHERE plate_number = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, plateNumber);
            int rowsUpdated = pstmt.executeUpdate();
            
            if (rowsUpdated > 0) {
                System.out.println("✅ Duyệt biển '" + plateNumber + "' thành công");
                return true;
            } else {
                System.out.println("❌ Không tìm thấy biển '" + plateNumber + "'");
                return false;
            }
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
            return false;
        }
    }
    
    // Từ chối biển số (xóa hoặc đánh dấu REJECTED)
    public static boolean rejectItem(String plateNumber) {
        String sql = "UPDATE items SET approval_status = 'REJECTED' WHERE plate_number = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, plateNumber);
            int rowsUpdated = pstmt.executeUpdate();
            
            if (rowsUpdated > 0) {
                System.out.println("✅ Từ chối biển '" + plateNumber + "' thành công");
                return true;
            } else {
                System.out.println("❌ Không tìm thấy biển '" + plateNumber + "'");
                return false;
            }
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
            return false;
        }
    }
    
    // Kiểm tra biển số đã tồn tại chưa
    public static boolean plateExists(String plateNumber) {
        String sql = "SELECT COUNT(*) FROM items WHERE plate_number = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, plateNumber);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
        
        return false;
    }
    
    // Cập nhật giá hiện tại của item
    public static void updateCurrentPrice(String plateNumber, double newPrice) {
        String sql = "UPDATE items SET current_price = ? WHERE plate_number = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDouble(1, newPrice);
            pstmt.setString(2, plateNumber);
            
            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("✅ Cập nhật giá thành công");
            }
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }
}

```

---

### **File 2️⃣ : ProductService.java**
📍 Đường dẫn: `src/main/java/com/auction/service/ProductService.java`

```java
package com.auction.service;

import com.auction.data.DataStore;
import com.auction.database.ItemDAO;

import java.util.ArrayList;
import java.util.List;

public class ProductService {

    // ==========================================
    // CHO USER: Chỉ lấy những xe "ĐÃ DUYỆT" từ DATABASE
    // ==========================================
    public List<String[]> getApprovedProductsByType(String type) {
        // Lấy từ database
        List<String[]> allApproved = ItemDAO.getAllApprovedItems();
        List<String[]> result = new ArrayList<>();
        
        for (String[] product : allApproved) {
            // Cấu trúc từ database: [0]id [1]plate [2]start_price [3]current_price 
            // [4]description [5]vehicle_type [6]status [7]seller_name [8]seller_phone [9]seller_email
            if (product[5].equalsIgnoreCase(type)) {  // product[5] là vehicle_type
                result.add(product);
            }
        }
        
        // Fallback thêm dữ liệu từ DataStore nếu cần (legacy)
        for (String[] product : DataStore.products) {
            if (product[0].equalsIgnoreCase(type) && product[8].equals("APPROVED")) {
                result.add(product);
            }
        }
        
        return result;
    }

    // ==========================================
    // CHO ADMIN: Lấy tất cả xe "CHỜ DUYỆT" từ DATABASE
    // ==========================================
    public List<String[]> getPendingProducts() {
        // Lấy từ database (ưu tiên)
        List<String[]> pendingFromDB = ItemDAO.getPendingItems();
        if (!pendingFromDB.isEmpty()) {
            return pendingFromDB;
        }
        
        // Fallback từ DataStore nếu DB trống
        List<String[]> result = new ArrayList<>();
        for (String[] product : DataStore.products) {
            if (product[8].equals("PENDING")) {
                result.add(product);
            }
        }
        return result;
    }

    // ==========================================
    // CHO ADMIN: Logic bấm nút "Duyệt Xe" (✔) từ DATABASE
    // ==========================================
    public boolean approveProduct(String plateToApprove) {
        // Thử duyệt từ database trước
        if (ItemDAO.approveItem(plateToApprove)) {
            return true;
        }
        
        // Fallback vào DataStore nếu DB không có
        for (String[] product : DataStore.products) {
            if (product[1].equals(plateToApprove)) {
                product[8] = "APPROVED";
                return true;
            }
        }
        return false;
    }
    
    // ==========================================
    // CHO ADMIN: Logic bấm nút "Từ chối Xe" (✖️) vào DATABASE
    // ==========================================
    public boolean rejectProduct(String plateToReject) {
        return ItemDAO.rejectItem(plateToReject);
    }

    // ==========================================
    // CHO USER: Logic bấm nút "Đăng Ký Bán" - LƯU VÀO DATABASE
    // ==========================================
    public boolean addNewProduct(String type, String plate, String province, String price,
                                 String name, String phone, String email, String address) {

        // BƯỚC 1: Kiểm tra chống trùng lặp - kiểm tra DATABASE trước
        if (ItemDAO.plateExists(plate)) {
            return false;  // Biển số đã tồn tại
        }
        
        // BƯỚC 2: Kiểm tra chống trùng lặp trong DataStore (legacy)
        for (String[] product : DataStore.products) {
            if (product[1].equalsIgnoreCase(plate)) {
                return false;
            }
        }

        // BƯỚC 3: Nếu không trùng, thêm vào DATABASE với status PENDING
        try {
            double priceDouble = Double.parseDouble(price);
            ItemDAO.addItemFromSeller(plate, priceDouble, "Biển số xe đẹp", type, name, phone, email, address);
            return true;
        } catch (NumberFormatException e) {
            System.out.println("❌ Lỗi parse giá: " + e.getMessage());
            return false;
        }
    }
}

```

---

### **File 3️⃣ : AdminController.java** 
📍 Đường dẫn: `src/main/java/com/auction/controller/AdminController.java`

👉 **CHỈ CẬP NHẬT 2 PHẦN:**

**Phần 1:** Hàm `buildActionBox()` (khoảng dòng 99-125)
```java
    /** Tạo cột thao tác — nội dung thay đổi tùy trạng thái xe (PENDING / LIVE) */
    private HBox buildActionBox(String plate, HBox parentRow, String status) {
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setPrefWidth(120);

        if ("PENDING".equals(status)) {
            Button btnApprove = new Button("✔");
            btnApprove.setStyle("-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #10b981; -fx-cursor: hand; -fx-font-weight: bold;");
            btnApprove.setOnAction(e -> handleApprove(plate, parentRow));

            Button btnReject = new Button("✖");
            btnReject.setStyle("-fx-background-color: rgba(239, 68, 68, 0.2); -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-font-weight: bold;");
            btnReject.setOnAction(e -> handleReject(plate, parentRow));  // ← THÊMN DÒNG NÀY

            actionBox.getChildren().addAll(btnApprove, btnReject);

        } else if ("LIVE".equals(status)) {
            Label lblUsers = new Label("👤 12");
            lblUsers.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");

            Button btnView = new Button("Theo dõi");
            btnView.setStyle("-fx-background-color: transparent; -fx-border-color: #38bdf8; -fx-text-fill: #38bdf8; -fx-border-radius: 4; -fx-cursor: hand;");

            actionBox.getChildren().addAll(lblUsers, btnView);
        }

        return actionBox;
    }
```

**Phần 2:** Thêm hàm `handleReject()` sau hàm `handleApprove()` (khoảng dòng 131-145)
```java
    // =========================================================================
    // LOGIC TỪ CHỐI XE — Gọi Service để cập nhật Database
    // =========================================================================
    
    private void handleReject(String plate, HBox rowToRemove) {
        boolean success = productService.rejectProduct(plate);
        if (success) {
            adminListContainer.getChildren().remove(rowToRemove);
            // Cập nhật lại số đếm sau khi từ chối
            int remaining = adminListContainer.getChildren().size();
            if (lblTotalRequests != null) {
                lblTotalRequests.setText("Tổng cộng: " + remaining + " yêu cầu");
            }
            showInfo("Từ chối thành công", "Biển số " + plate + " đã bị từ chối!");
        } else {
            showInfo("Thất bại", "Không tìm thấy biển số " + plate + " trong hệ thống.");
        }
    }
```

---

### **File 4️⃣ : DatabaseInitializer.java**
📍 Đường dẫn: `src/main/java/com/auction/database/DatabaseInitializer.java`

📍 **CHỈ CẬP NHẬT PHẦN TẠO BẢNG ITEMS** (khoảng dòng 38-50)

```java
            // === BẢNG 2: ITEMS - Lưu biển số xe (đồ đấu giá) ===
            String createItems = "CREATE TABLE IF NOT EXISTS items (" +
                    "  id INT PRIMARY KEY AUTO_INCREMENT," +
                    "  plate_number VARCHAR(20) NOT NULL UNIQUE," +
                    "  start_price DECIMAL(15,2)," +
                    "  current_price DECIMAL(15,2)," +
                    "  description TEXT," +
                    "  vehicle_type VARCHAR(50)," +
                    "  status VARCHAR(20)," +
                    "  approval_status VARCHAR(20) DEFAULT 'PENDING'," +
                    "  seller_name VARCHAR(100)," +
                    "  seller_phone VARCHAR(15)," +
                    "  seller_email VARCHAR(100)," +
                    "  seller_address TEXT" +
                    ")";
            stmt.execute(createItems);
```

---

## 🗄️ BƯỚC 2: CẬP NHẬT DATABASE

### **Trường hợp 1: Bạn chạy lần đầu (Database trống)**
✅ **Bỏ qua bước này!** App sẽ tự tạo bảng mới với tất cả các trường.

---

### **Trường hợp 2: Bạn đã chạy app trước (Database đã có dữ liệu)**
⚠️ **Phải chạy migration!** Để thêm các trường mới vào bảng cũ.

**Cách làm:**
1. Mở **MySQL Workbench**
2. Copy toàn bộ nội dung file `DATABASE_MIGRATION.sql`
3. Paste vào Query Window
4. Bấm **Execute** (⚡)
5. Kiểm tra kết quả ở bảng items

Nếu báo lỗi "Field 'approval_status' already exists" → Bảng đã được cập nhật sẵn ✅

---

## 🎯 CÁCH CHẠY TOÀN BỘ HỆ THỐNG

### **Khi người dùng chuẩn bị biển số:**

1. **Người bán đăng nhập** → Click "Đăng Ký Bán"
2. **Nhập thông tin biển số**
   - Loại xe: Ô tô / Xe máy
   - Biển số: 30K-888.88
   - Giá khởi điểm: 150000000
   - (Các thông tin khác)
3. **Bấm Submit** → Biển số lưu vào DATABASE với status **"PENDING"**
4. Hiển thị: "Biển số đã được gửi đi. Vui lòng chờ hệ thống duyệt!"

---

### **Khi Admin duyệt:**

1. **Admin đăng nhập** → Vào trang Admin
2. **Xem danh sách "Chờ Duyệt"** (lấy từ DATABASE)
3. **Với mỗi biển số:**
   - Bấm ✔ → Biển số được **APPROVED** (hiển thị ở Dashboard người dùng)
   - Bấm ✖ → Biển số bị **REJECTED** (ẩn khỏi Dashboard)

---

### **Khi người dùng xem:**

1. **User đăng nhập** → Vào Dashboard
2. **Chỉ xem biển số APPROVED** (lấy từ DATABASE)
3. **Đăng nhập lại** → Biển số vẫn hiển thị (vì lưu trong DATABASE)
4. **Biển số tồn tại** đến khi được đấu giá xong (thay status = "SOLD")

---

## ❌ CÁC LỖI CÓ THỂ GẶP

### **1. Lỗi: "Column 'approval_status' doesn't exist"**
**Nguyên nhân:** Database cũ chưa được migration
**Cách sửa:** Chạy file `DATABASE_MIGRATION.sql` (Bước 2)

### **2. Lỗi: "Duplicate entry for key 'plate_number'"**
**Nguyên nhân:** Biển số đã tồn tại trong database
**Cách sửa:** Kiểm tr lại biển số (phải là UNIQUE)

### **3. App khởi động nhưng biển số mới không hiển thị**
**Nguyên nhân:** Chưa được duyệt
**Cách sửa:** Vào trang Admin để duyệt

### **4. Admin xem không có biển số nào**
**Nguyên nhân:** 
- Chưa có ai đăng bán
- Tất cả biển số đã được duyệt
**Cách sửa:** Tạo biển số mới từ người bán

---

## 📝 CẤU TRÚC BẢNG ITEMS (mới)

```
items
├─ id (khóa chính, tự tăng)
├─ plate_number (biển số, UNIQUE)
├─ start_price (giá khởi điểm)
├─ current_price (giá hiện tại)
├─ description (mô tả)
├─ vehicle_type (Ô tô / Xe máy)
├─ status (ACTIVE / SOLD)
├─ approval_status (PENDING / APPROVED / REJECTED)  ← MỚI
├─ seller_name (tên người bán)  ← MỚI
├─ seller_phone (SĐT người bán)  ← MỚI
├─ seller_email (email người bán)  ← MỚI
└─ seller_address (địa chỉ nhân bán)  ← MỚI
```

---

## ✅ KIỂM TRA HOÀN THÀNH

Sau khi áp dụng toàn bộ:

- [ ] Cập nhật 4 file Java
- [ ] Restart IDE/Maven Project
- [ ] Chạy lần đầu hoặc chạy DATABASE_MIGRATION.sql
- [ ] Người dùng tạo biển số mới
- [ ] Admin xem "Chờ Duyệt" (phải thấy biển số)
- [ ] Admin duyệt (✔)
- [ ] Người dùng xem Dashboard (phải thấy biển số)
- [ ] Đăng nhập lại (biển số vẫn hiển thị) ✅

---

## 📞 LƯU Ý QUAN TRỌNG

1. **Dữ liệu DataStore** chỉ là dữ liệu mẫu
2. **Dữ liệu thực** từ **DATABASE** (ProductService sẽ ưu tiên DB)
3. **Tất cả người dùng đăng nhập lại** sẽ vẫn thấy biển số (persistent ✅)
4. **Biển số tồn tại** cho đến khi được đấu giá xong (status = SOLD)

---

## 🎉 XONG!

Hệ thống đã sẵn sàng. Bạn có thể chạy app ngay bây giờ!

---

**Tác giả:** GitHub Copilot  
**Ngày:** 2026-05-14  
**Phiên bản:** 1.0

