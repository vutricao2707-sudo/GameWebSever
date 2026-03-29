package com.auction;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    @FXML
    private void handleLogin(ActionEvent event) {
        String user = txtUsername.getText();
        String pass = txtPassword.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            showAlert("Thông báo", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if (user.equals("admin") && pass.equals("123")) {
            showAlert("Thành công", "Đăng nhập thành công!");
        } else {
            showAlert("Lỗi", "Tài khoản hoặc mật khẩu không chính xác!");
        }
    }

    @FXML
    private void handleSwitchToRegister(ActionEvent event) throws IOException {
        switchScene(event, "/fxml/Register.fxml");
    }

    @FXML
    private void handleShowSupport(ActionEvent event) throws IOException {
        switchScene(event, "/fxml/Support.fxml");
    }

    @FXML
    private void handleShowGuide(ActionEvent event) throws IOException {
        switchScene(event, "/fxml/Guide.fxml");
    }

    // Hàm dùng chung để chuyển trang - cực kỳ quan trọng
    private void switchScene(ActionEvent event, String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}