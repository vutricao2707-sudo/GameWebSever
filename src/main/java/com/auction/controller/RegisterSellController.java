package com.auction.controller;

import com.auction.model.DataStore;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterSellController {

    // --- PHẦN MỚI THÊM: Khai báo 4 ô thông tin cá nhân ---
    @FXML private TextField txtFullName;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;
    @FXML private TextField txtAddress;
    // ------------------------------------------------------

    @FXML private ComboBox<String> cbVehicleType;
    @FXML private TextField txtPlateNumber;
    @FXML private TextField txtProvince;
    @FXML private TextField txtStartingPrice;

    // Hàm này tự chạy khi trang vừa mở lên để nạp dữ liệu cho nút thả xuống (ComboBox)
    @FXML
    public void initialize() {
        cbVehicleType.getItems().addAll("Ô tô", "Xe máy");
    }

    // Nút XÁC NHẬN ĐĂNG
    @FXML
    private void handleSubmit(ActionEvent event) {
        // --- PHẦN MỚI THÊM: Lấy dữ liệu cá nhân ---
        String fullName = txtFullName.getText();
        String phone = txtPhone.getText();
        String email = txtEmail.getText();
        String address = txtAddress.getText();
        // ------------------------------------------

        String type = cbVehicleType.getValue();
        String plate = txtPlateNumber.getText();
        String province = txtProvince.getText();
        String price = txtStartingPrice.getText();

        // ĐÃ SỬA: Cập nhật điều kiện if để check thêm 4 ô mới không được bỏ trống
        if (fullName.isEmpty() || phone.isEmpty() || email.isEmpty() || address.isEmpty() ||
                type == null || plate.isEmpty() || province.isEmpty() || price.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập đầy đủ các ô có dấu *");
            return;
        }

        // --- DÒNG MỚI THÊM: Lưu vào Kho dữ liệu tạm ---
        // ĐÃ SỬA: Lưu 8 thông tin thay vì 4. (Bên Dashboard vẫn lấy vị trí 0, 1, 2, 3 nên không ảnh hưởng gì cả)
        DataStore.products.add(new String[]{type, plate, province, price, fullName, phone, email, address});

        System.out.println("DEBUG 1: Đã lưu vào DataStore. Tổng số xe hiện có: " + DataStore.products.size());
        showAlert(Alert.AlertType.INFORMATION, "Thành công!", "Đã đăng ký bán biển số: " + plate);

        handleCancel(event); // Quay về trang chủ
    }

    // Nút HỦY / QUAY LẠI
    @FXML
    private void handleCancel(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Hàm tiện ích để gọi hộp thoại thông báo
    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}