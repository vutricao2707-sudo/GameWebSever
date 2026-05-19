package com.auction.main;

import com.auction.database.DatabaseInitializer;
import com.auction.server.AuctionServer;
import com.auction.server.AuctionWebSocketServer;
import com.auction.util.DatabaseHealthCheck;
import com.auction.util.ThemeManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Kiểm tra kết nối Database trước khi làm các việc khác
        if (!DatabaseHealthCheck.isDatabaseAlive()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi hệ thống");
            alert.setHeaderText("Không thể kết nối đến Cơ sở dữ liệu!");
            alert.setContentText("Vui lòng kiểm tra xem MySQL đã được bật chưa, sau đó mở lại ứng dụng.");
            alert.showAndWait();

            Platform.exit(); // Tắt ứng dụng an toàn
            return;          // Dừng chạy các code bên dưới
        }

        DatabaseInitializer.initDatabase();  // Khởi tạo database tự động

        // ✅ Khởi động WebSocket Server
        AuctionWebSocketServer wsServer = AuctionWebSocketServer.getInstance();
        wsServer.start();
        System.out.println("🚀 WebSocket Server đã khởi động!");

        // ✅ Khởi động Socket Server ở thread riêng
        Thread socketServerThread = new Thread(() -> AuctionServer.main(new String[]{}), "SocketServer-Thread");
        socketServerThread.setDaemon(true);
        socketServerThread.start();
        System.out.println("🚀 Socket Server đã khởi động!");

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root, 1024, 720);

        // ✅ Khởi tạo theme
        ThemeManager themeManager = ThemeManager.getInstance();
        themeManager.setCurrentScene(scene);

        stage.setTitle("Hệ Thống Đấu Giá Biển Số - VinGroup");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();

        // ✅ Dừng servers khi đóng ứng dụng
        stage.setOnCloseRequest(event -> {
            System.out.println("Đóng ứng dụng, dừng servers...");
            try {
                wsServer.stop();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Lỗi khi dừng WebSocket Server: " + e.getMessage());
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}