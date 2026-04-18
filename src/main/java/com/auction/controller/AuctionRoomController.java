package com.auction.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Optional;

public class AuctionRoomController {

    @FXML private Label lblNumberViewers;

    // Quay lại Trang Chủ
    @FXML
    private void handleBack(ActionEvent event) {
        switchScene(event, "/fxml/Dashboard.fxml");
    }

    // Đăng xuất từ Menu Tài Khoản
    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            MenuItem menuItem = (MenuItem) event.getSource();
            Scene currentScene = menuItem.getParentPopup().getOwnerWindow().getScene();
            Stage stage = (Stage) currentScene.getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi hệ thống", "Không thể đăng xuất!");
        }
    }

    // Placeholder: Trả giá nhanh
    @FXML
    private void handleQuickBid(ActionEvent event) {
        showAlert("Thành công", "Đã gửi yêu cầu trả giá!");
    }

    // Xử lý nút Hủy tham gia với Popup xác nhận
    @FXML
    private void handleCancelBid(ActionEvent event) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận hủy");
        confirmAlert.setHeaderText("Cảnh báo: Hủy tham gia đấu giá!");
        confirmAlert.setContentText("Bạn có chắc chắn muốn hủy tham gia biển số này không?\nTiền hồ sơ và tiền đặt trước sẽ không được hoàn lại.");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            showAlert("Đã hủy", "Bạn đã hủy tham gia đấu giá thành công.");
            handleBack(event); // Tự động quay về trang chủ
        }
    }

    private void switchScene(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi hệ thống", "Không thể tải trang!");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}