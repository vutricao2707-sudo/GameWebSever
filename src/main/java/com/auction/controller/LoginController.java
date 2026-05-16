package com.auction.controller;

import com.auction.database.UserDAO;
import com.auction.model.User;
import com.auction.util.NavigationUtil;
import com.auction.util.SessionManager; // <-- Đã thêm dòng import này
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
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        // Kiểm tra thông tin đăng nhập từ database
        if (UserDAO.checkLogin(username, password)) {
            try {
                // Lấy thông tin user từ database
                User loggedInUser = UserDAO.getUserByUsernameAndPassword(username, password);

                if (loggedInUser != null) {
                    // =========================================================
                    // Lưu thông tin người dùng vừa đăng nhập vào Session
                    SessionManager.getInstance().setCurrentUser(loggedInUser);
                    // =========================================================

                    // SỬA DÒNG NÀY: Thay vì check username, ta check quyền user_type
                    // Gọi thẳng cái hàm ta vừa tạo ở Bước 1 để lấy quyền từ DB
                    String currentRole = UserDAO.getRoleByUsername(username);

// So sánh bằng biến currentRole, không dùng loggedInUser nữa
                    if ("ADMIN".equalsIgnoreCase(currentRole)) {
                        // 1. TẢI TRANG ADMIN VÀ TRUYỀN DỮ LIỆU
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminDashboard.fxml"));
                        Parent root = loader.load();

                        AdminController adminController = loader.getController();
                        adminController.setAdminInfo(loggedInUser);

                        switchScene(event, root);
                    } else {
                        // 2. TẢI TRANG USER VÀ TRUYỀN DỮ LIỆU
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Dashboard.fxml"));
                        Parent root = loader.load();

                        DashboardController userController = loader.getController();
                        userController.setUserInfo(loggedInUser);

                        switchScene(event, root);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Không thể tải giao diện!");
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi đăng nhập", "Tài khoản hoặc mật khẩu không chính xác!");
        }
    }

    // Hàm tiện ích để chuyển cảnh
    private void switchScene(ActionEvent event, Parent root) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
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