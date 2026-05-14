package com.auction.controller;

import com.auction.model.User;
import com.auction.service.ProductService;
import com.auction.dto.ItemDTO;
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
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("ALL")
public class DashboardController {

    @FXML private Button btnHome, btnHistory, btnResults, btnCar, btnBike;
    @FXML private FlowPane productContainer;

    private static final Logger logger = Logger.getLogger(DashboardController.class.getName());
    private final ProductService productService = new ProductService();

    // =========================================================================
    // KHỞI TẠO
    // =========================================================================

    @FXML
    public void initialize() {
        loadProducts("Ô tô");
        setActiveTab(btnHome);
    }

    @FXML private Label lblUserName;
    private User currentUser; // Lưu lại để dùng cho chức năng khác

    public void setUserInfo(User user) {
        this.currentUser = user;
        if (user != null && user.getFullName() != null) {
            // Đổi chữ mặc định thành tên thật của user
            lblUserName.setText("Xin chào, người dùng");
        }
    }

    // =========================================================================
    // ĐIỀU HƯỚNG — Tất cả đều dùng navigateTo() dùng chung, không còn copy-paste
    // =========================================================================

    @FXML private void handleHome(ActionEvent event)           { setActiveTab(btnHome); loadProducts("Ô tô"); }
    @FXML private void handleShowHistory(ActionEvent event)    { navigateTo(event, "/fxml/AuctionHistory.fxml"); }
    @FXML private void handleShowResults(ActionEvent event)    { navigateTo(event, "/fxml/AuctionResult.fxml"); }
    @FXML private void handleRegisterToSell(ActionEvent event) { navigateTo(event, "/fxml/RegisterSell.fxml"); }
    @FXML private void handleLogout(ActionEvent event)         { navigateTo(event, "/fxml/Login.fxml"); }

    // =========================================================================
    // BỘ LỌC LOẠI XE
    // =========================================================================

    @FXML
    private void handleToggleCar(ActionEvent event) {
        setActiveFilterTab(btnCar, btnBike);
        loadProducts("Ô tô");
    }

    @FXML
    private void handleToggleBike(ActionEvent event) {
        setActiveFilterTab(btnBike, btnCar);
        loadProducts("Xe máy");
    }

    // =========================================================================
    // TẢI VÀ HIỂN THỊ SẢN PHẨM
    // =========================================================================

    private void loadProducts(String type) {
        if (productContainer == null) return;
        productContainer.getChildren().clear();

        List<String[]> products = productService.getApprovedProductsByType(type);
        if (products.isEmpty()) {
            showEmptyMessage("Chưa có " + type.toUpperCase() + " nào được đăng bán.");
            return;
        }
        for (String[] p : products) {
            ItemDTO item = ItemDTO.fromApprovedArray(p);
            if (item != null) {
                productContainer.getChildren().add(createProductCard(item));
            }
        }
    }

    private void showEmptyMessage(String msg) {
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 18px;");
        productContainer.getChildren().add(lbl);
    }

    // =========================================================================
    // XÂY DỰNG THẺ SẢN PHẨM — Orchestrator gọi 5 hàm build* chuyên biệt
    // =========================================================================

    // ĐÃ SỬA THAM SỐ Ở ĐÂY TỪ String[] p THÀNH ItemDTO item
    private VBox createProductCard(ItemDTO item) {
        // Giải nén mảng dữ liệu một lần ở đây để các hàm helper nhận tham số rõ ràng
        String type     = item.getVehicleType();
        String plate    = item.getPlateNumber();
        String province = item.getProvince();
        String price    = item.getFormattedPrice();
        String fullName = item.getSellerName();
        String phone    = item.getSellerPhone();
        String email    = item.getSellerEmail();
        String address  = item.getSellerAddress();

        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: #1a2230; -fx-background-radius: 12; " +
                "-fx-border-color: #2a364a; -fx-border-radius: 12; " +
                "-fx-border-width: 1; -fx-padding: 15;");
        card.setPrefWidth(280);
        card.getChildren().addAll(
                buildPlateSection(plate, type),
                buildLocationRow(province),
                buildUsersRow(),
                buildTimeRow(),
                buildCardButtons(plate, price, fullName, address, phone, email)
        );
        return card;
    }

    // =========================================================================
    // 5 HÀM BUILD* — Mỗi hàm chịu trách nhiệm DUY NHẤT 1 khu vực của card
    // =========================================================================

    /** Khu vực biển số: hộp trắng + huy hiệu loại xe góc phải trên */
    private StackPane buildPlateSection(String plate, String type) {
        VBox plateBox = new VBox();
        plateBox.setAlignment(Pos.CENTER);
        plateBox.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 15 10;");

        String[] parts = plate.split("-");
        Label lblTop    = new Label(parts.length > 0 ? parts[0] : plate);
        Label lblBottom = new Label(parts.length > 1 ? parts[1] : "");
        lblTop.setStyle("-fx-text-fill: black; -fx-font-size: 32px; -fx-font-weight: bold;");
        lblBottom.setStyle("-fx-text-fill: black; -fx-font-size: 40px; -fx-font-weight: bold;");
        plateBox.getChildren().addAll(lblTop, lblBottom);

        Label badge = new Label(type.contains("máy") ? "🏍️" : "🚗");
        badge.setStyle("-fx-background-color: #00b894; -fx-text-fill: white; " +
                "-fx-background-radius: 20; -fx-padding: 8; -fx-font-size: 14px;");
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(-10, -10, 0, 0));

        return new StackPane(plateBox, badge);
    }

    /** Hàng tỉnh thành + nhãn phiên đấu giá */
    private HBox buildLocationRow(String province) {
        Label lblProv = new Label("• " + province);
        lblProv.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblSession = new Label("Phiên 9");
        lblSession.setStyle("-fx-background-color: #475569; -fx-text-fill: white; " +
                "-fx-background-radius: 12; -fx-padding: 3 10; -fx-font-size: 12px;");

        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(lblProv, spacer, lblSession);
        return row;
    }

    /** Hàng số người tham gia + icon biểu đồ */
    private HBox buildUsersRow() {
        Label lblUsers = new Label("👤 9");
        lblUsers.setStyle("-fx-text-fill: #00b894; -fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblChart = new Label("📈");
        lblChart.setStyle("-fx-text-fill: #00b894; -fx-font-size: 14px;");

        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(lblUsers, spacer, lblChart);
        return row;
    }

    /** Hàng đồng hồ đếm ngược thời gian đăng ký */
    private HBox buildTimeRow() {
        Label clockIcon = new Label("🕒");
        clockIcon.setStyle("-fx-text-fill: white; -fx-font-size: 20px;");

        Label lblTimeLabel = new Label("Thời gian đăng ký còn lại");
        lblTimeLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        Label lblTimeValue = new Label("2 ngày 4 giờ 59 phút");
        lblTimeValue.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");

        VBox timeTexts = new VBox();
        timeTexts.getChildren().addAll(lblTimeLabel, lblTimeValue);

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(clockIcon, timeTexts);
        return row;
    }

    /** Hàng 2 nút bấm: Đăng ký đấu giá + Xem chi tiết */
    private HBox buildCardButtons(String plate, String price, String fullName,
                                  String address, String phone, String email) {
        Button btnBid = new Button("Đăng ký đấu giá");
        btnBid.setStyle("-fx-background-color: #00b894; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 8 0;");
        btnBid.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnBid, Priority.ALWAYS);
        btnBid.setOnAction(e -> enterAuctionRoom(e, plate, price));

        Button btnDetail = new Button("Xem chi tiết");
        btnDetail.setStyle("-fx-background-color: transparent; -fx-border-color: #00b894; " +
                "-fx-border-radius: 20; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 0;");
        btnDetail.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnDetail, Priority.ALWAYS);
        btnDetail.setOnAction(e -> showProductDetails(plate, fullName, address, phone, email, price));

        HBox box = new HBox(10);
        box.getChildren().addAll(btnBid, btnDetail);
        return box;
    }

    // =========================================================================
    // CHUYỂN VÀO PHÒNG ĐẤU GIÁ
    // =========================================================================

    private void enterAuctionRoom(ActionEvent event, String plate, String stringPrice) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AuctionRoom.fxml"));
            Parent root = loader.load();

            double startingPrice = parsePrice(stringPrice);

            AuctionRoomController roomController = loader.getController();
            roomController.setAuctionRoomData(plate, startingPrice);

            ((Node) event.getSource()).getScene().setRoot(root);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Lỗi khi mở phòng đấu giá", e);
            showInfo("Lỗi hệ thống", "Không thể tải phòng đấu giá.");
        }
    }

    /** Chuyển chuỗi giá "150.000.000" thành số thực 150000000.0 */
    private double parsePrice(String rawPrice) {
        try {
            String clean = rawPrice.replaceAll("[^\\d]", "");
            return clean.isEmpty() ? 0.0 : Double.parseDouble(clean);
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Không thể đọc giá trị: " + rawPrice, e);
            return 0.0;
        }
    }

    // =========================================================================
    // DIALOG CHI TIẾT SẢN PHẨM
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

        Button btnClose = new Button("Quay lại");
        btnClose.setStyle("-fx-background-color: transparent; -fx-border-color: white; " +
                "-fx-border-radius: 20; -fx-text-fill: white; -fx-cursor: hand; " +
                "-fx-padding: 8 30; -fx-font-weight: bold;");
        btnClose.setOnAction(e ->
                ((StackPane) btnHome.getScene().getRoot()).getChildren().remove(overlay)
        );

        dialog.getChildren().addAll(
                lblTitle,
                makeInfoBox("GIÁ KHỞI ĐIỂM",          price + " VNĐ", "#00b894", true),
                makeInfoBox("HỌ VÀ TÊN NGƯỜI BÁN",    fullName,        "#0ea5e9", false),
                makeInfoBox("ĐỊA CHỈ LIÊN HỆ",        address,         "#0ea5e9", false),
                makeInfoBox("SỐ ĐIỆN THOẠI LIÊN HỆ",  phone,           "#0ea5e9", false),
                makeInfoBox("EMAIL LIÊN HỆ",           email,           "#0ea5e9", false),
                btnClose
        );

        overlay.getChildren().add(dialog);
        ((StackPane) btnHome.getScene().getRoot()).getChildren().add(overlay);
    }

    /** Tạo một cặp (tiêu đề màu + nội dung trắng) trong dialog chi tiết */
    private VBox makeInfoBox(String titleText, String valueText, String titleColor, boolean isBold) {
        Label lblTitle = new Label(titleText);
        lblTitle.setStyle("-fx-text-fill: " + titleColor + "; -fx-font-weight: bold; -fx-font-size: 15px;");

        Label lblValue = new Label("• " + valueText);
        lblValue.setStyle(isBold
                ? "-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;"
                : "-fx-text-fill: white; -fx-font-size: 14px;");

        VBox box = new VBox(5);
        box.getChildren().addAll(lblTitle, lblValue);
        return box;
    }

    // =========================================================================
    // HÀM TIỆN ÍCH DÙNG CHUNG
    // =========================================================================

    /** Chuyển màn hình bằng cách thay Scene của Stage — dùng chung cho mọi navigation */
    private void navigateTo(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Lỗi khi chuyển trang: " + fxmlPath, e);
            showInfo("Lỗi hệ thống", "Không thể tải màn hình: " + fxmlPath);
        }
    }

    /** Đặt trạng thái active cho nút lọc loại xe */
    private void setActiveFilterTab(Button active, Button inactive) {
        active.setStyle("-fx-background-color: #00b894; -fx-background-radius: 32;");
        active.setTextFill(Color.WHITE);
        inactive.setStyle("-fx-background-color: transparent; -fx-background-radius: 32;");
        inactive.setTextFill(Color.web("#cccccc"));
    }

    /** Đặt trạng thái active cho tab điều hướng chính (Trang Chủ / Lịch Sử / Kết Quả) */
    private void setActiveTab(Button activeButton) {
        Button[] tabs = {btnHome, btnHistory, btnResults};
        for (Button tab : tabs) {
            if (tab == null) continue;
            if (tab == activeButton) {
                tab.setStyle("-fx-background-color: transparent; " +
                        "-fx-border-color: #00b894; -fx-border-width: 0 0 2 0;");
                tab.setTextFill(Color.web("#00b894"));
            } else {
                tab.setStyle("-fx-background-color: transparent;");
                tab.setTextFill(Color.WHITE);
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