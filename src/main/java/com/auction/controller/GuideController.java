package com.auction.controller;

import com.auction.util.NavigationUtil;
import com.auction.util.UIUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class GuideController {

    @FXML
    private void handleBack(ActionEvent event) {
        String path = "/fxml/Login.fxml";

        // Giờ đây NavigationUtil.navigateTo trả về boolean nên khớp với logic if
        if (!NavigationUtil.navigateTo(event, path)) {
            UIUtils.showError("Lỗi hệ thống", "Không thể tải trang: " + path);
        }
    }
}