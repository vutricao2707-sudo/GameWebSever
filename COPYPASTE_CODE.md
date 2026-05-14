# 📋 COPY-PASTE CODE HOÀN CHỈNH

## 1️⃣ DatabaseInitializer.java (Cập nhật phần schema)

### Vị trí: `src/main/java/com/auction/database/DatabaseInitializer.java`

Thay đổi dòng 39-53 từ:

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
```

Thành:

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
        "  seller_address TEXT," +
        "  registration_duration INT DEFAULT 1440," +
        "  registration_start_time DATETIME," +
        "  registration_end_time DATETIME," +
        "  auction_start_time DATETIME," +
        "  auction_end_time DATETIME," +
        "  participant_count INT DEFAULT 0," +
        "  auction_status VARCHAR(20) DEFAULT 'WAITING'" +
        ")";
```

---

## 2️⃣ ItemDAO.java (Thêm 7 hàm mới)

### Vị trí: `src/main/java/com/auction/database/ItemDAO.java`

Thêm vào cuối file (trước dấu `}`):

```java
    // =====================================================================
    // PHẦN MỚI: QUẢN LÝ THỜI GIAN VÀ NGƯỜI THAM GIA
    // =====================================================================

    // Bắt đầu giai đoạn đăng ký (khi admin duyệt biển số)
    public static boolean startRegistrationPhase(String plateNumber, int durationMinutes) {
        String sql = "UPDATE items SET " +
                "registration_start_time = NOW(), " +
                "registration_end_time = DATE_ADD(NOW(), INTERVAL ? MINUTE), " +
                "auction_start_time = DATE_ADD(NOW(), INTERVAL ? MINUTE), " +
                "auction_end_time = DATE_ADD(NOW(), INTERVAL ? + 60 MINUTE), " +
                "registration_duration = ?, " +
                "auction_status = 'REGISTRATION' " +
                "WHERE plate_number = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, durationMinutes);
            pstmt.setInt(2, durationMinutes);
            pstmt.setInt(3, durationMinutes);
            pstmt.setInt(4, durationMinutes);
            pstmt.setString(5, plateNumber);
            
            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("✅ Bắt đầu giai đoạn đăng ký cho biển '" + plateNumber + "'");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
        return false;
    }

    // Tăng số người tham gia
    public static void incrementParticipantCount(String plateNumber) {
        String sql = "UPDATE items SET participant_count = participant_count + 1 WHERE plate_number = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, plateNumber);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }

    // Lấy số người tham gia
    public static int getParticipantCount(String plateNumber) {
        String sql = "SELECT participant_count FROM items WHERE plate_number = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, plateNumber);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("participant_count");
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
        return 0;
    }

    // Lấy thời gian đấu giá còn lại (seconds)
    public static long getAuctionTimeRemaining(String plateNumber) {
        String sql = "SELECT UNIX_TIMESTAMP(auction_end_time) - UNIX_TIMESTAMP(NOW()) as remaining FROM items WHERE plate_number = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, plateNumber);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getLong("remaining");
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
        return 0;
    }

    // Hoàn thành phiên đấu giá
    public static void finishAuction(String plateNumber) {
        String sql = "UPDATE items SET auction_status = 'FINISHED', status = 'INACTIVE' WHERE plate_number = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, plateNumber);
            pstmt.executeUpdate();
            System.out.println("✅ Phiên đấu giá kết thúc cho biển '" + plateNumber + "'");
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }

    // Lấy item theo plate với tất cả thông tin thời gian
    public static String[] getItemFullData(String plateNumber) {
        String sql = "SELECT id, plate_number, start_price, current_price, description, vehicle_type, status, " +
                "registration_duration, registration_end_time, auction_end_time, participant_count, auction_status " +
                "FROM items WHERE plate_number = ? AND approval_status = 'APPROVED'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, plateNumber);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return new String[]{
                        String.valueOf(rs.getInt("id")),
                        rs.getString("plate_number"),
                        String.valueOf(rs.getDouble("start_price")),
                        String.valueOf(rs.getDouble("current_price")),
                        rs.getString("description"),
                        rs.getString("vehicle_type"),
                        rs.getString("status"),
                        String.valueOf(rs.getInt("registration_duration")),
                        rs.getString("registration_end_time"),
                        rs.getString("auction_end_time"),
                        String.valueOf(rs.getInt("participant_count")),
                        rs.getString("auction_status")
                };
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
        return null;
    }

    // Lấy tất cả items chỉ những cái ACTIVE (chưa kết thúc) cho Dashboard
    public static List<String[]> getAllApprovedActiveItems() {
        List<String[]> result = new ArrayList<>();
        String sql = "SELECT id, plate_number, start_price, current_price, description, vehicle_type, status, " +
                "seller_name, seller_phone, seller_email, seller_address, auction_status " +
                "FROM items WHERE approval_status = 'APPROVED' AND status = 'ACTIVE' AND auction_status != 'FINISHED'";

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
                        rs.getString("seller_email"),
                        rs.getString("seller_address"),
                        rs.getString("auction_status")
                };
                result.add(item);
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }

        return result;
    }

    // Kiểm tra lần đặt giá cuối cùng của user (để tính cooldown)
    public static long getLastBidTime(String plateNumber, int userId) {
        String sql = "SELECT UNIX_TIMESTAMP(bid_time) FROM bids " +
                "WHERE auction_id = (SELECT id FROM auctions WHERE item_id = (SELECT id FROM items WHERE plate_number = ?)) " +
                "AND bidder_id = ? ORDER BY bid_time DESC LIMIT 1";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, plateNumber);
            pstmt.setInt(2, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
        return 0;
    }
```

---

## 3️⃣ AuctionRoomController.java (Cập nhật)

### Vị trí: `src/main/java/com/auction/controller/AuctionRoomController.java`

**A) Thêm import:**

```java
import com.auction.database.ItemDAO;
```

**B) Thêm các biến mới (thay thế khe khác):**

```java
@FXML private Label lblMinutes, lblSeconds;
@FXML private Label lblPlateTop, lblPlateBottom;
@FXML private VBox liveHistoryContainer;
@FXML private Label lblCurrentPrice, lblStepMultiplier;
@FXML private Button btnPlaceBid;
@FXML private Label lblParticipants;  // Mới: Hiển thị số người tham gia

// --- Biến lưu trữ Logic Đấu Giá ---
private double currentPrice;                   // Giá khởi điểm từ database
private final double stepValue = 5000000.0;    // Bước giá cố định 5 triệu
private int stepMultiplier = 1;                // Hệ số nhân (1, 2, 3...)
private String currentPlateNumber;             // Lưu biển số hiện tại
private int currentParticipants = 0;           // Số người tham gia

// --- Biến Đồng hồ ---
private int timeRemainingSeconds = 60 * 60;   // Đếm ngược 60 phút
private Timeline countdownTimeline;
private final DecimalFormat formatter = new DecimalFormat("#,###");

// --- Biến Cooldown (1 phút sau mỗi lần đặt giá) ---
private long lastBidTime = 0;
private static final long BID_COOLDOWN_SECONDS = 60; // 1 phút

@FXML
public void initialize() {
    startCountdown();
    updatePricingUI();
    updateParticipantCount();
}
```

**C) Cập nhật hàm `handlePlaceBid()`:**

```java
@FXML
private void handlePlaceBid() {
    // Kiểm tra cooldown (phải chờ 60 giây sau lần cuối)
    long currentTime = System.currentTimeMillis() / 1000; // Thời gian hiện tại (seconds)
    long timeSinceLastBid = currentTime - lastBidTime;
    
    if (timeSinceLastBid < BID_COOLDOWN_SECONDS) {
        long remainingTime = BID_COOLDOWN_SECONDS - timeSinceLastBid;
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Chờ một chút");
        alert.setHeaderText(null);
        alert.setContentText("Bạn phải chờ " + remainingTime + " giây nữa mới được đặt giá!\n(Tránh spam)");
        alert.showAndWait();
        return; // Dừng lại, không cho đặt giá
    }

    // 1. Tính toán giá mới
    double newBid = currentPrice + (stepValue * stepMultiplier);
    currentPrice = newBid;
    
    // Cập nhật vào database
    if (currentPlateNumber != null && !currentPlateNumber.isEmpty()) {
        ItemDAO.updateCurrentPrice(currentPlateNumber, newBid);
    }

    // 2. Reset hệ số về 1
    stepMultiplier = 1;
    updatePricingUI();

    // 3. Ghi lại thời gian đặt giá
    lastBidTime = currentTime;

    // 4. Thêm vào đầu danh sách lịch sử (LIFO)
    addHistoryRow(formatter.format(newBid), currentPlateNumber + " - Bạn");
}
```

**D) Cập nhật `startCountdown()`:**

```java
private void startCountdown() {
    countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
        if (timeRemainingSeconds > 0) {
            timeRemainingSeconds--;
            int minutes = timeRemainingSeconds / 60;
            int seconds = timeRemainingSeconds % 60;

            lblMinutes.setText(String.format("%02d", minutes));
            lblSeconds.setText(String.format("%02d", seconds));
        } else {
            countdownTimeline.stop();
            btnPlaceBid.setText("PHIÊN ĐẤU GIÁ KẾT THÚC");
            btnPlaceBid.setStyle("-fx-background-color: #334155; -fx-text-fill: #94a3b8; -fx-background-radius: 12; -fx-font-size: 16px; -fx-font-weight: bold;");
            btnPlaceBid.setDisable(true);
            // Cập nhật trạng thái trong database
            if (currentPlateNumber != null && !currentPlateNumber.isEmpty()) {
                ItemDAO.finishAuction(currentPlateNumber);
            }
        }
    }));
    countdownTimeline.setCycleCount(Timeline.INDEFINITE);
    countdownTimeline.play();
}
```

**E) Thêm hàm mới `updateParticipantCount()` và `setAuctionRoomData()`:**

```java
private void updateParticipantCount() {
    if (currentPlateNumber != null && !currentPlateNumber.isEmpty()) {
        currentParticipants = ItemDAO.getParticipantCount(currentPlateNumber);
        if (lblParticipants != null) {
            lblParticipants.setText("👥 " + currentParticipants + " người tham gia");
        }
    }
}

public void setAuctionRoomData(String plate, double startingPrice) {
    this.currentPlateNumber = plate;
    this.currentPrice = startingPrice;
    
    String[] parts = plate.split("-");
    lblPlateTop.setText(parts.length > 0 ? parts[0] : plate);
    lblPlateBottom.setText(parts.length > 1 ? parts[1] : "");

    updatePricingUI();
    updateParticipantCount();
}
```

---

## 4️⃣ AdminController.java (Cập nhật hàm duyệt)

### Vị trị: `src/main/java/com/auction/controller/AdminController.java`

**A) Thêm import:**

```java
import com.auction.database.ItemDAO;
import javafx.scene.control.TextInputDialog;
import java.util.Optional;
```

**B) Thay thế hàm `handleApprove()`:**

```java
private void handleApprove(String plate, HBox rowToRemove) {
    // Bước 1: Duyệt biển số (cập nhật APPROVED)
    boolean success = productService.approveProduct(plate);
    if (!success) {
        showInfo("Thất bại", "Không tìm thấy biển số " + plate + " trong hệ thống.");
        return;
    }

    // Bước 2: Hỏi admin thời gian đăng ký bao lâu (phút)
    TextInputDialog dialog = new TextInputDialog("60");
    dialog.setTitle("Cấu hình thời gian đăng ký");
    dialog.setHeaderText("Nhập thời gian đăng ký (phút):");
    dialog.setContentText("Thời gian (phút):");

    Optional<String> result = dialog.showAndWait();
    int registrationDurationMinutes = 60; // Mặc định 60 phút

    if (result.isPresent()) {
        try {
            registrationDurationMinutes = Integer.parseInt(result.get());
            if (registrationDurationMinutes <= 0) {
                showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng nhập một số dương!");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng nhập một số hợp lệ!");
            return;
        }
    }

    // Bước 3: Bắt đầu giai đoạn đăng ký (cập nhật thời gian vào database)
    boolean registrationStarted = ItemDAO.startRegistrationPhase(plate, registrationDurationMinutes);
    if (registrationStarted) {
        adminListContainer.getChildren().remove(rowToRemove);
        int remaining = adminListContainer.getChildren().size();
        if (lblTotalRequests != null) {
            lblTotalRequests.setText("Tổng cộng: " + remaining + " yêu cầu");
        }
        showInfo("Duyệt thành công", 
                "Biển số " + plate + " đã được duyệt!\n" +
                "Giai đoạn đăng ký: " + registrationDurationMinutes + " phút\n" +
                "Giai đoạn đấu giá: 60 phút (sau khi đăng ký hết)");
    } else {
        showInfo("Lỗi", "Không thể bắt đầu giai đoạn đăng ký cho biển số " + plate);
    }
}
```

**C) Thêm hàm `showAlert()`:**

```java
private void showAlert(Alert.AlertType type, String title, String message) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
}
```

---

## 5️⃣ ProductService.java (Cập nhật)

### Vị trí: `src/main/java/com/auction/service/ProductService.java`

Thay thế hàm `getApprovedProductsByType()`:

```java
public List<String[]> getApprovedProductsByType(String type) {
    // Lấy từ database - CHỈ lấy những xe đã duyệt và chưa kết thúc phiên
    List<String[]> allApproved = ItemDAO.getAllApprovedActiveItems();
    List<String[]> result = new ArrayList<>();

    for (String[] product : allApproved) {
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
```

---

## 6️⃣ AuctionRoom.fxml (Thêm label số người)

### Vị trí: `src/main/resources/fxml/AuctionRoom.fxml`

Thay thế từ dòng 27-38 (phần header):

```xml
<HBox alignment="CENTER_LEFT" style="-fx-padding: 20 30;">
    <Button onAction="#handleBack" text="⬅ Quay lại Trang Chủ" textFill="WHITE" style="-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 8 20;">
        <font><Font name="System Bold" size="14.0"/></font>
    </Button>

    <Region HBox.hgrow="ALWAYS"/>

    <Label fx:id="lblParticipants" text="👥 0 người tham gia" textFill="#38bdf8" style="-fx-padding: 6 15; -fx-font-size: 14; -fx-font-weight: bold;">
    </Label>

    <Region prefWidth="20.0"/>

    <HBox alignment="CENTER" spacing="8.0" style="-fx-background-color: rgba(16, 185, 129, 0.15); -fx-background-radius: 20; -fx-padding: 6 15;">
        <Region prefHeight="8.0" prefWidth="8.0" style="-fx-background-color: #10b981; -fx-background-radius: 50;"/>
        <Label text="ĐANG DIỄN RA" textFill="#10b981"><font><Font name="System Bold" size="12.0"/></font></Label>
    </HBox>
</HBox>
```

---

## 7️⃣ Chạy Migration SQL

**File:** MIGRATION_TIMING_SYSTEM.sql

```sql
ALTER TABLE items 
ADD COLUMN IF NOT EXISTS registration_duration INT DEFAULT 1440,
ADD COLUMN IF NOT EXISTS registration_start_time DATETIME,
ADD COLUMN IF NOT EXISTS registration_end_time DATETIME,
ADD COLUMN IF NOT EXISTS auction_start_time DATETIME,
ADD COLUMN IF NOT EXISTS auction_end_time DATETIME,
ADD COLUMN IF NOT EXISTS participant_count INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS auction_status VARCHAR(20) DEFAULT 'WAITING';

INSERT IGNORE INTO users 
(username, password, email, full_name, user_type, phone, address) 
VALUES 
('vpa_admin', 'Admin@2024!', 'admin@vpa.vn', 'QUẢN TRỊ VIÊN HỆ THỐNG', 'Admin', '1900636380', 'Hà Nội');
```

---

## ✅ HOÀN THÀNH

**Tất cả các file đã được cập nhật. Bây giờ:**

1. Copy các file .java vào IDE
2. Chạy migration SQL
3. Rebuild project
4. Restart IDE
5. Chạy app test

**Tài khoản admin test:**
- Username: `vpa_admin`
- Password: `Admin@2024!`

