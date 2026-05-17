# 🎯 READY-TO-USE CODE SNIPPETS

Các đoạn code này có thể copy-paste trực tiếp vào project

---

## 1️⃣ Auto-Extend Usage in AuctionRoomController

```java
// Add to AuctionRoomController class
private Auction currentAuction;

// In setAuctionRoomData() method
public void setAuctionRoomData(String plate, double startingPrice) {
    this.currentPlateNumber = plate;
    this.currentPrice = startingPrice;
    
    // ✅ NEW: Create Auction object for auto-extend
    currentAuction = new Auction("AUC-TEMP", new LicensePlate(...), currentSeller);
    
    // Rest of existing code...
}

// In handlePlaceBid() method, add this at the end:
// ✅ Auto-extend check
if (currentAuction != null) {
    currentAuction.setLastBidTime(System.currentTimeMillis());
    currentAuction.checkAndAutoExtend();
    
    // Update time display if extended
    long remaining = currentAuction.getTimeRemainingSeconds();
    lblMinutes.setText(String.format("%02d", remaining / 60));
    lblSeconds.setText(String.format("%02d", remaining % 60));
}
```

---

## 2️⃣ PlateValidator Quick Check

```java
// Quick validation in any controller
String plate = txtPlateNumber.getText().trim();

if (!PlateValidator.isValidPlate(plate)) {
    showAlert(Alert.AlertType.ERROR, 
        "Biển số không hợp lệ", 
        "Vui lòng nhập: 29A-12345 hoặc 30B.54321");
    return;
}

// If valid, proceed...
```

---

## 3️⃣ Email Service Integration

### Send approval email:
```java
// When admin approves a plate
EmailService.sendPlateApprovedNotification(
    userEmail,           // String
    plateNumber,         // "29A-12345"
    startingPrice        // 5000000.0
);
```

### Send outbid notification:
```java
// In AuctionRoomController, after someone outbids
if (currentAuction != null && currentAuction.getBids().size() >= 2) {
    List<BidTransaction> bids = currentAuction.getBids();
    BidTransaction previousBid = bids.get(bids.size() - 2);
    Bidder previousBidder = previousBid.getBidder();
    
    EmailService.sendOutbidNotification(
        previousBidder.getEmail(),
        currentPlateNumber,
        newBidAmount
    );
}
```

### Send winner email:
```java
// When auction finishes
List<BidTransaction> bids = currentAuction.getBids();
if (!bids.isEmpty()) {
    BidTransaction lastBid = bids.get(bids.size() - 1);
    Bidder winner = lastBid.getBidder();
    
    EmailService.sendWinnerNotification(
        winner.getEmail(),
        currentPlateNumber,
        lastBid.getAmount()
    );
}
```

---

## 4️⃣ Watchlist Usage

### Add to watchlist:
```java
// In DashboardController button handler
int userId = SessionManager.getInstance().getCurrentUser().getId();
boolean success = WatchlistService.addToWatchlist(userId, plateNumber);

if (success) {
    btnWatch.setText("❤️ Đã theo dõi");
    btnWatch.setStyle("-fx-text-fill: #ef4444;");
}
```

### Remove from watchlist:
```java
int userId = SessionManager.getInstance().getCurrentUser().getId();
boolean success = WatchlistService.removeFromWatchlist(userId, plateNumber);

if (success) {
    btnWatch.setText("🤍 Theo dõi");
    btnWatch.setStyle("-fx-text-fill: #94a3b8;");
}
```

### Check if in watchlist:
```java
int userId = SessionManager.getInstance().getCurrentUser().getId();
boolean inWatch = WatchlistService.isInWatchlist(userId, plateNumber);

if (inWatch) {
    btn.setText("❤️ Đã theo dõi");
} else {
    btn.setText("🤍 Theo dõi");
}
```

### Get user's watchlist:
```java
int userId = SessionManager.getInstance().getCurrentUser().getId();
List<Watchlist> myWatchlist = WatchlistService.getUserWatchlist(userId);

for (Watchlist w : myWatchlist) {
    System.out.println("Đang theo dõi: " + w.getPlateNumber());
}
```

---

## 5️⃣ Dashboard Enhancement with Watchlist

```java
// In DashboardController.initialize()
@FXML
private void initialize() {
    currentUserId = SessionManager.getInstance().getCurrentUser().getId();
    loadAuctionsWithWatchlist();
}

private void loadAuctionsWithWatchlist() {
    List<Auction> auctions = AuctionManager.getInstance().getActiveAuctions();
    itemsContainer.getChildren().clear();
    
    for (Auction auction : auctions) {
        VBox card = new VBox(10);
        card.setStyle("-fx-border-color: #334155; -fx-border-width: 1; -fx-padding: 10;");
        
        // Title
        Label lblPlate = new Label(auction.getItem().toString());
        lblPlate.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Price
        Label lblPrice = new Label("Giá: " + 
            new DecimalFormat("#,###").format(auction.getItem().getStartingPrice()) + " đ");
        
        // ✅ Watchlist Button
        String plate = auction.getItem().toString();
        Button btnWatch = new Button();
        boolean inWatch = WatchlistService.isInWatchlist(currentUserId, plate);
        
        btnWatch.setText(inWatch ? "❤️ Đã theo dõi" : "🤍 Theo dõi");
        btnWatch.setStyle(inWatch ? 
            "-fx-text-fill: #ef4444; -fx-padding: 8px; -fx-font-size: 12px;" : 
            "-fx-text-fill: #94a3b8; -fx-padding: 8px; -fx-font-size: 12px;");
        
        btnWatch.setOnAction(e -> {
            if (WatchlistService.isInWatchlist(currentUserId, plate)) {
                WatchlistService.removeFromWatchlist(currentUserId, plate);
                btnWatch.setText("🤍 Theo dõi");
                btnWatch.setStyle("-fx-text-fill: #94a3b8;");
            } else {
                WatchlistService.addToWatchlist(currentUserId, plate);
                btnWatch.setText("❤️ Đã theo dõi");
                btnWatch.setStyle("-fx-text-fill: #ef4444;");
            }
        });
        
        card.getChildren().addAll(lblPlate, lblPrice, btnWatch);
        itemsContainer.getChildren().add(card);
    }
}
```

---

## 6️⃣ Timer Enhancement in AuctionRoom

```java
// Replace existing startCountdown() with this:
private void startCountdown() {
    countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
        if (currentAuction != null) {
            long remaining = currentAuction.getTimeRemainingSeconds();
            
            if (remaining > 0) {
                int minutes = (int) (remaining / 60);
                int seconds = (int) (remaining % 60);
                
                lblMinutes.setText(String.format("%02d", minutes));
                lblSeconds.setText(String.format("%02d", seconds));
                
                // ✅ Change color when < 2 minutes
                if (remaining <= 120) {
                    lblMinutes.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 32px; -fx-font-weight: bold;");
                    lblSeconds.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 32px; -fx-font-weight: bold;");
                } else {
                    lblMinutes.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 32px; -fx-font-weight: bold;");
                    lblSeconds.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 32px; -fx-font-weight: bold;");
                }
            } else {
                // Auction expired
                countdownTimeline.stop();
                btnPlaceBid.setDisable(true);
                btnPlaceBid.setText("PHIÊN ĐẤU GIÁ KẾT THÚC");
                btnPlaceBid.setStyle("-fx-background-color: #334155; -fx-text-fill: #94a3b8;");
                
                // ✅ Send winner email
                if (!currentAuction.getBids().isEmpty()) {
                    List<BidTransaction> bids = currentAuction.getBids();
                    BidTransaction lastBid = bids.get(bids.size() - 1);
                    EmailService.sendWinnerNotification(
                        lastBid.getBidder().getEmail(),
                        currentPlateNumber,
                        lastBid.getAmount()
                    );
                }
            }
        }
    }));
    countdownTimeline.setCycleCount(Timeline.INDEFINITE);
    countdownTimeline.play();
}
```

---

## 7️⃣ CSS for Timer Critical State

Add to your CSS file (e.g., style.css):
```css
.timer-critical {
    -fx-text-fill: #ef4444;
    -fx-font-size: 32px;
    -fx-font-weight: bold;
    -fx-effect: dropshadow(three-pass-box, rgba(239, 68, 68, 0.5), 10, 0, 0, 0);
}

.timer-normal {
    -fx-text-fill: #ffffff;
    -fx-font-size: 32px;
    -fx-font-weight: bold;
}

.btn-watchlist-active {
    -fx-text-fill: #ef4444;
    -fx-background-color: rgba(239, 68, 68, 0.1);
    -fx-border-color: #ef4444;
    -fx-padding: 8px;
}

.btn-watchlist-inactive {
    -fx-text-fill: #94a3b8;
    -fx-background-color: transparent;
    -fx-border-color: #334155;
    -fx-padding: 8px;
}
```

---

## 8️⃣ Database Setup for Watchlist

```sql
-- Create watchlist table
CREATE TABLE watchlist (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    plate_number VARCHAR(20) NOT NULL,
    added_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notified BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_watch (user_id, plate_number)
);

-- Create index for faster queries
CREATE INDEX idx_user_watchlist ON watchlist(user_id);
CREATE INDEX idx_plate_watchlist ON watchlist(plate_number);

-- Sample insert
INSERT INTO watchlist (user_id, plate_number) VALUES (1, '29A-12345');
```

---

## 9️⃣ Email Configuration (Gmail)

Para configurar para usar Gmail SMTP:

1. Enable 2-Factor Authentication en Gmail
2. Generar App Password en https://myaccount.google.com/apppasswords
3. Copiar el app password (16 caracteres)
4. Actualizar en `EmailService.java`:

```java
private static final String SENDER_EMAIL = "tu-email@gmail.com";
private static final String APP_PASSWORD = "xxxx xxxx xxxx xxxx";
```

5. Descomentar la sección `Session.getInstance()` en método `sendEmail()`

6. Agregar dependency en pom.xml:
```xml
<dependency>
    <groupId>javax.mail</groupId>
    <artifactId>mail</artifactId>
    <version>1.4.7</version>
</dependency>
```

---

## 🔟 Testing Snippets

### Test Auto-Extend:
```java
// Simulate bid in last 2 minutes
Auction auction = new Auction("AUC-1", item, seller);
auction.setDurationSeconds(3600); // 60 minutes

// Simulate 58 minutes have passed
LocalDateTime endTime = LocalDateTime.now().plusSeconds(120); // 2 minutes left
auction.setEndTime(endTime);

// Simulate bid
auction.setLastBidTime(System.currentTimeMillis());
auction.checkAndAutoExtend();

// Check extended
assert auction.getTimeRemainingSeconds() > 120 : "Should extend";
```

### Test PlateValidator:
```java
assert PlateValidator.isValidPlate("29A-12345") == true;
assert PlateValidator.isValidPlate("30B.54321") == true;
assert PlateValidator.isValidPlate("invalid") == false;
assert PlateValidator.isValidPlate("29A12345") == false; // No separator
```

### Test Watchlist:
```java
int userId = 1;
String plate = "29A-12345";

assert !WatchlistService.isInWatchlist(userId, plate);
WatchlistService.addToWatchlist(userId, plate);
assert WatchlistService.isInWatchlist(userId, plate);
WatchlistService.removeFromWatchlist(userId, plate);
assert !WatchlistService.isInWatchlist(userId, plate);
```

---

**Copy-paste ready! 🚀**

