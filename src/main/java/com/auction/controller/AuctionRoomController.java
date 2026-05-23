package com.auction.controller;

import com.auction.database.BidDAO;
import com.auction.database.ItemDAO;
import com.auction.service.AuctionSyncManager;
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
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.text.DecimalFormat;
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
    private int timeRemainingSeconds = 0;
    private Timeline countdownTimeline;
    private final DecimalFormat formatter = new DecimalFormat("#,###");

    // --- Biến Cooldown (1 phút sau mỗi lần đặt giá) ---
    private long lastBidTime = 0;
    private static final long BID_COOLDOWN_SECONDS = 60; // 1 phút

    @FXML
    public void initialize() {
        logger.info("📍 [AuctionRoom] Bắt đầu initialize controller");

        try {
            if (lblMinutes == null || lblSeconds == null || lblCurrentPrice == null ||
                    lblStepMultiplier == null || btnPlaceBid == null) {
                logger.log(Level.SEVERE, "Một hoặc nhiều thành phần FXML là null - FXML không load đúng");
                return;
            }

            logger.info("✅ [AuctionRoom] Tất cả FXML elements loaded");

            // Initialize UI
            lblMinutes.setText("00");
            lblSeconds.setText("00");

            updatePricingUI();
            updateParticipantCount();

            // ========================= Fix Memory Leak =========================
            javafx.application.Platform.runLater(() -> {
                try {
                    Stage stage = (Stage) lblMinutes.getScene().getWindow();
                    if (stage != null) {
                        stage.setOnCloseRequest(event -> {
                            logger.info("🛑 [AuctionRoom] Window closing - stopping timelines");
                            unregisterAuctionListener();
                            stopAllTimelines();

                            if (wsClient != null && currentItemId != -1) {
                                try {
                                    wsClient.unsubscribe(currentItemId);
                                    wsClient.removeListener(this);
                                    logger.info("🔌 [AuctionRoom] WebSocket unsubscribed on window close");
                                } catch (Exception eWs) {
                                    logger.log(Level.WARNING, "Error unsubscribing WebSocket", eWs);
                                }
                            }
                        });
                    }
                } catch (Exception eStage) {
                    logger.log(Level.WARNING, "Error setting window close handler", eStage);
                }
            });

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi trong initialize", e);
        }
    }

    // =========================================================================
    // LOGIC ĐỒNG HỒ ĐẾM NGƯỢC
    // =========================================================================
    private AuctionSyncManager.AuctionListener auctionListener;

    // Thay thế startCountdown() bằng:
    private void registerAuctionListener() {
        logger.info("📍 [AuctionRoom] Registering AuctionListener with AuctionSyncManager");

        auctionListener = new AuctionSyncManager.AuctionListener() {
            @Override
            public void onTimeTick(int itemId, long remainingSeconds, double currentPrice, String auctionStatus) {
                try {
                    if (itemId != currentItemId) return;

                    javafx.application.Platform.runLater(() -> {
                        try {
                            // Cập nhật timer
                            int minutes = (int) (remainingSeconds / 60);
                            int seconds = (int) (remainingSeconds % 60);

                            lblMinutes.setText(String.format("%02d", minutes));
                            lblSeconds.setText(String.format("%02d", seconds));

                            // Cập nhật giá từ DB - không poll riêng nữa
                            AuctionRoomController.this.currentPrice = currentPrice;
                            updatePricingUI();

                        } catch (Exception ignored) {}
                    });
                } catch (Exception e) {}
            }

            @Override
            public void onAuctionEnded(int itemId, double finalPrice) {
                try {
                    if (itemId != currentItemId) return;

                    javafx.application.Platform.runLater(() -> {
                        try {
                            if (btnPlaceBid != null) {
                                btnPlaceBid.setText("PHIÊN ĐẤU GIÁ KẾT THÚC");
                                btnPlaceBid.setStyle("-fx-background-color: #334155; -fx-text-fill: #94a3b8; -fx-background-radius: 12; -fx-font-size: 16px; -fx-font-weight: bold;");
                                btnPlaceBid.setDisable(true);
                            }

                            if (currentPlateNumber != null && !currentPlateNumber.isEmpty()) {
                                int itemIdDb = ItemDAO.getItemIdByPlate(currentPlateNumber);
                                if (itemIdDb != -1) {
                                    ItemDAO.finishAuction(itemIdDb, finalPrice);
                                    logger.info("✅ [AuctionRoom] Auction finished in database");
                                }
                            }
                        } catch (Exception ignored) {}
                    });
                } catch (Exception e) {}
            }
        };

        try {
            AuctionSyncManager.getInstance().addListener(currentItemId, auctionListener);
            logger.info("✅ [AuctionRoom] AuctionListener registered");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error registering AuctionListener", e);
        }
    }

    // Xóa listener khi rời phòng
    private void unregisterAuctionListener() {
        try {
            if (auctionListener != null && currentItemId != -1) {
                AuctionSyncManager.getInstance().removeListener(currentItemId, auctionListener);
                logger.info("🔌 [AuctionRoom] AuctionListener unregistered");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error unregistering AuctionListener", e);
        }
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
        try {
            if (lblStepMultiplier == null || lblCurrentPrice == null || btnPlaceBid == null) return;

            lblStepMultiplier.setText(String.valueOf(stepMultiplier));
            lblCurrentPrice.setText(formatter.format(currentPrice) + " đ");

            double nextBid = currentPrice + (stepValue * stepMultiplier);
            btnPlaceBid.setText("ĐẶT GIÁ: " + formatter.format(nextBid) + " đ");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in updatePricingUI", e);
        }
    }

    // =========================================================================
    // LOGIC BẤM NÚT "ĐẶT GIÁ"
    // =========================================================================
    @FXML
    private void handlePlaceBid() {
        logger.info("📍 [AuctionRoom] handlePlaceBid() called");

        try {
            if (currentPlateNumber == null || currentPlateNumber.isEmpty()) {
                showAlert("Lỗi", "Biển số không xác định");
                return;
            }

            String[] itemData = ItemDAO.getItemFullData(currentPlateNumber);
            if (itemData != null && itemData.length >= 10) {
                if ("FINISHED".equalsIgnoreCase(itemData[9])) {
                    if (btnPlaceBid != null) {
                        btnPlaceBid.setDisable(true);
                        btnPlaceBid.setText("PHIÊN ĐẤU GIÁ ĐÃ KẾT THÚC");
                    }
                    showAlert("Phiên đã kết thúc", "Không thể đặt giá nữa.");
                    return;
                }
            }

            long currentTime = System.currentTimeMillis() / 1000;
            long timeSinceLastBid = currentTime - lastBidTime;

            if (timeSinceLastBid < BID_COOLDOWN_SECONDS) {
                long remainingTime = BID_COOLDOWN_SECONDS - timeSinceLastBid;
                showAlert("Chờ một chút", "Bạn phải chờ " + remainingTime + " giây nữa mới được đặt giá!");
                return;
            }

            if (SessionManager.INSTANCE.getCurrentUser() == null) {
                showAlert("Lỗi", "Bạn chưa đăng nhập");
                return;
            }

            int userId = SessionManager.INSTANCE.getCurrentUser().getId();
            double latestPrice = ItemDAO.getCurrentPrice(currentPlateNumber);
            double newBid = latestPrice + (stepValue * stepMultiplier);

            boolean bidSaved = BidDAO.saveBid(currentPlateNumber, userId, newBid);
            if (!bidSaved) {
                long dbLastBidTime = BidDAO.getLastBidTime(currentPlateNumber, userId);
                long remainingTime = (dbLastBidTime + 60) - (System.currentTimeMillis() / 1000);
                showAlert("Chờ một chút", "⏳ Server chặn spam.\nBạn phải chờ " + Math.max(1, remainingTime) + " giây nữa.");
                return;
            }

            boolean updated = ItemDAO.updateCurrentPriceIfHigher(currentPlateNumber, latestPrice, newBid);
            if (!updated) {
                refreshCurrentPrice();
                showAlert("Đặt giá thất bại", "⚠️ Đã có người đặt giá trước bạn.\nGiá đã được cập nhật.");
                return;
            }

            logger.info("✅ [AuctionRoom] Bid placed successfully!");
            currentPrice = newBid;
            stepMultiplier = 1;
            lastBidTime = currentTime;

            updatePricingUI();
            addHistoryRow(formatter.format(newBid), currentPlateNumber + " - Bạn");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi không mong đợi trong handlePlaceBid", e);
            showAlert("Lỗi hệ thống", "Có lỗi xảy ra: " + e.getMessage());
        }
    }

    private void updateParticipantCount() {
        try {
            if (currentPlateNumber != null && !currentPlateNumber.isEmpty() && lblParticipants != null) {
                currentParticipants = ItemDAO.getParticipantCount(currentPlateNumber);
                lblParticipants.setText("👥 " + currentParticipants + " người tham gia");
            }
        } catch (Exception eDb) {
            logger.log(Level.WARNING, "Error getting participant count", eDb);
        }
    }

    // =========================================================================
    // TRUYỀN DỮ LIỆU VÀ ĐIỀU HƯỚNG
    // Đã sửa: Truyền thêm endTimeRaw để đồng bộ chính xác thời gian thực
    // =========================================================================
    public void setAuctionRoomData(String plate, double startingPrice, String endTimeRaw) {
        logger.info("📍 [AuctionRoom] setAuctionRoomData() - Plate: " + plate);

        try {
            if (plate == null || plate.trim().isEmpty()) {
                showAlert("Lỗi", "Biển số không hợp lệ");
                return;
            }

            String[] itemData = ItemDAO.getItemFullData(plate);
            if (itemData == null || itemData.length < 10) {
                showAlert("Lỗi", "Không tìm thấy dữ liệu item hợp lệ trong database");
                return;
            }

            String auctionStatus = itemData[9];

            try {
                String plateTop = "";
                String plateBottom = "";
                if (plate.contains("-")) {
                    String[] parts = plate.split("-");
                    plateTop = (parts.length > 0) ? parts[0].trim() : "";
                    plateBottom = (parts.length > 1) ? parts[1].trim() : "";
                } else {
                    if (plate.length() >= 2) {
                        plateTop = plate.substring(0, 2);
                        plateBottom = plate.substring(2);
                    } else {
                        plateTop = plate;
                        plateBottom = "";
                    }
                }
                lblPlateTop.setText(plateTop);
                lblPlateBottom.setText(plateBottom);
            } catch (Exception ePlate) {
                logger.log(Level.SEVERE, "Lỗi display plate", ePlate);
            }

            if ("REGISTRATION".equalsIgnoreCase(auctionStatus)) {
                showAlert("Phiên chưa bắt đầu", "Hiện vẫn đang trong giai đoạn đăng ký tham gia đấu giá.");
                return;
            }
            if ("FINISHED".equalsIgnoreCase(auctionStatus)) {
                showAlert("Phiên đã kết thúc", "Phiên đấu giá này đã kết thúc.");
                return;
            }
            if ("WAITING".equalsIgnoreCase(auctionStatus)) {
                showAlert("Đang chờ", "Phiên này đang chờ khai mạc.");
                return;
            }

            try {
                if (wsClient != null && currentItemId != -1) {
                    wsClient.unsubscribe(currentItemId);
                    wsClient.removeListener(this);
                }
                stopAllTimelines();
            } catch (Exception eClean) {
                logger.log(Level.WARNING, "Error cleaning old session", eClean);
            }

            this.currentPlateNumber = plate;

            try {
                double dbPrice = ItemDAO.getCurrentPrice(plate);
                this.currentPrice = dbPrice > 0 ? dbPrice : startingPrice;
            } catch (Exception ePriceLoad) {
                this.currentPrice = startingPrice;
            }

            try {
                this.currentItemId = ItemDAO.getItemIdByPlate(plate);
            } catch (Exception eItemId) {
                currentItemId = -1;
            }

            // ====================== Cập nhật tính toán thời gian thực ======================
            this.timeRemainingSeconds = 0;
            if (endTimeRaw != null && !endTimeRaw.isEmpty()) {
                try {
                    String cleaned = endTimeRaw.replace("T", " ");
                    if (cleaned.contains(".")) cleaned = cleaned.substring(0, cleaned.indexOf("."));
                    LocalDateTime targetTime = Timestamp.valueOf(cleaned).toLocalDateTime();
                    java.time.Duration diff = java.time.Duration.between(LocalDateTime.now(), targetTime);

                    if (diff.isNegative() || diff.isZero()) {
                        this.timeRemainingSeconds = 0;
                    } else {
                        this.timeRemainingSeconds = (int) diff.getSeconds();
                    }
                    logger.info("✅ [AuctionRoom] Đã tính chuẩn thời gian còn lại: " + this.timeRemainingSeconds + " giây");
                } catch (Exception eTime) {
                    logger.log(Level.SEVERE, "Lỗi phân tích thời gian kết thúc", eTime);
                    this.timeRemainingSeconds = 0;
                }
            }

            updatePricingUI();
            updateParticipantCount();
            loadPriceHistoryChart();
            loadInitialBidHistory();

            // ====================== Khởi động đếm ngược hoặc Kết thúc ngay ======================
            if (this.timeRemainingSeconds > 0) {
                registerAuctionListener();  // Thay vì startCountdown()
            } else {
                logger.info("🏁 [AuctionRoom] Thời gian đã hết ngay khi vào phòng!");
                lblMinutes.setText("00");
                lblSeconds.setText("00");
                if (btnPlaceBid != null) {
                    btnPlaceBid.setText("PHIÊN ĐẤU GIÁ KẾT THÚC");
                    btnPlaceBid.setStyle("-fx-background-color: #334155; -fx-text-fill: #94a3b8; -fx-background-radius: 12; -fx-font-size: 16px; -fx-font-weight: bold;");
                    btnPlaceBid.setDisable(true);
                }
            }

            try {
                if (wsClient == null) {
                    wsClient = AuctionWebSocketClient.getInstance();
                }
                if (wsClient != null) {
                    wsClient.addListener(this);
                    wsClient.subscribe(currentItemId);
                }
            } catch (Exception eWs) {
                logger.log(Level.WARNING, "Error setting up WebSocket", eWs);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi không mong đợi trong setAuctionRoomData", e);
            showAlert("Lỗi hệ thống", "Có lỗi xảy ra: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            unregisterAuctionListener();  // Thêm dòng này
            stopAllTimelines();
            if (wsClient != null && currentItemId != -1) {
                try {
                    wsClient.unsubscribe(currentItemId);
                    wsClient.removeListener(this);
                } catch (Exception eWs) {}
            }

            Parent newRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/Dashboard.fxml")));
            Scene currentScene = ((Node) event.getSource()).getScene();
            if (currentScene != null) {
                currentScene.setRoot(newRoot);
            }
        } catch (Exception e) {
            showAlert("Lỗi hệ thống", "Có lỗi xảy ra: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        try {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        } catch (Exception e) {
            System.err.println("❌ Error showing alert: " + title + " - " + message);
        }
    }

    private void stopAllTimelines() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
    }

    // =====================================================================
    // PRICE HISTORY CHART & HISTORY LOG
    // =====================================================================
    private void loadPriceHistoryChart() {
        try {
            if (currentItemId == -1 || priceHistoryChart == null) return;

            List<String[]> bidHistory = BidDAO.getBidHistoryForChart(currentItemId);
            int bidCount = BidDAO.getBidCount(currentItemId);

            if (lblBidCount != null) {
                lblBidCount.setText(bidCount + " lần đặt");
            }

            priceHistoryChart.getData().clear();
            if (bidHistory.isEmpty()) return;

            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName("Mức giá");

            for (int i = 0; i < bidHistory.size(); i++) {
                try {
                    String[] bidData = bidHistory.get(i);
                    double bidAmount = Double.parseDouble(bidData[1]);
                    String bidderName = bidData[3];

                    XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(i + 1, bidAmount);
                    dataPoint.setNode(new javafx.scene.shape.Circle(3) {{
                        setStyle("-fx-fill: #38bdf8; -fx-cursor: hand;");
                    }});

                    javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(
                            bidderName + " - " + String.format("%.0f", bidAmount) + " đ"
                    );
                    javafx.scene.control.Tooltip.install(dataPoint.getNode(), tooltip);

                    series.getData().add(dataPoint);
                } catch (Exception ignored) {}
            }
            priceHistoryChart.getData().add(series);
            if (series.getNode() != null) {
                series.getNode().setStyle("-fx-stroke: #38bdf8; -fx-stroke-width: 2;");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi trong loadPriceHistoryChart", e);
        }
    }

    private void loadInitialBidHistory() {
        if (currentItemId == -1 || liveHistoryContainer == null) return;
        try {
            liveHistoryContainer.getChildren().clear();
            List<String[]> bidHistory = BidDAO.getBidHistoryForChart(currentItemId);

            if (bidHistory.isEmpty()) return;

            for (int i = bidHistory.size() - 1; i >= 0; i--) {
                String[] bid = bidHistory.get(i);
                try {
                    double amount = Double.parseDouble(bid[1]);
                    String bidderName = bid[3] != null ? bid[3] : "Người dùng";
                    addHistoryRow(formatter.format(amount), bidderName);
                } catch (NumberFormatException ignored) {}
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi trong loadInitialBidHistory", e);
        }
    }

    private void updatePriceHistoryChart() {
        javafx.application.Platform.runLater(this::loadPriceHistoryChart);
    }

    // =====================================================================
    // WEBSOCKET LISTENER IMPLEMENTATIONS
    // =====================================================================
    @Override
    public void onPriceUpdate(int itemId, double currentPrice, int bidderId, String bidderName) {
        try {
            if (itemId != currentItemId) return;
            javafx.application.Platform.runLater(() -> {
                try {
                    this.currentPrice = currentPrice;
                    updatePricingUI();
                    addHistoryRow(formatter.format(currentPrice), bidderName);
                    updatePriceHistoryChart();
                } catch (Exception ignored) {}
            });
        } catch (Exception e) {}
    }

    @Override
    public void onParticipantUpdate(int itemId, int participantCount) {
        try {
            if (itemId != currentItemId) return;
            javafx.application.Platform.runLater(() -> {
                try {
                    currentParticipants = participantCount;
                    if (lblParticipants != null) {
                        lblParticipants.setText("👥 " + participantCount + " người tham gia");
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception e) {}
    }

    @Override
    public void onAuctionFinished(int itemId, double finalPrice, int winnerId, String winnerName) {
        try {
            if (itemId != currentItemId) return;
            javafx.application.Platform.runLater(() -> {
                try {
                    if (btnPlaceBid != null) {
                        btnPlaceBid.setDisable(true);
                        btnPlaceBid.setText("PHIÊN ĐẤU GIÁ ĐÃ KẾT THÚC");
                        btnPlaceBid.setStyle("-fx-background-color: #334155; -fx-text-fill: #94a3b8; -fx-background-radius: 12; -fx-font-size: 16px; -fx-font-weight: bold;");
                    }
                    if (countdownTimeline != null) {
                        countdownTimeline.stop();
                    }
                    String message = "Phiên đấu giá kết thúc!\n\nGiá cuối cùng: " + formatter.format(finalPrice) + " đ\nNgười thắng: " + winnerName;
                    showAlert("Phiên đấu giá kết thúc", message);
                } catch (Exception ignored) {}
            });
        } catch (Exception e) {}
    }

    @Override
    public void onTimerExtended(int itemId, int newTimeRemaining) {
        try {
            if (itemId != currentItemId) return;
            javafx.application.Platform.runLater(() -> {
                try {
                    this.timeRemainingSeconds = newTimeRemaining;
                    long minutes = newTimeRemaining / 60;
                    long seconds = newTimeRemaining % 60;
                    String newTimeStr = String.format("%02d:%02d", minutes, seconds);

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("⏱️ Timer Kéo Dài!");
                    alert.setHeaderText(null);
                    alert.setContentText("🎯 Có bid trong 2 phút cuối!\n\n✨ Timer được tự động kéo dài thêm 2 phút.\n\nThời gian còn lại: " + newTimeStr);
                    alert.showAndWait();
                } catch (Exception ignored) {}
            });
        } catch (Exception e) {}
    }

    @Override public void onConnected() {}
    @Override public void onDisconnected() {}

    private void refreshCurrentPrice() {
        try {
            if (currentPlateNumber == null || currentPlateNumber.isEmpty()) return;
            currentPrice = ItemDAO.getCurrentPrice(currentPlateNumber);
            updatePricingUI();
        } catch (Exception ignored) {}
    }

    private void addHistoryRow(String price, String bidderName) {
        try {
            if (liveHistoryContainer == null || price == null || bidderName == null) return;

            HBox row = new HBox();
            row.setSpacing(10);

            Label lblBidder = new Label(bidderName);
            lblBidder.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label lblPrice = new Label(price + " đ");
            lblPrice.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 14px; -fx-font-weight: bold;");

            row.getChildren().addAll(lblBidder, spacer, lblPrice);
            liveHistoryContainer.getChildren().add(0, row);

            if (liveHistoryContainer.getChildren().size() > 20) {
                liveHistoryContainer.getChildren().remove(liveHistoryContainer.getChildren().size() - 1);
            }
        } catch (Exception ignored) {}
    }
}