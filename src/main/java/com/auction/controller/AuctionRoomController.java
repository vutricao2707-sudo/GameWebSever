package com.auction.controller;

import com.auction.database.BidDAO;
import com.auction.database.ItemDAO;
import com.auction.server.AuctionWebSocketServer;
import com.auction.service.AuctionSyncManager;
import com.auction.ui.AuctionWebSocketClient;
import com.auction.util.SessionManager;
import com.auction.util.UIUtils;
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
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

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


        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi trong initialize", e);
        }
    }

    public void setAuctionRoomData(String plate, double startingPrice, String endTimeRaw) {
        logger.info("📍 [AuctionRoom] setAuctionRoomData() - Plate: " + plate);

        try {
            if (plate == null || plate.trim().isEmpty()) {
                UIUtils.showError("Lỗi", "Biển số không hợp lệ");
                return;
            }

            String[] itemData = ItemDAO.getItemFullData(plate);
            if (itemData == null || itemData.length < 10) {
                UIUtils.showError("Lỗi", "Không tìm thấy dữ liệu hợp lệ trong database");
                return;
            }

            String auctionStatus = ItemDAO.getAuctionStatus(plate);
            logger.info("📍 Status từ DB: " + auctionStatus);

            // CHỈ CHẶN NẾU TRẠNG THÁI LÀ ĐĂNG KÝ HOẶC CHỜ
            if ("REGISTRATION".equalsIgnoreCase(auctionStatus) || "WAITING".equalsIgnoreCase(auctionStatus)) {
                UIUtils.showCustomPopup("Chưa mở đấu giá", "Hiện đang trong thời gian đăng ký, vui lòng quay lại khi đến giờ.", "⏳", "#00b894");
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
            UIUtils.showError("Lỗi hệ thống", "Có lỗi xảy ra: " + e.getMessage());
        }
    }

    // =========================================================================
    // TỐI ƯU 1: KHÔI PHỤC LỊCH SỬ (CHẠY THREAD NỀN, KHÔNG QUERIES LẶP LẠI)
    // =========================================================================
    private void initChartSeries() {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Giá đặt");
        chartPointCount = 0;
        priceHistoryChart.setAnimated(false);
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

        // Gọi UIUtils xịn xò, bay sạch viền trắng
        UIUtils.showAuctionResult(iWon, formatter.format(finalPrice), winnerName, this::navigateToDashboard);
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
                UIUtils.showError("Phiên đã kết thúc", "Không thể đặt giá nữa.");
                return;
            }

            if (SessionManager.getCurrentUser() == null) {
                UIUtils.showError("Lỗi", "Bạn chưa đăng nhập");
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

                // Popup Cooldown xịn xò
                UIUtils.showCustomPopup("ĐẶT GIÁ THẤT BẠI",
                        "Vui lòng chờ hết thời gian 1 phút từ lần đặt gần nhất để đặt giá tiếp.\nThời gian còn lại: " + Math.max(0, remainingTime) + " giây",
                        "⏳", "#ef4444");
                return;
            }

            // =========================================================
            // 👇 THÊM VÀO: LOGIC GIA HẠN 60 GIÂY PHÚT CHÓT (ANTI-SNIPPING)
            // Dùng getAuctionTimeRemaining để lấy thời gian thực chuẩn tuyệt đối từ DB
            // =========================================================
            long currentRemainingTime = ItemDAO.getAuctionTimeRemaining(currentPlateNumber);
            if (currentRemainingTime > 0 && currentRemainingTime <= 30) {

                // 1. Cộng thêm 60 giây xuống Database
                ItemDAO.extendAuctionTime(currentPlateNumber, 60);

                // 2. Bắn lệnh EXTEND_TIME lên Server để phát loa thông báo cho toàn phòng
                if (wsClient != null && wsClient.isConnected()) {
                    JSONObject extendMsg = new JSONObject();
                    extendMsg.put("action", "EXTEND_TIME");
                    extendMsg.put("itemId", currentItemId);
                    extendMsg.put("addedSeconds", 60);
                    wsClient.send(extendMsg.toString());
                }
            }
            // =========================================================
            // 👆 KẾT THÚC PHẦN THÊM MỚI
            // =========================================================

            currentPrice = ItemDAO.getCurrentPrice(currentPlateNumber);
            stepMultiplier = 1;
            updatePricingUI();

            String bidderName = SessionManager.getCurrentUser().getFullName();
            if (bidderName == null || bidderName.isBlank()) {
                bidderName = "Người dùng #" + userId;
            }

            // Logic cũ: Bắn NEW_BID lên Server
            if (wsClient != null && wsClient.isConnected()) {
                JSONObject bidMsg = new JSONObject();
                bidMsg.put("action", "NEW_BID");
                bidMsg.put("itemId", currentItemId);
                bidMsg.put("bidAmount", newBid);
                bidMsg.put("bidderId", userId);
                bidMsg.put("bidderName", bidderName);
                bidMsg.put("bidTime", System.currentTimeMillis());

                wsClient.send(bidMsg.toString());
                System.out.println("📤 Client đã bắn NEW_BID lên Server!");
            } else {
                System.err.println("⚠️ Mất kết nối WebSocket, không thể gửi real-time!");
            }
        } catch (Exception e) {
            UIUtils.showError("Lỗi", e.getMessage());
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
    public void onPriceUpdate(int itemId, double currentPrice, int bidderId, String bidderName, long bidTime) {
        try {
            if (itemId != currentItemId) return;

            javafx.application.Platform.runLater(() -> {
                try {
                    AuctionRoomController.this.currentPrice = currentPrice;
                    updatePricingUI();
                    addHistoryRow(formatter.format(currentPrice), bidderName + " - " + formatBidTime(bidTime));

                    // 🚀 Thêm điểm vào chart thay vì load lại toàn bộ
                    addPricePointToChart(currentPrice, bidderName);
                    incrementBidCount();

                } catch (Exception ignored) {}
            });
        } catch (Exception e) {}
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

        // Đảm bảo chỉ xử lý 1 lần
        if (!auctionEnded.compareAndSet(false, true)) {
            return;
        }

        Platform.runLater(() -> {
            try {
                int myId = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : -1;

                boolean iWon = false;

                // Nếu có session user
                if (myId > 0) {
                    try {
                        // Kiểm tra bid cao nhất của user trong phiên này (theo plate)
                        double myHighest = ItemDAO.getUserHighestBid(currentPlateNumber, myId);

                        // Nếu user từng đặt giá và giá cao nhất của họ trùng với finalPrice → thắng
                        if (myHighest > 0 && Math.abs(myHighest - finalPrice) < 0.0001) {
                            iWon = true;
                        } else if (myId == winnerId) {
                            // Dự phòng: nếu WinnerId trùng với myId thì cũng là thắng
                            iWon = true;
                        }
                    } catch (Exception eDb) {
                        // Nếu truy vấn DB lỗi, fallback dùng winnerId so sánh
                        if (myId == winnerId) iWon = true;
                    }
                } else {
                    // Guest / chưa đăng nhập => chắc chắn không thắng
                    iWon = false;
                }

                // Hiển thị dialog kết quả — iWon true => hiện success, ngược lại failure
                showAuctionEndedUI(finalPrice, winnerName, iWon, winnerId);

            } catch (Exception e) {
                logger.log(Level.WARNING, "Error handling onAuctionFinished", e);
                // Fallback: hiển thị kết quả theo winnerId
                int myId = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : -1;
                showAuctionEndedUI(finalPrice, winnerName, myId == winnerId, winnerId);
            }
        });
    }

    @Override
    public void onTimerExtended(int itemId, int newTime) {
        if (itemId != currentItemId) return;
        Platform.runLater(() -> {
            addHistoryRow("GIA HẠN", "Hệ thống +60 giây (Anti-Snipping)");
            UIUtils.showCustomPopup("GIA HẠN THỜI GIAN",
                    "Có người đặt giá phút chót! Phiên đấu giá được cộng thêm 60 giây.",
                    "⏳", "#f59e0b");
        });
    }

    @Override public void onConnected() {}
    @Override public void onDisconnected() {}

    private String formatBidTime(long bidTime) {
        try {
            return Instant.ofEpochMilli(bidTime)
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime()
                    .format(timeFormatter);
        } catch (Exception e) {
            return timeFormatter.format(LocalDateTime.now());
        }
    }

    private void incrementBidCount() {
        if (lblBidCount == null) return;
        String text = lblBidCount.getText();
        int count = 0;
        try {
            count = Integer.parseInt(text.replaceAll("\\D+", ""));
        } catch (Exception ignored) {}
        lblBidCount.setText((count + 1) + " lần đặt");
    }

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
    // =========================================================================
// TỐI ƯU CHART: INCREMENTAL UPDATE — Thêm điểm mới thay vì load lại toàn bộ
// =========================================================================

    private int chartDataPointCount = 0;  // Theo dõi số điểm trên chart

    /**
     * Thêm một điểm dữ liệu mới vào biểu đồ MÀ KHÔNG load lại từ đầu.
     * Được gọi ngay sau khi user đặt giá thành công.
     * @param newPrice Giá mới vừa đặt
     * @param bidderName Tên người đặt
     */
    private void addPricePointToChart(double newPrice, String bidderName) {
        try {
            if (priceHistoryChart == null || priceHistoryChart.getData().isEmpty()) {
                return; // Chart chưa khởi tạo hoặc rỗng
            }

            XYChart.Series<Number, Number> series = priceSeries != null
                    ? priceSeries
                    : priceHistoryChart.getData().get(0);
            if (series == null) return;

            // Tăng index điểm (x-axis)
            chartPointCount++;

            // Tạo điểm mới
            XYChart.Data<Number, Number> newDataPoint =
                    new XYChart.Data<>(chartPointCount, newPrice);

            // Styling điểm
            newDataPoint.setNode(new javafx.scene.shape.Circle(3) {{
                setStyle("-fx-fill: #38bdf8; -fx-cursor: hand;");
            }});

            // Tooltip
            javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(
                    bidderName + " - " + formatter.format(newPrice) + " đ"
            );
            javafx.scene.control.Tooltip.install(newDataPoint.getNode(), tooltip);

            // Thêm vào series
            series.getData().add(newDataPoint);

            logger.info("✅ [Chart] Added new data point: " + chartPointCount
                    + " | Price: " + formatter.format(newPrice));

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error adding price point to chart", e);
        }
    }

    /**
     * Load lại toàn bộ chart từ DB (chỉ gọi lần đầu khi vào phòng).
     * Sau đó dùng addPricePointToChart() để update incremental.
     */
    private void loadPriceHistoryChart() {
        try {
            if (currentItemId == -1 || priceHistoryChart == null) return;

            List<String[]> bidHistory = BidDAO.getBidHistoryForChart(currentItemId);
            int bidCount = BidDAO.getBidCount(currentItemId);

            if (lblBidCount != null) {
                lblBidCount.setText(bidCount + " lần đặt");
            }

            priceHistoryChart.getData().clear();
            chartDataPointCount = 0;  // Reset counter

            if (bidHistory.isEmpty()) return;

            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName("Mức giá");

            for (int i = 0; i < bidHistory.size(); i++) {
                try {
                    String[] bidData = bidHistory.get(i);
                    double bidAmount = Double.parseDouble(bidData[1]);
                    String bidderName = bidData[3];

                    chartDataPointCount = i + 1;

                    XYChart.Data<Number, Number> dataPoint =
                            new XYChart.Data<>(chartDataPointCount, bidAmount);

                    dataPoint.setNode(new javafx.scene.shape.Circle(3) {{
                        setStyle("-fx-fill: #38bdf8; -fx-cursor: hand;");
                    }});

                    javafx.scene.control.Tooltip tooltip =
                            new javafx.scene.control.Tooltip(
                                    bidderName + " - " + formatter.format(bidAmount) + " đ"
                            );
                    javafx.scene.control.Tooltip.install(dataPoint.getNode(), tooltip);

                    series.getData().add(dataPoint);
                } catch (Exception ignored) {}
            }

            priceHistoryChart.getData().add(series);

            if (series.getNode() != null) {
                series.getNode().setStyle("-fx-stroke: #38bdf8; -fx-stroke-width: 2;");
            }

            logger.info("✅ [Chart] Initial load: " + chartDataPointCount + " data points");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi trong loadPriceHistoryChart", e);
        }
    }
}
