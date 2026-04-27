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

public class AuctionResultController {
    private static final Logger logger = Logger.getLogger(AuctionResultController.class.getName());
    // Nơi chứa các hàng kết quả
    @FXML private VBox resultListContainer;

    @FXML
    public void initialize() {
        // Gọi hàm giả lập dữ liệu ngay khi mở trang để xem giao diện
        loadDummyData();
    }

    // =========================================================================
    // HÀM TẠO VÀ NẠP DỮ LIỆU
    // =========================================================================

    private void loadDummyData() {
        resultListContainer.getChildren().clear();

        // Tạo 3 hàng giả lập để test giao diện
        resultListContainer.getChildren().add(createRow(1, "30K-888.88", 150000000, "27/04/2026 10:30", true));
        resultListContainer.getChildren().add(createRow(2, "51G-123.45", 45000000, "26/04/2026 15:45", false));
        resultListContainer.getChildren().add(createRow(3, "29A-999.99", 300000000, "25/04/2026 09:00", true));
    }

    /**
     * Hàm tự động vẽ một hàng (HBox) dữ liệu mới
     */
    private HBox createRow(int stt, String plate, double price, String time, boolean isSuccess) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        // Style cho từng hàng (Thêm gạch dưới mờ mờ)
        row.setStyle("-fx-padding: 15 15; -fx-border-color: #1e293b; -fx-border-width: 0 0 1 0;");

        // Format giá tiền
        DecimalFormat formatter = new DecimalFormat("#,###");
        String formattedPrice = formatter.format(price) + " VNĐ";

        // Cột 1: STT
        Label lblStt = new Label(String.valueOf(stt));
        lblStt.setPrefWidth(60);
        lblStt.setStyle("-fx-text-fill: white; -fx-font-size: 15px;");

        // Cột 2: Biển số
        Label lblPlate = new Label(plate);
        lblPlate.setPrefWidth(180);
        lblPlate.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        // Cột 3: Giá trúng (Màu vàng rực rỡ)
        Label lblPrice = new Label(formattedPrice);
        lblPrice.setPrefWidth(220);
        lblPrice.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 16px; -fx-font-weight: bold;");

        // Cột 4: Thời gian
        Label lblTime = new Label(time);
        lblTime.setPrefWidth(200);
        lblTime.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");

        // Cột 5: Trạng thái (Xanh lá hoặc Đỏ)
        Label lblStatus = new Label(isSuccess ? "Thành công" : "Thất bại");
        lblStatus.setPrefWidth(180);
        if (isSuccess) {
            lblStatus.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 15px;");
        } else {
            lblStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 15px;");
        }

        row.getChildren().addAll(lblStt, lblPlate, lblPrice, lblTime, lblStatus);

        // Hiệu ứng Hover (Di chuột vào hàng nào thì hàng đó sáng lên)
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-padding: 15 15; -fx-border-color: #1e293b; -fx-border-width: 0 0 1 0;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent; -fx-padding: 15 15; -fx-border-color: #1e293b; -fx-border-width: 0 0 1 0;"));

        return row;
    }

    // =========================================================================
    // XỬ LÝ ĐIỀU HƯỚNG TỪ HEADER VÀ TAB
    // =========================================================================

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
            // 2. Ghi lỗi vào sổ một cách chuyên nghiệp
            logger.log(Level.SEVERE, "Lỗi khi chuyển đổi màn hình", e);
            showInfo("Lỗi", "Không thể tải màn hình!");
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