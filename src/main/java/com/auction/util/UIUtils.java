package com.auction.util;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class UIUtils {

    // ─── Màu sắc chủ đạo (Dark Mode) ─────────────────────────────────────────
    private static final String BG_DARK        = "#1a2230";
    private static final String TEXT_SECONDARY = "#cbd5e1";
    private static final String BORDER_RADIUS  = "15";

    private UIUtils() {}

    // =========================================================================
    // CÁC HÀM TIỆN ÍCH GỌI NHANH
    // =========================================================================

    public static void showError(String title, String message) {
        showCustomPopup(title, message, "⚠️", "#ef4444");
    }

    public static void showSuccess(String title, String message) {
        showCustomPopup(title, message, "✅", "#00b894");
    }

    public static void showInfo(String title, String message) {
        showCustomPopup(title, message, "ℹ️", "#38bdf8");
    }

    // =========================================================================
    // POPUP THÔNG BÁO TÙY CHỈNH (Cửa sổ tàng hình 100%)
    // =========================================================================

    public static void showCustomPopup(String title, String message, String emoji, String themeColor) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showCustomPopup(title, message, emoji, themeColor));
            return;
        }

        // Tự tạo một Stage (Cửa sổ) mới tinh, bỏ qua Alert
        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT); // Xóa sạch viền Windows
        stage.initModality(Modality.APPLICATION_MODAL); // Chặn không cho bấm ra ngoài khi popup đang hiện
        stage.setTitle("Thông báo");

        // Khung nội dung chính
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setStyle(
                "-fx-background-color: " + BG_DARK + ";" +
                        "-fx-border-color: " + themeColor + ";" +
                        "-fx-border-radius: " + BORDER_RADIUS + ";" +
                        "-fx-background-radius: " + BORDER_RADIUS + ";" +
                        "-fx-border-width: 2;" +
                        "-fx-padding: 30;"
        );
        // Thiết lập kích thước khung
        content.setPrefWidth(350);
        content.setMinHeight(250);

        Label lblEmoji = new Label(emoji);
        lblEmoji.setStyle("-fx-font-size: 55px;");

        Label lblTitle = new Label(title.toUpperCase());
        lblTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + themeColor + ";");

        Label lblMessage = new Label(message);
        lblMessage.setStyle("-fx-font-size: 15px; -fx-text-fill: " + TEXT_SECONDARY + "; -fx-text-alignment: center;");
        lblMessage.setWrapText(true);
        lblMessage.setMaxWidth(300);

        // Nút bấm căn giữa hoàn hảo
        Button btnOk = new Button("Đã hiểu");
        btnOk.setStyle(
                "-fx-background-color: " + themeColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 15px;" +
                        "-fx-padding: 8 30;" +
                        "-fx-background-radius: 20;" +
                        "-fx-cursor: hand;"
        );
        // Bấm nút thì đóng cửa sổ
        btnOk.setOnAction(e -> stage.close());

        content.getChildren().addAll(lblEmoji, lblTitle, lblMessage, btnOk);

        // Ném khung nội dung vào Scene, làm nền Scene trong suốt
        Scene scene = new Scene(content);
        scene.setFill(Color.TRANSPARENT);

        stage.setScene(scene);
        stage.centerOnScreen(); // Ép nó ra giữa màn hình
        stage.showAndWait();
    }

    // =========================================================================
    // POPUP KẾT QUẢ ĐẤU GIÁ (Cửa sổ tàng hình 100%)
    // =========================================================================

    public static void showAuctionResult(boolean iWon, String finalPrice, String winnerName, Runnable onBack) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> showAuctionResult(iWon, finalPrice, winnerName, onBack));
            return;
        }

        String emoji      = iWon ? "🏆" : "😔";
        String title      = iWon ? "CHÚC MỪNG! BẠN ĐÃ THẮNG" : "PHIÊN ĐẤU GIÁ KẾT THÚC";
        String subtitle   = iWon ? "Bạn là người đặt giá cao nhất!" : "Người thắng: " + winnerName;
        String themeColor = iWon ? "#00b894" : "#ef4444";

        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setStyle(
                "-fx-background-color: " + BG_DARK + ";" +
                        "-fx-border-color: " + themeColor + ";" +
                        "-fx-border-radius: " + BORDER_RADIUS + ";" +
                        "-fx-background-radius: " + BORDER_RADIUS + ";" +
                        "-fx-border-width: 2;" +
                        "-fx-padding: 35;"
        );
        content.setPrefWidth(420);

        Label lblEmoji = new Label(emoji);
        lblEmoji.setStyle("-fx-font-size: 65px;");

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + themeColor + ";");
        lblTitle.setWrapText(true);

        Label lblPrice = new Label("Giá cuối: " + finalPrice + " đ");
        lblPrice.setStyle("-fx-font-size: 19px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");

        Label lblSubtitle = new Label(subtitle);
        lblSubtitle.setStyle("-fx-font-size: 16px; -fx-text-fill: " + TEXT_SECONDARY + ";");

        Button btnBack = new Button("Quay lại Dashboard");
        btnBack.setStyle(
                "-fx-background-color: " + themeColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 15px;" +
                        "-fx-padding: 10 30;" +
                        "-fx-background-radius: 20;" +
                        "-fx-cursor: hand;"
        );
        btnBack.setOnAction(e -> {
            stage.close();
            if (onBack != null) onBack.run();
        });

        // Bắt sự kiện nếu lỡ người dùng dùng Alt+F4 tắt popup thì vẫn chạy callback
        stage.setOnCloseRequest(e -> {
            if (onBack != null) onBack.run();
        });

        content.getChildren().addAll(lblEmoji, lblTitle, lblPrice, lblSubtitle, btnBack);

        Scene scene = new Scene(content);
        scene.setFill(Color.TRANSPARENT);

        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }
}