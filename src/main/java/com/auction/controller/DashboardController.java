package com.auction.controller;

import com.auction.dto.ItemDTO;
import com.auction.model.User;
import com.auction.service.ProductService;
import com.auction.util.SessionManager;
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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("ALL")
public class DashboardController {

    @FXML private Button btnHome, btnHistory, btnResults, btnCar, btnBike;
    @FXML private FlowPane productContainer;
    @FXML private Label lblUserName;

    private static final Logger logger = Logger.getLogger(DashboardController.class.getName());
    private final ProductService productService = new ProductService();
    private User currentUser;

    // =========================================================================
    // KHỞI TẠO
    // =========================================================================

    @FXML
    public void initialize() {
        // Lấy user từ Session hiện tại
        User user = SessionManager.INSTANCE.getCurrentUser();
        if (user != null && user.getFullName() != null) {
            lblUserName.setText("Xin chào, " + user.getFullName());
        } else {
            lblUserName.setText("Xin chào, Người dùng");
        }

        // Các logic tải dữ liệu khác
        loadProducts("Ô tô");
        setActiveTab(btnHome);
    }

    // Giữ lại hàm này để LoginController gọi không bị báo lỗi đỏ
    public void setUserInfo(User user) {
        this.currentUser = user;
    }

    // =========================================================================
    // ĐIỀU HƯỚNG — Tất cả đều dùng navigateTo() dùng chung
    // =========================================================================

    @FXML private void handleHome(ActionEvent event)           { setActiveTab(btnHome); loadProducts("Ô tô"); }
    @FXML private void handleShowHistory(ActionEvent event)    { navigateTo(event, "/fxml/AuctionHistory.fxml"); }
    @FXML private void handleShowResults(ActionEvent event)    { navigateTo(event, "/fxml/AuctionResult.fxml"); }
    @FXML private void handleRegisterToSell(ActionEvent event) { navigateTo(event, "/fxml/RegisterSell.fxml"); }
    @FXML private void handleLogout(ActionEvent event) {
        SessionManager.INSTANCE.logout(); // Đăng xuất thì xóa Session
        navigateTo(event, "/fxml/Login.fxml");
    }

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
        for (int i = 0; i < products.size(); i++) {
            ItemDTO item = ItemDTO.fromApprovedArray(products.get(i));
            if (item != null) {
                productContainer.getChildren().add(createProductCard(item, i + 1));
            }
        }
    }

    private void showEmptyMessage(String msg) {
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 18px;");
        productContainer.getChildren().add(lbl);
    }

    // =========================================================================
    // XÂY DỰNG THẺ SẢN PHẨM
    // =========================================================================

    private VBox createProductCard(ItemDTO item, int sessionIndex) {

        String type     = item.getVehicleType();
        String plate    = item.getPlateNumber();
        String province = item.getProvince();
        String price    = item.getFormattedPrice();
        String fullName = item.getSellerName();
        String phone    = item.getSellerPhone();
        String email    = item.getSellerEmail();
        String address  = item.getSellerAddress();

        // =========================
        // BUTTON ĐẤU GIÁ
        // =========================

        Button btnBid = new Button("Đăng ký đấu giá");

        btnBid.setStyle(
                "-fx-background-color: #00b894; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 20;"
        );

        btnBid.setMaxWidth(Double.MAX_VALUE);

        String endTime = item.getRegistrationEndTime();

        btnBid.setOnAction(e -> {

            try {

                if (endTime != null && !endTime.isEmpty()) {

                    String fixedEndTime =
                            endTime.replace("T", " ");

                    if (fixedEndTime.contains(".")) {

                        fixedEndTime =
                                fixedEndTime.substring(
                                        0,
                                        fixedEndTime.indexOf(".")
                                );
                    }

                    Timestamp timestamp =
                            Timestamp.valueOf(fixedEndTime);

                    LocalDateTime targetTime =
                            timestamp.toLocalDateTime();

                    LocalDateTime now =
                            LocalDateTime.now();

                    // =====================================
                    // CHƯA HẾT THỜI GIAN ĐĂNG KÝ
                    // =====================================

                    if (now.isBefore(targetTime)) {

                        showInfo(
                                "Chưa mở đấu giá",
                                "Hiện vẫn đang trong thời gian đăng ký tham gia đấu giá."
                        );

                        return;
                    }
                }

                // =====================================
                // ĐÃ HẾT ĐĂNG KÝ
                // =====================================

                enterAuctionRoom(e, plate, price);

            } catch (Exception ex) {

                ex.printStackTrace();

                showInfo(
                        "Lỗi thời gian",
                        "Không thể kiểm tra thời gian đấu giá."
                );
            }
        });
        // =========================
        // CARD
        // =========================

        VBox card = new VBox(12);

        card.setStyle(
                "-fx-background-color: #1a2230; " +
                        "-fx-background-radius: 12; " +
                        "-fx-border-color: #2a364a; " +
                        "-fx-border-radius: 12; " +
                        "-fx-border-width: 1; " +
                        "-fx-padding: 15;"
        );

        card.setPrefWidth(280);

        HBox buttonBox =
                buildCardButtons(
                        btnBid,
                        plate,
                        price,
                        fullName,
                        address,
                        phone,
                        email
                );

        card.getChildren().addAll(
                buildPlateSection(plate, type),
                buildLocationRow(province, sessionIndex),
                buildUsersRow(),
                buildTimeRow(item),
                buttonBox
        );

        return card;
    }

    // =========================================================================
    // 5 HÀM BUILD* — Mỗi hàm chịu trách nhiệm DUY NHẤT 1 khu vực của card
    // =========================================================================

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

    private HBox buildLocationRow(String province, int sessionIndex) {

        Label lblProv = new Label("• " + province);

        lblProv.setStyle(
                "-fx-text-fill: #94a3b8; -fx-font-size: 13px;"
        );

        Region spacer = new Region();

        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblSession =
                new Label("Phiên " + sessionIndex);

        lblSession.setStyle(
                "-fx-background-color: #475569; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 12; " +
                        "-fx-padding: 3 10; " +
                        "-fx-font-size: 12px;"
        );

        HBox row = new HBox();

        row.setAlignment(Pos.CENTER_LEFT);

        row.getChildren().addAll(
                lblProv,
                spacer,
                lblSession
        );

        return row;
    }

    private HBox buildUsersRow() {
        // Tạo một hàng trống, không chứa hình người hay số 9 nữa
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        // Có thể set padding nhỏ lại để thẻ xe không bị trống quá
        row.setStyle("-fx-padding: 0;");
        return row;
    }
    private HBox buildCardButtons(
            Button btnBid,
            String plate,
            String price,
            String fullName,
            String address,
            String phone,
            String email
    ) {

        btnBid.setStyle(
                "-fx-background-color: #00b894; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 20; " +
                        "-fx-cursor: hand; " +
                        "-fx-padding: 8 0;"
        );

        btnBid.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(btnBid, Priority.ALWAYS);

        Button btnDetail = new Button("Xem chi tiết");

        btnDetail.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-border-color: #00b894; " +
                        "-fx-border-radius: 20; " +
                        "-fx-text-fill: white; " +
                        "-fx-cursor: hand; " +
                        "-fx-padding: 8 0;"
        );

        btnDetail.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(btnDetail, Priority.ALWAYS);

        btnDetail.setOnAction(e ->
                showProductDetails(
                        plate,
                        fullName,
                        address,
                        phone,
                        email,
                        price
                )
        );

        HBox box = new HBox(10);

        box.getChildren().addAll(
                btnBid,
                btnDetail
        );

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

    private void navigateTo(ActionEvent event, String fxmlPath) {
        try {
            Parent newRoot = FXMLLoader.load(getClass().getResource(fxmlPath));
            // Thống nhất dùng setRoot() thay vì tạo Scene mới
            Scene currentScene = ((Node) event.getSource()).getScene();
            currentScene.setRoot(newRoot);
        } catch (IOException e) {
            e.printStackTrace();
            // Có thể thêm Alert thông báo lỗi ở đây
        }
    }

    private void setActiveFilterTab(Button active, Button inactive) {
        active.setStyle("-fx-background-color: #00b894; -fx-background-radius: 32;");
        active.setTextFill(Color.WHITE);
        inactive.setStyle("-fx-background-color: transparent; -fx-background-radius: 32;");
        inactive.setTextFill(Color.web("#cccccc"));
    }

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
    private HBox buildTimeRow(ItemDTO item) {

        Label clockIcon = new Label("🕒");

        clockIcon.setStyle(
                "-fx-text-fill: white; " +
                        "-fx-font-size: 20px;"
        );

        Label lblTimeLabel = new Label();

        lblTimeLabel.setStyle(
                "-fx-text-fill: #94a3b8; " +
                        "-fx-font-size: 11px;"
        );

        Label lblTimeValue = new Label();

        lblTimeValue.setStyle(
                "-fx-text-fill: white; " +
                        "-fx-font-size: 15px; " +
                        "-fx-font-weight: bold;"
        );

        VBox timeTexts = new VBox();
        timeTexts.getChildren().addAll(
                lblTimeLabel,
                lblTimeValue
        );

        HBox row = new HBox(10);

        row.setAlignment(Pos.CENTER_LEFT);

        row.getChildren().addAll(
                clockIcon,
                timeTexts
        );

        String auctionStatus =
                item.getAuctionStatus();

        // =====================================
        // WAITING
        // =====================================

        if ("WAITING".equalsIgnoreCase(auctionStatus)
                || "PENDING".equalsIgnoreCase(auctionStatus)) {

            lblTimeLabel.setText("Trạng thái");

            lblTimeValue.setText("⏳ Chờ khai mạc");

            return row;
        }

        // =====================================
        // FINISHED
        // =====================================

        if ("FINISHED".equalsIgnoreCase(auctionStatus)) {

            lblTimeLabel.setText("Trạng thái");

            lblTimeValue.setText("🏁 Đã kết thúc");

            return row;
        }

        // =====================================
        // REGISTRATION / AUCTION
        // =====================================

        try {

            String targetRaw;

            boolean isRegistration =
                    "REGISTRATION".equalsIgnoreCase(
                            auctionStatus
                    );

            if (isRegistration) {

                lblTimeLabel.setText(
                        "Đăng ký còn lại"
                );

                targetRaw =
                        item.getRegistrationEndTime();

            } else {

                lblTimeLabel.setText(
                        "Đấu giá còn lại"
                );

                targetRaw =
                        item.getAuctionEndTime();
            }

            targetRaw =
                    targetRaw.replace("T", " ");

            if (targetRaw.contains(".")) {

                targetRaw =
                        targetRaw.substring(
                                0,
                                targetRaw.indexOf(".")
                        );
            }

            Timestamp timestamp =
                    Timestamp.valueOf(targetRaw);

            LocalDateTime targetTime =
                    timestamp.toLocalDateTime();

            Timeline timeline =
                    new Timeline(
                            new KeyFrame(
                                    Duration.seconds(1),
                                    event -> {

                                        LocalDateTime now =
                                                LocalDateTime.now();

                                        if (now.isBefore(targetTime)) {

                                            java.time.Duration d =
                                                    java.time.Duration.between(
                                                            now,
                                                            targetTime
                                                    );

                                            long days =
                                                    d.toDays();

                                            long hours =
                                                    d.toHoursPart();

                                            long minutes =
                                                    d.toMinutesPart();

                                            long seconds =
                                                    d.toSecondsPart();

                                            lblTimeValue.setText(
                                                    String.format(
                                                            "%d ngày %02d:%02d:%02d",
                                                            days,
                                                            hours,
                                                            minutes,
                                                            seconds
                                                    )
                                            );

                                        } else {

                                            lblTimeValue.setText(
                                                    isRegistration
                                                            ? "⚡ Đang đấu giá"
                                                            : "🏁 Đã kết thúc"
                                            );
                                        }
                                    }
                            )
                    );

            timeline.setCycleCount(
                    Timeline.INDEFINITE
            );

            timeline.play();

        } catch (Exception e) {

            e.printStackTrace();

            lblTimeValue.setText(
                    "Không xác định"
            );
        }

        return row;
    }
}