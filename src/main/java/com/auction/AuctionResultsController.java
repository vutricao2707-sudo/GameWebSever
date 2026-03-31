package com.auction;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import java.io.IOException;

public class AuctionResultsController {

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