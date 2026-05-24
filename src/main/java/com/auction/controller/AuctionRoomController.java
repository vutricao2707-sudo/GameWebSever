package com.auction.controller;

import com.auction.database.BidDAO;
import com.auction.database.ItemDAO;
import com.auction.service.AuctionSyncManager;
import com.auction.ui.AuctionWebSocketClient;
import com.auction.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
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
    @FXML private Label lblParticipants;
    @FXML private LineChart<Number, Number> priceHistoryChart;
    @FXML private Label lblBidCount;

    // --- Biến lưu trữ Logic Đấu Giá ---
    private double currentPrice;
    private final double stepValue = 5000000.0;
    private int stepMultiplier = 1;
    private String currentPlateNumber;
    private int timeRemainingSeconds = 0;
    private final java.util.concurrent.atomic.AtomicBoolean auctionEnded =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    // --- Biến Đồng hồ & Cấu trúc mới ---
    private Timeline exitCountdownTimeline; // Thay thế countdownTimeline cũ
    private final DecimalFormat formatter = new DecimalFormat("#,###");

    private XYChart.Series<Number, Number> priceSeries;
    private int chartPointCount = 0;

    private AuctionSyncManager.AuctionListener auctionListener;

    @FXML
    public void initialize() {
        logger.info("📍 [AuctionRoom] Bắt đầu initialize controller");

        try {
            if (lblMinutes == null || lblSeconds == null || lblCurrentPrice == null ||
                    lblStepMultiplier == null || btnPlaceBid == null) {
                logger.log(Level.SEVERE, "Một hoặc nhiều thành phần FXML là null");
                return;
            }

            lblMinutes.setText("00");
            lblSeconds.setText("00");
            updatePricingUI();
            updateParticipantCount();

            // Dọn dẹp tập trung (Teardown) khi đóng cửa sổ
            Platform.runLater(() -> {
                try {
                    Stage stage = (Stage) lblMinutes.getScene().getWindow();
                    if (stage != null) {
                        stage.setOnCloseRequest(event -> teardown(currentItemId));
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Lỗi khi gán sự kiện đóng cửa sổ", e);
                }
            });

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi trong initialize", e);
        }
    }

    public void setAuctionRoomData(String plate, double startingPrice, String endTimeRaw) {
        logger.info("📍 [AuctionRoom] setAuctionRoomData() - Plate: " + plate);

        try {
            if (plate == null || plate.trim().isEmpty()) {
                showAlert("Lỗi", "Biển số không hợp lệ");
                return;
            }

            String[] itemData = ItemDAO.getItemFullData(plate);
            if (itemData == null || itemData.length < 10) {
                showAlert("Lỗi", "Không tìm thấy dữ liệu hợp lệ trong database");
                return;
            }

            // Sửa toàn bộ khối kiểm tra trạng thái trong setAuctionRoomData thành:
            String auctionStatus = ItemDAO.getAuctionStatus(plate);
            logger.info("📍 Status từ DB: " + auctionStatus);

            // CHỈ CHẶN NẾU TRẠNG THÁI LÀ ĐĂNG KÝ HOẶC CHỜ
            if ("REGISTRATION".equalsIgnoreCase(auctionStatus) || "WAITING".equalsIgnoreCase(auctionStatus)) {
                showCustomStatusPopup("Chưa mở đấu giá", "Hiện đang trong thời gian đăng ký, vui lòng quay lại khi đến giờ.", "⏳", "#00b894");
                return;
            }
            // 2. Nếu đã kết thúc, vẫn cho vào phòng nhưng hiện popup kết quả
            boolean isFinished = "FINISHED".equalsIgnoreCase(auctionStatus);
            // -----------------------------------

            // Dọn dẹp session cũ
            final int oldItemId = currentItemId;
            teardown(oldItemId);
            auctionEnded.set(false);
            this.currentPlateNumber = plate;

            // Xử lý hiển thị biển số
            try {
                String plateTop = "", plateBottom = "";
                if (plate.contains("-")) {
                    String[] parts = plate.split("-");
                    plateTop = (parts.length > 0) ? parts[0].trim() : "";
                    plateBottom = (parts.length > 1) ? parts[1].trim() : "";
                } else if (plate.length() >= 3) {
                    plateTop = plate.substring(0, 2);
                    plateBottom = plate.substring(2);
                } else {
                    plateTop = plate;
                }
                lblPlateTop.setText(plateTop);
                lblPlateBottom.setText(plateBottom);
            } catch (Exception e) {}

            // Lấy thông tin giá và ID
            try {
                double dbPrice = ItemDAO.getCurrentPrice(plate);
                this.currentPrice = dbPrice > 0 ? dbPrice : startingPrice;
                this.currentItemId = ItemDAO.getItemIdByPlate(plate);
            } catch (Exception e) {
                this.currentPrice = startingPrice;
                this.currentItemId = -1;
            }

            // 3. TÍNH TOÁN THỜI GIAN
            this.timeRemainingSeconds = 0;
            if (endTimeRaw != null && !endTimeRaw.isEmpty()) {
                try {
                    String cleaned = endTimeRaw.replace("T", " ");
                    if (cleaned.contains(".")) cleaned = cleaned.substring(0, cleaned.indexOf("."));
                    LocalDateTime targetTime = Timestamp.valueOf(cleaned).toLocalDateTime();
                    java.time.Duration diff = java.time.Duration.between(LocalDateTime.now(), targetTime);
                    this.timeRemainingSeconds = (int) Math.max(diff.getSeconds(), 0);
                    logger.info("⏱ Time Remaining: " + this.timeRemainingSeconds + " giây");
                } catch (Exception e) {}
            }

            updatePricingUI();
            updateParticipantCount();
            initChartSeries();
            restoreBidHistoryAsync();

            // 4. QUYẾT ĐỊNH VÀO PHÒNG HAY KẾT THÚC
            // ĐIỂM SỬA QUAN TRỌNG: Cho phép vào phòng nếu status là AUCTION
            // kể cả khi timeRemainingSeconds tính ra là 0 (do lệch time server/client)
            if (isFinished) {

                auctionEnded.set(true);

                if (btnPlaceBid != null) {
                    btnPlaceBid.setDisable(true);
                    btnPlaceBid.setText("PHIÊN ĐẤU GIÁ ĐÃ KẾT THÚC");
                }

                lblMinutes.setText("00");
                lblSeconds.setText("00");

                determineResultAndShowDialog(currentPrice);

            }else if ("AUCTION".equalsIgnoreCase(auctionStatus)) {

                registerAuctionListener();

            } else {

                showAuctionEndedUI(currentPrice, "— (Kết thúc)", false, 0);
            }
            // Setup WebSocket
            try {
                if (wsClient == null) wsClient = AuctionWebSocketClient.getInstance();
                if (wsClient != null) {
                    wsClient.removeListener(this);
                    wsClient.addListener(this);
                    wsClient.subscribe(currentItemId);
                }
            } catch (Exception e) {}

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi hệ thống", e);
            showAlert("Lỗi hệ thống", "Có lỗi xảy ra: " + e.getMessage());
        }
    }

    // =========================================================================
    // TỐI ƯU 1: KHÔI PHỤC LỊCH SỬ (CHẠY THREAD NỀN, KHÔNG QUERIES LẶP LẠI)
    // =========================================================================
    private void initChartSeries() {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Giá đặt");
        chartPointCount = 0;

        priceHistoryChart.getData().clear();
        priceHistoryChart.getData().add(priceSeries);

        Platform.runLater(() -> {
            if (priceSeries.getNode() != null) {
                priceSeries.getNode().setStyle("-fx-stroke: #38bdf8; -fx-stroke-width: 2;");
            }
        });
    }

    private void restoreBidHistoryAsync() {
        if (currentItemId == -1) return;

        Thread worker = new Thread(() -> {
            try {
                List<String[]> history = BidDAO.getBidHistoryForChart(currentItemId);
                int count = BidDAO.getBidCount(currentItemId);

                Platform.runLater(() -> {
                    if (lblBidCount != null) lblBidCount.setText(count + " lần đặt");
                    if (liveHistoryContainer != null) liveHistoryContainer.getChildren().clear();

                    if (history.isEmpty()) return;

                    // Vẽ Chart (từ cũ tới mới)
                    for (int i = 0; i < history.size(); i++) {
                        try {
                            double amount = Double.parseDouble(history.get(i)[1]);
                            appendChartPoint(amount);
                        } catch (Exception ignored) {}
                    }

                    // Đổ Log VBox (từ mới tới cũ)
                    for (int i = history.size() - 1; i >= 0; i--) {
                        try {
                            double amount = Double.parseDouble(history.get(i)[1]);
                            String bidder = history.get(i)[3] != null ? history.get(i)[3] : "Người dùng";
                            addHistoryRow(formatter.format(amount), bidder);
                        } catch (Exception ignored) {}
                    }
                });
            } catch (Exception e) {
                logger.log(Level.WARNING, "restoreBidHistory error", e);
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    // TỐI ƯU 2: THÊM ĐIỂM VÀO CHART (KHÔNG XÓA VÀ VẼ LẠI)
    private void appendChartPoint(double amount) {
        chartPointCount++;
        XYChart.Data<Number, Number> point = new XYChart.Data<>(chartPointCount, amount);

        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(4);
        dot.setStyle("-fx-fill: #38bdf8;");
        point.setNode(dot);

        Tooltip tip = new Tooltip(formatter.format(amount) + " đ");
        Tooltip.install(dot, tip);

        priceSeries.getData().add(point);
    }

    // =========================================================================
    // LẮNG NGHE ĐỒNG HỒ TỪ SYNC MANAGER (GIỮ NGUYÊN CODE CỦA BẠN)
    // =========================================================================
    private void registerAuctionListener() {
        if (auctionListener != null && currentItemId != -1) {
            try {
                AuctionSyncManager.getInstance()
                        .removeListener(currentItemId, auctionListener);
            } catch (Exception ignored) {}
        }
        auctionListener = new AuctionSyncManager.AuctionListener() {
            @Override
            public void onTimeTick(int itemId,
                                   long remainingSeconds,
                                   double currentPriceParam,
                                   String auctionStatus) {

                if (itemId != currentItemId || auctionEnded.get()) return;

                try {

                    int minutes = (int) (remainingSeconds / 60);
                    int seconds = (int) (remainingSeconds % 60);

                    lblMinutes.setText(String.format("%02d", minutes));
                    lblSeconds.setText(String.format("%02d", seconds));

                    updateTimerColor(remainingSeconds);

                    if (currentPriceParam != currentPrice) {
                        currentPrice = currentPriceParam;
                        updatePricingUI();
                    }

                } catch (Exception ignored) {}
            }

            @Override
            public void onAuctionEnded(int itemId, double finalPrice) {
                if (itemId != currentItemId) return;

                if (!auctionEnded.compareAndSet(false, true)) {
                    return;
                }
                Platform.runLater(() -> {
                    currentPrice = finalPrice;
                    determineResultAndShowDialog(finalPrice);
                });
            }
        };

        try {
            AuctionSyncManager.getInstance().addListener(currentItemId, auctionListener);
        } catch (Exception e) {}
    }

    // =========================================================================
    // TỐI ƯU 3: KẾT THÚC PHIÊN + DIALOG ĐẾM NGƯỢC 60S
    // =========================================================================
    private void determineResultAndShowDialog(double finalPrice) {
        if (lblMinutes == null || lblMinutes.getScene() == null) {
            return;
        }
        Thread t = new Thread(() -> {
            String winnerName = "Không xác định";
            int winnerId = 0; // 1. Khai báo biến ở đây để nó có phạm vi (scope) rộng hơn
            boolean currentUserWon = false;

            try {
                String[] winner = ItemDAO.getWinnerByPlate(currentPlateNumber);
                if (winner != null) {
                    winnerName = winner[1];
                    winnerId = Integer.parseInt(winner[0]); // 2. Gán giá trị vào biến đã khai báo
                    int myId = SessionManager.getCurrentUser() != null
                            ? SessionManager.getCurrentUser().getId() : -1;
                    currentUserWon = (myId == winnerId);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Lỗi khi lấy thông tin người thắng", e);
            }

            final String wName = winnerName;
            final boolean won = currentUserWon;
            final int wId = winnerId; // 3. Giờ thì biến winnerId đã tồn tại ở đây để gán cho wId

            Platform.runLater(() -> showAuctionEndedUI(finalPrice, wName, won, wId));
        });
        t.setDaemon(true);
        t.start();
    }
    // ==========================================================================
    // END OF AUCTION (Popup Báo Kết Quả - Dark Theme)
    // ==========================================================================
    private void showAuctionEndedUI(double finalPrice, String winnerName, boolean iWon, int winnerId) {
        if (btnPlaceBid != null) {
            btnPlaceBid.setDisable(true);
            btnPlaceBid.setText("PHIÊN ĐẤU GIÁ ĐÃ KẾT THÚC");
            btnPlaceBid.setStyle("-fx-background-color: #334155; -fx-text-fill: #94a3b8; "
                    + "-fx-background-radius: 12; -fx-font-size: 16px; -fx-font-weight: bold;");
        }

        teardown(currentItemId);
        if (lblMinutes != null) lblMinutes.setText("00");
        if (lblSeconds != null) lblSeconds.setText("00");

        String resultEmoji    = iWon ? "🏆" : "😔";
        String resultTitle    = iWon ? "CHÚC MỪNG! BẠN ĐÃ THẮNG" : "PHIÊN ĐẤU GIÁ KẾT THÚC";
        String resultSubtitle = iWon ? "Bạn là người đặt giá cao nhất!" : "Người thắng: " + winnerName;
        String themeColor     = iWon ? "#00b894" : "#ef4444"; // Xanh ngọc nếu thắng, Đỏ nếu thua

        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Kết quả đấu giá");
        alert.setHeaderText(null);
        alert.setGraphic(null);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #1a2230; -fx-border-color: " + themeColor + "; -fx-border-radius: 8; -fx-border-width: 2;");

        VBox content = new VBox(15);
        content.setStyle("-fx-alignment: center; -fx-padding: 25;");

        Label emojiLbl = new Label(resultEmoji);
        emojiLbl.setStyle("-fx-font-size: 55px;");

        Label titleLbl = new Label(resultTitle);
        titleLbl.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + themeColor + ";");

        // FIX LỖI "fmt" -> Đã đổi thành "formatter"
        Label priceLbl = new Label("Giá cuối: " + formatter.format(finalPrice) + " đ");
        priceLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");

        Label subtitleLbl = new Label(resultSubtitle);
        subtitleLbl.setStyle("-fx-font-size: 15px; -fx-text-fill: #cbd5e1;");

        content.getChildren().addAll(emojiLbl, titleLbl, priceLbl, subtitleLbl);
        dialogPane.setContent(content);
        dialogPane.setMinWidth(400);

        ButtonType btnBack = new ButtonType("Quay lại Dashboard", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(btnBack);

        // Nút bo tròn màu nổi bật
        Node backButton = dialogPane.lookupButton(btnBack);
        if (backButton != null) {
            backButton.setStyle("-fx-background-color: " + themeColor + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px; -fx-padding: 10 30; -fx-background-radius: 20; -fx-cursor: hand;");
        }

        alert.setOnHidden(ev -> navigateToDashboard());
        alert.show();
    }

    // =========================================================================
    // LOGIC ĐẶT GIÁ (GIỮ NGUYÊN)
    // =========================================================================
    @FXML private void handleIncreaseStep() { stepMultiplier++; updatePricingUI(); }
    @FXML private void handleDecreaseStep() { if (stepMultiplier > 1) { stepMultiplier--; updatePricingUI(); } }

    private void updatePricingUI() {
        if (lblStepMultiplier == null || lblCurrentPrice == null || btnPlaceBid == null) return;
        lblStepMultiplier.setText(String.valueOf(stepMultiplier));
        lblCurrentPrice.setText(formatter.format(currentPrice) + " đ");
        double nextBid = currentPrice + (stepValue * stepMultiplier);
        btnPlaceBid.setText("ĐẶT GIÁ: " + formatter.format(nextBid) + " đ");
    }
    private void updateTimerColor(long remainingSeconds) {

        String timerColor =
                remainingSeconds < 120
                        ? "#ff4444"
                        : "#ffffff";

        String style =
                "-fx-text-fill: " + timerColor + ";" +
                        "-fx-font-size: 48px;" +
                        "-fx-font-weight: bold;";

        if (lblMinutes != null) lblMinutes.setStyle(style);
        if (lblSeconds != null) lblSeconds.setStyle(style);
    }

    @FXML
    private void handlePlaceBid() {
        try {
            if (currentPlateNumber == null || currentPlateNumber.isEmpty()) return;

            String[] itemData = ItemDAO.getItemFullData(currentPlateNumber);
            if (itemData != null && "FINISHED".equalsIgnoreCase(itemData[9])) {
                showAlert("Phiên đã kết thúc", "Không thể đặt giá nữa.");
                return;
            }

            if (SessionManager.getCurrentUser() == null) {
                showAlert("Lỗi", "Bạn chưa đăng nhập");
                return;
            }

            int userId = SessionManager.getCurrentUser().getId();
            double latestPrice = ItemDAO.getCurrentPrice(currentPlateNumber);
            double newBid = latestPrice + (stepValue * stepMultiplier);

            boolean bidSaved = BidDAO.saveBid(currentPlateNumber, userId, newBid);

            if (!bidSaved) {
                long dbLastBidTime = BidDAO.getLastBidTime(currentPlateNumber, userId);
                long remainingTime = (dbLastBidTime + 60) - (System.currentTimeMillis() / 1000);
                refreshCurrentPrice();
                showAlert("Đặt giá thất bại", "Vui lòng thử lại. Cooldown còn: " + Math.max(0, remainingTime) + " s");
                return;
            }

            currentPrice = ItemDAO.getCurrentPrice(currentPlateNumber);
            stepMultiplier = 1;
            updatePricingUI();

        } catch (Exception e) {
            showAlert("Lỗi", e.getMessage());
        }
    }

    private void refreshCurrentPrice() {
        try {
            currentPrice = ItemDAO.getCurrentPrice(currentPlateNumber);
            updatePricingUI();
        } catch (Exception ignored) {}
    }

    // =========================================================================
    // WEBSOCKET & CẬP NHẬT UI
    // =========================================================================
    @Override
    public void onPriceUpdate(int itemId,
                              double currentPriceParam,
                              int bidderId,
                              String bidderName) {

        if (itemId != currentItemId) return;

        Platform.runLater(() -> {

            try {

                // KHÔNG CHECK > nữa
                currentPrice = currentPriceParam;

                updatePricingUI();

                appendChartPoint(currentPriceParam);

                addHistoryRow(
                        formatter.format(currentPriceParam),
                        bidderName
                );

                // update bid count realtime
                if (lblBidCount != null) {

                    int current =
                            Integer.parseInt(
                                    lblBidCount.getText()
                                            .replace(" lần đặt", "")
                            );

                    lblBidCount.setText((current + 1) + " lần đặt");
                }

            } catch (Exception e) {

                e.printStackTrace();
            }
        });
    }

    @Override
    public void onParticipantUpdate(int itemId, int count) {
        if (itemId != currentItemId) return;
        Platform.runLater(() -> {
            if (lblParticipants != null) lblParticipants.setText("👥 " + count + " người tham gia");
        });
    }

    @Override
    public void onAuctionFinished(int itemId, double finalPrice, int winnerId, String winnerName) {
        if (itemId != currentItemId) return;

        if (!auctionEnded.compareAndSet(false, true)) {
            return;
        }
        Platform.runLater(() -> {
            int myId = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : -1;
            // Thêm tham số winnerId vào đây:
            showAuctionEndedUI(finalPrice, winnerName, myId == winnerId, winnerId);
        });
    }

    @Override
    public void onTimerExtended(int itemId, int newTime) {}

    @Override public void onConnected() {}
    @Override public void onDisconnected() {}

    private void addHistoryRow(String price, String bidderName) {
        if (liveHistoryContainer == null) return;
        HBox row = new HBox(10);
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
    }

    private void updateParticipantCount() {
        try {
            if (currentPlateNumber != null && lblParticipants != null) {
                lblParticipants.setText("👥 " + ItemDAO.getParticipantCount(currentPlateNumber) + " người tham gia");
            }
        } catch (Exception ignored) {}
    }

    // =========================================================================
    // TỐI ƯU 4: DỌN DẸP TẬP TRUNG (TEARDOWN)
    // =========================================================================
    private void teardown(int itemId) {

        if (exitCountdownTimeline != null) {
            exitCountdownTimeline.stop();
            exitCountdownTimeline = null;
        }

        try {

            if (auctionListener != null && itemId != -1) {

                AuctionSyncManager.getInstance()
                        .removeListener(itemId, auctionListener);

                auctionListener = null;
            }

            if (wsClient != null && itemId != -1) {

                wsClient.unsubscribe(itemId);

                wsClient.removeListener(this);
            }

        } catch (Exception ignored) {}
    }
    @FXML
    private void handleBack(ActionEvent event) {
        teardown(currentItemId);
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/Dashboard.fxml")));
            Scene scene = ((Node) event.getSource()).getScene();
            if (scene != null) scene.setRoot(root);
        } catch (Exception e) {}
    }

    private void navigateToDashboard() {
        auctionEnded.set(true);
        teardown(currentItemId);
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/Dashboard.fxml")));
            if (lblMinutes != null && lblMinutes.getScene() != null) {
                lblMinutes.getScene().setRoot(root);
            }
        } catch (Exception e) {}
    }

    private void showAlert(String title, String message) {
        try {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(message);
            a.showAndWait();
        } catch (Exception ignored) {}
    }
    // =========================================================================
    // HÀM POPUP TÙY CHỈNH (DARK THEME)
    // =========================================================================
    private void showCustomStatusPopup(String title, String message, String emoji, String themeColor) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setGraphic(null);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #1a2230; -fx-border-color: " + themeColor + "; -fx-border-radius: 8; -fx-border-width: 2;");

        VBox content = new VBox(15);
        content.setStyle("-fx-alignment: center; -fx-padding: 25;");

        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size: 55px;");

        Label titleLbl = new Label(title.toUpperCase());
        titleLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + themeColor + ";");

        Label msgLbl = new Label(message);
        msgLbl.setStyle("-fx-font-size: 15px; -fx-text-fill: #cbd5e1; -fx-text-alignment: center;");
        msgLbl.setWrapText(true);
        msgLbl.setMaxWidth(300);

        content.getChildren().addAll(emojiLbl, titleLbl, msgLbl);
        dialogPane.setContent(content);

        ButtonType btnClose = new ButtonType("Đã hiểu", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(btnClose);

        Node closeButton = dialogPane.lookupButton(btnClose);
        if (closeButton != null) {
            closeButton.setStyle("-fx-background-color: " + themeColor + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 25; -fx-background-radius: 20; -fx-cursor: hand;");
        }

        alert.showAndWait();
    }
}