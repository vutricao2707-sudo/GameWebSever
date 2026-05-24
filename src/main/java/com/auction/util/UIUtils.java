package com.auction.util;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;

/**
 * UIUtils — Bộ tiện ích giao diện dùng chung cho toàn bộ ứng dụng.
 *
 * Mọi Popup/Alert phải đi qua class này để đảm bảo:
 *  • Giao diện Dark Mode nhất quán (nền #1a2230, viền màu, bo góc)
 *  • Không còn Alert mặc định xấu xí của JavaFX
 *  • Dễ bảo trì: muốn đổi theme chỉ sửa 1 chỗ
 */
public class UIUtils {

    // ─── Màu sắc chủ đạo (Dark Mode) ─────────────────────────────────────────
    private static final String BG_DARK        = "#1a2230";
    private static final String TEXT_SECONDARY = "#cbd5e1";
    private static final String BORDER_RADIUS  = "8";

    // Không cho khởi tạo
    private UIUtils() {}

    // =========================================================================
    // POPUP THÔNG BÁO TÙY CHỈNH (Dark Mode)
    // Dùng cho: trạng thái biển số, kết quả đấu giá, lỗi hệ thống
    // =========================================================================

    /**
     * Hiển thị một Popup thông báo đơn giản với Dark Mode.
     *
     * @param title      Tiêu đề popup (VD: "Chưa mở đấu giá")
     * @param message    Nội dung thông báo
     * @param emoji      Biểu tượng lớn (VD: "⏳", "🏁", "⚠️")
     * @param themeColor Màu viền + màu tiêu đề (VD: "#00b894", "#ef4444")
     */
    public static void showCustomPopup(String title, String message,
                                       String emoji, String themeColor) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setGraphic(null);

        DialogPane pane = alert.getDialogPane();
        pane.setStyle(
                "-fx-background-color: " + BG_DARK + ";" +
                        "-fx-border-color: " + themeColor + ";" +
                        "-fx-border-radius: " + BORDER_RADIUS + ";" +
                        "-fx-border-width: 2;"
        );

        VBox content = new VBox(15);
        content.setStyle("-fx-alignment: center; -fx-padding: 25;");

        Label lblEmoji = new Label(emoji);
        lblEmoji.setStyle("-fx-font-size: 55px;");

        Label lblTitle = new Label(title.toUpperCase());
        lblTitle.setStyle(
                "-fx-font-size: 20px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + themeColor + ";"
        );

        Label lblMessage = new Label(message);
        lblMessage.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-text-fill: " + TEXT_SECONDARY + ";" +
                        "-fx-text-alignment: center;"
        );
        lblMessage.setWrapText(true);
        lblMessage.setMaxWidth(320);

        content.getChildren().addAll(lblEmoji, lblTitle, lblMessage);
        pane.setContent(content);

        ButtonType btnOk = new ButtonType("Đã hiểu", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(btnOk);

        Node okButton = pane.lookupButton(btnOk);
        if (okButton != null) {
            okButton.setStyle(
                    "-fx-background-color: " + themeColor + ";" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: bold;" +
                            "-fx-font-size: 14px;" +
                            "-fx-padding: 8 25;" +
                            "-fx-background-radius: 20;" +
                            "-fx-cursor: hand;"
            );
        }

        alert.showAndWait();
    }

    // =========================================================================
    // POPUP KẾT QUẢ ĐẤU GIÁ (thắng / thua)
    // =========================================================================

    /**
     * Hiển thị kết quả sau khi phiên đấu giá kết thúc.
     *
     * @param iWon        true nếu người dùng thắng
     * @param finalPrice  Giá cuối cùng (đã format)
     * @param winnerName  Tên người thắng
     * @param onBack      Callback khi người dùng bấm "Quay lại Dashboard"
     */
    public static void showAuctionResult(boolean iWon, String finalPrice,
                                         String winnerName, Runnable onBack) {
        String emoji      = iWon ? "🏆" : "😔";
        String title      = iWon ? "CHÚC MỪNG! BẠN ĐÃ THẮNG" : "PHIÊN ĐẤU GIÁ KẾT THÚC";
        String subtitle   = iWon ? "Bạn là người đặt giá cao nhất!" : "Người thắng: " + winnerName;
        String themeColor = iWon ? "#00b894" : "#ef4444";

        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Kết quả đấu giá");
        alert.setHeaderText(null);
        alert.setGraphic(null);

        DialogPane pane = alert.getDialogPane();
        pane.setStyle(
                "-fx-background-color: " + BG_DARK + ";" +
                        "-fx-border-color: " + themeColor + ";" +
                        "-fx-border-radius: " + BORDER_RADIUS + ";" +
                        "-fx-border-width: 2;"
        );

        VBox content = new VBox(15);
        content.setStyle("-fx-alignment: center; -fx-padding: 25;");

        Label lblEmoji = new Label(emoji);
        lblEmoji.setStyle("-fx-font-size: 55px;");

        Label lblTitle = new Label(title);
        lblTitle.setStyle(
                "-fx-font-size: 22px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + themeColor + ";"
        );
        lblTitle.setWrapText(true);
        lblTitle.setMaxWidth(350);

        Label lblPrice = new Label("Giá cuối: " + finalPrice + " đ");
        lblPrice.setStyle(
                "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #f59e0b;"
        );

        Label lblSubtitle = new Label(subtitle);
        lblSubtitle.setStyle(
                "-fx-font-size: 15px;" +
                        "-fx-text-fill: " + TEXT_SECONDARY + ";"
        );

        content.getChildren().addAll(lblEmoji, lblTitle, lblPrice, lblSubtitle);
        pane.setContent(content);
        pane.setMinWidth(420);

        ButtonType btnBack = new ButtonType("Quay lại Dashboard", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(btnBack);

        Node backButton = pane.lookupButton(btnBack);
        if (backButton != null) {
            backButton.setStyle(
                    "-fx-background-color: " + themeColor + ";" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: bold;" +
                            "-fx-font-size: 15px;" +
                            "-fx-padding: 10 30;" +
                            "-fx-background-radius: 20;" +
                            "-fx-cursor: hand;"
            );
        }

        // Khi popup đóng (dù bấm nút hay X), gọi callback để quay lại Dashboard
        alert.setOnHidden(e -> {
            if (onBack != null) onBack.run();
        });

        alert.show();
    }

    // =========================================================================
    // SHORTCUT — Popup lỗi đơn giản (thay thế showAlert cũ)
    // =========================================================================

    /** Popup lỗi màu đỏ */
    public static void showError(String title, String message) {
        showCustomPopup(title, message, "⚠️", "#ef4444");
    }

    /** Popup thành công màu xanh ngọc */
    public static void showSuccess(String title, String message) {
        showCustomPopup(title, message, "✅", "#00b894");
    }

    /** Popup thông tin màu xanh dương */
    public static void showInfo(String title, String message) {
        showCustomPopup(title, message, "ℹ️", "#38bdf8");
    }
}