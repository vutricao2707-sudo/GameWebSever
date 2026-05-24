package com.auction.controller;

import com.auction.database.BidDAO;
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
import com.auction.util.UIUtils;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionHistoryController {

    private static final Logger logger = Logger.getLogger(AuctionHistoryController.class.getName());

    // Nơi chứa các hàng lịch sử
    @FXML private VBox historyListContainer;

    // ✅ ĐÃ THÊM: Khai báo nhãn hiển thị tên người dùng
    @FXML private Label lblUserName;

    @FXML
    public void initialize() {
        // ✅ ĐÃ THÊM: Hiển thị tên người dùng từ Session
        updateUserInfo();

        // Tải dữ liệu từ database - lịch sử đặt giá
        loadAuctionHistory();
    }

    // =========================================================================
    // HÀM HIỂN THỊ THÔNG TIN NGƯỜI DÙNG
    // =========================================================================
    private void updateUserInfo() {
        if (lblUserName != null && SessionManager.getCurrentUser() != null) {
            String fullName = SessionManager.getCurrentUser().getFullName();
            lblUserName.setText("Xin chào, " + fullName);
        }
    }

    // =========================================================================
    // HÀM TẠO VÀ NẠP DỮ LIỆU LỊCH SỬ TỪ DATABASE
    // =========================================================================

    private void loadAuctionHistory() {
        if (historyListContainer == null) return;
        historyListContainer.getChildren().clear();

        // 1. Kiểm tra xem có người dùng nào đang đăng nhập không
        if (SessionManager.getCurrentUser() == null) {
            Label emptyLabel = new Label("🔒 Vui lòng đăng nhập để xem lịch sử của bạn.");
            emptyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-padding: 30;");
            historyListContainer.getChildren().add(emptyLabel);
            return;
        }

        // 2. Lấy ID của tài khoản đang đăng nhập
        int userId = SessionManager.getCurrentUser().getId();

        // 3. Lấy dữ liệu cá nhân từ BidDAO (Chỉ lấy của user này)
        List<String[]> bids = BidDAO.getBidsByUserId(userId);

        if (bids.isEmpty()) {
            Label emptyLabel = new Label("📝 Bạn chưa có lịch sử đặt giá nào.");
            emptyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-padding: 30;");
            historyListContainer.getChildren().add(emptyLabel);
            return;
        }

        // ✅ BỔ SUNG: Tính tổng số lần đặt để hiển thị "Lần X" cho đúng
        int totalBids = bids.size();

        // 4. Duyệt qua các bids và thêm vào container
        for (int i = 0; i < totalBids; i++) {
            String[] bid = bids.get(i);
            // Cấu trúc từ DAO: [0]stt, [1]plate, [2]amount, [3]bid_time
            int stt = i + 1;
            String plate = bid[1];
            String amountStr = bid[2];
            String bidTime = bid[3];

            try {
                double amount = Double.parseDouble(amountStr);

                // ✅ BỔ SUNG LOGIC:
                // Vì danh sách lấy ra là DESC (mới nhất ở index 0),
                // nên dòng đầu tiên (i=0) thực tế là lần đặt giá cuối cùng của người dùng.
                int actualAttempt = totalBids - i;

                historyListContainer.getChildren().add(createRow(stt, plate, amount, bidTime, actualAttempt));
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "Lỗi định dạng giá tiền tại hàng " + i);
            }
        }
    }

    private HBox createRow(int stt, String plate, double bidAmount, String time, int attemptNumber) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 15 15; -fx-border-color: #1e293b; -fx-border-width: 0 0 1 0;");

        DecimalFormat formatter = new DecimalFormat("#,###");
        String formattedPrice = formatter.format(bidAmount) + " VNĐ";

        Label lblStt = new Label(String.valueOf(stt));
        lblStt.setPrefWidth(60);
        lblStt.setStyle("-fx-text-fill: white; -fx-font-size: 15px;");

        Label lblPlate = new Label(plate);
        lblPlate.setPrefWidth(180);
        lblPlate.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label lblPrice = new Label(formattedPrice);
        lblPrice.setPrefWidth(220);
        lblPrice.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 16px; -fx-font-weight: bold;");

        // ✅ BỔ SUNG: Cắt bớt phần mili giây dư thừa để thời gian trông chuyên nghiệp hơn
        String displayTime = time.length() > 19 ? time.substring(0, 19) : time;
        Label lblTime = new Label(displayTime);
        lblTime.setPrefWidth(200);
        lblTime.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");

        Label lblAttempt = new Label("Lần " + attemptNumber);
        lblAttempt.setPrefWidth(80);
        lblAttempt.setAlignment(Pos.CENTER);
        lblAttempt.setStyle("-fx-background-color: #334155; -fx-text-fill: #38bdf8; " +
                "-fx-background-radius: 12; -fx-padding: 4 10; -fx-font-size: 12px; -fx-font-weight: bold;");

        HBox boxAttempt = new HBox(lblAttempt);
        boxAttempt.setAlignment(Pos.CENTER_LEFT);
        boxAttempt.setPrefWidth(180);

        row.getChildren().addAll(lblStt, lblPlate, lblPrice, lblTime, boxAttempt);

        // Hiệu ứng Hover - Sáng nhẹ khi di chuột qua
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: rgba(56, 189, 248, 0.1); -fx-padding: 15 15; -fx-border-color: #1e293b; -fx-border-width: 0 0 1 0;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent; -fx-padding: 15 15; -fx-border-color: #1e293b; -fx-border-width: 0 0 1 0;"));

        return row;
    }

    // =========================================================================
    // XỬ LÝ ĐIỀU HƯỚNG CÁC TAB
    // =========================================================================

    @FXML
    private void handleHome(ActionEvent event) {
        navigateTo(event, "/fxml/Dashboard.fxml");
    }

    @FXML
    private void handleShowResults(ActionEvent event) {
        navigateTo(event, "/fxml/AuctionResult.fxml");
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
            var resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                // Thay bằng Popup lỗi Dark Mode
                UIUtils.showError("Lỗi tải trang", "Không thể tìm trang: " + fxmlPath);
                return;
            }
            Parent newRoot = FXMLLoader.load(resource);
            Scene currentScene = ((Node) event.getSource()).getScene();
            currentScene.setRoot(newRoot);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Lỗi khi chuyển trang", e);
            // Thay bằng Popup lỗi Dark Mode
            UIUtils.showError("Lỗi hệ thống", "Không thể tải trang: " + fxmlPath);
        }
    }
}