# 💻 COPYPASTE CODE - READY TO USE

## 📌 QUICK START

**Copy each Section → Paste into correct file → Save → Rebuild**

---

## 1️⃣ FILE: `BidDAO.java` - ADD 2 NEW METHODS

### Location: End of file (before last `}`)

**Copy this section:**

```java
    // ===== NEW METHODS FOR UI =====

    // Get all bids of one auction (Return List for UI)
    public static List<String[]> getAllBidsForAuction(int auctionId) {
        List<String[]> bids = new ArrayList<>();
        String sql = "SELECT b.id, u.full_name, b.amount, b.bid_time, i.plate_number " +
                     "FROM bids b " +
                     "JOIN users u ON b.bidder_id = u.id " +
                     "JOIN auctions a ON b.auction_id = a.id " +
                     "JOIN items i ON a.item_id = i.id " +
                     "WHERE b.auction_id = ? " +
                     "ORDER BY b.bid_time DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                bids.add(new String[]{
                    rs.getString("id"),
                    rs.getString("plate_number"),
                    String.valueOf(rs.getDouble("amount")),
                    rs.getTimestamp("bid_time").toString(),
                    String.valueOf(rs.getInt("id"))
                });
            }
            
        } catch (SQLException e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
        return bids;
    }
    
    // Get all bids from database
    public static List<String[]> getAllBids() {
        List<String[]> bids = new ArrayList<>();
        String sql = "SELECT b.id, u.full_name, b.amount, b.bid_time, i.plate_number, a.id as auction_id " +
                     "FROM bids b " +
                     "JOIN users u ON b.bidder_id = u.id " +
                     "JOIN auctions a ON b.auction_id = a.id " +
                     "JOIN items i ON a.item_id = i.id " +
                     "ORDER BY b.bid_time DESC LIMIT 100";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = pstmt.executeQuery();
            
            int stt = 1;
            while (rs.next()) {
                bids.add(new String[]{
                    String.valueOf(stt++),
                    rs.getString("plate_number"),
                    String.valueOf(rs.getDouble("amount")),
                    rs.getTimestamp("bid_time").toString()
                });
            }
            
        } catch (SQLException e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
        return bids;
    }
```

---

## 2️⃣ FILE: `ItemDAO.java` - FIX SQL + ADD METHOD

### Part A: Import (if missing)

Add at top:
```java
import java.util.ArrayList;
import java.util.List;
```

### Part B: Fix `startRegistrationPhase` (~line 271)

**Replace OLD CODE:**
```java
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
```

**With NEW CODE:**
```java
    // Bắt đầu giai đoạn đăng ký (khi admin duyệt biển số)
    public static boolean startRegistrationPhase(String plateNumber, int durationMinutes) {
        String sql = "UPDATE items SET " +
                "registration_start_time = NOW(), " +
                "registration_end_time = DATE_ADD(NOW(), INTERVAL ? MINUTE), " +
                "auction_start_time = DATE_ADD(NOW(), INTERVAL ? MINUTE), " +
                "auction_end_time = DATE_ADD(DATE_ADD(NOW(), INTERVAL ? MINUTE), INTERVAL 60 MINUTE), " +
                "registration_duration = ?, " +
                "auction_status = 'REGISTRATION', " +
                "approval_status = 'APPROVED' " +
                "WHERE plate_number = ? AND approval_status = 'PENDING'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, durationMinutes);
            pstmt.setInt(2, durationMinutes);
            pstmt.setInt(3, durationMinutes);
            pstmt.setInt(4, durationMinutes);
            pstmt.setString(5, plateNumber);
```

### Part C: Add `getFinishedAuctions` method (end of file)

**Before last `}`:**

```java
    // ===== NEW METHOD: Get auction results =====
    
    // Get all finished items
    public static List<String[]> getFinishedAuctions() {
        List<String[]> result = new ArrayList<>();
        String sql = "SELECT id, plate_number, start_price, current_price, vehicle_type, seller_name, auction_status, auction_end_time " +
                "FROM items WHERE auction_status = 'FINISHED' " +
                "ORDER BY auction_end_time DESC LIMIT 50";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String[] item = {
                        String.valueOf(rs.getInt("id")),
                        rs.getString("plate_number"),
                        String.valueOf(rs.getDouble("start_price")),
                        String.valueOf(rs.getDouble("current_price")),
                        rs.getString("vehicle_type"),
                        rs.getString("seller_name"),
                        rs.getString("auction_status"),
                        rs.getString("auction_end_time")
                };
                result.add(item);
            }

        } catch (SQLException e) {
            System.out.println("❌ Error: " + e.getMessage());
        }

        return result;
    }
```

---

## 3️⃣ FILE: `DashboardController.java` - FIX LABEL

### Location: ~line 57

**Replace OLD:**
```java
    public void setUserInfo(User user) {
        this.currentUser = user;
        if (user != null && user.getFullName() != null) {
            lblUserName.setText("Xin chào, " + user.getFullName());
        }
    }
```

**With NEW:**
```java
    public void setUserInfo(User user) {
        this.currentUser = user;
        if (user != null && user.getFullName() != null) {
            lblUserName.setText("Xin chào, người dùng");
        }
    }
```

---

## 4️⃣ FILE: `AuctionHistoryController.java` - REPLACE ENTIRE FILE

```java
package com.auction.controller;

import com.auction.database.BidDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionHistoryController {

    private static final Logger logger = Logger.getLogger(AuctionHistoryController.class.getName());

    @FXML private VBox historyListContainer;

    @FXML
    public void initialize() {
        loadAuctionHistory();
    }

    private void loadAuctionHistory() {
        if (historyListContainer == null) return;
        historyListContainer.getChildren().clear();

        List<String[]> bids = BidDAO.getAllBids();
        
        if (bids.isEmpty()) {
            Label emptyLabel = new Label("📝 No bid history yet.");
            emptyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-padding: 30;");
            historyListContainer.getChildren().add(emptyLabel);
            return;
        }

        for (int i = 0; i < bids.size(); i++) {
            String[] bid = bids.get(i);
            int stt = i + 1;
            String plate = bid[1];
            String amount = bid[2];
            String bidTime = bid[3];
            
            historyListContainer.getChildren().add(0, createRow(stt, plate, 
                    Double.parseDouble(amount), bidTime, stt));
        }
    }

    private HBox createRow(int stt, String plate, double bidAmount, String time, int attemptNumber) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 15 15; -fx-border-color: #1e293b; -fx-border-width: 0 0 1 0;");

        DecimalFormat formatter = new DecimalFormat("#,###");
        String formattedPrice = formatter.format(bidAmount) + " đ";

        Label lblStt = new Label(String.valueOf(stt));
        lblStt.setPrefWidth(60);
        lblStt.setStyle("-fx-text-fill: white; -fx-font-size: 15px;");

        Label lblPlate = new Label(plate);
        lblPlate.setPrefWidth(180);
        lblPlate.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label lblPrice = new Label(formattedPrice);
        lblPrice.setPrefWidth(220);
        lblPrice.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label lblTime = new Label(time);
        lblTime.setPrefWidth(200);
        lblTime.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");

        Label lblAttempt = new Label("Lần " + attemptNumber);
        lblAttempt.setPrefWidth(80);
        lblAttempt.setAlignment(Pos.CENTER);
        lblAttempt.setStyle("-fx-background-color: #475569; -fx-text-fill: white; " +
                "-fx-background-radius: 12; -fx-padding: 4 10; -fx-font-size: 13px; -fx-font-weight: bold;");

        HBox boxAttempt = new HBox(lblAttempt);
        boxAttempt.setAlignment(Pos.CENTER_LEFT);
        boxAttempt.setPrefWidth(180);

        row.getChildren().addAll(lblStt, lblPlate, lblPrice, lblTime, boxAttempt);

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-padding: 15 15; -fx-border-color: #1e293b; -fx-border-width: 0 0 1 0;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent; -fx-padding: 15 15; -fx-border-color: #1e293b; -fx-border-width: 0 0 1 0;"));

        return row;
    }

    @FXML
    private void handleHome(ActionEvent event) {
        switchScene(event, "/fxml/Dashboard.fxml");
    }

    @FXML
    private void handleShowResults(ActionEvent event) {
        switchScene(event, "/fxml/AuctionResult.fxml");
    }

    @FXML
    private void handleRegisterToSell(ActionEvent event) {
        switchScene(event, "/fxml/RegisterSell.fxml");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        switchScene(event, "/fxml/Login.fxml");
    }

    private void switchScene(ActionEvent event, String fxmlPath) {
        try {
            Parent newRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            Scene currentScene = ((Node) event.getSource()).getScene();
            currentScene.setRoot(newRoot);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading file: " + fxmlPath, e);
            showInfo("Error loading: " + fxmlPath);
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("System Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
```

---

## 5️⃣ FILE: `AuctionResultController.java` - REPLACE ENTIRE FILE

```java
package com.auction.controller;

import com.auction.database.ItemDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Objects;

public class AuctionResultController {
    private static final Logger logger = Logger.getLogger(AuctionResultController.class.getName());
    
    @FXML private VBox resultListContainer;

    @FXML
    public void initialize() {
        loadAuctionResults();
    }

    private void loadAuctionResults() {
        if (resultListContainer == null) return;
        resultListContainer.getChildren().clear();

        List<String[]> finishedItems = ItemDAO.getFinishedAuctions();
        
        if (finishedItems.isEmpty()) {
            Label emptyLabel = new Label("🏆 No results yet.");
            emptyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-padding: 30;");
            resultListContainer.getChildren().add(emptyLabel);
            return;
        }

        for (int i = 0; i < finishedItems.size(); i++) {
            String[] item = finishedItems.get(i);
            int stt = i + 1;
            String plate = item[1];
            double finalPrice = Double.parseDouble(item[3]);
            String endTime = item[7];
            boolean isSuccess = finalPrice > Double.parseDouble(item[2]);
            
            resultListContainer.getChildren().add(createRow(stt, plate, finalPrice, endTime, isSuccess));
        }
    }

    private HBox createRow(int stt, String plate, double price, String time, boolean isSuccess) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 15 15; -fx-border-color: #1e293b; -fx-border-width: 0 0 1 0;");

        DecimalFormat formatter = new DecimalFormat("#,###");
        String formattedPrice = formatter.format(price) + " đ";

        Label lblStt = new Label(String.valueOf(stt));
        lblStt.setPrefWidth(60);
        lblStt.setStyle("-fx-text-fill: white; -fx-font-size: 15px;");

        Label lblPlate = new Label(plate);
        lblPlate.setPrefWidth(180);
        lblPlate.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label lblPrice = new Label(formattedPrice);
        lblPrice.setPrefWidth(220);
        lblPrice.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label lblTime = new Label(time != null ? time.substring(0, Math.min(16, time.length())) : "N/A");
        lblTime.setPrefWidth(200);
        lblTime.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");

        Label lblStatus = new Label(isSuccess ? "✅ Success" : "❌ Failed");
        lblStatus.setPrefWidth(180);
        if (isSuccess) {
            lblStatus.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 15px;");
        } else {
            lblStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 15px;");
        }

        row.getChildren().addAll(lblStt, lblPlate, lblPrice, lblTime, lblStatus);

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-padding: 15 15; -fx-border-color: #1e293b; -fx-border-width: 0 0 1 0;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent; -fx-padding: 15 15; -fx-border-color: #1e293b; -fx-border-width: 0 0 1 0;"));

        return row;
    }

    @FXML
    private void handleHome(ActionEvent event) {
        switchScene(event, "/fxml/Dashboard.fxml");
    }

    @FXML
    private void handleShowHistory(ActionEvent event) {
        switchScene(event, "/fxml/AuctionHistory.fxml");
    }

    @FXML
    private void handleRegisterToSell(ActionEvent event) {
        switchScene(event, "/fxml/RegisterSell.fxml");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        switchScene(event, "/fxml/Login.fxml");
    }

    private void switchScene(ActionEvent event, String fxmlPath) {
        try {
            Parent newRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            Scene currentScene = ((Node) event.getSource()).getScene();
            currentScene.setRoot(newRoot);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error switching screen", e);
            showInfo("Error", "Cannot load screen!");
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
```

---

## ✅ COMPLETION CHECKLIST

```
☐ 1. Copy & Paste BidDAO.java - 2 methods
☐ 2. Copy & Paste ItemDAO.java - Fix SQL + method
☐ 3. Copy & Paste DashboardController.java - Label fix
☐ 4. Copy & Paste AuctionHistoryController.java - Full file
☐ 5. Copy & Paste AuctionResultController.java - Full file
☐ 6. Rebuild (Ctrl + Shift + F9)
☐ 7. Test app
```

---

**Status: ✅ READY TO USE**


