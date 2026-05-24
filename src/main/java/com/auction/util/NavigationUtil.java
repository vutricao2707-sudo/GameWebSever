package com.auction.util;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Objects;

@SuppressWarnings("ALL")
public class NavigationUtil {

    private NavigationUtil() {}

    /**
     * Chuyển màn hình sang FXML khác.
     * @return true nếu chuyển trang thành công, false nếu lỗi
     */
    public static boolean navigateTo(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(NavigationUtil.class.getResource(fxmlPath)));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
            return true; // Trả về true thành công
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
            // Lưu ý: Không gọi UIUtils trực tiếp ở đây để tránh bị vòng lặp phụ thuộc (circular dependency)
            // Lỗi sẽ được bắt ở Controller để hiện Popup bằng UIUtils
            return false; // Trả về false thất bại
        }
    }
}