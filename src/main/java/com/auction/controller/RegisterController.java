package com.auction.controller;

import com.auction.database.UserDAO;
import com.auction.util.NavigationUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import com.auction.util.UIUtils;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

    @FXML private TextField     txtUsername;
    @FXML private TextField     txtFullName;
    @FXML private TextField     txtPhone;
    @FXML private TextField     txtEmail;
    @FXML private PasswordField txtPass;
    @FXML private PasswordField txtConfirmPass;
    @FXML private CheckBox      cbCommit;
    @FXML private Button        btnRegister;

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
        String phone    = txtPhone.getText().trim();
        String email    = txtEmail.getText().trim();
        String password = txtPass.getText().trim();
        String confirm  = txtConfirmPass.getText().trim();

        if (username.isEmpty() || fullName.isEmpty() || phone.isEmpty() ||
                email.isEmpty() || password.isEmpty()) {
            UIUtils.showInfo("Thông báo", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if (UserDAO.isReservedUsername(username)) {
            UIUtils.showError("Tên đăng nhập không hợp lệ", "Tên đăng nhập \"" + username + "\" không được phép sử dụng.");
            txtUsername.requestFocus();
            return;
        }

        if (UserDAO.usernameExists(username)) {
            UIUtils.showError("Tên đăng nhập đã tồn tại", "Tên đăng nhập \"" + username + "\" đã được đăng ký trước đó.");
            txtUsername.clear();
            txtUsername.requestFocus();
            return;
        }

        if (!password.equals(confirm)) {
            UIUtils.showError("Lỗi mật khẩu", "Mật khẩu xác nhận không khớp!");
            return;
        }

        if (password.length() < 6) {
            UIUtils.showError("Cảnh báo", "Mật khẩu phải có ít nhất 6 ký tự!");
            return;
        }

        if (!email.contains("@")) {
            UIUtils.showError("Cảnh báo", "Email không hợp lệ!");
            return;
        }

        try {
            UserDAO.addUser(username, password, email, fullName, "Seller", phone, "");
            UIUtils.showSuccess("Thành công", "Tài khoản " + username + " đã được tạo thành công!");

            // Dùng NavigationUtil để chuyển trang, nếu lỗi hiện lỗi
            if (!NavigationUtil.navigateTo(event, "/fxml/Login.fxml")) {
                UIUtils.showError("Lỗi", "Không thể quay lại màn hình đăng nhập.");
            }
        } catch (Exception e) {
            UIUtils.showError("Lỗi", "Không thể tạo tài khoản: " + e.getMessage());
        }
    }
    @FXML
    private void handleBackToLogin(ActionEvent event) {
        NavigationUtil.navigateTo(event, "/fxml/Login.fxml");
    }
}