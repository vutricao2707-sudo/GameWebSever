package com.auction.controller;

import com.auction.service.ProductService;
import com.auction.util.NavigationUtil;
import com.auction.util.PlateValidator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import com.auction.util.UIUtils;
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

        // 1. Kiểm tra rỗng
        if (fullName.isEmpty() || phone.isEmpty() || email.isEmpty() || address.isEmpty()
                || type == null || plate.isEmpty() || province.isEmpty() || price.isEmpty()) {
            UIUtils.showInfo("Thiếu thông tin", "Vui lòng nhập đầy đủ các ô có dấu *");
            return;
        }

// 2. Validate biển số
        if (!PlateValidator.isValidPlate(plate)) {
            UIUtils.showError("Biển số không hợp lệ", "Biển số phải theo định dạng: 29A-12345 hoặc 29A.12345");
            return;
        }

        // 3. Validate giá tiền
        double validPrice; // Khai báo kiểu dữ liệu cho nó
        try {
            validPrice = Double.parseDouble(price);
            if (validPrice <= 0) {
                UIUtils.showError("Lỗi nhập liệu", "Giá khởi điểm phải lớn hơn 0!");
                return;
            }
        } catch (NumberFormatException e) {
            UIUtils.showError("Lỗi nhập liệu", "Giá tiền chỉ được phép nhập số!");
            return;
        }

// 4. Gọi Service để thêm mới và kiểm tra trùng lặp
        boolean isSuccess = productService.addNewProduct(type, plate, province, price, fullName, phone, email, address);

        // Nếu isSuccess là false (tức là bị trùng biển số)
        if (!isSuccess) {
            UIUtils.showError("Từ chối đăng ký", "Biển số " + plate + " đã tồn tại trên hệ thống! Vui lòng kiểm tra lại.");
            return;
        }

// 5. Thông báo thành công
        UIUtils.showSuccess("Gửi yêu cầu thành công!", "Biển số " + plate + " đã được gửi đi.\nVui lòng chờ hệ thống duyệt!");
        // (Tùy chọn) Xóa trắng các ô nhập liệu sau khi đăng ký thành công
        // txtPlateNumber.clear();
        // txtStartingPrice.clear();
    }
    @FXML
    private void handleCancel(ActionEvent event) {
        NavigationUtil.navigateTo(event, "/fxml/Dashboard.fxml");
    }

}