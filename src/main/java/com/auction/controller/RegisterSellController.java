package com.auction.controller;

import com.auction.service.ProductService;
import com.auction.util.NavigationUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public class RegisterSellController {

    @FXML private TextField txtFullName;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;
    @FXML private TextField txtAddress;
    @FXML private ComboBox<String> cbVehicleType;
    @FXML private TextField txtPlateNumber;
    @FXML private TextField txtProvince;
    @FXML private TextField txtStartingPrice;

    // THAY ĐỔI: Dùng ProductService thay vì gọi DataStore trực tiếp
    private final ProductService productService = new ProductService();

    @FXML
    public void initialize() {
        cbVehicleType.getItems().addAll("Ô tô", "Xe máy");
    }

    @FXML
    private void handleSubmit(ActionEvent event) {
        String fullName = txtFullName.getText().trim();
        String phone    = txtPhone.getText().trim();
        String email    = txtEmail.getText().trim();
        String address  = txtAddress.getText().trim();
        String type     = cbVehicleType.getValue();
        String plate    = txtPlateNumber.getText().trim();
        String province = txtProvince.getText().trim();
        String price    = txtStartingPrice.getText().trim();

        if (fullName.isEmpty() || phone.isEmpty() || email.isEmpty() || address.isEmpty()
                || type == null || plate.isEmpty() || province.isEmpty() || price.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập đầy đủ các ô có dấu *");
            return;
        }

        // THAY ĐỔI CHÍNH: Không còn DataStore.products.add() nằm trong Controller.
        // Controller chỉ đóng gói dữ liệu từ form và giao cho Service xử lý.
        // Đây đúng trách nhiệm của Controller trong MVC.
        // Gọi hàm addNewProduct từ Service (hàm này đã tự động gán nhãn "PENDING" ở dưới đáy)
        productService.addNewProduct(type, plate, province, price, fullName, phone, email, address);

        // Đổi câu thông báo để User biết là phải chờ duyệt chứ chưa lên sàn ngay
        showAlert(Alert.AlertType.INFORMATION, "Gửi yêu cầu thành công!",
                "Biển số " + plate + " đã được gửi đi.\nVui lòng chờ hệ thống duyệt!.");
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        NavigationUtil.navigateTo(event, "/fxml/Dashboard.fxml");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}