package com.auction.controller;

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
        String password = txtPass.getText().trim();
        String confirm  = txtConfirmPass.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if (!password.equals(confirm)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi mật khẩu", "Mật khẩu xác nhận không khớp!");
            return;
        }

        showAlert(Alert.AlertType.INFORMATION, "Thành công",
                "Tài khoản " + username + " đã được tạo thành công!");
        NavigationUtil.navigateTo(event, "/fxml/Login.fxml");
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