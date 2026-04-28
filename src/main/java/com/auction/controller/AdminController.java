package com.auction.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.auction.model.User;

public class AdminController {
    // Khai báo nhãn hiển thị tên vừa thêm ở file FXML
    @FXML private Label lblAdminName;

    // Hàm này sẽ được gọi từ LoginController truyền sang
    public void setAdminInfo(User adminUser) {
        if (adminUser != null && adminUser.getFullName() != null) {
            // Đổi chữ VPA ADMIN thành tên thật, in hoa cho đẹp
            lblAdminName.setText(adminUser.getFullName().toUpperCase());
        }
    }
    private static final Logger logger = Logger.getLogger(AdminController.class.getName());

    @FXML private VBox adminListContainer;

    @FXML
    public void initialize() {
        // Tải thử dữ liệu giả lập (Vừa có xe chờ duyệt, vừa có xe đang đấu)
        loadPendingItems();
    }

    private void loadPendingItems() {
        adminListContainer.getChildren().clear();

        // 1. Xe chờ duyệt (Mới đăng, cần admin bấm nút Xanh)
        adminListContainer.getChildren().add(createAdminRow("30K-999.99", "Nguyễn Văn A", "1.500.000.000", "28/04/2026", "PENDING"));
        adminListContainer.getChildren().add(createAdminRow("51G-123.45", "Trần Thị B", "50.000.000", "29/04/2026", "PENDING"));

        // 2. Xe đang đấu giá trên sàn (Đã duyệt, Admin đang theo dõi)
        adminListContainer.getChildren().add(createAdminRow("29A-111.11", "Lê Văn C", "Giá chót: 850Tr", "Còn 02:15:00", "LIVE"));
    }

    /**
     * Hàm vẽ ra một hàng (HBox) trong bảng Admin
     */
    private HBox createAdminRow(String plate, String owner, String priceInfo, String timeInfo, String status) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 15 20; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;");

        Label lblPlate = new Label(plate);
        lblPlate.setPrefWidth(150);
        lblPlate.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label lblOwner = new Label(owner);
        lblOwner.setPrefWidth(180);
        lblOwner.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 14px;");

        Label lblPrice = new Label(priceInfo + " đ");
        lblPrice.setPrefWidth(180);
        lblPrice.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 15px; -fx-font-weight: bold;");

        Label lblTime = new Label(timeInfo);
        lblTime.setPrefWidth(150);
        lblTime.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");

        // CỘT THAO TÁC (Tùy thuộc vào trạng thái là Đang chờ duyệt hay Đang live)
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setPrefWidth(120);

        if (status.equals("PENDING")) {
            // Hiển thị nút Duyệt và Từ chối
            Button btnApprove = new Button("✔");
            btnApprove.setStyle("-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #10b981; -fx-cursor: hand; -fx-font-weight: bold;");
            btnApprove.setOnAction(e -> handleApprove(plate, row)); // Truyền row vào để xóa nó đi sau khi duyệt

            Button btnReject = new Button("✖");
            btnReject.setStyle("-fx-background-color: rgba(239, 68, 68, 0.2); -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-font-weight: bold;");

            actionBox.getChildren().addAll(btnApprove, btnReject);

        } else if (status.equals("LIVE")) {
            // Nếu xe đang live, hiển thị nút View Details và số người tham gia
            Label lblUsers = new Label("👤 12");
            lblUsers.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");

            Button btnView = new Button("Theo dõi");
            btnView.setStyle("-fx-background-color: transparent; -fx-border-color: #38bdf8; -fx-text-fill: #38bdf8; -fx-border-radius: 4; -fx-cursor: hand;");

            actionBox.getChildren().addAll(lblUsers, btnView);
        }

        row.getChildren().addAll(lblPlate, lblOwner, lblPrice, lblTime, actionBox);

        // Hiệu ứng Hover
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-padding: 15 20; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent; -fx-padding: 15 20; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;"));

        return row;
    }

    // =========================================================================
    // LOGIC DUYỆT XE LÊN SÀN
    // =========================================================================
    private void handleApprove(String plate, HBox rowToRemove) {
        // Logic thực tế: Gọi Service để update Database trạng thái xe thành APPROVED
        // productService.approveCar(plate);

        // Cập nhật giao diện: Xóa cái hàng này khỏi danh sách "Chờ duyệt"
        adminListContainer.getChildren().remove(rowToRemove);

        showInfo("Duyệt thành công", "Biển số " + plate + " đã được đẩy lên sàn đấu giá Dashboard!");
    }

    // =========================================================================
    // ĐIỀU HƯỚNG
    // =========================================================================
    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            Parent newRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/Login.fxml")));
            Scene currentScene = ((Node) event.getSource()).getScene();
            currentScene.setRoot(newRoot);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Lỗi khi tải màn hình Login", e);
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}