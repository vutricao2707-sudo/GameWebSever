package com.auction.controller;

import com.auction.util.UIUtils;
import com.auction.util.NavigationUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class SupportController {

    @FXML // đánh dấu cho FXML loader biết phương thức này được gọi từ FXML
    private void handleBack(ActionEvent event) {
        String path = "/fxml/Login.fxml";
        if (!NavigationUtil.navigateTo(event, path)) {
            UIUtils.showError("Lỗi hệ thống", "Không thể tải trang: " + path);
        }
    }
}