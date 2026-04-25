package com.auction.controller;

import com.auction.service.ProductService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class DashboardController {

    @FXML private Button btnHome, btnHistory, btnResults, btnCar, btnBike;
    @FXML private FlowPane productContainer;

    // =========================================================================
    // THAY ĐỔI CHÍNH: Khai báo Service — Controller chỉ giao tiếp với Service,
    // không bao giờ nhìn thấy DataStore nữa.
    // =========================================================================
    private final ProductService productService = new ProductService();
    private ActionEvent event;

    @FXML
    public void initialize() {
        loadProducts("Ô tô");
        setActiveTab(btnHome);
    }

    // =========================================================================
    // XỬ LÝ SỰ KIỆN ĐIỀU HƯỚNG (Navigation Handlers)
    // =========================================================================

    @FXML
    private void handleHome(ActionEvent event) {
        this.event = event;
        setActiveTab(btnHome);
        loadProducts("Ô tô");
    }

    @FXML
    private void handleShowHistory(ActionEvent event) {
        setActiveTab(btnHistory);
        showInfo("Lịch Sử Đấu Giá", "Tính năng đang được phát triển.");
    }

    @FXML
    private void handleShowResults(ActionEvent event) {
        setActiveTab(btnResults);
        showInfo("Kết Quả Đấu Giá", "Tính năng đang được phát triển.");
    }

    @FXML
    private void handleRegisterToSell(ActionEvent event) {
        // THAY ĐỔI: Gọi hàm navigateTo() dùng chung thay vì copy-paste try-catch
        navigateTo(event, "/fxml/RegisterSell.fxml");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        // THAY ĐỔI: Gọi hàm navigateTo() dùng chung thay vì copy-paste try-catch
        navigateTo(event, "/fxml/Login.fxml");
    }

    // =========================================================================
    // XỬ LÝ SỰ KIỆN BỘ LỌC (Filter Handlers)
    // =========================================================================

    @FXML
    private void handleToggleCar(ActionEvent event) {
        // THAY ĐỔI: Gọi hàm setActiveFilterTab() dùng chung, xóa 4 dòng style lặp lại
        setActiveFilterTab(btnCar, btnBike);
        loadProducts("Ô tô");
    }

    @FXML
    private void handleToggleBike(ActionEvent event) {
        // THAY ĐỔI: Gọi hàm setActiveFilterTab() dùng chung, xóa 4 dòng style lặp lại
        setActiveFilterTab(btnBike, btnCar);
        loadProducts("Xe máy");
    }

    // =========================================================================
    // LOGIC TẢI SẢN PHẨM — Giờ chỉ hỏi Service và render kết quả
    // =========================================================================

    private void loadProducts(String type) {
        if (productContainer == null) return;
        productContainer.getChildren().clear();

        // THAY ĐỔI CHÍNH: Không còn for-loop lọc DataStore ở đây nữa.
        // Controller chỉ hỏi Service: "Cho tôi danh sách xe loại X" và nhận về kết quả.
        List<String[]> products = productService.getProductsByType(type);

        if (products.isEmpty()) {
            showEmptyMessage("Chưa có " + type.toUpperCase() + " nào được đăng bán.");
            return;
        }

        for (String[] p : products) {
            productContainer.getChildren().add(createProductCard(p));
        }
    }

    private void showEmptyMessage(String msg) {
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 18px;");
        productContainer.getChildren().add(lbl);
    }

    // =========================================================================
    // XÂY DỰNG THẺ SẢN PHẨM (Product Card Builder)
    // Giữ nguyên bố cục UI, chỉ dọn sạch cấu trúc bên trong.
    // =========================================================================

    private VBox createProductCard(String[] p) {
        String plate    = p.length > 1 ? p[1] : "";
        String province = p.length > 2 ? p[2] : "";
        String price    = p.length > 3 ? p[3] : "Đang cập nhật";
        String fullName = p.length > 4 ? p[4] : "Chưa cập nhật";
        String phone    = p.length > 5 ? p[5] : "Chưa cập nhật";
        String email    = p.length > 6 ? p[6] : "Chưa cập nhật";
        String address  = p.length > 7 ? p[7] : "Chưa cập nhật";

        // --- Khung thẻ chính ---
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: #1a2230; -fx-background-radius: 12; " +
                "-fx-border-color: #2a364a; -fx-border-radius: 12; " +
                "-fx-border-width: 1; -fx-padding: 15;");
        card.setPrefWidth(280);

        // --- Khu vực biển số ---
        VBox plateBox = new VBox();
        plateBox.setAlignment(Pos.CENTER);
        plateBox.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 15 10;");

        String[] plateParts = plate.split("-");
        String topText    = plateParts.length > 0 ? plateParts[0] : plate;
        String bottomText = plateParts.length > 1 ? plateParts[1] : "";

        Label lblPlateTop = new Label(topText);
        lblPlateTop.setStyle("-fx-text-fill: black; -fx-font-size: 32px; -fx-font-weight: bold;");

        Label lblPlateBottom = new Label(bottomText);
        lblPlateBottom.setStyle("-fx-text-fill: black; -fx-font-size: 40px; -fx-font-weight: bold;");

        plateBox.getChildren().addAll(lblPlateTop, lblPlateBottom);

        StackPane plateContainer = new StackPane(plateBox);
        Label carBadge = new Label(p[0] != null && p[0].contains("máy") ? "🏍️" : "🚗");
        carBadge.setStyle("-fx-background-color: #00b894; -fx-text-fill: white; " +
                "-fx-background-radius: 20; -fx-padding: 8; -fx-font-size: 14px;");
        StackPane.setAlignment(carBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(carBadge, new Insets(-10, -10, 0, 0));
        plateContainer.getChildren().add(carBadge);

        // --- Khu vực tỉnh thành + phiên đấu giá ---
        HBox locationBox = new HBox();
        locationBox.setAlignment(Pos.CENTER_LEFT);

        Label lblProv = new Label("• " + province);
        lblProv.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");

        Region locSpacer = new Region();
        HBox.setHgrow(locSpacer, Priority.ALWAYS);

        Label lblSession = new Label("Phiên 9");
        lblSession.setStyle("-fx-background-color: #475569; -fx-text-fill: white; " +
                "-fx-background-radius: 12; -fx-padding: 3 10; -fx-font-size: 12px;");

        locationBox.getChildren().addAll(lblProv, locSpacer, lblSession);

        // --- Khu vực số người tham gia ---
        HBox usersBox = new HBox();
        usersBox.setAlignment(Pos.CENTER_LEFT);

        Label lblUsers = new Label("👤 9");
        lblUsers.setStyle("-fx-text-fill: #00b894; -fx-font-size: 14px;");

        Region userSpacer = new Region();
        HBox.setHgrow(userSpacer, Priority.ALWAYS);

        Label lblChart = new Label("📈");
        lblChart.setStyle("-fx-text-fill: #00b894; -fx-font-size: 14px;");

        usersBox.getChildren().addAll(lblUsers, userSpacer, lblChart);

        // --- Khu vực đồng hồ đếm ngược ---
        HBox timeBox = new HBox(10);
        timeBox.setAlignment(Pos.CENTER_LEFT);

        Label clockIcon = new Label("🕒");
        clockIcon.setStyle("-fx-text-fill: white; -fx-font-size: 20px;");

        VBox timeTexts = new VBox();
        Label lblTimeLabel = new Label("Thời gian đăng ký còn lại");
        lblTimeLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        Label lblTimeValue = new Label("2 ngày 4 giờ 59 phút");
        lblTimeValue.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
        timeTexts.getChildren().addAll(lblTimeLabel, lblTimeValue);

        timeBox.getChildren().addAll(clockIcon, timeTexts);

        // --- Khu vực 2 nút bấm ---
        HBox buttonBox = new HBox(10);

        Button btnBid = new Button("Đăng ký đấu giá");
        btnBid.setStyle("-fx-background-color: #00b894; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 20; " +
                "-fx-cursor: hand; -fx-padding: 8 0;");
        btnBid.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnBid, Priority.ALWAYS);

        Button btnDetail = new Button("Xem chi tiết");
        btnDetail.setStyle("-fx-background-color: transparent; -fx-border-color: #00b894; " +
                "-fx-border-radius: 20; -fx-text-fill: white; " +
                "-fx-cursor: hand; -fx-padding: 8 0;");
        btnDetail.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnDetail, Priority.ALWAYS);

        btnDetail.setOnAction(e -> showProductDetails(plate, fullName, address, phone, email, price));

        buttonBox.getChildren().addAll(btnBid, btnDetail);

        card.getChildren().addAll(plateContainer, locationBox, usersBox, timeBox, buttonBox);
        return card;
    }

    // =========================================================================
    // DIALOG CHI TIẾT SẢN PHẨM
    // THAY ĐỔI: Thêm hàm makeInfoBox() để xóa 5 đoạn code VBox lặp lại y hệt nhau.
    // =========================================================================

    private void showProductDetails(String plate, String fullName, String address,
                                    String phone, String email, String price) {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8);");

        VBox dialog = new VBox(25);
        dialog.setMaxSize(450, 520);
        dialog.setStyle("-fx-background-color: #1a2230; -fx-background-radius: 15; " +
                "-fx-border-color: #334155; -fx-border-radius: 15; " +
                "-fx-border-width: 1; -fx-padding: 35;");
        dialog.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label("THÔNG TIN BIỂN: " + plate);
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 20px;");
        VBox.setMargin(lblTitle, new Insets(0, 0, 10, 0));

        // THAY ĐỔI: Dùng makeInfoBox() thay vì viết tay 5 lần cùng 1 đoạn code
        VBox boxPrice   = makeInfoBox("GIÁ KHỞI ĐIỂM",          price + " VNĐ", "#00b894", true);
        VBox boxName    = makeInfoBox("HỌ VÀ TÊN NGƯỜI BÁN",    fullName,       "#0ea5e9", false);
        VBox boxAddress = makeInfoBox("ĐỊA CHỈ LIÊN HỆ",        address,        "#0ea5e9", false);
        VBox boxPhone   = makeInfoBox("SỐ ĐIỆN THOẠI LIÊN HỆ",  phone,          "#0ea5e9", false);
        VBox boxEmail   = makeInfoBox("EMAIL LIÊN HỆ",           email,          "#0ea5e9", false);

        Button btnClose = new Button("Quay lại");
        btnClose.setStyle("-fx-background-color: transparent; -fx-border-color: white; " +
                "-fx-border-radius: 20; -fx-text-fill: white; " +
                "-fx-cursor: hand; -fx-padding: 8 30; -fx-font-weight: bold;");
        btnClose.setOnAction(e ->
                ((StackPane) btnHome.getScene().getRoot()).getChildren().remove(overlay)
        );

        dialog.getChildren().addAll(lblTitle, boxPrice, boxName, boxAddress, boxPhone, boxEmail, btnClose);
        overlay.getChildren().add(dialog);
        ((StackPane) btnHome.getScene().getRoot()).getChildren().add(overlay);
    }

    /**
     * Hàm tiện ích: Tạo một nhóm thông tin (tiêu đề màu + nội dung trắng) trong dialog.
     * Giúp xóa 5 đoạn VBox lặp lại y hệt nhau ở showProductDetails().
     *
     * @param titleText  Chữ tiêu đề, ví dụ: "HỌ VÀ TÊN NGƯỜI BÁN"
     * @param valueText  Nội dung hiển thị
     * @param titleColor Màu hex của tiêu đề
     * @param isBold     true nếu muốn chữ nội dung to và đậm hơn (dùng cho giá tiền)
     */
    private VBox makeInfoBox(String titleText, String valueText, String titleColor, boolean isBold) {
        VBox box = new VBox(5);

        Label lblTitle = new Label(titleText);
        lblTitle.setStyle("-fx-text-fill: " + titleColor + "; " +
                "-fx-font-weight: bold; -fx-font-size: 15px;");

        Label lblValue = new Label("• " + valueText);
        if (isBold) {
            lblValue.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        } else {
            lblValue.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        }

        box.getChildren().addAll(lblTitle, lblValue);
        return box;
    }

    // =========================================================================
    // HÀM TIỆN ÍCH DÙNG CHUNG
    // =========================================================================

    /**
     * Chuyển màn hình sang một FXML khác.
     * THAY ĐỔI: Gộp handleRegisterToSell và handleLogout thành 1 hàm dùng chung,
     * xóa 2 đoạn try-catch copy-paste y hệt nhau.
     */
    private void navigateTo(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
            showInfo("Lỗi hệ thống", "Không thể tải màn hình: " + fxmlPath);
        }
    }

    /**
     * Đặt trạng thái active cho nút lọc loại xe (Ô tô / Xe máy).
     * THAY ĐỔI: Gộp 2 hàm handleToggleCar và handleToggleBike thành 1 hàm dùng chung,
     * xóa 4 dòng setStyle và setTextFill lặp lại.
     */
    private void setActiveFilterTab(Button active, Button inactive) {
        active.setStyle("-fx-background-color: #00b894; -fx-background-radius: 32;");
        active.setTextFill(javafx.scene.paint.Color.WHITE);

        inactive.setStyle("-fx-background-color: transparent; -fx-background-radius: 32;");
        inactive.setTextFill(javafx.scene.paint.Color.web("#cccccc"));
    }

    /**
     * Đặt trạng thái active cho tab điều hướng chính (Trang Chủ / Lịch Sử / Kết Quả).
     */
    private void setActiveTab(Button activeButton) {
        Button[] tabs = {btnHome, btnHistory, btnResults};
        for (Button tab : tabs) {
            if (tab == null) continue;
            if (tab == activeButton) {
                tab.setStyle("-fx-background-color: transparent; " +
                        "-fx-border-color: #00b894; -fx-border-width: 0 0 2 0;");
                tab.setTextFill(javafx.scene.paint.Color.web("#00b894"));
            } else {
                tab.setStyle("-fx-background-color: transparent;");
                tab.setTextFill(javafx.scene.paint.Color.WHITE);
            }
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