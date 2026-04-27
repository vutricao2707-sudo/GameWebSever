package com.auction.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

    // --- Các thành phần FXML ---
    @FXML private Label lblMinutes, lblSeconds;
    @FXML private Label lblPlateTop, lblPlateBottom;
    @FXML private VBox liveHistoryContainer;
    @FXML private Label lblCurrentPrice, lblStepMultiplier;
    @FXML private Button btnPlaceBid;

    // --- Biến lưu trữ Logic Đấu Giá ---
    private double currentPrice = 14120000000.0; // VD: 14.12 tỷ
    private final double stepValue = 5000000.0;  // Bước giá cố định 5 triệu
    private int stepMultiplier = 1;              // Hệ số nhân (1, 2, 3...)

    // --- Biến Đồng hồ ---
    private int timeRemainingSeconds = 5 * 60;   // Đếm ngược 5 phút
    private Timeline countdownTimeline;
    private final DecimalFormat formatter = new DecimalFormat("#,###");

    @FXML
    public void initialize() {
        startCountdown();
        updatePricingUI();

        // Load vài dòng lịch sử mẫu cho giống ảnh
        addHistoryRow("14.120.000.000", "VPA-I0ZH...");
        addHistoryRow("14.115.000.000", "VPA-625U...");
        addHistoryRow("14.105.000.000", "VPA-GFUC...");
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
                // ĐÃ SỬA: Làm cho nút bị khóa trông chìm hơn
                btnPlaceBid.setText("PHIÊN ĐẤU GIÁ KẾT THÚC");
                btnPlaceBid.setStyle("-fx-background-color: #334155; -fx-text-fill: #94a3b8; -fx-background-radius: 12; -fx-font-size: 16px; -fx-font-weight: bold;");
                btnPlaceBid.setDisable(true); // Khóa nút bấm
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
    // LOGIC BẤM NÚT "TRẢ GIÁ"
    // =========================================================================
    @FXML
    private void handlePlaceBid() {
        // 1. Tính toán giá mới
        double newBid = currentPrice + (stepValue * stepMultiplier);
        currentPrice = newBid;

        // 2. Reset hệ số về 1
        stepMultiplier = 1;
        updatePricingUI();

        // 3. Thêm vào đầu danh sách lịch sử (LIFO)
        addHistoryRow(formatter.format(newBid), "VPA-USER (Bạn)");
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

    // =========================================================================
    // TRUYỀN DỮ LIỆU VÀ ĐIỀU HƯỚNG
    // =========================================================================

    // Hàm này sẽ được gọi từ Dashboard truyền sang
    public void setAuctionRoomData(String plate, double startingPrice) {
        String[] parts = plate.split("-");
        lblPlateTop.setText(parts.length > 0 ? parts[0] : plate);
        lblPlateBottom.setText(parts.length > 1 ? parts[1] : "");

        this.currentPrice = startingPrice;
        updatePricingUI();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        // Tắt đồng hồ để tiết kiệm RAM khi thoát trang
        if (countdownTimeline != null) countdownTimeline.stop();

        try {
            Parent newRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/Dashboard.fxml")));
            Scene currentScene = ((Node) event.getSource()).getScene();
            currentScene.setRoot(newRoot);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Lỗi khi tải trang Dashboard", e);
        }
    }
}