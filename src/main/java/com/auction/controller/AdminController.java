package com.auction.controller;

import com.auction.database.ItemDAO;
import com.auction.model.User;
import com.auction.service.ProductService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdminController {

    private static final Logger logger = Logger.getLogger(AdminController.class.getName());

    @FXML private Label lblAdminName;
    @FXML private Label lblTotalRequests;
    @FXML private VBox adminListContainer;

    // Admin chỉ nói chuyện với Service — không bao giờ đụng thẳng vào DataStore
    private final ProductService productService = new ProductService();

    // =========================================================================
    // KHỞI TẠO
    // =========================================================================

    /** Được gọi từ LoginController để hiển thị tên quản trị viên trên sidebar */
    public void setAdminInfo(User adminUser) {
        if (adminUser != null && adminUser.getFullName() != null) {
            lblAdminName.setText(adminUser.getFullName().toUpperCase());
        }
    }

    @FXML
    public void initialize() {
        loadPendingItems();
    }

    // =========================================================================
    // TẢI DANH SÁCH CHỜ DUYỆT — Lấy từ Service thay vì hardcode
    // =========================================================================
    private void loadPendingItems() {
        if (adminListContainer == null) return;
        adminListContainer.getChildren().clear();
        List<String[]> pendingList = productService.getPendingProducts();
        if (lblTotalRequests != null) {
            lblTotalRequests.setText("Tổng cộng: " + pendingList.size() + " yêu cầu");
        }
        if (pendingList.isEmpty()) {
            Label emptyLabel = new Label("✅ Không có xe nào đang chờ duyệt.");
            emptyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-padding: 30;");
            adminListContainer.getChildren().add(emptyLabel);
            return;
        }
        for (String[] product : pendingList) {
            // Cấu trúc mảng từ DB:
            // [0]id  [1]plate  [2]start_price  [3]vehicle_type
            // [4]seller_name  [5]phone  [6]email  [7]address  [8]approval_status

            String plate = product[1];  // Lấy Biển số
            String owner = product[4];  // Lấy Tên người bán

            // Xử lý và format giá tiền hiển thị cho đẹp
            String price;
            try {
                double priceVal = Double.parseDouble(product[2]);
                price = String.format("%,.0f", priceVal);  // Ví dụ: 50000000.0 -> "50,000,000"
            } catch (NumberFormatException e) {
                price = product[2];
            }

            // Gọi hàm tạo giao diện 1 hàng (Row) và ném nó vào màn hình
            adminListContainer.getChildren().add(
                    createAdminRow(plate, owner, price, "Đang chờ", "PENDING")
            );
        }
    }

    // =========================================================================
    // VẼ TỪNG HÀNG TRONG BẢNG
    // =========================================================================

    private HBox createAdminRow(String plate, String owner, String priceInfo,
                                String timeInfo, String status) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        applyRowStyle(row, false);

        Label lblPlate = buildLabel(plate,          150, "-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label lblOwner = buildLabel(owner,          180, "-fx-text-fill: #cbd5e1; -fx-font-size: 14px;");
        Label lblPrice = buildLabel(priceInfo + " đ", 180, "-fx-text-fill: #f59e0b; -fx-font-size: 15px; -fx-font-weight: bold;");
        Label lblTime  = buildLabel(timeInfo,       150, "-fx-text-fill: #94a3b8; -fx-font-size: 14px;");

        row.getChildren().addAll(lblPlate, lblOwner, lblPrice, lblTime, buildActionBox(plate, row, status));

        row.setOnMouseEntered(e -> applyRowStyle(row, true));
        row.setOnMouseExited(e  -> applyRowStyle(row, false));

        return row;
    }

    /** Tạo cột thao tác — nội dung thay đổi tùy trạng thái xe (PENDING / LIVE) */
    private HBox buildActionBox(String plate, HBox parentRow, String status) {
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setPrefWidth(120);

        if ("PENDING".equals(status)) {
            Button btnApprove = new Button("✔");
            btnApprove.setStyle("-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #10b981; -fx-cursor: hand; -fx-font-weight: bold;");
            btnApprove.setOnAction(e -> handleApprove(plate, parentRow));

            Button btnReject = new Button("✖");
            btnReject.setStyle("-fx-background-color: rgba(239, 68, 68, 0.2); -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-font-weight: bold;");
            btnReject.setOnAction(e -> handleReject(plate, parentRow));

            actionBox.getChildren().addAll(btnApprove, btnReject);

        } else if ("LIVE".equals(status)) {
            Label lblUsers = new Label("👤 12");
            lblUsers.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");

            Button btnView = new Button("Theo dõi");
            btnView.setStyle("-fx-background-color: transparent; -fx-border-color: #38bdf8; -fx-text-fill: #38bdf8; -fx-border-radius: 4; -fx-cursor: hand;");

            actionBox.getChildren().addAll(lblUsers, btnView);
        }

        return actionBox;
    }

    // =========================================================================
    // LOGIC DUYỆT XE — Gọi Service để cập nhật Database + Bắt đầu giai đoạn đăng ký
    // =========================================================================

    private void handleApprove(String plate, HBox rowToRemove) {
        // Bước 1: Duyệt biển số (cập nhật APPROVED)
        boolean success = productService.approveProduct(plate);
        if (!success) {
            showInfo("Thất bại", "Không tìm thấy biển số " + plate + " trong hệ thống.");
            return;
        }

        // Bước 2: Hỏi admin thời gian đăng ký bao lâu (phút)
        TextInputDialog dialog = new TextInputDialog("60");
        dialog.setTitle("Cấu hình thời gian đăng ký");
        dialog.setHeaderText("Nhập thời gian đăng ký (phút):");
        dialog.setContentText("Thời gian (phút):");

        Optional<String> result = dialog.showAndWait();
        int registrationDurationMinutes = 60; // Mặc định 60 phút

        if (result.isPresent()) {
            try {
                registrationDurationMinutes = Integer.parseInt(result.get());
                if (registrationDurationMinutes <= 0) {
                    showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng nhập một số dương!");
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng nhập một số hợp lệ!");
                return;
            }
        }

        // Bước 3: Bắt đầu giai đoạn đăng ký (cập nhật thời gian vào database)
        boolean registrationStarted = ItemDAO.startRegistrationPhase(plate, registrationDurationMinutes);
        if (registrationStarted) {
            adminListContainer.getChildren().remove(rowToRemove);
            int remaining = adminListContainer.getChildren().size();
            if (lblTotalRequests != null) {
                lblTotalRequests.setText("Tổng cộng: " + remaining + " yêu cầu");
            }
            showInfo("Duyệt thành công",
                    "Biển số " + plate + " đã được duyệt!\n" +
                    "Giai đoạn đăng ký: " + registrationDurationMinutes + " phút\n" +
                    "Giai đoạn đấu giá: 60 phút (sau khi đăng ký hết)");
        } else {
            showInfo("Lỗi", "Không thể bắt đầu giai đoạn đăng ký cho biển số " + plate);
        }
    }

    // =========================================================================
    // LOGIC TỪ CHỐI XE — Gọi Service để cập nhật Database
    // =========================================================================

    private void handleReject(String plate, HBox rowToRemove) {
        boolean success = productService.rejectProduct(plate);
        if (success) {
            adminListContainer.getChildren().remove(rowToRemove);
            // Cập nhật lại số đếm sau khi từ chối
            int remaining = adminListContainer.getChildren().size();
            if (lblTotalRequests != null) {
                lblTotalRequests.setText("Tổng cộng: " + remaining + " yêu cầu");
            }
            showInfo("Từ chối thành công", "Biển số " + plate + " đã bị từ chối!");
        } else {
            showInfo("Thất bại", "Không tìm thấy biển số " + plate + " trong hệ thống.");
        }
    }

    // =========================================================================
    // HÀM TIỆN ÍCH
    // =========================================================================

    /** Tạo Label với độ rộng cố định — giúp xóa 4 dòng lặp lại trong mỗi hàng */
    private Label buildLabel(String text, double width, String style) {
        Label label = new Label(text);
        label.setPrefWidth(width);
        label.setStyle(style);
        return label;
    }

    /** Áp dụng style hover / bình thường cho một hàng — thay vì lặp string 2 lần */
    private void applyRowStyle(HBox row, boolean isHovered) {
        String bg = isHovered
                ? "-fx-background-color: rgba(255,255,255,0.05);"
                : "-fx-background-color: transparent;";
        row.setStyle(bg + " -fx-padding: 15 20; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;");
    }

    // =========================================================================
    // ĐIỀU HƯỚNG
    // =========================================================================

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            Parent newRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/Login.fxml")));
            ((Node) event.getSource()).getScene().setRoot(newRoot);
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}