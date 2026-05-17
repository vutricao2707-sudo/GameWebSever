package com.auction.controller;

import com.auction.database.AuctionDAO;
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
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AdminController {

    private static final Logger logger =
            Logger.getLogger(AdminController.class.getName());

    private static final DecimalFormat MONEY_FMT =
            new DecimalFormat("#,###");

    private static final DateTimeFormatter DB_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter DISP_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ================= FXML =================

    @FXML private Label lblAdminName;
    @FXML private Label lblTotalRequests;
    @FXML private Label lblViewTitle;
    @FXML private Label lblViewSubtitle;

    @FXML private HBox tableHeaderRow;
    @FXML private VBox adminListContainer;

    @FXML private Button btnPendingTab;
    @FXML private Button btnMonitorTab;
    @FXML private Button btnUserTab;

    // ================= STATE =================

    private final ProductService productService =
            new ProductService();

    private Timeline monitoringTimeline;
    private Timeline autoRefreshTimeline;

    private int lastItemCount = -1;

    private final List<Label> cntLabels = new ArrayList<>();
    private final List<LocalDateTime> cntEndTimes = new ArrayList<>();
    private final List<Boolean> cntIsAuction = new ArrayList<>();

    private enum ViewMode {
        PENDING,
        MONITORING,
        USERS
    }

    private ViewMode currentView = ViewMode.PENDING;

    // ================= INIT =================

    @FXML
    public void initialize() {

        stopAllTimelines();

        loadPendingView();

        if (lblAdminName == null) {
            System.out.println("FXML chưa inject lblAdminName");
            return;
        }

        javafx.application.Platform.runLater(() -> {

            if (lblAdminName.getScene() == null) {
                return;
            }

            Stage stage =
                    (Stage) lblAdminName.getScene().getWindow();

            if (stage != null) {

                stage.setOnCloseRequest(event -> {

                    stopAllTimelines();

                    System.out.println(
                            "🛑 Admin window closed"
                    );
                });
            }
        });
    }

    // ================= ADMIN INFO =================

    public void setAdminInfo(User adminUser) {

        if (adminUser != null &&
                adminUser.getFullName() != null) {

            lblAdminName.setText(
                    adminUser.getFullName().toUpperCase()
            );
        }
    }

    // ================= SIDEBAR =================

    @FXML
    private void handleShowPending() {
        switchView(ViewMode.PENDING);
    }

    @FXML
    private void handleShowMonitor() {
        switchView(ViewMode.MONITORING);
    }

    @FXML
    private void handleShowUsers() {
        switchView(ViewMode.USERS);
    }

    private void switchView(ViewMode mode) {

        stopAllTimelines();

        currentView = mode;

        updateSidebarStyles();

        switch (mode) {

            case PENDING:
                loadPendingView();
                break;

            case MONITORING:
                loadMonitoringView();
                break;

            case USERS:
                loadUsersView();
                break;
        }
    }

    private void updateSidebarStyles() {

        String active =
                "-fx-background-color: rgba(56,189,248,0.15);" +
                        "-fx-text-fill: #38bdf8;" +
                        "-fx-border-color: #38bdf8;" +
                        "-fx-border-width: 0 0 0 4;" +
                        "-fx-padding: 15;" +
                        "-fx-cursor: hand;";

        String inactive =
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #94a3b8;" +
                        "-fx-padding: 15;" +
                        "-fx-cursor: hand;";

        if (btnPendingTab != null) {
            btnPendingTab.setStyle(
                    currentView == ViewMode.PENDING
                            ? active : inactive
            );
        }

        if (btnMonitorTab != null) {
            btnMonitorTab.setStyle(
                    currentView == ViewMode.MONITORING
                            ? active : inactive
            );
        }

        if (btnUserTab != null) {
            btnUserTab.setStyle(
                    currentView == ViewMode.USERS
                            ? active : inactive
            );
        }
    }

    // ================= PENDING VIEW =================

    private void loadPendingView() {

        lblViewTitle.setText("DANH SÁCH CHỜ DUYỆT");

        lblViewSubtitle.setText(
                "Các biển số vừa được đăng ký."
        );

        adminListContainer.getChildren().clear();

        List<String[]> pendingList =
                productService.getPendingProducts();

        updateCounter(
                pendingList.size(),
                "yêu cầu"
        );

        if (pendingList.isEmpty()) {

            adminListContainer.getChildren().add(
                    emptyLabel(
                            "✅ Không có biển số nào đang chờ duyệt."
                    )
            );

            return;
        }

        for (String[] p : pendingList) {

            String plate = p[1];
            String owner = p[4];
            String price = formatMoney(p[2]);
            String type = p[3];

            HBox row = createBaseRow();

            row.getChildren().addAll(

                    cell(plate,150,
                            "-fx-text-fill:white;"),

                    cell(owner,180,
                            "-fx-text-fill:#cbd5e1;"),

                    cell(price + " đ",180,
                            "-fx-text-fill:#f59e0b;"),

                    cell(type,130,
                            "-fx-text-fill:#94a3b8;")
            );

            hoverEffect(row);

            adminListContainer.getChildren().add(row);
        }
    }

    // ================= MONITOR VIEW =================

    private void loadMonitoringView() {

        stopAllTimelines();

        lblViewTitle.setText("GIÁM SÁT ĐẤU GIÁ");

        adminListContainer.getChildren().clear();

        List<String[]> items =
                AuctionDAO.getOpenAuctions();

        updateCounter(
                items.size(),
                "phiên đang diễn ra"
        );

        if (items.isEmpty()) {

            adminListContainer.getChildren().add(
                    emptyLabel(
                            "📭 Không có phiên đấu giá nào."
                    )
            );

            return;
        }

        for (String[] item : items) {

            String auctionId = item[0];

            HBox row = createBaseRow();

            row.setUserData(auctionId);

            row.getChildren().add(
                    cell(
                            auctionId,
                            120,
                            "-fx-text-fill:white;"
                    )
            );

            hoverEffect(row);

            adminListContainer.getChildren().add(row);
        }

        startMonitoringTimeline();

        startAutoRefresh();
    }

    // ================= USERS VIEW =================

    private void loadUsersView() {

        lblViewTitle.setText("QUẢN LÝ USER");

        adminListContainer.getChildren().clear();

        List<String[]> users =
                UserDAO.getAllUsersForAdmin();

        updateCounter(users.size(), "tài khoản");

        if (users.isEmpty()) {

            adminListContainer.getChildren().add(
                    emptyLabel("Không có user.")
            );

            return;
        }

        for (String[] u : users) {

            HBox row = createBaseRow();

            row.getChildren().add(
                    cell(
                            u[1],
                            150,
                            "-fx-text-fill:white;"
                    )
            );

            hoverEffect(row);

            adminListContainer.getChildren().add(row);
        }
    }

    // ================= AUTO REFRESH =================

    private void startAutoRefresh() {

        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }

        autoRefreshTimeline = new Timeline(

                new KeyFrame(
                        Duration.seconds(10),

                        e -> {

                            if (currentView ==
                                    ViewMode.MONITORING) {

                                refreshMonitoringData();
                            }
                        }
                )
        );

        autoRefreshTimeline.setCycleCount(
                Timeline.INDEFINITE
        );

        autoRefreshTimeline.play();
    }

    private void refreshMonitoringData() {

        List<String[]> items =
                AuctionDAO.getOpenAuctions();

        updateCounter(
                items.size(),
                "phiên đang diễn ra"
        );

        List<String> newIds = items.stream()

                .map(i -> i[0])

                .collect(Collectors.toList());

        List<String> oldIds =
                adminListContainer.getChildren()

                        .stream()

                        .filter(node -> node instanceof HBox)

                        .map(Node::getUserData)

                        .filter(Objects::nonNull)

                        .map(Object::toString)

                        .collect(Collectors.toList());

        if (newIds.equals(oldIds)) {
            return;
        }

        lastItemCount = items.size();

        loadMonitoringView();
    }

    // ================= TIMELINE =================

    private void startMonitoringTimeline() {

        monitoringTimeline = new Timeline(

                new KeyFrame(
                        Duration.seconds(1),

                        e -> {

                            LocalDateTime now =
                                    LocalDateTime.now();

                            for (int i = 0;
                                 i < cntLabels.size();
                                 i++) {

                                if (i >= cntEndTimes.size()) {
                                    break;
                                }

                                long totalSecs =
                                        java.time.Duration
                                                .between(
                                                        now,
                                                        cntEndTimes.get(i)
                                                )
                                                .getSeconds();

                                Label lbl = cntLabels.get(i);

                                if (totalSecs <= 0) {

                                    lbl.setText(
                                            "⏰ Đã kết thúc"
                                    );

                                } else {

                                    long h = totalSecs / 3600;
                                    long m = (totalSecs % 3600) / 60;
                                    long s = totalSecs % 60;

                                    lbl.setText(
                                            String.format(
                                                    "%02d:%02d:%02d",
                                                    h,m,s
                                            )
                                    );
                                }
                            }
                        }
                )
        );

        monitoringTimeline.setCycleCount(
                Timeline.INDEFINITE
        );

        monitoringTimeline.play();
    }

    private void stopAllTimelines() {

        if (monitoringTimeline != null) {

            monitoringTimeline.stop();

            monitoringTimeline = null;
        }

        if (autoRefreshTimeline != null) {

            autoRefreshTimeline.stop();

            autoRefreshTimeline = null;
        }
    }

    // ================= UI =================

    private HBox createBaseRow() {

        HBox row = new HBox();

        row.setAlignment(Pos.CENTER_LEFT);

        row.setStyle(
                "-fx-padding:12 20;" +
                        "-fx-border-color:#334155;" +
                        "-fx-border-width:0 0 1 0;"
        );

        return row;
    }

    private Label cell(
            String text,
            double width,
            String style
    ) {

        Label lbl = new Label(
                text != null ? text : "N/A"
        );

        lbl.setPrefWidth(width);

        lbl.setStyle(style);

        return lbl;
    }

    private void hoverEffect(HBox row) {

        String base =
                "-fx-padding:12 20;" +
                        "-fx-border-color:#334155;" +
                        "-fx-border-width:0 0 1 0;";

        String hover =
                "-fx-background-color:rgba(255,255,255,0.04);" +
                        "-fx-padding:12 20;" +
                        "-fx-border-color:#334155;" +
                        "-fx-border-width:0 0 1 0;";

        row.setOnMouseEntered(
                e -> row.setStyle(hover)
        );

        row.setOnMouseExited(
                e -> row.setStyle(base)
        );
    }

    private Label emptyLabel(String msg) {

        Label lbl = new Label(msg);

        lbl.setStyle(
                "-fx-text-fill:#64748b;" +
                        "-fx-font-size:15;" +
                        "-fx-padding:30;"
        );

        return lbl;
    }

    private void updateCounter(int count, String unit) {

        if (lblTotalRequests != null) {

            lblTotalRequests.setText(
                    "Tổng cộng: " + count + " " + unit
            );
        }
    }

    private String formatMoney(String raw) {

        try {

            return MONEY_FMT.format(
                    Double.parseDouble(raw)
            );

        } catch (Exception e) {

            return "0";
        }
    }

    // ================= LOGOUT =================

    @FXML
    private void handleLogout(ActionEvent event) {

        stopAllTimelines();

        try {

            Parent newRoot = FXMLLoader.load(
                    Objects.requireNonNull(
                            getClass().getResource(
                                    "/fxml/Login.fxml"
                            )
                    )
            );

            Stage stage =
                    (Stage) ((Node) event.getSource())
                            .getScene()
                            .getWindow();

            Scene scene = new Scene(newRoot);

            stage.setScene(scene);

            stage.show();

        } catch (IOException e) {

            logger.log(
                    Level.SEVERE,
                    "Lỗi khi tải Login",
                    e
            );
        }
    }
}