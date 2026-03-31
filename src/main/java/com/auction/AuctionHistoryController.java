package com.auction;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class AuctionHistoryController {

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            // Chuyển về TrangChu.fxml (hoặc Dashboard.fxml tùy tên file của bạn)
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/DashBoard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}