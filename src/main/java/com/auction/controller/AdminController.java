package com.auction.controller;

import com.auction.database.ItemDAO;
import com.auction.database.UserDAO;
import com.auction.model.User;
import com.auction.service.ProductService;
import com.auction.util.UIUtils;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.Scene;
import com.auction.database.AuctionDAO;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class  AdminController {
    private static final Logger logger = Logger.getLogger(AdminController.class.getName());
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,###");
    private static final DateTimeFormatter DB_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DISP_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private Label  lblAdminName;
    @FXML private Label  lblTotalRequests;
    @FXML private Label  lblViewTitle;
    @FXML private Label  lblViewSubtitle;
    @FXML private HBox   tableHeaderRow;
    @FXML private VBox   adminListContainer;

    // Sidebar buttons — cần fx:id trong FXML
    @FXML private Button btnPendingTab;
    @FXML private Button btnMonitorTab;
    @FXML private Button btnUserTab;
    // ── State ─────────────────────────────────────────────────────────────────
    private final ProductService productService = new ProductService();
    private Timeline monitoringTimeline;
    private Timeline autoRefreshTimeline;
    private int lastItemCount = -1; // 👈 thêm dòng này

    /** Cặp (label cần cập nhật, thời điểm kết thúc, true = phase đấu giá / false = đăng ký) */
    private final List<Label>         cntLabels     = new ArrayList<>();
    private final List<LocalDateTime> cntEndTimes   = new ArrayList<>();
    private final List<Boolean>       cntIsAuction  = new ArrayList<>();

    private enum ViewMode { PENDING, MONITORING, USERS }
    private ViewMode currentView = ViewMode.PENDING;

    // ── Khởi tạo ──────────────────────────────────────────────────────────────

    public void setAdminInfo(User adminUser) {
        if (adminUser != null && adminUser.getFullName() != null) {
            lblAdminName.setText(adminUser.getFullName().toUpperCase());
        }
    }

    @FXML
    public void initialize() {
        stopAllTimelines();

        loadPendingView();
        if (lblAdminName == null) return;

        javafx.application.Platform.runLater(() -> {

            Stage stage =
                    (Stage) lblAdminName.getScene().getWindow();

            stage.setOnCloseRequest(event -> {

                stopAllTimelines();

                System.out.println(
                        "🛑 Admin window closed"
                );
            });
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SIDEBAR — Chuyển Tab
    // ═════════════════════════════════════════════════════════════════════════

    @FXML private void handleShowPending()  { switchView(ViewMode.PENDING); }
    @FXML private void handleShowMonitor()  { switchView(ViewMode.MONITORING); }
    @FXML private void handleShowUsers()    { switchView(ViewMode.USERS); }

    private void switchView(ViewMode mode) {
        stopAllTimelines();
        currentView = mode;
        updateSidebarStyles();
        switch (mode) {
            case PENDING    -> loadPendingView();
            case MONITORING -> loadMonitoringView();
            case USERS      -> loadUsersView();
        }
    }

    /** Highlight nút đang active trên sidebar */
    private void updateSidebarStyles() {
        String active   = "-fx-background-color: rgba(56, 189, 248, 0.15); -fx-text-fill: #38bdf8; " +
                "-fx-border-color: #38bdf8; -fx-border-width: 0 0 0 4; -fx-padding: 15; -fx-cursor: hand;";
        String inactive = "-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-padding: 15; -fx-cursor: hand;";

        if (btnPendingTab != null) btnPendingTab.setStyle(currentView == ViewMode.PENDING  ? active : inactive);
        if (btnMonitorTab != null) btnMonitorTab.setStyle(currentView == ViewMode.MONITORING? active : inactive);
        if (btnUserTab    != null) btnUserTab.setStyle(currentView    == ViewMode.USERS    ? active : inactive);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // VIEW 1 — DUYỆT XE (PENDING)
    // ═════════════════════════════════════════════════════════════════════════

    private void loadPendingView() {
        lblViewTitle.setText("DANH SÁCH CHỜ DUYỆT");
        lblViewSubtitle.setText("Các biển số vừa được đăng ký, cần xét duyệt trước khi lên sàn.");
        buildHeader(new String[]{"Biển số", "Người đăng", "Giá khởi điểm", "Loại xe", "Thao tác"},
                new double[]{150, 180, 180, 130, 150});

        adminListContainer.getChildren().clear();
        List<String[]> pendingList = productService.getPendingProducts();
        updateCounter(pendingList.size(), "yêu cầu");

        if (pendingList.isEmpty()) {
            adminListContainer.getChildren().add(emptyLabel("✅ Không có biển số nào đang chờ duyệt."));
            return;
        }

        for (String[] p : pendingList) {
            // [0]id [1]plate [2]start_price [3]vehicle_type [4]seller_name …
            String plate  = p[1];
            String owner  = p[4] != null ? p[4] : "N/A";
            String price  = formatMoney(p[2]);
            String type   = p[3] != null ? p[3] : "N/A";

            HBox row = createBaseRow();
            row.getChildren().addAll(
                    cell(plate,  150, "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15;"),
                    cell(owner,  180, "-fx-text-fill: #cbd5e1; -fx-font-size: 14;"),
                    cell(price + " đ", 180, "-fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-size: 14;"),
                    cell(type,   130, "-fx-text-fill: #94a3b8; -fx-font-size: 13;"),
                    buildApproveRejectBox(plate, row)
            );
            hoverEffect(row);
            adminListContainer.getChildren().add(row);
        }
    }

    private HBox buildApproveRejectBox(String plate, HBox parentRow) {
        Button btnApprove = iconBtn("✔ Duyệt",
                "-fx-background-color: rgba(16,185,129,0.2); -fx-text-fill: #10b981; " +
                        "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 5 10;");
        btnApprove.setOnAction(e -> handleApprove(plate, parentRow));

        Button btnReject = iconBtn("✖ Từ chối",
                "-fx-background-color: rgba(239,68,68,0.2); -fx-text-fill: #ef4444; " +
                        "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 5 10;");
        btnReject.setOnAction(e -> handleReject(plate, parentRow));

        HBox box = new HBox(8, btnApprove, btnReject);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPrefWidth(150);
        return box;
    }

    private void handleApprove(String plate, HBox rowToRemove) {
        boolean ok = productService.approveProduct(plate);
        if (!ok) {
            UIUtils.showError("Thất bại", "Không tìm thấy biển số " + plate);
            return;
        }

        // --- CHUYỂN SANG DÙNG STAGE TRONG SUỐT HOÀN TOÀN ---
        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        VBox vbox = new VBox(15);
        vbox.setAlignment(Pos.CENTER);
        vbox.setStyle("-fx-background-color: #1a2230; -fx-border-color: #00b894; -fx-border-radius: 12; -fx-background-radius: 12; -fx-border-width: 2; -fx-padding: 25;");
        vbox.setPrefWidth(380);

        Label title = new Label("CÀI ĐẶT THỜI GIAN ĐẤU GIÁ");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #00b894;");

        Label subtitle = new Label("Cấu hình cho biển số: " + plate);
        subtitle.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 14px;");

        Label lbl1 = new Label("Thời gian chờ trước khi mở phiên (Phút):");
        lbl1.setStyle("-fx-text-fill: white;");
        TextField txtRegMinutes = new TextField("60");
        txtRegMinutes.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-border-color: #475569; -fx-border-radius: 20; -fx-padding: 6 12;");

        Label lbl2 = new Label("Thời lượng diễn ra đấu giá (Phút):");
        lbl2.setStyle("-fx-text-fill: white;");
        TextField txtAucMinutes = new TextField("60");
        txtAucMinutes.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-border-color: #475569; -fx-border-radius: 20; -fx-padding: 6 12;");

        // Tạo biến flag kiểm tra bấm nút
        final boolean[] isOkPressed = {false};

        Button btnConfirm = new Button("Xác nhận");
        btnConfirm.setStyle("-fx-background-color: #00b894; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 25; -fx-background-radius: 20; -fx-cursor: hand;");
        btnConfirm.setOnAction(ev -> { isOkPressed[0] = true; stage.close(); });

        Button btnCancel = new Button("Hủy bỏ");
        btnCancel.setStyle("-fx-background-color: #475569; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 25; -fx-background-radius: 20; -fx-cursor: hand;");
        btnCancel.setOnAction(ev -> { isOkPressed[0] = false; stage.close(); });

        HBox btnBox = new HBox(15, btnCancel, btnConfirm);
        btnBox.setAlignment(Pos.CENTER);

        vbox.getChildren().addAll(title, subtitle, lbl1, txtRegMinutes, lbl2, txtAucMinutes, btnBox);

        Scene scene = new Scene(vbox);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.showAndWait();

        // Xử lý logic sau khi đóng cửa sổ Stage
        if (isOkPressed[0]) {
            try {
                int regMinutes = Integer.parseInt(txtRegMinutes.getText().trim());
                int aucMinutes = Integer.parseInt(txtAucMinutes.getText().trim());

                if (regMinutes <= 0 || aucMinutes <= 0) throw new NumberFormatException();

                boolean started = ItemDAO.startRegistrationPhase(plate, regMinutes, aucMinutes);
                if (started) {
                    adminListContainer.getChildren().remove(rowToRemove);
                    updateCounter(adminListContainer.getChildren().size(), "yêu cầu");
                    UIUtils.showSuccess("Duyệt thành công",
                            "Biển số " + plate + " đã lên sàn!\nSẽ mở phiên sau: " + regMinutes + " phút.\nThời lượng: " + aucMinutes + " phút.");
                } else {
                    UIUtils.showError("Lỗi Database", "Không thể cập nhật thời gian.");
                }
            } catch (NumberFormatException ex) {
                UIUtils.showError("Lỗi nhập liệu", "Vui lòng nhập số nguyên dương!");
            }
        }
    }

    private void handleReject(String plate, HBox rowToRemove) {
        Optional<ButtonType> confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn chắc chắn muốn từ chối biển số " + plate + "?",
                ButtonType.YES, ButtonType.NO).showAndWait();
        if (confirm.isEmpty() || confirm.get() != ButtonType.YES) return;

        if (productService.rejectProduct(plate)) {
            adminListContainer.getChildren().remove(rowToRemove);
            updateCounter(adminListContainer.getChildren().size(), "yêu cầu");
            showAlert(Alert.AlertType.INFORMATION, "Đã từ chối", "Biển số " + plate + " bị từ chối.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Thất bại", "Không tìm thấy biển số " + plate);
        }
    }
    @FXML
    private void handleClearFinished() {
        List<String[]> activeItems = ItemDAO.getAllApprovedActiveItems();

        if (activeItems.isEmpty()) {
            UIUtils.showInfo("Thông báo", "Hiện tại không có biển số nào trên Dashboard.");
            return;
        }

        // --- CHUYỂN SANG DÙNG STAGE TRONG SUỐT HOÀN TOÀN ---
        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        VBox mainVbox = new VBox(15);
        mainVbox.setAlignment(Pos.CENTER);
        mainVbox.setStyle("-fx-background-color: #1a2230; -fx-border-color: #38bdf8; -fx-border-radius: 12; -fx-background-radius: 12; -fx-border-width: 2; -fx-padding: 25;");
        mainVbox.setPrefWidth(420);

        Label title = new Label("QUẢN LÝ DANH SÁCH DASHBOARD");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        Label subtitle = new Label("Tích chọn những biển số bạn muốn XÓA/ẨN khỏi Dashboard:");
        subtitle.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

        VBox checklistContainer = new VBox(12);
        checklistContainer.setStyle("-fx-background-color: #1a2230; -fx-padding: 10;");

        List<CheckBox> listCheckBoxes = new ArrayList<>();
        for (String[] item : activeItems) {
            String plate = item[1];
            String status = item[13];
            String displayInfo = String.format("%-15s [%s]", plate, status);

            CheckBox cb = new CheckBox(displayInfo);
            cb.setUserData(plate);
            cb.setStyle("-fx-text-fill: white; -fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-font-size: 14px;");

            checklistContainer.getChildren().add(cb);
            listCheckBoxes.add(cb);
        }

        ScrollPane scrollPane = new ScrollPane(checklistContainer);
        scrollPane.setPrefSize(380, 220);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #1a2230; -fx-background: #1a2230; -fx-border-color: #334155;");

        final boolean[] isOkPressed = {false};

        Button btnConfirm = new Button("Xác nhận ẩn");
        btnConfirm.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 25; -fx-background-radius: 20; -fx-cursor: hand;");
        btnConfirm.setOnAction(ev -> { isOkPressed[0] = true; stage.close(); });

        Button btnCancel = new Button("Quay lại");
        btnCancel.setStyle("-fx-background-color: #475569; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 25; -fx-background-radius: 20; -fx-cursor: hand;");
        btnCancel.setOnAction(ev -> { isOkPressed[0] = false; stage.close(); });

        HBox btnBox = new HBox(15, btnCancel, btnConfirm);
        btnBox.setAlignment(Pos.CENTER);

        mainVbox.getChildren().addAll(title, subtitle, scrollPane, btnBox);

        Scene scene = new Scene(mainVbox);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.showAndWait();

        // Xử lý logic sau khi đóng Stage
        if (isOkPressed[0]) {
            List<String> selectedPlates = new ArrayList<>();
            for (CheckBox cb : listCheckBoxes) {
                if (cb.isSelected()) {
                    selectedPlates.add(cb.getUserData().toString());
                }
            }

            if (selectedPlates.isEmpty()) {
                UIUtils.showError("Thông báo", "Bạn chưa tích chọn biển số nào cả!");
                return;
            }

            boolean success = ItemDAO.hideSelectedItems(selectedPlates);
            if (success) {
                UIUtils.showSuccess("Thành công", "Đã xóa ẩn thành công " + selectedPlates.size() + " biển số khỏi Dashboard!");
                if (currentView == ViewMode.MONITORING) loadMonitoringView();
            } else {
                UIUtils.showError("Lỗi", "Không thể cập nhật dữ liệu. Vui lòng thử lại.");
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // VIEW 2 — GIÁM SÁT ĐẤU GIÁ (MONITORING)
    // ═════════════════════════════════════════════════════════════════════════

    private void loadMonitoringView() {
        stopAllTimelines();
        lblViewTitle.setText("GIÁM SÁT ĐẤU GIÁ");
        lblViewSubtitle.setText("Theo dõi realtime các phiên đấu giá đang mở.");

        buildHeader(
                new String[]{"ID", "Item ID", "Seller", "Winner", "Bắt đầu", "Kết thúc", "Giá cuối", "Trạng thái"},
                new double[]{80, 100, 100, 100, 170, 170, 120, 120}
        );

        adminListContainer.getChildren().clear();
        cntLabels.clear();
        cntEndTimes.clear();
        cntIsAuction.clear();

        List<String[]> items = AuctionDAO.getOpenAuctions();
        updateCounter(items.size(), "phiên đang diễn ra");

        if (items.isEmpty()) {
            adminListContainer.getChildren().add(emptyLabel("📭 Không có phiên đấu giá nào đang mở."));
            return;
        }

        for (String[] item : items) {

            String auctionId = item[0];
            String itemId    = item[1];
            String sellerId  = item[2];
            String winnerId  = item[3];
            String startTime = item[4];
            String endTime   = item[5];
            String finalPrice = item[6];
            boolean isOpen   = "1".equals(item[7]);

            String statusLabel = isOpen ? "🟢 OPEN" : "🔴 CLOSED";
            String statusStyle = isOpen
                    ? "-fx-text-fill: #10b981; -fx-font-weight: bold;"
                    : "-fx-text-fill: #ef4444; -fx-font-weight: bold;";

            Label lbl = new Label(isOpen ? "Đang chạy..." : "Đã đóng");
            lbl.setPrefWidth(170);
            lbl.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");

            HBox row = createBaseRow();
            row.setUserData(auctionId);

            row.getChildren().addAll(
                    cell(auctionId, 80, "-fx-text-fill:white;"),
                    cell(itemId, 100, "-fx-text-fill:#94a3b8;"),
                    cell(sellerId, 100, "-fx-text-fill:#94a3b8;"),
                    cell(winnerId, 100, "-fx-text-fill:#94a3b8;"),
                    cell(startTime, 170, "-fx-text-fill:#94a3b8;"),
                    cell(endTime, 170, "-fx-text-fill:#94a3b8;"),
                    cell(finalPrice, 120, "-fx-text-fill:#10b981;"),
                    cell(statusLabel, 120, statusStyle)
            );

            hoverEffect(row);
            adminListContainer.getChildren().add(row);
        }

        startMonitoringTimeline();

        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
            autoRefreshTimeline = null;
        }

        startAutoRefresh();
    }

    private void startMonitoringTimeline() {
        monitoringTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> {
            LocalDateTime now = LocalDateTime.now();
            for (int i = 0; i < cntLabels.size(); i++) {
                if (i >= cntEndTimes.size()) break;
                LocalDateTime end = cntEndTimes.get(i);
                boolean isAuction = i < cntIsAuction.size() && cntIsAuction.get(i);

                long totalSecs = java.time.Duration.between(now, end).getSeconds();
                Label lbl = cntLabels.get(i);

                if (totalSecs <= 0) {
                    lbl.setText("⏰ Đã kết thúc");
                    lbl.setStyle("-fx-text-fill: #64748b; -fx-font-weight: bold; -fx-font-size: 13;");
                } else {
                    long h = totalSecs / 3600;
                    long m = (totalSecs % 3600) / 60;
                    long s = totalSecs % 60;
                    String phase = isAuction ? "⚡ " : "📝 ";
                    lbl.setText(phase + String.format("%02d:%02d:%02d", h, m, s));
                    // Cảnh báo đỏ nếu còn dưới 10 phút
                    if (totalSecs < 600) {
                        lbl.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 14;");
                    } else {
                        lbl.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-size: 14;");
                    }
                }
            }
        }));
        monitoringTimeline.setCycleCount(Timeline.INDEFINITE);
        monitoringTimeline.play();
    }


    // ═════════════════════════════════════════════════════════════════════════
    // VIEW 3 — QUẢN LÝ USER
    // ═════════════════════════════════════════════════════════════════════════

    private void loadUsersView() {
        lblViewTitle.setText("QUẢN LÝ TÀI KHOẢN");
        lblViewSubtitle.setText("Xem thông tin và xóa các tài khoản vi phạm hoặc không xác thực.");
        buildHeader(
                new String[]{"ID", "Tên đăng nhập", "Họ và tên", "Email", "Điện thoại", "Loại", "Ngày tạo", "Thao tác"},
                new double[]{45, 120, 150, 175, 110, 70, 130, 90}
        );

        adminListContainer.getChildren().clear();
        List<String[]> users = UserDAO.getAllUsersForAdmin();
        updateCounter(users.size(), "tài khoản");

        if (users.isEmpty()) {
            adminListContainer.getChildren().add(emptyLabel("👤 Chưa có tài khoản nào ngoài Admin."));
            return;
        }

        for (String[] u : users) {
            // [0]id [1]username [2]full_name [3]email [4]phone [5]address [6]user_type [7]created_at
            int    uid      = Integer.parseInt(u[0]);
            String username = u[1] != null ? u[1] : "";
            String fullName = u[2] != null ? u[2] : "N/A";
            String email    = u[3] != null ? u[3] : "N/A";
            String phone    = u[4] != null ? u[4] : "N/A";
            String type     = u[6] != null ? u[6] : "N/A";
            String created  = formatDate(u[7]);

            String typeStyle = "Seller".equalsIgnoreCase(type)
                    ? "-fx-text-fill: #38bdf8; -fx-font-size: 12;"
                    : "-fx-text-fill: #a78bfa; -fx-font-size: 12;";

            HBox row = createBaseRow();
            row.getChildren().addAll(
                    cell(u[0],    45,  "-fx-text-fill: #64748b; -fx-font-size: 12;"),
                    cell(username,120, "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13;"),
                    cell(fullName,150, "-fx-text-fill: #cbd5e1; -fx-font-size: 13;"),
                    cell(email,   175, "-fx-text-fill: #94a3b8; -fx-font-size: 12;"),
                    cell(phone,   110, "-fx-text-fill: #94a3b8; -fx-font-size: 12;"),
                    cell(type,    70,  typeStyle),
                    cell(created, 130, "-fx-text-fill: #64748b; -fx-font-size: 12;"),
                    buildDeleteBox(uid, username, row)
            );
            hoverEffect(row);
            adminListContainer.getChildren().add(row);
        }
    }

    private HBox buildDeleteBox(int userId, String username, HBox parentRow) {
        Button btnDel = iconBtn("🗑 Xóa",
                "-fx-background-color: rgba(239,68,68,0.15); -fx-text-fill: #ef4444; " +
                        "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 5 10; -fx-font-size: 12;");
        btnDel.setOnAction(e -> handleDeleteUser(userId, username, parentRow));

        HBox box = new HBox(btnDel);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPrefWidth(90);
        return box;
    }

    private void handleDeleteUser(int userId, String username, HBox rowToRemove) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa tài khoản");
        confirm.setHeaderText("Xóa tài khoản \"" + username + "\"?");
        confirm.setContentText("Hành động này không thể hoàn tác.\n" +
                "Toàn bộ lịch sử đặt giá của tài khoản này cũng sẽ bị xóa.");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) return;

        boolean ok = UserDAO.deleteUserById(userId);
        if (ok) {
            adminListContainer.getChildren().remove(rowToRemove);
            updateCounter(adminListContainer.getChildren().size(), "tài khoản");
            showAlert(Alert.AlertType.INFORMATION, "Đã xóa",
                    "Tài khoản \"" + username + "\" đã bị xóa khỏi hệ thống.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Thất bại",
                    "Không thể xóa tài khoản này. Vui lòng thử lại.");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HÀM TIỆN ÍCH — UI Builder
    // ═════════════════════════════════════════════════════════════════════════

    /** Xây dựng hàng header động trong bảng */
    private void buildHeader(String[] titles, double[] widths) {
        if (tableHeaderRow == null) return;
        tableHeaderRow.getChildren().clear();
        for (int i = 0; i < titles.length; i++) {
            Label lbl = new Label(titles[i]);
            lbl.setPrefWidth(widths[i]);
            lbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-font-size: 13;");
            tableHeaderRow.getChildren().add(lbl);
        }
    }

    /** Tạo một hàng dữ liệu cơ bản (chưa có children) */
    private HBox createBaseRow() {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: transparent; -fx-padding: 12 20; " +
                "-fx-border-color: #334155; -fx-border-width: 0 0 1 0;");
        return row;
    }

    /** Tạo Label ô dữ liệu với độ rộng cố định */
    private Label cell(String text, double width, String style) {
        Label lbl = new Label(text != null ? text : "N/A");
        lbl.setPrefWidth(width);
        lbl.setStyle(style);
        lbl.setWrapText(false);
        lbl.setMaxWidth(width);
        return lbl;
    }

    /** Tạo Button nhỏ */
    private Button iconBtn(String text, String style) {
        Button btn = new Button(text);
        btn.setStyle(style);
        return btn;
    }

    /** Hiệu ứng hover cho hàng */
    private void hoverEffect(HBox row) {
        String base  = "-fx-background-color: transparent; -fx-padding: 12 20; " +
                "-fx-border-color: #334155; -fx-border-width: 0 0 1 0;";
        String hover = "-fx-background-color: rgba(255,255,255,0.04); -fx-padding: 12 20; " +
                "-fx-border-color: #334155; -fx-border-width: 0 0 1 0;";
        row.setOnMouseEntered(e -> row.setStyle(hover));
        row.setOnMouseExited(e  -> row.setStyle(base));
    }

    /** Nhãn hiển thị khi danh sách trống */
    private Label emptyLabel(String msg) {
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 15; -fx-padding: 30;");
        return lbl;
    }

    /** Cập nhật nhãn số lượng ở góc phải tiêu đề */
    private void updateCounter(int count, String unit) {
        if (lblTotalRequests != null)
            lblTotalRequests.setText("Tổng cộng: " + count + " " + unit);
    }

    /** Format số tiền */
    private String formatMoney(String raw) {
        try { return MONEY_FMT.format(Double.parseDouble(raw)); }
        catch (Exception e) { return raw != null ? raw : "0"; }
    }

    /** Format ngày giờ từ DB */
    private String formatDate(String raw) {
        if (raw == null) return "N/A";
        try {
            String cleaned = raw.replace("T", " ");
            if (cleaned.contains(".")) cleaned = cleaned.substring(0, cleaned.indexOf("."));
            return LocalDateTime.parse(cleaned, DB_FMT).format(DISP_FMT);
        } catch (Exception e) { return raw; }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ĐIỀU HƯỚNG & THÔNG BÁO
    // ═════════════════════════════════════════════════════════════════════════

    @FXML
    private void handleLogout(ActionEvent event) {
        stopAllTimelines();
        try {
            Parent newRoot = FXMLLoader.load(
                    Objects.requireNonNull(getClass().getResource("/fxml/Login.fxml")));
            ((Node) event.getSource()).getScene().setRoot(newRoot);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Lỗi khi tải màn hình Login", e);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void startAutoRefresh() {

        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }

        autoRefreshTimeline = new Timeline(

                new KeyFrame(javafx.util.Duration.seconds(10), e -> {

                    if (currentView == ViewMode.MONITORING) {

                        refreshMonitoringData();
                    }
                })
        );

        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);

        autoRefreshTimeline.play();
    }
    private void refreshMonitoringData() {

        List<String[]> items = AuctionDAO.getOpenAuctions();

        updateCounter(items.size(), "phiên đang diễn ra");

        // so sánh theo danh sách ID thay vì size
        List<String> newIds = items.stream()
                .map(i -> i[0])
                .toList();

        List<String> oldIds = adminListContainer.getChildren().stream()
                .filter(node -> node instanceof HBox)
                .map(node -> node.getUserData())
                .filter(Objects::nonNull)
                .map(Object::toString)
                .toList();

        if (newIds.equals(oldIds)) {
            return; // không đổi gì → khỏi reload
        }

        // ✅ DIFF UPDATE (không rebuild toàn bộ - FIX flicker)

        // 1️⃣ Xóa các row đã kết thúc
        List<String> toRemove = oldIds.stream()
                .filter(id -> !newIds.contains(id))
                .toList();

        if (!toRemove.isEmpty()) {
            adminListContainer.getChildren().removeIf(node ->
                    node instanceof HBox && toRemove.contains(node.getUserData().toString())
            );
            System.out.println("✅ Removed " + toRemove.size() + " finished auctions");
        }

        // 2️⃣ Thêm items mới (chỉ những item không có trong oldIds)
        for (String[] item : items) {
            String auctionId = item[0];

            // Nếu item này đã có trong oldIds → skip (không add lại, keep countdown running)
            if (oldIds.contains(auctionId)) {
                continue;
            }

            // Item mới → tạo row và add vào
            String itemId    = item[1];
            String sellerId  = item[2];
            String winnerId  = item[3];
            String startTime = item[4];
            String endTime   = item[5];
            String finalPrice = item[6];
            boolean isOpen   = "1".equals(item[7]);

            String statusLabel = isOpen ? "🟢 OPEN" : "🔴 CLOSED";
            String statusStyle = isOpen
                    ? "-fx-text-fill: #10b981; -fx-font-weight: bold;"
                    : "-fx-text-fill: #ef4444; -fx-font-weight: bold;";

            HBox row = createBaseRow();
            row.setUserData(auctionId);

            row.getChildren().addAll(
                    cell(auctionId, 80, "-fx-text-fill:white;"),
                    cell(itemId, 100, "-fx-text-fill:#94a3b8;"),
                    cell(sellerId, 100, "-fx-text-fill:#94a3b8;"),
                    cell(winnerId, 100, "-fx-text-fill:#94a3b8;"),
                    cell(startTime, 170, "-fx-text-fill:#94a3b8;"),
                    cell(endTime, 170, "-fx-text-fill:#94a3b8;"),
                    cell(finalPrice, 120, "-fx-text-fill:#10b981;"),
                    cell(statusLabel, 120, statusStyle)
            );

            hoverEffect(row);
            adminListContainer.getChildren().add(row);
            System.out.println("✅ Added new auction: " + auctionId);
        }

        lastItemCount = items.size();
    }
    private void stopAllTimelines() {

        if (monitoringTimeline != null) {
            monitoringTimeline.stop();
            monitoringTimeline = null;
            System.out.println("🛑 Monitoring timeline stopped");
        }

        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
            autoRefreshTimeline = null;
            System.out.println("🛑 Auto refresh stopped");
        }
    }
    // =========================================================================
    // POPUP XÁC NHẬN (Cửa sổ tàng hình 100%)
    // =========================================================================
    public static boolean showConfirm(String title, String message) {
        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        VBox content = new VBox(15);
        content.setAlignment(javafx.geometry.Pos.CENTER);
        content.setStyle("-fx-background-color: #1a2230; -fx-border-color: #f59e0b; -fx-border-radius: 15; -fx-background-radius: 15; -fx-border-width: 2; -fx-padding: 30;");
        content.setPrefWidth(380);

        Label lblEmoji = new Label("❓");
        lblEmoji.setStyle("-fx-font-size: 55px;");

        Label lblTitle = new Label(title.toUpperCase());
        lblTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");

        Label lblMessage = new Label(message);
        lblMessage.setStyle("-fx-font-size: 15px; -fx-text-fill: #cbd5e1; -fx-text-alignment: center;");
        lblMessage.setWrapText(true);

        final boolean[] result = {false};

        javafx.scene.control.Button btnYes = new javafx.scene.control.Button("Đồng ý");
        btnYes.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 25; -fx-background-radius: 20; -fx-cursor: hand;");
        btnYes.setOnAction(e -> { result[0] = true; stage.close(); });

        javafx.scene.control.Button btnNo = new javafx.scene.control.Button("Hủy");
        btnNo.setStyle("-fx-background-color: #475569; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 25; -fx-background-radius: 20; -fx-cursor: hand;");
        btnNo.setOnAction(e -> { result[0] = false; stage.close(); });

        javafx.scene.layout.HBox btnBox = new javafx.scene.layout.HBox(15, btnNo, btnYes);
        btnBox.setAlignment(javafx.geometry.Pos.CENTER);

        content.getChildren().addAll(lblEmoji, lblTitle, lblMessage, btnBox);
        javafx.scene.Scene scene = new javafx.scene.Scene(content);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.showAndWait();

        return result[0];
    }
}