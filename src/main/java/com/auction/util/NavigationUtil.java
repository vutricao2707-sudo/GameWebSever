package com.auction.util;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * NavigationUtil: Lớp tiện ích tập trung hoá việc chuyển màn hình.
 * <p>
 * VẤN ĐỀ GỐC: Mỗi Controller đang tự viết riêng 4 dòng try-catch + FXMLLoader
 * giống hệt nhau. Khi muốn sửa (ví dụ: thêm hiệu ứng chuyển cảnh), phải sửa
 * ở 5-6 chỗ khác nhau.
 * <p>
 * GIẢI PHÁP: Gom vào 1 hàm static. Sau này muốn thêm animation hay logging,
 * chỉ cần sửa đúng 1 chỗ này.
 */
public class NavigationUtil {

    // Constructor private để không ai khởi tạo class này — nó chỉ chứa hàm static
    private NavigationUtil() {}

    /**
     * Chuyển màn hình sang một file FXML khác.
     *
     * @param event    Sự kiện từ nút bấm, dùng để lấy Stage hiện tại
     * @param fxmlPath Đường dẫn file FXML, ví dụ: "/fxml/Dashboard.fxml"
     */
    public static void navigateTo(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(NavigationUtil.class.getResource(fxmlPath)));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Không thể tải màn hình: " + fxmlPath);
        }
    }

    /**
     * Hiển thị hộp thoại lỗi.
     * Dùng nội bộ khi navigateTo() thất bại.
     */
    private static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi hệ thống");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}