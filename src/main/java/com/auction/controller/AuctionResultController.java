package com.auction.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Objects;

public class AuctionResultController {

    @FXML private Label lblIcon;
    @FXML private Label lblStatus;
    @FXML private Label lblPlate;
    @FXML private Label lblFinalPrice;

    // Nạp dữ liệu vào từng hàng ngang
    public void setAuctionData(String plate, double finalBid, boolean isSuccess) {
        lblPlate.setText(plate);

        DecimalFormat formatter = new DecimalFormat("#,###");
        lblFinalPrice.setText(formatter.format(finalBid) + " VNĐ");

        if (isSuccess) {
            lblIcon.setText("✔");
            lblIcon.setStyle("-fx-background-color: rgba(16, 185, 129, 0.15); -fx-text-fill: #10b981; -fx-background-radius: 60; -fx-padding: 20 30; -fx-font-size: 50px; -fx-font-weight: bold;");

            lblStatus.setText("ĐẤU GIÁ THÀNH CÔNG");
            lblStatus.setStyle("-fx-text-fill: #10b981;");
        } else {
            lblIcon.setText("✖");
            lblIcon.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); -fx-text-fill: #ef4444; -fx-background-radius: 60; -fx-padding: 20 30; -fx-font-size: 50px; -fx-font-weight: bold;");

            lblStatus.setText("ĐẤU GIÁ THẤT BẠI");
            lblStatus.setStyle("-fx-text-fill: #ef4444;");
        }
    }

    // =========================================================================
    // XỬ LÝ ĐIỀU HƯỚNG TỪ HEADER VÀ TAB (Không cần nút quay lại bên dưới nữa)
    // =========================================================================

    @FXML
    private void handleHome(ActionEvent event) {
        switchScene(event, "/fxml/Dashboard.fxml");
    }

    @FXML
    private void handleShowHistory() {
        showInfo("Lịch Sử Đấu Giá", "Tính năng đang được phát triển.");
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
            e.printStackTrace();
            showInfo("Lỗi hệ thống", "Không thể tải trang: " + fxmlPath);
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