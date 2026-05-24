package com.auction.controller;

import com.auction.util.UIUtils;
import com.auction.util.NavigationUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
@FXML // Dòng này báo cho IntelliJ biết: "Hàm này được FXML dùng đấy"
private void handleBack(ActionEvent event) {
    String path = "/fxml/Login.fxml";
    if (!NavigationUtil.navigateTo(event, path)) {
        UIUtils.showError("Lỗi hệ thống", "Không thể tải trang: " + path);
    }
}