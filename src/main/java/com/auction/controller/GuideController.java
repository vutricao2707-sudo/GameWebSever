package com.auction.controller;

import com.auction.util.NavigationUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class GuideController {

    @FXML
    private void handleBack(ActionEvent event) {
        // THAY ĐỔI: Dùng NavigationUtil thay vì tự viết FXMLLoader + Stage + Scene
        NavigationUtil.navigateTo(event, "/fxml/Login.fxml");
    }
}