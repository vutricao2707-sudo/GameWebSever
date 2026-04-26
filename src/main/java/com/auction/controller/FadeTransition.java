package com.auction.controller;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;

public class AuctionResultController {
    @FXML private StackPane rootPane;
    @FXML private Label lblPlateTop, lblPlateBottom, lblFinalPrice, lblStatus;

    @FXML
    public void initialize() {
        // Tạo hiệu ứng mờ dần khi vừa vào trang (Smooth transition)
        rootPane.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), rootPane);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    public void setAuctionData(String plate, String price, boolean isSuccess) {
        // Tách biển số để hiển thị 2 dòng cho đẹp
        String[] parts = plate.split("-");
        lblPlateTop.setText(parts[0]);
        lblPlateBottom.setText(parts[1]);

        lblFinalPrice.setText(price + " VNĐ");

        if (isSuccess) {
            lblStatus.setText("ĐẤU GIÁ THÀNH CÔNG");
            lblStatus.setStyle("-fx-text-fill: #00b894;");
        } else {
            lblStatus.setText("ĐẤU GIÁ THẤT BẠI");
            lblStatus.setStyle("-fx-text-fill: #ff6b6b;");
        }
    }

    @FXML
    private void handleBackHome(ActionEvent event) {
        switchScene(event, "/fxml/Dashboard.fxml");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        switchScene(event, "/fxml/Login.fxml");
    }

    private void switchScene(ActionEvent event, String fxmlPath) {
        // Hiệu ứng mờ dần trước khi thoát trang
        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), rootPane);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            try {
                Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (IOException ex) { ex.printStackTrace(); }
        });
        fadeOut.play();
    }
}