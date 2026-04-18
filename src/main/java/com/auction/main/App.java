package com.auction.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Đường dẫn ĐÚNG: bắt đầu bằng "/" và trỏ đến file trong resources
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root, 1024, 720);
        stage.setTitle("Hệ Thống Đấu Giá Biển Số - VinGroup");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}