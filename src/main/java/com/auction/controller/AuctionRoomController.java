package com.auction.controller;

import com.auction.database.BidDAO;
import com.auction.database.ItemDAO;
import com.auction.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionRoomController {

    private static final Logger logger = Logger.getLogger(AuctionRoomController.class.getName());
    // --- Biến cho Fix 6 ---
    private Timeline bidPollingTimeline;
    private double lastKnownPrice;
    // --- Các thành phần FXML ---
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
        // Ép nhãn hiển thị 60:00 ngay khi vừa mở page, tránh bị hiện 05:00 của FXML
        lblMinutes.setText("60");
        lblSeconds.setText("00");

        updatePricingUI();
        updateParticipantCount();
    }

    // =========================================================================
    // LOGIC ĐỒNG HỒ ĐẾM NGƯỢC
    // =========================================================================
    private void startCountdown() {
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
            // Cập nhật giá mới trên bảng items
            ItemDAO.updateCurrentPrice(currentPlateNumber, newBid);

            // =========================================================
            // THÊM MỚI (FIX 3 & 4): LƯU LỊCH SỬ VÀO BẢNG BIDS
            // =========================================================
            if (SessionManager.getInstance().getCurrentUser() != null) {
                int userId = SessionManager.getInstance().getCurrentUser().getId();
                BidDAO.saveBid(currentPlateNumber, userId, newBid);
            }
        }

        // 2. Reset hệ số về 1
        stepMultiplier = 1;
        updatePricingUI();

        // 3. Ghi lại thời gian đặt giá
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
        this.currentPlateNumber = plate;
        this.currentPrice = startingPrice;

        // Hiển thị biển số lên giao diện
        String[] parts = plate.split("-");
        lblPlateTop.setText(parts.length > 0 ? parts[0] : plate);
        lblPlateBottom.setText(parts.length > 1 ? parts[1] : "");

        // ✅ FIX 5: ĐỌC thời gian còn lại thực tế từ database
        long remaining = ItemDAO.getAuctionTimeRemaining(plate);
        if (remaining > 0) {
            this.timeRemainingSeconds = (int) remaining;
        } else {
            // Nếu có lỗi hoặc chưa bắt đầu, mặc định là 60 phút
            this.timeRemainingSeconds = 60 * 60;
        }

        // Cập nhật giao diện giá và số người xem
        updatePricingUI();
        updateParticipantCount();

        // ✅ Bắt đầu đếm ngược ngay sau khi lấy được thời gian từ DB
        startCountdown();

        // ✅ Bắt đầu quét giá tự động (Chuẩn bị cho Fix 6)
        startBidPolling();
    }
    private void startBidPolling() {
        lastKnownPrice = currentPrice;

        bidPollingTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            // Mỗi 5 giây, kiểm tra dữ liệu mới nhất từ DB
            String[] data = ItemDAO.getItemFullData(currentPlateNumber);

            if (data != null) {
                // Lấy giá hiện tại từ cột số [3] trong DB
                double dbPrice = Double.parseDouble(data[3]);

                if (dbPrice > lastKnownPrice) {
                    // ✅ Có người khác đã đặt giá cao hơn!
                    lastKnownPrice = dbPrice;
                    currentPrice = dbPrice;
                    updatePricingUI(); // Cập nhật nút bấm và nhãn giá

                    // Thêm thông báo vào lịch sử đấu giá
                    addHistoryRow(formatter.format(dbPrice), "Người dùng khác");
                }

                // Cập nhật số người tham gia (cột số [10])
                int count = Integer.parseInt(data[10]);
                if (count != currentParticipants) {
                    currentParticipants = count;
                    if (lblParticipants != null) {
                        lblParticipants.setText("👥 " + currentParticipants + " người tham gia");
                    }
                }
            }
        }));

        bidPollingTimeline.setCycleCount(Timeline.INDEFINITE);
        bidPollingTimeline.play();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        // ✅ Dừng tất cả đồng hồ và luồng chạy ngầm khi thoát
        if (countdownTimeline != null) countdownTimeline.stop();
        if (bidPollingTimeline != null) bidPollingTimeline.stop();

        try {
            Parent newRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/Dashboard.fxml")));
            Scene currentScene = ((Node) event.getSource()).getScene();
            currentScene.setRoot(newRoot);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Lỗi khi tải trang Dashboard", e);
        }
    }
}