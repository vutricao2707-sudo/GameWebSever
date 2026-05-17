package com.auction.controller;

import com.auction.model.User;
import com.auction.service.ProductService;
import com.auction.util.NavigationUtil;
import com.auction.util.SessionManager;
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
        
        // Auto-fill thông tin cá nhân từ Session
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            txtFullName.setText(currentUser.getFullName());
            txtPhone.setText(currentUser.getPhone());
            txtEmail.setText(currentUser.getEmail());
            txtAddress.setText(currentUser.getAddress());
            
            // Vô hiệu hóa các trường không cho chỉnh sửa (tùy chọn)
            txtFullName.setEditable(true);
            txtPhone.setEditable(true);
            txtEmail.setEditable(true);
            txtAddress.setEditable(true);
        }
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
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập đầy đủ các ô có dấu *");
            return;
        }

        // ✅ THÊM MỚI: Validate định dạng biển số
        if (!PlateValidator.isValidPlate(plate)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi biển số", 
                    "Định dạng biển số không hợp lệ!\n" +
                    "Ví dụ hợp lệ: 29A-12345, 30B.54321, etc.");
            return;
        }

        // 2. Validate giá tiền (Chặn nhập chữ và số âm)
        double validPrice = 0;
        try {
            validPrice = Double.parseDouble(price);
            if (validPrice <= 0) {
                showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Giá khởi điểm phải lớn hơn 0!");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Giá tiền chỉ được phép nhập số (ví dụ: 50000000)!");
            return;
        }

        // 3. Gọi Service để thêm mới và kiểm tra trùng lặp
        boolean isSuccess = productService.addNewProduct(type, plate, province, price, fullName, phone, email, address);

        // Nếu isSuccess là false (tức là bị trùng biển số)
        if (!isSuccess) {
            showAlert(Alert.AlertType.ERROR, "Từ chối đăng ký", "Biển số " + plate + " đã tồn tại trên hệ thống! Vui lòng kiểm tra lại.");
            return; // Dừng lại, không cho đăng ký tiếp
        }

        // 4. Nếu qua hết các ải trên thì thông báo thành công
        showAlert(Alert.AlertType.INFORMATION, "Gửi yêu cầu thành công!",
                "Biển số " + plate + " đã được gửi đi.\nVui lòng chờ hệ thống duyệt!");

        // (Tùy chọn) Xóa trắng các ô nhập liệu sau khi đăng ký thành công
        // txtPlateNumber.clear();
        // txtStartingPrice.clear();
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