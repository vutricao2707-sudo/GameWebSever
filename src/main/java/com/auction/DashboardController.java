package com.auction;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;       // <-- Bổ sung
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;         // <-- Bổ sung
import javafx.scene.layout.Priority;     // <-- Bổ sung
import javafx.scene.layout.Region;       // <-- Bổ sung
import javafx.scene.layout.StackPane;    // <-- Bổ sung
import javafx.scene.layout.VBox;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

public class DashboardController {

    @FXML private Button btnHome, btnHistory, btnResults, btnCar, btnBike;
    @FXML private FlowPane productContainer;

    @FXML
    public void initialize() {
        System.out.println("DEBUG: ----- BẮT ĐẦU MỞ TRANG DASHBOARD -----");
        loadCarProducts();
        setActiveTab(btnHome);
    }

    @FXML
    private void handleHome(ActionEvent event) {
        setActiveTab(btnHome);
        loadCarProducts();
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
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/RegisterSell.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleToggleCar(ActionEvent event) {
        btnCar.setStyle("-fx-background-color: #00b894; -fx-background-radius: 32;");
        btnBike.setStyle("-fx-background-color: transparent; -fx-background-radius: 32;");
        btnCar.setTextFill(javafx.scene.paint.Color.WHITE);
        btnBike.setTextFill(javafx.scene.paint.Color.web("#cccccc"));
        System.out.println("DEBUG: Bấm nút xem Ô TÔ");
        loadCarProducts();
    }

    @FXML
    private void handleToggleBike(ActionEvent event) {
        btnBike.setStyle("-fx-background-color: #00b894; -fx-background-radius: 32;");
        btnCar.setStyle("-fx-background-color: transparent; -fx-background-radius: 32;");
        btnBike.setTextFill(javafx.scene.paint.Color.WHITE);
        btnCar.setTextFill(javafx.scene.paint.Color.web("#cccccc"));
        System.out.println("DEBUG: Bấm nút xem XE MÁY");
        loadBikeProducts();
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- LOGIC LOAD DỮ LIỆU ĐÃ FIX LỖI FONT TIẾNG VIỆT ---

    private void loadCarProducts() {
        System.out.println("DEBUG 2: Đang chạy hàm loadCarProducts().");
        System.out.println("DEBUG 3: Số lượng xe trong DataStore lúc này là: " + DataStore.products.size());

        if (productContainer == null) {
            System.out.println("DEBUG 4: LỖI NẶNG - productContainer bị NULL (Chưa map fx:id trong file fxml)");
            return;
        }

        System.out.println("DEBUG 4: productContainer an toàn. Bắt đầu quét kho...");
        productContainer.getChildren().clear();

        for (String[] p : DataStore.products) {
            // ĐÃ FIX: Dùng contains để tránh lỗi bảng mã khi so sánh chữ "Ô tô"
            if (p[0] != null && (p[0].equals("Ô tô") || p[0].contains("tô") || p[0].contains("to"))) {
                System.out.println("DEBUG 5: Tìm thấy 1 Ô TÔ (Biển: " + p[1] + "). Đang vẽ thẻ...");
                // THAY ĐỔI NHỎ: Truyền thẳng cả mảng 'p' vào thay vì tách lẻ 3 tham số
                productContainer.getChildren().add(createProductCard(p));
            }
        }

        if (productContainer.getChildren().isEmpty()) {
            System.out.println("DEBUG 6: Vùng chứa trống, in ra thông báo mặc định.");
            showEmptyMessage("Chưa có Ô TÔ nào được đăng bán.");
        }
    }

    private void loadBikeProducts() {
        System.out.println("DEBUG 2: Đang chạy hàm loadBikeProducts().");
        System.out.println("DEBUG 3: Số lượng xe trong DataStore lúc này là: " + DataStore.products.size());

        if (productContainer == null) return;

        productContainer.getChildren().clear();

        for (String[] p : DataStore.products) {
            // ĐÃ FIX: Dùng contains để tránh lỗi bảng mã chữ "Xe máy"
            if (p[0] != null && (p[0].equals("Xe máy") || p[0].contains("máy") || p[0].contains("may"))) {
                System.out.println("DEBUG 5: Tìm thấy 1 XE MÁY (Biển: " + p[1] + "). Đang vẽ thẻ...");
                // THAY ĐỔI NHỎ: Truyền thẳng cả mảng 'p' vào
                productContainer.getChildren().add(createProductCard(p));
            }
        }

        if (productContainer.getChildren().isEmpty()) {
            showEmptyMessage("Chưa có XE MÁY nào được đăng bán.");
        }
    }

    private void showEmptyMessage(String msg) {
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 18px;");
        productContainer.getChildren().add(lbl);
    }

    // THAY ĐỔI: Sửa hàm nhận vào cả 1 mảng dữ liệu (String[] p) thay vì 3 chữ rời rạc
    private VBox createProductCard(String[] p) {
        // Tách dữ liệu từ mảng
        String plate = p.length > 1 ? p[1] : "";
        String province = p.length > 2 ? p[2] : "";
        String price = p.length > 3 ? p[3] : "";
        // Lấy thêm 4 thông tin cá nhân (Nếu mảng có đủ 8 phần tử)
        String fullName = p.length > 4 ? p[4] : "Chưa cập nhật";
        String phone = p.length > 5 ? p[5] : "Chưa cập nhật";
        String email = p.length > 6 ? p[6] : "Chưa cập nhật";
        String address = p.length > 7 ? p[7] : "Chưa cập nhật";

        // 1. KHUNG THẺ CHÍNH (Nền tối, bo góc)
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: #1a2230; -fx-background-radius: 12; -fx-border-color: #2a364a; -fx-border-radius: 12; -fx-border-width: 1; -fx-padding: 15;");
        card.setPrefWidth(280);

        // 2. KHU VỰC BIỂN SỐ (Nền trắng chữ đen to)
        VBox plateBox = new VBox();
        plateBox.setAlignment(Pos.CENTER);
        plateBox.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 15 10;");

        // Tách biển số (Ví dụ: 30C-567.89 -> Trên: 30C, Dưới: 567.89)
        String[] plateParts = plate.split("-");
        String topText = plateParts.length > 0 ? plateParts[0] : plate;
        String bottomText = plateParts.length > 1 ? plateParts[1] : "";

        Label lblPlateTop = new Label(topText);
        lblPlateTop.setStyle("-fx-text-fill: black; -fx-font-size: 32px; -fx-font-weight: bold;");
        Label lblPlateBottom = new Label(bottomText);
        lblPlateBottom.setStyle("-fx-text-fill: black; -fx-font-size: 40px; -fx-font-weight: bold;");
        plateBox.getChildren().addAll(lblPlateTop, lblPlateBottom);

        // Đặt biển số vào StackPane để dán đè cái logo xe màu xanh lên góc
        StackPane plateContainer = new StackPane(plateBox);
        Label carBadge = new Label("🚗"); // Dùng icon xe hơi
        carBadge.setStyle("-fx-background-color: #00b894; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8; -fx-font-size: 14px;");
        StackPane.setAlignment(carBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(carBadge, new Insets(-10, -10, 0, 0)); // Kéo lồi ra ngoài góc 1 chút
        plateContainer.getChildren().add(carBadge);

        // 3. KHU VỰC THÔNG TIN (Tỉnh & Phiên)
        HBox locationBox = new HBox();
        locationBox.setAlignment(Pos.CENTER_LEFT);
        Label lblProv = new Label("• " + province);
        lblProv.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
        Region locSpacer = new Region();
        HBox.setHgrow(locSpacer, Priority.ALWAYS); // Đẩy chữ Phiên sang sát mép phải
        Label lblSession = new Label("Phiên 9");
        lblSession.setStyle("-fx-background-color: #475569; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 3 10; -fx-font-size: 12px;");
        locationBox.getChildren().addAll(lblProv, locSpacer, lblSession);

        // 4. KHU VỰC NGƯỜI THAM GIA
        HBox usersBox = new HBox();
        usersBox.setAlignment(Pos.CENTER_LEFT);
        Label lblUsers = new Label("👤 9");
        lblUsers.setStyle("-fx-text-fill: #00b894; -fx-font-size: 14px;");
        Region userSpacer = new Region();
        HBox.setHgrow(userSpacer, Priority.ALWAYS);
        Label lblChart = new Label("📈");
        lblChart.setStyle("-fx-text-fill: #00b894; -fx-font-size: 14px;");
        usersBox.getChildren().addAll(lblUsers, userSpacer, lblChart);

        // 5. KHU VỰC THỜI GIAN
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

        // 6. KHU VỰC 2 NÚT BẤM (Nằm ngang)
        HBox buttonBox = new HBox(10);
        Button btnBid = new Button("Đăng ký đấu giá");
        btnBid.setStyle("-fx-background-color: #00b894; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 8 0;");
        btnBid.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnBid, Priority.ALWAYS); // Ép nút giãn đều 50%

        Button btnDetail = new Button("Xem chi tiết");
        btnDetail.setStyle("-fx-background-color: transparent; -fx-border-color: #00b894; -fx-border-radius: 20; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 0;");
        btnDetail.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnDetail, Priority.ALWAYS); // Ép nút giãn đều 50%

        // THAY ĐỔI: Gắn sự kiện để khi bấm vào sẽ gọi hàm bật bảng Popup
        btnDetail.setOnAction(e -> showProductDetails(plate, fullName, address, phone, email));

        buttonBox.getChildren().addAll(btnBid, btnDetail);

        // Lắp ghép tất cả vào Thẻ chính
        card.getChildren().addAll(plateContainer, locationBox, usersBox, timeBox, buttonBox);
        return card;
    }

    // =========================================================================
    // HÀM MỚI TẠO POPUP - GIỮ NGUYÊN HOÀN TOÀN TỪ YÊU CẦU TRƯỚC CỦA BẠN
    // =========================================================================
    private void showProductDetails(String plate, String fullName, String address, String phone, String email) {
        // 1. Tạo lớp phủ đen mờ toàn màn hình
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8);");

        // 2. Tạo cái bảng thông tin (Màu nền tối, viền bo tròn)
        VBox dialog = new VBox(25);
        dialog.setMaxSize(450, 450);
        dialog.setStyle("-fx-background-color: #1a2230; -fx-background-radius: 15; -fx-border-color: #334155; -fx-border-radius: 15; -fx-border-width: 1; -fx-padding: 35;");
        dialog.setAlignment(Pos.CENTER_LEFT);

        // Tiêu đề
        Label lblTitle = new Label("THÔNG TIN BIỂN: " + plate);
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 20px;");
        VBox.setMargin(lblTitle, new Insets(0, 0, 10, 0));

        // Nhóm 1: Tên người bán
        VBox boxName = new VBox(5);
        Label lblNameTitle = new Label("HỌ VÀ TÊN NGƯỜI BÁN");
        lblNameTitle.setStyle("-fx-text-fill: #0ea5e9; -fx-font-weight: bold; -fx-font-size: 15px;");
        Label lblName = new Label("• " + fullName);
        lblName.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        boxName.getChildren().addAll(lblNameTitle, lblName);

        // Nhóm 2: Địa chỉ
        VBox boxAddress = new VBox(5);
        Label lblAddressTitle = new Label("ĐỊA CHỈ LIÊN HỆ");
        lblAddressTitle.setStyle("-fx-text-fill: #0ea5e9; -fx-font-weight: bold; -fx-font-size: 15px;");
        Label lblAddress = new Label("• " + address);
        lblAddress.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        boxAddress.getChildren().addAll(lblAddressTitle, lblAddress);

        // Nhóm 3: SĐT
        VBox boxPhone = new VBox(5);
        Label lblPhoneTitle = new Label("SỐ ĐIỆN THOẠI LIÊN HỆ");
        lblPhoneTitle.setStyle("-fx-text-fill: #0ea5e9; -fx-font-weight: bold; -fx-font-size: 15px;");
        Label lblPhone = new Label("• " + phone);
        lblPhone.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        boxPhone.getChildren().addAll(lblPhoneTitle, lblPhone);

        // Nhóm 4: Email
        VBox boxEmail = new VBox(5);
        Label lblEmailTitle = new Label("EMAIL LIÊN HỆ");
        lblEmailTitle.setStyle("-fx-text-fill: #0ea5e9; -fx-font-weight: bold; -fx-font-size: 15px;");
        Label lblEmail = new Label("• " + email);
        lblEmail.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        boxEmail.getChildren().addAll(lblEmailTitle, lblEmail);

        // Nút Quay lại
        Button btnClose = new Button("Quay lại");
        btnClose.setStyle("-fx-background-color: transparent; -fx-border-color: white; -fx-border-radius: 20; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 30; -fx-font-weight: bold;");

        // Sự kiện: Bấm Quay lại thì xóa cái overlay mờ đi
        btnClose.setOnAction(e -> {
            ((StackPane) btnHome.getScene().getRoot()).getChildren().remove(overlay);
        });

        // Ghép mọi thứ lại
        dialog.getChildren().addAll(lblTitle, boxName, boxAddress, boxPhone, boxEmail, btnClose);
        overlay.getChildren().add(dialog);

        // Ném cái overlay này lên trên cùng màn hình
        ((StackPane) btnHome.getScene().getRoot()).getChildren().add(overlay);
    }
    // =========================================================================

    private void setActiveTab(Button activeButton) {
        Button[] tabs = {btnHome, btnHistory, btnResults};
        for (Button tab : tabs) {
            if (tab != null) {
                if (tab == activeButton) {
                    tab.setStyle("-fx-background-color: transparent; -fx-border-color: #00b894; -fx-border-width: 0 0 2 0;");
                    tab.setTextFill(javafx.scene.paint.Color.web("#00b894"));
                } else {
                    tab.setStyle("-fx-background-color: transparent;");
                    tab.setTextFill(javafx.scene.paint.Color.WHITE);
                }
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