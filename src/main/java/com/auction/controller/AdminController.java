package com.auction.controller;

import com.auction.database.ItemDAO;
import com.auction.database.UserDAO;
import com.auction.model.User;
import com.auction.service.ProductService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdminController {

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

    /** Timeline đếm ngược realtime cho màn hình Giám Sát */
    private Timeline monitoringTimeline;
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
        loadPendingView();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SIDEBAR — Chuyển Tab
    // ═════════════════════════════════════════════════════════════════════════

    @FXML private void handleShowPending()  { switchView(ViewMode.PENDING); }
    @FXML private void handleShowMonitor()  { switchView(ViewMode.MONITORING); }
    @FXML private void handleShowUsers()    { switchView(ViewMode.USERS); }

    private void switchView(ViewMode mode) {
        stopMonitoringTimeline();
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
        if (!ok) { showAlert(Alert.AlertType.ERROR, "Thất bại", "Không tìm thấy biển số " + plate); return; }

        TextInputDialog dlg = new TextInputDialog("60");
        dlg.setTitle("Cấu hình thời gian đăng ký");
        dlg.setHeaderText("Biển số " + plate + " đã được duyệt!");
        dlg.setContentText("Nhập thời gian đăng ký tham gia (phút):");
        Optional<String> res = dlg.showAndWait();

        int minutes = 60;
        if (res.isPresent()) {
            try {
                minutes = Integer.parseInt(res.get().trim());
                if (minutes <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng nhập số dương hợp lệ!"); return;
            }
        }

        boolean started = ItemDAO.startRegistrationPhase(plate, minutes);
        if (started) {
            adminListContainer.getChildren().remove(rowToRemove);
            updateCounter(adminListContainer.getChildren().size(), "yêu cầu");
            showAlert(Alert.AlertType.INFORMATION, "Duyệt thành công",
                    "Biển số " + plate + " đã lên sàn!\n" +
                            "Giai đoạn đăng ký: " + minutes + " phút\n" +
                            "Giai đoạn đấu giá: 60 phút (sau khi đăng ký kết thúc)");
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể bắt đầu giai đoạn đăng ký.");
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

    // ═════════════════════════════════════════════════════════════════════════
    // VIEW 2 — GIÁM SÁT ĐẤU GIÁ (MONITORING)
    // ═════════════════════════════════════════════════════════════════════════

    private void loadMonitoringView() {
        lblViewTitle.setText("GIÁM SÁT ĐẤU GIÁ");
        lblViewSubtitle.setText("Theo dõi realtime các biển số đang trong giai đoạn đăng ký / đấu giá.");
        buildHeader(
                new String[]{"Biển số", "Loại xe", "Trạng thái", "Giá cao nhất", "Thời gian còn lại", "Người tham gia"},
                new double[]{130, 100, 130, 175, 185, 120}
        );

        adminListContainer.getChildren().clear();
        cntLabels.clear();
        cntEndTimes.clear();
        cntIsAuction.clear();

        List<String[]> items = ItemDAO.getAuctionsForMonitoring();
        updateCounter(items.size(), "phiên đang diễn ra");

        if (items.isEmpty()) {
            adminListContainer.getChildren().add(emptyLabel("📭 Hiện không có phiên đấu giá nào đang diễn ra."));
            return;
        }

        for (String[] item : items) {
            // [0]plate [1]type [2]status [3]price [4]reg_end [5]auc_end [6]participants
            String plate    = item[0];
            String type     = item[1] != null ? item[1] : "N/A";
            String status   = item[2];
            String price    = formatMoney(item[3]) + " đ";
            String endStr   = "REGISTRATION".equals(status) ? item[4] : item[5];
            String parts    = item[6];

            boolean isAuction = "AUCTION".equals(status);
            String statusLabel = isAuction ? "⚡ Đấu giá" : "📝 Đăng ký";
            String statusStyle = isAuction
                    ? "-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 13;"
                    : "-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 13;";

            Label lblCountdown = new Label("Đang tính...");
            lblCountdown.setPrefWidth(185);
            lblCountdown.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-size: 14;");

            // Lưu để Timeline cập nhật
            cntLabels.add(lblCountdown);
            cntIsAuction.add(isAuction);
            try {
                String cleaned = endStr != null ? endStr.replace("T", " ") : null;
                if (cleaned != null && cleaned.contains(".")) cleaned = cleaned.substring(0, cleaned.indexOf("."));
                cntEndTimes.add(cleaned != null ? LocalDateTime.parse(cleaned, DB_FMT) : LocalDateTime.now());
            } catch (Exception ex) {
                cntEndTimes.add(LocalDateTime.now());
            }

            HBox row = createBaseRow();
            row.getChildren().addAll(
                    cell(plate,    130, "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15;"),
                    cell(type,     100, "-fx-text-fill: #94a3b8; -fx-font-size: 13;"),
                    cell(statusLabel, 130, statusStyle),
                    cell(price,    175, "-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 14;"),
                    lblCountdown,
                    cell(parts + " người", 120, "-fx-text-fill: #cbd5e1; -fx-font-size: 13;")
            );
            hoverEffect(row);
            adminListContainer.getChildren().add(row);
        }

        startMonitoringTimeline();
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

    private void stopMonitoringTimeline() {
        if (monitoringTimeline != null) {
            monitoringTimeline.stop();
            monitoringTimeline = null;
        }
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
        stopMonitoringTimeline();
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
}