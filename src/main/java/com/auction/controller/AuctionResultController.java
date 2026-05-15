package com.auction.controller;

import com.auction.database.ItemDAO;
import com.auction.util.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.logging.Logger;

public class AuctionResultController {
    private static final Logger logger = Logger.getLogger(AuctionResultController.class.getName());

    // Nơi chứa các hàng kết quả
    @FXML private VBox resultListContainer;

    // ✅ ĐÃ THÊM: Khai báo nhãn hiển thị tên người dùng
    @FXML private Label lblUserName;

    @FXML
    public void initialize() {
        // ✅ ĐÃ THÊM: Hiển thị tên người dùng từ Session
        updateUserInfo();

        // Tải dữ liệu từ database - kết quả đấu giá đã kết thúc
        loadAuctionResults();
    }

    // =========================================================================
    // HÀM HIỂN THỊ THÔNG TIN NGƯỜI DÙNG
    // =========================================================================
    private void updateUserInfo() {
        if (lblUserName != null && SessionManager.getInstance().getCurrentUser() != null) {
            String fullName = SessionManager.getInstance().getCurrentUser().getFullName();
            lblUserName.setText("Xin chào, " + fullName);
        }
    }

    // =========================================================================
    // HÀM TẠO VÀ NẠP DỮ LIỆU KẾT QUẢ TỪ DATABASE
    // =========================================================================

    private void loadAuctionResults() {
        if (resultListContainer == null) return;
        resultListContainer.getChildren().clear();

        // Lấy dữ liệu từ ItemDAO - những phiên đã FINISHED
        List<String[]> finishedItems = ItemDAO.getFinishedAuctions();

        if (finishedItems.isEmpty()) {
            Label emptyLabel = new Label("🏆 Chưa có kết quả đấu giá nào.");
            emptyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-padding: 30;");
            resultListContainer.getChildren().add(emptyLabel);
            return;
        }

        // Duyệt qua các items đã kết thúc
        for (int i = 0; i < finishedItems.size(); i++) {
            String[] item = finishedItems.get(i);
            // Cấu trúc: [0]id, [1]plate, [2]start_price, [3]current_price, [4]vehicle_type, [5]seller_name, [6]auction_status, [7]auction_end_time
            int stt = i + 1;
            String plate = item[1];
            double finalPrice = Double.parseDouble(item[3]);
            String endTime = item[7];
            boolean isSuccess = finalPrice > Double.parseDouble(item[2]); // Nếu final_price > start_price thì thành công

            resultListContainer.getChildren().add(createRow(stt, plate, finalPrice, endTime, isSuccess));
        }
    }

    /**
     * Hàm tự động vẽ một hàng (HBox) dữ liệu mới
     */
    private HBox createRow(int stt, String plate, double price, String time, boolean isSuccess) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        // Style cho từng hàng (Thêm gạch dưới mờ mờ)
        row.setStyle("-fx-padding: 15 15; -fx-border-color: #1e293b; -fx-border-width: 0 0 1 0;");

        // Format giá tiền
        DecimalFormat formatter = new DecimalFormat("#,###");
        String formattedPrice = formatter.format(price) + " VNĐ";

        // Cột 1: STT
        Label lblStt = new Label(String.valueOf(stt));
        lblStt.setPrefWidth(60);
        lblStt.setStyle("-fx-text-fill: white; -fx-font-size: 15px;");

        // Cột 2: Biển số
        Label lblPlate = new Label(plate);
        lblPlate.setPrefWidth(180);
        lblPlate.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        // Cột 3: Giá trúng (Màu vàng rực rỡ)
        Label lblPrice = new Label(formattedPrice);
        lblPrice.setPrefWidth(220);
        lblPrice.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 16px; -fx-font-weight: bold;");

        // Cột 4: Thời gian
        Label lblTime = new Label(time != null ? time.substring(0, Math.min(16, time.length())) : "N/A");
        lblTime.setPrefWidth(200);
        lblTime.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");

        // Cột 5: Trạng thái (Xanh lá hoặc Đỏ)
        Label lblStatus = new Label(isSuccess ? "✅ Thành công" : "❌ Thất bại");
        lblStatus.setPrefWidth(180);
        if (isSuccess) {
            lblStatus.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 15px;");
        } else {
            lblStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 15px;");
        }

        row.getChildren().addAll(lblStt, lblPlate, lblPrice, lblTime, lblStatus);

        // Hiệu ứng Hover (Di chuột vào hàng nào thì hàng đó sáng lên)
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-padding: 15 15; -fx-border-color: #1e293b; -fx-border-width: 0 0 1 0;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent; -fx-padding: 15 15; -fx-border-color: #1e293b; -fx-border-width: 0 0 1 0;"));

        return row;
    }

    // =========================================================================
    // XỬ LÝ ĐIỀU HƯỚNG TỪ HEADER VÀ TAB
    // =========================================================================

    @FXML
    private void handleHome(ActionEvent event) {
        navigateTo(event, "/fxml/Dashboard.fxml");
    }

    @FXML
    private void handleShowHistory(ActionEvent event) {
        navigateTo(event, "/fxml/AuctionHistory.fxml");
    }

    @FXML
    private void handleRegisterToSell(ActionEvent event) {
        navigateTo(event, "/fxml/RegisterSell.fxml");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        SessionManager.getInstance().logout(); // Đăng xuất thì xóa Session
        navigateTo(event, "/fxml/Login.fxml");
    }

    private void navigateTo(ActionEvent event, String fxmlPath) {
        try {
            Parent newRoot = FXMLLoader.load(getClass().getResource(fxmlPath));
            Scene currentScene = ((Node) event.getSource()).getScene();
            currentScene.setRoot(newRoot);
        } catch (IOException e) {
            e.printStackTrace();
            showInfo("Lỗi hệ thống", "Không thể tải màn hình: " + fxmlPath);
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