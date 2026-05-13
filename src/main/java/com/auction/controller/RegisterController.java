package com.auction.controller;

import com.auction.database.UserDAO;
import com.auction.util.NavigationUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

    @FXML private TextField txtUsername;
    @FXML private TextField txtFullName;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPass;
    @FXML private PasswordField txtConfirmPass;
    @FXML private CheckBox cbCommit;
    @FXML private Button btnRegister;

    private static final String STYLE_BTN_ACTIVE   = "-fx-background-color: #0984e3; -fx-background-radius: 30; -fx-cursor: hand;";
    private static final String STYLE_BTN_INACTIVE = "-fx-background-color: #444;    -fx-background-radius: 30;";

    @FXML
    public void initialize() {
        cbCommit.selectedProperty().addListener((obs, oldVal, isChecked) -> {
            btnRegister.setDisable(!isChecked);
            btnRegister.setStyle(isChecked ? STYLE_BTN_ACTIVE : STYLE_BTN_INACTIVE);
        });
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String fullName = txtFullName.getText().trim();
        String phone = txtPhone.getText().trim();
        String email = txtEmail.getText().trim();
        String password = txtPass.getText().trim();
        String confirm  = txtConfirmPass.getText().trim();

        // Kiểm tra tất cả trường bắt buộc
        if (username.isEmpty() || fullName.isEmpty() || phone.isEmpty() ||
                email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        // Kiểm tra mật khẩu khớp
        if (!password.equals(confirm)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi mật khẩu", "Mật khẩu xác nhận không khớp!");
            return;
        }

        // Kiểm tra độ dài mật khẩu
        if (password.length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Mật khẩu phải có ít nhất 6 ký tự!");
            return;
        }

        // Kiểm tra email hợp lệ
        if (!email.contains("@")) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Email không hợp lệ!");
            return;
        }

        // Lưu tài khoản vào database
        try {
            UserDAO.addUser(username, password, email, fullName, "Seller", phone, "");
            showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    "Tài khoản " + username + " đã được tạo thành công!\nBạn có thể đăng nhập ngay.");
            NavigationUtil.navigateTo(event, "/fxml/Login.fxml");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tạo tài khoản: " + e.getMessage());
        }
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        NavigationUtil.navigateTo(event, "/fxml/Login.fxml");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}