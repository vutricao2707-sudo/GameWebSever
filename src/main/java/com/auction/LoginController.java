package com.auction;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;

public class LoginController {

    // Các biến này phải trùng tên với fx:id bạn đặt trong Scene Builder
    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;
    @FXML
    public void handleLogin() {
        String user = txtUsername.getText();
        String pass = txtPassword.getText();
        if (user.equals("admin") && pass.equals("123")) {
            showAlert("Thông báo", "Đăng nhập thành công! Chào " + user);
        } else {
            showAlert("Lỗi", "Sai tài khoản hoặc mật khẩu rồi Cao ơi!");
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
