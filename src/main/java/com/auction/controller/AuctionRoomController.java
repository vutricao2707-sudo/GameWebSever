package com.auction.controller;

import com.auction.database.BidDAO;
import com.auction.database.ItemDAO;
import com.auction.database.WatchlistDAO;
import com.auction.ui.AuctionWebSocketClient;
import com.auction.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.stage.Stage;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionRoomController implements AuctionWebSocketClient.AuctionWebSocketListener {

    private static final Logger logger = Logger.getLogger(AuctionRoomController.class.getName());

    // --- WebSocket Client ---
    private AuctionWebSocketClient wsClient;
    private int currentItemId = -1;

    // --- Các thành phần FXML ---
    @FXML private Label lblMinutes, lblSeconds;
    @FXML private Label lblPlateTop, lblPlateBottom;
    @FXML private VBox liveHistoryContainer;
    @FXML private Label lblCurrentPrice, lblStepMultiplier;
    @FXML private Button btnPlaceBid;
    @FXML private Button btnWatchlist;  // Nút theo dõi
    @FXML private Label lblParticipants;  // Hiển thị số người tham gia
    @FXML private LineChart<Number, Number> priceHistoryChart;  // Biểu đồ lịch sử giá
    @FXML private Label lblBidCount;  // Số lần đặt giá

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

        lblMinutes.setText("60");
        lblSeconds.setText("00");

        updatePricingUI();
        updateParticipantCount();

        // =========================
        // FIX MEMORY LEAK
        // =========================

        javafx.application.Platform.runLater(() -> {

            Stage stage = (Stage)
                    lblMinutes.getScene().getWindow();

            stage.setOnCloseRequest(event -> {

                stopAllTimelines();

                System.out.println(
                        "🛑 Window closed - timelines cleaned"
                );
            });
        });
    }

    // =========================================================================
    // LOGIC ĐỒNG HỒ ĐẾM NGƯỢC
    // =========================================================================
    private void startCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (timeRemainingSeconds > 0) {
                timeRemainingSeconds--;
                int minutes = timeRemainingSeconds / 60;
                int seconds = timeRemainingSeconds % 60;

                // Format luôn có 2 chữ số (VD: 05:09)
                lblMinutes.setText(String.format("%02d", minutes));
                lblSeconds.setText(String.format("%02d", seconds));
            } else {
                countdownTimeline.stop();
                // Phiên đấu giá kết thúc
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

    // =========================================================================
    // LOGIC TĂNG GIẢM BƯỚC GIÁ
    // =========================================================================
    @FXML
    private void handleIncreaseStep() {
        stepMultiplier++;
        updatePricingUI();
    }

    @FXML
    private void handleDecreaseStep() {
        if (stepMultiplier > 1) {
            stepMultiplier--;
            updatePricingUI();
        }
    }

    private void updatePricingUI() {
        lblStepMultiplier.setText(String.valueOf(stepMultiplier));
        lblCurrentPrice.setText(formatter.format(currentPrice) + " đ");

        double nextBid = currentPrice + (stepValue * stepMultiplier);

        // ĐÃ SỬA: Đổi chữ "Trả giá" thành "ĐẶT GIÁ:" cho ngầu
        btnPlaceBid.setText("ĐẶT GIÁ: " + formatter.format(nextBid) + " đ");
    }

    // =========================================================================
    // LOGIC BẤM NÚT "TRẢ GIÁ" - CÓ COOLDOWN
    // =========================================================================
    @FXML
    private void handlePlaceBid() {
        String[] itemData =
                ItemDAO.getItemFullData(currentPlateNumber);

        if (itemData != null) {

            String auctionStatus =
                    itemData[9];

            if ("FINISHED".equalsIgnoreCase(auctionStatus)) {

                btnPlaceBid.setDisable(true);

                btnPlaceBid.setText(
                        "PHIÊN ĐẤU GIÁ ĐÃ KẾT THÚC"
                );

                showAlert(
                        "Phiên đã kết thúc",
                        "Không thể đặt giá nữa."
                );

                return;
            }
        }
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

        // Cập nhật vào database
        if (currentPlateNumber != null && !currentPlateNumber.isEmpty()) {
            // =========================================================
            // ✅ SERVER-SIDE COOLDOWN VALIDATION
            // =========================================================
            if (SessionManager.INSTANCE.getCurrentUser() != null) {
                int userId = SessionManager.INSTANCE.getCurrentUser().getId();
                boolean bidSaved = BidDAO.saveBid(currentPlateNumber, userId, newBid);

                if (!bidSaved) {
                    // Server từ chối do cooldown
                    long dbLastBidTime = BidDAO.getLastBidTime(currentPlateNumber, userId);
                    long remainingTime = (dbLastBidTime + 60) - (System.currentTimeMillis() / 1000);
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Chờ một chút");
                    alert.setHeaderText(null);
                    alert.setContentText("⏳ Server nhập chặn: Bạn phải chờ " + Math.max(1, remainingTime) + " giây nữa.\n" +
                            "(Điều này ngăn việc bypass bằng cách mở 2 cửa sổ)");
                    alert.showAndWait();
                    return; // Dừng lại
                }
            }

            // Chỉ cập nhật nếu bid được lưu thành công
            currentPrice = newBid;

            // Cập nhật giá mới trên bảng items
            ItemDAO.updateCurrentPrice(currentPlateNumber, newBid);
        }

        // 2. Reset hệ số về 1
        stepMultiplier = 1;
        updatePricingUI();

        // 3. Ghi lại thời gian đặt giá (cho client-side feedback)
        lastBidTime = currentTime;

        // 4. Thêm vào đầu danh sách lịch sử (LIFO) trên giao diện hiện tại
        addHistoryRow(formatter.format(newBid), currentPlateNumber + " - Bạn");
    }

    private void addHistoryRow(String priceStr, String userId) {
        VBox row = new VBox(5);
        row.setStyle("-fx-padding: 15; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;");

        // Chữ xanh lá cho mức giá
        Label lblPrice = new Label(priceStr + " đ");
        lblPrice.setStyle("-fx-text-fill: #10b981; -fx-font-size: 16px; -fx-font-weight: bold;");

        HBox detailBox = new HBox();

        // Lấy giờ hệ thống hiện tại
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy • HH:mm:ss"));
        Label lblTime = new Label(currentTime);
        lblTime.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblUser = new Label(userId);
        if (userId.contains("Bạn")) {
            lblUser.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 13px; -fx-font-weight: bold;"); // Màu xanh dương nếu là mình
        } else {
            lblUser.setStyle("-fx-text-fill: #eab308; -fx-font-size: 13px; -fx-font-weight: bold;"); // Màu vàng nếu là đối thủ
        }

        detailBox.getChildren().addAll(lblTime, spacer, lblUser);
        row.getChildren().addAll(lblPrice, detailBox);

        // Hiệu ứng Hover
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-padding: 15; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent; -fx-padding: 15; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;"));

        // Add lên đầu (Index 0)
        liveHistoryContainer.getChildren().add(0, row);
    }

    /**
     * Xử lý nút "Theo dõi" - Thêm/Xóa item khỏi watchlist
     */
    @FXML
    private void handleWatchlist() {
        if (SessionManager.INSTANCE.getCurrentUser() == null) {
            showAlert("Chưa đăng nhập", "Bạn cần đăng nhập để theo dõi biển số");
            return;
        }

        if (currentPlateNumber == null || currentPlateNumber.isEmpty()) {
            showAlert("Lỗi", "Không tìm thấy biển số");
            return;
        }

        int userId = SessionManager.INSTANCE.getCurrentUser().getId();
        int itemId = currentItemId;

        if (itemId == -1) {
            itemId = ItemDAO.getItemIdByPlate(currentPlateNumber);
            if (itemId == -1) {
                showAlert("Lỗi", "Không tìm thấy item trong database");
                return;
            }
        }

        // Kiểm tra xem đã theo dõi chưa
        boolean isWatched = WatchlistDAO.isInWatchlist(userId, itemId);

        if (isWatched) {
            // Xóa khỏi watchlist
            boolean removed = WatchlistDAO.removeFromWatchlist(userId, itemId);
            if (removed) {
                btnWatchlist.setStyle("-fx-background-color: rgba(59, 130, 246, 0.3); -fx-border-color: #3b82f6; -fx-background-radius: 20; -fx-border-radius: 20; -fx-cursor: hand; -fx-padding: 6 15; -fx-font-size: 14; -fx-font-weight: bold;");
                btnWatchlist.setText("⭐ Theo dõi");
                showAlert("Đã xóa", "Biển số đã được xóa khỏi danh sách theo dõi");
                System.out.println("✅ Đã xóa từ watchlist: " + currentPlateNumber);
            }
        } else {
            // Thêm vào watchlist
            boolean added = WatchlistDAO.addToWatchlist(userId, itemId);
            if (added) {
                btnWatchlist.setStyle("-fx-background-color: rgba(234, 179, 8, 0.3); -fx-border-color: #eab308; -fx-background-radius: 20; -fx-border-radius: 20; -fx-cursor: hand; -fx-padding: 6 15; -fx-font-size: 14; -fx-font-weight: bold;");
                btnWatchlist.setText("⭐ Bỏ theo dõi");
                showAlert("Thành công", "Biển số đã được thêm vào danh sách theo dõi");
                System.out.println("✅ Đã thêm vào watchlist: " + currentPlateNumber);
            }
        }
    }

    /**
     * Cập nhật trạng thái button theo dõi khi load trang
     */
    private void updateWatchlistButtonState() {
        if (SessionManager.INSTANCE.getCurrentUser() == null || currentItemId == -1) {
            btnWatchlist.setDisable(true);
            return;
        }

        int userId = SessionManager.INSTANCE.getCurrentUser().getId();
        boolean isWatched = WatchlistDAO.isInWatchlist(userId, currentItemId);

        if (isWatched) {
            btnWatchlist.setStyle("-fx-background-color: rgba(234, 179, 8, 0.3); -fx-border-color: #eab308; -fx-background-radius: 20; -fx-border-radius: 20; -fx-cursor: hand; -fx-padding: 6 15; -fx-font-size: 14; -fx-font-weight: bold;");
            btnWatchlist.setText("⭐ Bỏ theo dõi");
        } else {
            btnWatchlist.setStyle("-fx-background-color: rgba(59, 130, 246, 0.3); -fx-border-color: #3b82f6; -fx-background-radius: 20; -fx-border-radius: 20; -fx-cursor: hand; -fx-padding: 6 15; -fx-font-size: 14; -fx-font-weight: bold;");
            btnWatchlist.setText("⭐ Theo dõi");
        }
    }

    // =====================================================================
    // PHẦN MỚI: QUẢN LÝ SỐ NGƯỜI THAM GIA
    // =====================================================================
    private void updateParticipantCount() {
        if (currentPlateNumber != null && !currentPlateNumber.isEmpty()) {
            currentParticipants = ItemDAO.getParticipantCount(currentPlateNumber);
            if (lblParticipants != null) {
                lblParticipants.setText("👥 " + currentParticipants + " người tham gia");
            }
        }
    }

    // =========================================================================
    // TRUYỀN DỮ LIỆU VÀ ĐIỀU HƯỚNG
    // =========================================================================

    // Hàm này sẽ được gọi từ Dashboard truyền sang tất cả thông tin từ database
    public void setAuctionRoomData(String plate, double startingPrice) {
        String[] itemData =
                ItemDAO.getItemFullData(plate);
        if (itemData != null) {

            String auctionStatus = itemData[9];

            // =====================================
            // CHƯA BẮT ĐẦU
            // =====================================

            if ("REGISTRATION".equalsIgnoreCase(auctionStatus)) {

                showAlert(
                        "Phiên chưa bắt đầu",
                        "Hiện vẫn đang trong giai đoạn đăng ký tham gia đấu giá."
                );

                return;
            }

            // =====================================
            // ĐÃ KẾT THÚC
            // =====================================

            if ("FINISHED".equalsIgnoreCase(auctionStatus)) {

                showAlert(
                        "Phiên đã kết thúc",
                        "Phiên đấu giá này đã kết thúc."
                );

                return;
            }
        }
        this.currentPlateNumber = plate;
        this.currentPrice = startingPrice;
        this.currentItemId = ItemDAO.getItemIdByPlate(plate);

        // Hiển thị biển số lên giao diện
        String[] parts = plate.split("-");
        lblPlateTop.setText(parts.length > 0 ? parts[0] : plate);
        lblPlateBottom.setText(parts.length > 1 ? parts[1] : "");

        // ✅ Cập nhật trạng thái watchlist button
        updateWatchlistButtonState();

        // ✅ FIX 5: ĐỌC thời gian còn lại thực tế từ database
        long remaining = ItemDAO.getAuctionTimeRemaining(plate);
        if (remaining > 0) {
            this.timeRemainingSeconds = (int) remaining;
        } else {
            // Nếu có lỗi hoặc chưa bắt đầu, mặc định là 60 phút
            this.timeRemainingSeconds = 60 * 60;
        }
        stopAllTimelines();
        // ✅ Cập nhật giao diện giá và số người xem
        updatePricingUI();
        updateParticipantCount();
        // ✅ Tải biểu đồ lịch sử giá
        loadPriceHistoryChart();
        // ✅ Bắt đầu đếm ngược ngay sau khi lấy được thời gian từ DB
        startCountdown();

        // ✅ WebSocket: Subscribe tới cập nhật realtime của item này
        if (wsClient == null) {
            wsClient = AuctionWebSocketClient.getInstance();
        }
        wsClient.addListener(this);
        wsClient.subscribe(currentItemId);
        System.out.println("🔌 Subscribed tới WebSocket item: " + currentItemId);
    }

    @FXML
    private void handleBack(ActionEvent event) {
        // ✅ Dừng tất cả đồng hồ và luồng chạy ngầm khi thoát
        stopAllTimelines();

        // ✅ Unsubscribe từ WebSocket khi rời khỏi phòng đấu giá
        if (wsClient != null && currentItemId != -1) {
            wsClient.unsubscribe(currentItemId);
            wsClient.removeListener(this);
            System.out.println("🔌 Unsubscribed từ WebSocket item: " + currentItemId);
        }

        try {
            Parent newRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/Dashboard.fxml")));
            Scene currentScene = ((Node) event.getSource()).getScene();
            currentScene.setRoot(newRoot);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Lỗi khi tải trang Dashboard", e);
        }
    }
    private void showAlert(String title, String message) {

        Alert alert =
                new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle(title);

        alert.setHeaderText(null);

        alert.setContentText(message);

        alert.showAndWait();
    }
    private void stopAllTimelines() {

        if (countdownTimeline != null) {

            countdownTimeline.stop();

            System.out.println(
                    "🛑 Countdown stopped"
            );
        }
    }

    // =====================================================================
    // PRICE HISTORY CHART - Biểu đồ lịch sử giá real-time
    // =====================================================================

    /**
     * Load lịch sử giá từ database và vẽ biểu đồ
     * Called when entering auction room or when new bid is placed via WebSocket
     */
    private void loadPriceHistoryChart() {
        if (currentItemId == -1 || priceHistoryChart == null) {
            return;
        }

        // Lấy dữ liệu từ database
        List<String[]> bidHistory = BidDAO.getBidHistoryForChart(currentItemId);
        int bidCount = BidDAO.getBidCount(currentItemId);

        // Cập nhật label số lần đặt giá
        if (lblBidCount != null) {
            lblBidCount.setText(bidCount + " lần đặt");
        }

        // Clear chart cũ
        priceHistoryChart.getData().clear();

        if (bidHistory.isEmpty()) {
            System.out.println("ℹ️ Chưa có bids nào cho item này");
            return;
        }

        // Tạo series dữ liệu
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Mức giá");

        // Thêm dữ liệu vào series
        for (int i = 0; i < bidHistory.size(); i++) {
            String[] bidData = bidHistory.get(i);
            double bidAmount = Double.parseDouble(bidData[1]);
            String bidderName = bidData[3];

            // X axis: Thứ tự đặt giá (1, 2, 3...)
            // Y axis: Mức giá
            XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(i + 1, bidAmount);

            // Thêm tooltip hiển thị tên người đặt giá khi hover
            dataPoint.setNode(new javafx.scene.shape.Circle(3) {
                {
                    setStyle("-fx-fill: #38bdf8; -fx-cursor: hand;");
                }
            });

            // Tooltip
            javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(
                    bidderName + " - " + String.format("%.0f", bidAmount) + " đ"
            );
            javafx.scene.control.Tooltip.install(dataPoint.getNode(), tooltip);

            series.getData().add(dataPoint);
        }

        // Thêm series vào chart
        priceHistoryChart.getData().add(series);

        // Styling
        series.getNode().setStyle("-fx-stroke: #38bdf8; -fx-stroke-width: 2;");

        System.out.println("✅ Đã tải biểu đồ giá với " + bidCount + " bids");
    }

    /**
     * Update chart khi có bid mới từ WebSocket
     * Called từ onPriceUpdate listener
     */
    private void updatePriceHistoryChart() {
        javafx.application.Platform.runLater(this::loadPriceHistoryChart);
    }

    // =====================================================================
    // WEBSOCKET LISTENER IMPLEMENTATIONS
    // =====================================================================

    @Override
    public void onPriceUpdate(int itemId, double currentPrice, int bidderId, String bidderName) {
        if (itemId != currentItemId) return;

        javafx.application.Platform.runLater(() -> {
            this.currentPrice = currentPrice;
            updatePricingUI();
            addHistoryRow(formatter.format(currentPrice), bidderName);
            // ✅ Update chart với bid mới
            updatePriceHistoryChart();
            System.out.println("💰 Cập nhật giá từ WebSocket: " + currentPrice);
        });
    }

    @Override
    public void onParticipantUpdate(int itemId, int participantCount) {
        if (itemId != currentItemId) return;

        javafx.application.Platform.runLater(() -> {
            currentParticipants = participantCount;
            if (lblParticipants != null) {
                lblParticipants.setText("👥 " + participantCount + " người tham gia");
            }
            System.out.println("👥 Cập nhật số người tham gia: " + participantCount);
        });
    }

    @Override
    public void onAuctionFinished(int itemId, double finalPrice, int winnerId, String winnerName) {
        if (itemId != currentItemId) return;

        javafx.application.Platform.runLater(() -> {
            btnPlaceBid.setDisable(true);
            btnPlaceBid.setText("PHIÊN ĐẤU GIÁ ĐÃ KẾT THÚC");
            btnPlaceBid.setStyle("-fx-background-color: #334155; -fx-text-fill: #94a3b8; -fx-background-radius: 12; -fx-font-size: 16px; -fx-font-weight: bold;");

            // Dừng countdown
            if (countdownTimeline != null) {
                countdownTimeline.stop();
            }

            String message = "Phiên đấu giá kết thúc!\n\n" +
                    "Giá cuối cùng: " + formatter.format(finalPrice) + " đ\n" +
                    "Người thắng: " + winnerName;
            showAlert("Phiên đấu giá kết thúc", message);

            System.out.println("🏆 Phiên đấu giá kết thúc - Người thắng: " + winnerName);
        });
    }

     @Override
    public void onTimerExtended(int itemId, int newTimeRemaining) {
        if (itemId != currentItemId) return;

        javafx.application.Platform.runLater(() -> {
            this.timeRemainingSeconds = newTimeRemaining;

            // Display user-friendly message
            long minutes = newTimeRemaining / 60;
            long seconds = newTimeRemaining % 60;
            String newTimeStr = String.format("%02d:%02d", minutes, seconds);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("⏱️ Timer Kéo Dài!");
            alert.setHeaderText(null);
            alert.setContentText("🎯 Có bid trong 2 phút cuối!\n\n✨ Timer được tự động kéo dài thêm 2 phút.\n\n" +
                    "Thời gian còn lại: " + newTimeStr);
            alert.showAndWait();

            System.out.println("⏰ Timer kéo dài - Thời gian còn lại: " + newTimeRemaining + "s (" + newTimeStr + ")");
        });
    }

    @Override
    public void onConnected() {
        System.out.println("✅ WebSocket UI connected");
    }

    @Override
    public void onDisconnected() {
        System.out.println("❌ WebSocket UI disconnected");
    }
}