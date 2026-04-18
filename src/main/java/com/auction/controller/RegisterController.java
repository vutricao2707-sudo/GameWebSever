package com.auction.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;

public class RegisterController {

    @FXML private TextField txtUsername;
    @FXML private TextField txtFullName;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPass;
    @FXML private PasswordField txtConfirmPass;
    @FXML private CheckBox cbCommit;
    @FXML private Button btnRegister;

    @FXML
    public void initialize() {
        // Mở khóa nút Đăng ký khi tick Cam kết
        cbCommit.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                btnRegister.setDisable(false);
                btnRegister.setStyle("-fx-background-color: #0984e3; -fx-background-radius: 30; -fx-cursor: hand;");
            } else {
                btnRegister.setDisable(true);
                btnRegister.setStyle("-fx-background-color: #444; -fx-background-radius: 30;");
            }
        });
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        String user = txtUsername.getText();
        String pass = txtPass.getText();
        String confirm = txtConfirmPass.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if (!pass.equals(confirm)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi mật khẩu", "Mật khẩu xác nhận không khớp!");
        } else {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Tài khoản " + user + " đã được tạo thành công!");

            // Tự động quay lại trang Login sau khi nhấn OK
            handleBackToLogin(event);
        }
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(loginRoot));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}