package com.auction.controller;

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
import java.util.Objects;

public class AuctionHistoryController {
    private static final Logger logger = Logger.getLogger(AuctionHistoryController.class.getName());
    // Nơi chứa các hàng lịch sử
    @FXML private VBox historyListContainer;

    @FXML
    public void initialize() {
        // Tải dữ liệu mẫu khi vừa mở trang
        loadDummyData();
    }

    // =========================================================================
    // HÀM TẠO VÀ NẠP DỮ LIỆU
    // =========================================================================

    private void loadDummyData() {
        historyListContainer.getChildren().clear();

        // Mô phỏng: Dữ liệu được nạp vào từ quá khứ đến hiện tại
        // Dùng add(0, ...) để luôn đẩy hàng mới nhất LÊN ĐẦU DANH SÁCH

        // Phiên của biển 51G diễn ra ngày 26/04 (Cũ nhất)
        historyListContainer.getChildren().add(0, createRow(4, "51G-123.45", 45000000, "26/04/2026 15:45:00", 1));

        // Phiên của biển 30K diễn ra ngày 27/04 (Mới hơn)
        historyListContainer.getChildren().add(0, createRow(1, "30K-888.88", 50000000, "27/04/2026 10:15:02", 1));

        // Cập nhật giá lần 2
        historyListContainer.getChildren().add(0, createRow(2, "30K-888.88", 80000000, "27/04/2026 10:20:45", 2));

        // Cập nhật giá lần 3 (Đây là hành động vừa mới xảy ra xong)
        historyListContainer.getChildren().add(0, createRow(3, "30K-888.88", 150000000, "27/04/2026 10:29:50", 3));
    }

    /**
     * Hàm tự động vẽ một hàng (HBox) dữ liệu mới
     */
    private HBox createRow(int stt, String plate, double bidAmount, String time, int attemptNumber) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        // Style cho từng hàng
        row.setStyle("-fx-padding: 15 15; -fx-border-color: #1e293b; -fx-border-width: 0 0 1 0;");

        // Format giá tiền
        DecimalFormat formatter = new DecimalFormat("#,###");
        String formattedPrice = formatter.format(bidAmount) + " VNĐ";

        // Cột 1: STT
        Label lblStt = new Label(String.valueOf(stt));
        lblStt.setPrefWidth(60);
        lblStt.setStyle("-fx-text-fill: white; -fx-font-size: 15px;");

        // Cột 2: Biển số
        Label lblPlate = new Label(plate);
        lblPlate.setPrefWidth(180);
        lblPlate.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        // Cột 3: Giá đặt (Màu xanh dương sáng để phân biệt với Kết Quả)
        Label lblPrice = new Label(formattedPrice);
        lblPrice.setPrefWidth(220);
        lblPrice.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 16px; -fx-font-weight: bold;");

        // Cột 4: Thời gian
        Label lblTime = new Label(time);
        lblTime.setPrefWidth(200);
        lblTime.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");

        // Cột 5: Lần đặt thứ (Tạo một "Badge" nền nổi bật nhẹ)
        Label lblAttempt = new Label("Lần " + attemptNumber);
        lblAttempt.setPrefWidth(80); // Thu hẹp badge lại cho đẹp
        lblAttempt.setAlignment(Pos.CENTER);
        lblAttempt.setStyle("-fx-background-color: #475569; -fx-text-fill: white; " +
                "-fx-background-radius: 12; -fx-padding: 4 10; -fx-font-size: 13px; -fx-font-weight: bold;");

        // Gom các cột vào hàng (Cần bọc badge bằng HBox để canh lề chuẩn 180px)
        HBox boxAttempt = new HBox(lblAttempt);
        boxAttempt.setAlignment(Pos.CENTER_LEFT);
        boxAttempt.setPrefWidth(180);

        row.getChildren().addAll(lblStt, lblPlate, lblPrice, lblTime, boxAttempt);

        // Hiệu ứng Hover
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-padding: 15 15; -fx-border-color: #1e293b; -fx-border-width: 0 0 1 0;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent; -fx-padding: 15 15; -fx-border-color: #1e293b; -fx-border-width: 0 0 1 0;"));

        return row;
    }

    // =========================================================================
    // XỬ LÝ ĐIỀU HƯỚNG CÁC TAB
    // =========================================================================

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
            // ĐÃ SỬA: Dùng Logger thay cho e.printStackTrace()
            logger.log(Level.SEVERE, "Lỗi khi tải file giao diện: " + fxmlPath, e);
            showInfo("Không thể tải trang: " + fxmlPath);
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Lỗi hệ thống");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}