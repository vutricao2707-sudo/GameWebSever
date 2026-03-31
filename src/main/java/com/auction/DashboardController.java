package com.auction;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import java.io.IOException;

public class DashboardController {

    @FXML private TabPane mainTabPane;

    @FXML private void showHomeTab(ActionEvent event) { mainTabPane.getSelectionModel().select(0); }
    @FXML private void showHistoryTab(ActionEvent event) { mainTabPane.getSelectionModel().select(1); }
    @FXML private void showResultsTab(ActionEvent event) { mainTabPane.getSelectionModel().select(2); }

    @FXML
    private void handleGoToAuction(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/AuctionRoom.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) { e.printStackTrace(); }
    }
}