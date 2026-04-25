package com.auction.controller;

import com.auction.model.User;
import com.auction.service.AuctionManager;
import com.auction.util.NavigationUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        // THAY ĐỔI CHÍNH: Bỏ hoàn toàn đoạn "admin".equals(...) cứng ở đây.
        // Gọi AuctionManager.login() — class này đã có sẵn trong dự án từ đầu
        // nhưng chưa được dùng. Đây đúng là trách nhiệm của Service, không phải Controller.
        User loggedInUser = AuctionManager.getInstance().login(username, password);

        if (loggedInUser != null) {
            // Đăng nhập thành công → chuyển sang Dashboard
            NavigationUtil.navigateTo(event, "/fxml/Dashboard.fxml");
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng nhập", "Tài khoản hoặc mật khẩu không chính xác!");
        }
    }

    @FXML
    private void handleSwitchToRegister(ActionEvent event) {
        NavigationUtil.navigateTo(event, "/fxml/Register.fxml");
    }

    @FXML
    private void handleShowSupport(ActionEvent event) {
        NavigationUtil.navigateTo(event, "/fxml/Support.fxml");
    }

    @FXML
    private void handleShowGuide(ActionEvent event) {
        NavigationUtil.navigateTo(event, "/fxml/Guide.fxml");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}