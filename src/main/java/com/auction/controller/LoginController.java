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
import com.auction.util.UIUtils;
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
            UIUtils.showInfo("Thông báo", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        // Kiểm tra thông tin đăng nhập từ database
        if (UserDAO.checkLogin(username, password)) {
            try {
                // Lấy thông tin user từ database
                User loggedInUser = UserDAO.getUserByUsernameAndPassword(username, password);

                if (loggedInUser != null) {
                    // Lưu thông tin người dùng vừa đăng nhập vào Session
                    SessionManager.getInstance().setCurrentUser(loggedInUser);

                    String currentRole = UserDAO.getRoleByUsername(username);

                    if ("ADMIN".equalsIgnoreCase(currentRole)) {
                        // TẢI TRANG ADMIN VÀ TRUYỀN DỮ LIỆU
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminDashboard.fxml"));
                        Parent root = loader.load();

                        AdminController adminController = loader.getController();
                        adminController.setAdminInfo(loggedInUser);

                        switchScene(event, root);
                    } else {
                        // TẢI TRANG USER VÀ TRUYỀN DỮ LIỆU
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Dashboard.fxml"));
                        Parent root = loader.load();

                        DashboardController userController = loader.getController();
                        userController.setUserInfo(loggedInUser);

                        switchScene(event, root);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                UIUtils.showError("Lỗi hệ thống", "Không thể tải giao diện!");
            }
        } else {
            UIUtils.showError("Lỗi đăng nhập", "Tài khoản hoặc mật khẩu không chính xác!");
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

}