package com.auction.controller;
import com.auction.util.UIUtils;
import com.auction.database.ItemDAO;
import com.auction.dto.ItemDTO;
import com.auction.model.User;
import com.auction.service.AuctionFinishService;
import com.auction.service.AuctionSyncManager;
import com.auction.service.ProductService;
import com.auction.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.util.Duration;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.application.Platform;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("ALL")
public class DashboardController implements AuctionFinishService.AuctionResultListener {

    @FXML private Button btnHome, btnHistory, btnResults, btnCar, btnBike;
    @FXML private FlowPane productContainer;
    @FXML private Label lblUserName;

    private static final Logger logger = Logger.getLogger(DashboardController.class.getName());
    private final ProductService productService = new ProductService();
    private User currentUser;
    private final List<VBox> activeCards = new ArrayList<>();
    private Timeline autoRefreshTimeline;
    private final Map<Integer, AuctionSyncManager.AuctionListener> auctionListeners = new HashMap<>();
    @FXML
    public void initialize() {
        User user = SessionManager.INSTANCE.getCurrentUser();

        String fullName = "Người dùng";

        if (user != null &&
                user.getFullName() != null &&
                !user.getFullName().isBlank()) {

            fullName = user.getFullName();
        }

        lblUserName.setText("Xin chào, " + fullName);

        loadProducts("Ô tô");
        setActiveTab(btnHome);
        AuctionFinishService.getInstance().addListener(this);
        autoRefreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(10), e -> {
                    if (btnCar != null && btnCar.getStyle().contains("#00b894")) {
                        loadProducts("Ô tô");
                    } else {
                        loadProducts("Xe máy");
                    }
                })
        );

        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }

    public void setUserInfo(User user) { this.currentUser = user; }

    @Override
    public void onAuctionFinished(String plateNumber, double finalPrice, int winnerId, String winnerName) {
        activeCards.stream()
                .filter(card -> plateNumber.equals(card.getUserData()))
                .findFirst()
                .ifPresent(card -> handleCardFinished(card, plateNumber));
    }

    private void handleCardFinished(VBox card, String plateNumber) {
        card.getChildren().forEach(node -> {
            if (node instanceof HBox timeRow && timeRow.getChildren().size() >= 2) {
                try {
                    VBox texts = (VBox) timeRow.getChildren().get(1);
                    if (texts.getChildren().size() >= 2) {
                        Label lblValue = (Label) texts.getChildren().get(1);
                        lblValue.setText("🏁 Phiên đấu giá đã kết thúc");
                        lblValue.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 13px; -fx-font-weight: bold;");
                    }
                } catch (Exception ignored) {}
            }
        });

        card.getChildren().forEach(node -> {
            if (node instanceof HBox buttonBox) {
                buttonBox.getChildren().forEach(btn -> {
                    if (btn instanceof Button b && b.getText().contains("ĐẤU GIÁ")) {
                        b.setText("ĐÃ KẾT THÚC");
                        b.setDisable(true);
                        b.setStyle("-fx-background-color: #475569; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20;");
                    }
                });
            }
        });

        // Ẩn biển số sau khi kết thúc 3 giây để user kịp nhìn
        Timeline hideTimer = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            if (productContainer != null) {
                productContainer.getChildren().remove(card);
                activeCards.remove(card);
            }
        }));
        hideTimer.setCycleCount(1);
        hideTimer.setOnFinished(e2 -> hideTimer.stop());
        hideTimer.play();
    }

    @FXML private void handleHome(ActionEvent event)           { setActiveTab(btnHome); loadProducts("Ô tô"); }
    @FXML private void handleShowHistory(ActionEvent event)    { navigateTo(event, "/fxml/AuctionHistory.fxml"); }
    @FXML private void handleShowResults(ActionEvent event)    { navigateTo(event, "/fxml/AuctionResult.fxml"); }
    @FXML private void handleRegisterToSell(ActionEvent event) { navigateTo(event, "/fxml/RegisterSell.fxml"); }
    @FXML private void handleLogout(ActionEvent event) {
        cleanupResources();
        SessionManager.INSTANCE.logout();
        navigateTo(event, "/fxml/Login.fxml");
    }

    @FXML private void handleToggleCar(ActionEvent event)  { setActiveFilterTab(btnCar, btnBike); loadProducts("Ô tô"); }
    @FXML private void handleToggleBike(ActionEvent event) { setActiveFilterTab(btnBike, btnCar); loadProducts("Xe máy"); }

    private void loadProducts(String type) {

        if (productContainer == null) return;

        cleanupAuctionListeners();

        productContainer.setDisable(true);
        productContainer.getChildren().clear();
        activeCards.clear();

        List<String[]> products = productService.getApprovedProductsByType(type);

        if (products.isEmpty()) {
            showEmptyMessage("Chưa có " + type.toUpperCase() + " nào được đăng bán.");
            productContainer.setDisable(false);
            return;
        }

        for (int i = 0; i < products.size(); i++) {

            ItemDTO item = ItemDTO.fromApprovedArray(products.get(i));

            if (item != null &&
                    !"FINISHED".equalsIgnoreCase(item.getAuctionStatus())) {

                VBox card = createProductCard(item, i + 1);

                card.setUserData(item.getPlateNumber());

                activeCards.add(card);
                productContainer.getChildren().add(card);
            }
        }

        productContainer.setDisable(false);
    }

    private void showEmptyMessage(String msg) {
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 18px;");
        productContainer.getChildren().add(lbl);
    }

    private VBox createProductCard(ItemDTO item, int sessionIndex) {
        String type     = item.getVehicleType();
        String plate    = item.getPlateNumber();
        String province = item.getProvince();
        String auctionStatus = item.getAuctionStatus();

        Button btnBid = new Button();
        btnBid.setMaxWidth(Double.MAX_VALUE);

        if ("AUCTION".equalsIgnoreCase(auctionStatus)) {
            btnBid.setText("VÀO ĐẤU GIÁ NGAY");
            btnBid.setStyle("-fx-background-color: #eab308; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand;");
        } else {
            btnBid.setText("ĐĂNG KÝ ĐẤU GIÁ");
            btnBid.setStyle("-fx-background-color: #00b894; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand;");
        }

        btnBid.setOnAction(e -> {
            // Kiểm tra lại trạng thái thực tế từ DB để chống click lách luật
            String status = ItemDAO.getAuctionStatus(plate);

            if ("WAITING".equalsIgnoreCase(status) || "REGISTRATION".equalsIgnoreCase(status)) {
                // Gọi thẳng UIUtils tàng hình viền Windows
                UIUtils.showCustomPopup(
                        "Chưa mở đấu giá",
                        "Hiện đang trong thời gian đăng ký, vui lòng quay lại khi thời gian này kết thúc.",
                        "⏳",
                        "#00b894"
                );
                return;
            }
            if ("FINISHED".equalsIgnoreCase(status)) {
                // Gọi thẳng UIUtils tàng hình viền Windows
                UIUtils.showCustomPopup(
                        "Phiên đã kết thúc",
                        "Phiên đấu giá biển số này đã kết thúc! Quý khách vui lòng tham gia đấu giá biển số khác.",
                        "🏁",
                        "#ef4444"
                );
                return;
            }
            enterAuctionRoom(e, plate, item.getFormattedPrice(), item.getAuctionEndTime());
        });
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: #1a2230; -fx-background-radius: 12; -fx-border-color: #2a364a; -fx-border-radius: 12; -fx-border-width: 1; -fx-padding: 15;");
        card.setPrefWidth(280);
        card.setUserData(plate);

        card.getChildren().addAll(
                buildPlateSection(plate, type),
                buildLocationRow(province, sessionIndex),
                buildUsersRow(),
                buildTimeRow(item, btnBid, card),
                buildCardButtons(btnBid, item)
        );
        return card;
    }

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
        badge.setStyle("-fx-background-color: #00b894; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8; -fx-font-size: 14px;");
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(-10, -10, 0, 0));
        return new StackPane(plateBox, badge);
    }

    private HBox buildLocationRow(String province, int sessionIndex) {
        Label lblProv = new Label("• " + (province != null && !province.isEmpty() ? province : "—"));
        lblProv.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label lblSession = new Label("Phiên " + sessionIndex);
        lblSession.setStyle("-fx-background-color: #475569; -fx-text-fill: white; -fx-background-radius: 12; -fx-padding: 3 10; -fx-font-size: 12px;");
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(lblProv, spacer, lblSession);
        return row;
    }

    private HBox buildUsersRow() {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 0;");
        return row;
    }

    private HBox buildCardButtons(Button btnBid, ItemDTO item) {
        btnBid.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnBid, Priority.ALWAYS);

        Button btnDetail = new Button("Xem chi tiết");
        btnDetail.setStyle("-fx-background-color: transparent; -fx-border-color: #00b894; -fx-border-radius: 20; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 0;");
        btnDetail.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnDetail, Priority.ALWAYS);
        btnDetail.setOnAction(e -> showProductDetails(item));

        HBox box = new HBox(10);
        box.getChildren().addAll(btnBid, btnDetail);
        return box;
    }

    // ================== LOGIC THỜI GIAN ĐÃ ĐƯỢC FIX ==================
    private LocalDateTime parseTime(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        try {
            String cleaned = raw.replace("T", " ");
            if (cleaned.contains(".")) cleaned = cleaned.substring(0, cleaned.indexOf("."));
            return Timestamp.valueOf(cleaned).toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }

    private String formatDuration(java.time.Duration d) {
        long totalMin = d.toMinutes();
        if (totalMin < 60) {
            return String.format("Còn %dp %02ds", totalMin, d.toSecondsPart());
        } else if (totalMin < 1440) {
            return String.format("Còn %dh %02dp", totalMin / 60, totalMin % 60);
        } else {
            return String.format("Còn %d ngày %dh", totalMin / 1440, (totalMin % 1440) / 60);
        }
    }

    private HBox buildTimeRow(ItemDTO item, Button btnBid, VBox card) {
        Label clockIcon = new Label("🕒");
        clockIcon.setStyle("-fx-text-fill: white; -fx-font-size: 20px;");

        Label lblTimeLabel = new Label("Trạng thái");
        lblTimeLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        Label lblTimeValue = new Label("Đang tải...");
        lblTimeValue.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");

        VBox texts = new VBox(lblTimeLabel, lblTimeValue);
        HBox row = new HBox(10, clockIcon, texts);
        row.setAlignment(Pos.CENTER_LEFT);

        // Tạo Listener để nhận tick từ SyncManager
        AuctionSyncManager.AuctionListener listener = new AuctionSyncManager.AuctionListener() {
            @Override
            public void onTimeTick(int itemId, long remaining, double price, String status) {
                Platform.runLater(() -> {
                    if ("REGISTRATION".equalsIgnoreCase(status)) {

                        lblTimeLabel.setText("Đăng ký còn lại");
                        lblTimeValue.setText(formatSecondsToDHMS(remaining));

                        lblTimeValue.setStyle(
                                "-fx-text-fill: #38bdf8; " +
                                        "-fx-font-size: 15px; " +
                                        "-fx-font-weight: bold;"
                        );

                    }
                    else if ("AUCTION".equalsIgnoreCase(status)) {

                        lblTimeLabel.setText("Đấu giá còn lại");
                        lblTimeValue.setText(formatSecondsToDHMS(remaining));

                        lblTimeValue.setStyle(
                                "-fx-text-fill: #eab308; " +
                                        "-fx-font-size: 15px; " +
                                        "-fx-font-weight: bold;"
                        );

                        btnBid.setText("VÀO ĐẤU GIÁ NGAY");

                        btnBid.setStyle(
                                "-fx-background-color: #eab308; " +
                                        "-fx-text-fill: white; " +
                                        "-fx-font-weight: bold; " +
                                        "-fx-background-radius: 20; " +
                                        "-fx-cursor: hand;"
                        );

                    }
                    else if ("FINISHED".equalsIgnoreCase(status)) {

                        lblTimeLabel.setText("Trạng thái");

                        lblTimeValue.setText("🏁 Đã kết thúc");

                        lblTimeValue.setStyle(
                                "-fx-text-fill: #ef4444; " +
                                        "-fx-font-size: 15px; " +
                                        "-fx-font-weight: bold;"
                        );

                    }
                    else {

                        lblTimeLabel.setText("Trạng thái");

                        lblTimeValue.setText("⏳ Chờ khai mạc");

                        lblTimeValue.setStyle(
                                "-fx-text-fill: #94a3b8; " +
                                        "-fx-font-size: 15px; " +
                                        "-fx-font-weight: bold;"
                        );
                    }
                });
            }
        };

        // Đăng ký lắng nghe
        AuctionSyncManager.getInstance().addListener(item.getId(), listener);
        auctionListeners.put(item.getId(), listener);
        // Gỡ lắng nghe khi card bị xóa khỏi Scene
        card.sceneProperty().addListener((obs, oldScene, newScene) -> {

            if (newScene == null) {

                AuctionSyncManager.getInstance()
                        .removeListener(item.getId(), listener);

                auctionListeners.remove(item.getId());
            }
        });

        return row;
    }

    private String formatSecondsToDHMS(long totalSecs) {
        if (totalSecs <= 0) return "00:00:00";
        long days = totalSecs / 86400;
        long hours = (totalSecs % 86400) / 3600;
        long minutes = (totalSecs % 3600) / 60;
        long seconds = totalSecs % 60;
        if (days > 0) return String.format("%d ngày %02dh %02dm", days, hours, minutes);
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
    }
    // =================================================================

    private void enterAuctionRoom(ActionEvent event, String plate, String stringPrice, String endTimeRaw) {
        try {
            double startingPrice = parsePrice(stringPrice);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AuctionRoom.fxml"));
            Parent root = loader.load();

            AuctionRoomController roomController = loader.getController();
            if (roomController == null) {
                UIUtils.showError("Lỗi", "Không khởi tạo được controller");
                return;
            }

            roomController.setAuctionRoomData(plate, startingPrice, endTimeRaw);

            Scene scene = ((Node) event.getSource()).getScene();
            scene.setRoot(root);

        } catch (IOException eLoad) {
            String msg = eLoad.getCause() != null ? eLoad.getCause().getMessage() : eLoad.getMessage();
            UIUtils.showError("Lỗi tải giao diện", "Không thể tải phòng đấu giá:\n" + msg);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi enterAuctionRoom", e);
            UIUtils.showError("Lỗi hệ thống", e.getMessage());
        }
    }

    private double parsePrice(String raw) {
        try {
            String clean = raw.replaceAll("[^\\d]", "");
            return clean.isEmpty() ? 0.0 : Double.parseDouble(clean);
        } catch (NumberFormatException e) { return 0.0; }
    }

    private void showProductDetails(ItemDTO item) {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.8);");

        VBox dialog = new VBox(25);
        dialog.setMaxSize(450, 540);
        dialog.setStyle("-fx-background-color: #1a2230; -fx-background-radius: 15; -fx-border-color: #334155; -fx-border-radius: 15; -fx-border-width: 1; -fx-padding: 35;");
        dialog.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label("THÔNG TIN BIỂN: " + item.getPlateNumber());
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 20px;");

        Button btnClose = new Button("Quay lại");
        btnClose.setStyle("-fx-background-color: transparent; -fx-border-color: white; -fx-border-radius: 20; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 30; -fx-font-weight: bold;");
        btnClose.setOnAction(e -> ((StackPane) btnHome.getScene().getRoot()).getChildren().remove(overlay));

        String priceLabel = "GIÁ KHỞI ĐIỂM";
        String priceValue = String.format("%,.0f VNĐ", item.getStartPrice());
        String priceColor = "#00b894";
        if ("AUCTION".equalsIgnoreCase(item.getAuctionStatus())) {
            priceLabel = "GIÁ HIỆN TẠI"; priceValue = item.getFormattedPrice() + " VNĐ"; priceColor = "#38bdf8";
        } else if ("FINISHED".equalsIgnoreCase(item.getAuctionStatus())) {
            priceLabel = "GIÁ CUỐI CÙNG"; priceValue = item.getFormattedPrice() + " VNĐ"; priceColor = "#f59e0b";
        }

        dialog.getChildren().addAll(
                lblTitle,
                makeInfoBox(priceLabel, priceValue, priceColor, true),
                makeInfoBox("HỌ VÀ TÊN NGƯỜI BÁN", item.getSellerName(), "#0ea5e9", false),
                makeInfoBox("ĐỊA CHỈ LIÊN HỆ", item.getSellerAddress(), "#0ea5e9", false),
                makeInfoBox("SỐ ĐIỆN THOẠI LIÊN HỆ", item.getSellerPhone(), "#0ea5e9", false),
                makeInfoBox("EMAIL LIÊN HỆ", item.getSellerEmail(), "#0ea5e9", false),
                btnClose
        );

        overlay.getChildren().add(dialog);
        ((StackPane) btnHome.getScene().getRoot()).getChildren().add(overlay);
    }

    private VBox makeInfoBox(String title, String value, String color, boolean bold) {
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 14px;");
        Label v = new Label("• " + (value != null ? value : "N/A"));
        v.setStyle(bold ? "-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;" : "-fx-text-fill: white; -fx-font-size: 13px;");
        return new VBox(4, t, v);
    }

    private void navigateTo(ActionEvent event, String path) {
        try {

            cleanupResources();

            Parent root = FXMLLoader.load(getClass().getResource(path));
            ((Node) event.getSource()).getScene().setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setActiveFilterTab(Button active, Button inactive) {
        active.setStyle("-fx-background-color: #00b894; -fx-background-radius: 32;");
        active.setTextFill(Color.WHITE);
        inactive.setStyle("-fx-background-color: transparent; -fx-background-radius: 32;");
        inactive.setTextFill(Color.web("#cccccc"));
    }

    private void setActiveTab(Button activeButton) {
        for (Button tab : new Button[]{btnHome, btnHistory, btnResults}) {
            if (tab == null) continue;
            if (tab == activeButton) {
                tab.setStyle("-fx-background-color: transparent; -fx-border-color: #00b894; -fx-border-width: 0 0 2 0;");
                tab.setTextFill(Color.web("#00b894"));
            } else {
                tab.setStyle("-fx-background-color: transparent;");
                tab.setTextFill(Color.WHITE);
            }
        }
    }
    // =========================================================================
    // POPUP THÔNG BÁO TÙY CHỈNH CHO TRẠNG THÁI BIỂN SỐ
    // =========================================================================
    private void cleanupResources() {

        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
            autoRefreshTimeline = null;
        }

        AuctionFinishService.getInstance().removeListener(this);
        cleanupAuctionListeners();
        for (VBox card : activeCards) {

            Object data = card.getUserData();

            if (data instanceof String plate) {
                // optional cleanup
            }
        }

        activeCards.clear();
    }
    private void cleanupAuctionListeners() {

        for (Map.Entry<Integer, AuctionSyncManager.AuctionListener> entry
                : auctionListeners.entrySet()) {

            AuctionSyncManager.getInstance()
                    .removeListener(entry.getKey(), entry.getValue());
        }

        auctionListeners.clear();
    }
}