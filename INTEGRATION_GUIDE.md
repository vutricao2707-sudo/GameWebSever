# 📱 INTEGRATION GUIDE - Kết Nối Tính Năng với UI

## 🎯 Mục Tiêu
Hướng dẫn cách integrate các tính năng "xịn" vào các Controller FXML có sẵn

---

## 1️⃣ Integrate Auto-Extend Timer vào AuctionRoomController

### Hiện Tại:
```java
// AuctionRoomController.java (dòng 74)
private void startCountdown() {
    countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
        if (timeRemainingSeconds > 0) {
            timeRemainingSeconds--;
            // ... cập nhật UI ...
        } else {
            // PHIÊN KẾT THÚC
        }
    }));
}
```

### Cải Tiến:
```java
private Auction currentAuction; // Add field

private void startCountdown() {
    countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
        if (currentAuction != null) {
            // Sử dụng thời gian từ Auction model (có auto-extend)
            long remaining = currentAuction.getTimeRemainingSeconds();
            
            if (remaining > 0) {
                int minutes = (int) (remaining / 60);
                int seconds = (int) (remaining % 60);
                lblMinutes.setText(String.format("%02d", minutes));
                lblSeconds.setText(String.format("%02d", seconds));
                
                // ✅ Ngoài 2 phút, highlight thêm để alert người dùng
                if (remaining <= 120) {
                    lblMinutes.setStyle("-fx-text-fill: #ef4444;"); // Đỏ
                } else {
                    lblMinutes.setStyle("-fx-text-fill: #ffffff;"); // Trắng
                }
            } else {
                // PHIÊN KẾT THÚC
                finishAuction();
            }
        }
    }));
    countdownTimeline.setCycleCount(Timeline.INDEFINITE);
    countdownTimeline.play();
}

private void finishAuction() {
    if (countdownTimeline != null) countdownTimeline.stop();
    btnPlaceBid.setDisable(true);
    btnPlaceBid.setText("PHIÊN ĐẤU GIÁ KẾT THÚC");
    
    // ✅ Gửi email thông báo người thắng cuộc
    if (currentAuction != null && !currentAuction.getBids().isEmpty()) {
        BidTransaction lastBid = currentAuction.getBids().get(currentAuction.getBids().size() - 1);
        Bidder winner = lastBid.getBidder();
        EmailService.sendWinnerNotification(
            winner.getEmail(),
            currentPlateNumber,
            lastBid.getAmount()
        );
    }
}
```

---

## 2️⃣ Integrate PlateValidator vào RegisterSellController

### Đã Có (Line 63):
```java
if (!PlateValidator.isValidPlate(plate)) {
    showAlert(Alert.AlertType.ERROR, "Lỗi biển số", 
            "Định dạng biển số không hợp lệ!\nVí dụ hợp lệ: 29A-12345, 30B.54321, etc.");
    return;
}
```

### ✅ Đã Hoàn Tất - Không cần thay đổi thêm

---

## 3️⃣ Integrate EmailService vào ProductService

### Tìm ProductService.java và thêm:
```java
package com.auction.service;

import com.auction.util.SessionManager;

public class ProductService {
    // ... existing code ...
    
    public boolean addNewProduct(String type, String plate, String province, 
                                 String price, String fullName, String phone, 
                                 String email, String address) {
        // ... validation ...
        
        // ✅ Sau khi thêm vào database thành công
        boolean success = DataStore.getInstance().addProduct(type, plate, province, 
                                                            price, fullName, phone, 
                                                            email, address);
        if (success) {
            // Gửi email xác nhận (chưa duyệt)
            EmailService.sendEmail(email, 
                "Biển số của bạn đang chờ duyệt", 
                "Biển số " + plate + " của bạn đang chờ admin duyệt. Cảm ơn!");
        }
        
        return success;
    }
}
```

---

## 4️⃣ Integrate Watchlist vào DashboardController

### Highlight items trong FlowPane:
```java
// DashboardController.java
@FXML private FlowPane itemsContainer;
private int currentUserId;

@FXML
private void initialize() {
    currentUserId = SessionManager.getInstance().getCurrentUser().getId();
    loadAuctions();
}

private void loadAuctions() {
    List<Auction> auctions = AuctionManager.getInstance().getActiveAuctions();
    for (Auction auction : auctions) {
        VBox itemCard = createItemCard(auction);
        itemsContainer.getChildren().add(itemCard);
    }
}

private VBox createItemCard(Auction auction) {
    VBox card = new VBox();
    String plate = auction.getItem().toString(); // Biển số
    
    // ✅ Kiểm tra xem có trong watchlist không
    boolean inWatch = WatchlistService.isInWatchlist(currentUserId, plate);
    
    // Nút add/remove watchlist
    Button btnWatch = new Button(inWatch ? "❤️ Đã theo dõi" : "🤍 Theo dõi");
    btnWatch.setStyle(inWatch ? "-fx-text-fill: #ef4444;" : "-fx-text-fill: #94a3b8;");
    
    btnWatch.setOnAction(e -> {
        if (inWatch) {
            WatchlistService.removeFromWatchlist(currentUserId, plate);
            btnWatch.setText("🤍 Theo dõi");
        } else {
            WatchlistService.addToWatchlist(currentUserId, plate);
            btnWatch.setText("❤️ Đã theo dõi");
        }
    });
    
    card.getChildren().add(btnWatch);
    // ... other card elements ...
    
    return card;
}
```

---

## 5️⃣ Integrate Email Notifications vào AuctionRoomController

### Trong handlePlaceBid():
```java
@FXML
private void handlePlaceBid() {
    // ... existing logic ...
    
    double newBid = currentPrice + (stepValue * stepMultiplier);
    currentPrice = newBid;
    
    // 1. Lưu bid vào database
    if (currentPlateNumber != null) {
        ItemDAO.updateCurrentPrice(currentPlateNumber, newBid);
        if (SessionManager.getInstance().getCurrentUser() != null) {
            int userId = SessionManager.getInstance().getCurrentUser().getId();
            BidDAO.saveBid(currentPlateNumber, userId, newBid);
        }
    }
    
    // 2. Cập nhật UI hiện tại
    stepMultiplier = 1;
    updatePricingUI();
    lastBidTime = System.currentTimeMillis();
    addHistoryRow(formatter.format(newBid), currentPlateNumber + " - Bạn");
    
    // ✅ 3. GỬI EMAIL: ĐỊA CHỈ CẠO CƯA NGƯỜI CŨNG - OUTBID NOTIFICATION
    if (currentAuction != null && currentAuction.getBids().size() >= 2) {
        List<BidTransaction> bids = currentAuction.getBids();
        BidTransaction previousBid = bids.get(bids.size() - 2); // Bid người trước
        Bidder previousBidder = previousBid.getBidder();
        
        // Gửi thông báo outbid
        EmailService.sendOutbidNotification(
            previousBidder.getEmail(),
            currentPlateNumber,
            newBid
        );
    }
    
    // ✅ 4. AUTO-EXTEND CHECK
    if (currentAuction != null) {
        currentAuction.setLastBidTime(System.currentTimeMillis());
        currentAuction.checkAndAutoExtend();
    }
}
```

---

## 6️⃣ Integrate Watchlist Notification vào Admin Approval Flow

### Khi Admin duyệt biển số:
```java
// AdminController.java (hoặc ItemDAO khi approve)
public void approveItem(String plateNumber) {
    // 1. Update database
    ItemDAO.approveItem(plateNumber);
    
    // 2. ✅ Gửi email cho tất cả người theo dõi
    List<Watchlist> watchers = WatchlistDAO.getUnnotifiedWatchlistForPlate(plateNumber);
    
    Item item = ItemDAO.getItemByPlate(plateNumber);
    
    for (Watchlist watcher : watchers) {
        // Lấy email người dùng từ UserDAO
        User user = UserDAO.getUserById(watcher.getUserId());
        
        EmailService.sendPlateApprovedNotification(
            user.getEmail(),
            plateNumber,
            item.getStartingPrice()
        );
        
        // Đánh dấu đã thông báo
        WatchlistDAO.markAsNotified(watcher.getId());
    }
}
```

---

## 7️⃣ Các File Cần Chỉnh Sửa Tiếp Theo (Tùy Chọn)

### Dashboard.fxml
```xml
<!-- Thêm button/icon "Watchlist" cho mỗi item -->
<Button fx:id="btnWatch">🤍 Theo dõi</Button>
```

### AuctionRoom.fxml
```xml
<!-- Highlight timer khi còn < 2 phút -->
<Label fx:id="lblMinutes" styleClass="timer-critical"/>
```

### CSS (style.css)
```css
.timer-critical {
    -fx-text-fill: #ef4444; /* Đỏ khi còn < 2 phút */
    -fx-font-size: 32px;
    -fx-font-weight: bold;
}
```

---

## 🔗 MỐI LIÊN KẾT TÍNH NĂNG

```
placeBid() 
  ↓
  ├─→ Auction.checkAndAutoExtend() [⏱️ Auto-extend logic]
  ├─→ BidDAO.saveBid() [💾 Save to DB]
  ├─→ EmailService.sendOutbidNotification() [📧 Email outbid]
  └─→ WatchlistService.notify() [🔔 Watchlist alert]

approveItem()
  ↓
  └─→ WatchlistDAO.getUnnotified()
      ↓
      └─→ EmailService.sendPlateApprovedNotification() [📧 Email approved]

finishAuction()
  ↓
  └─→ EmailService.sendWinnerNotification() [🏆 Winner email]
```

---

## ✅ CHECKLIST IMPLEMENTATION

- [ ] Cập nhật `Auction.java` (Auto-extend) ✅ DONE
- [ ] Cập nhật `RegisterSellController.java` (PlateValidator) ✅ DONE  
- [ ] Tạo & setup `EmailService.java` ✅ DONE
- [ ] Tạo `Watchlist.java` model ✅ DONE
- [ ] Tạo `WatchlistService.java` ⏳ (file creation timeout)
- [ ] Cập nhật `AuctionRoomController.java` (integrate features)
- [ ] Cập nhật `DashboardController.java` (watchlist UI)
- [ ] Cập nhật `AdminController.java` (email on approve)
- [ ] Thêm CSS cho timer à critical
- [ ] Test email service (ngưng cấu hình SMTP)
- [ ] Test watchlist add/remove
- [ ] Test auto-extend khi bid trong 2 phút cuối

---

**Ghi chú:** Tất cả class đã được thiết kế theo design pattern singleton/DAO

