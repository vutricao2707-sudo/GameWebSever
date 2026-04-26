package com.auction.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.text.DecimalFormat;

public class AuctionResultController {

    @FXML private Label lblPlate;
    @FXML private Label lblBidAmount;
    @FXML private Label lblStatus;
    @FXML private Label lblSubStatus;

    // Hàm nhận dữ liệu từ màn hình Phòng Đấu Giá truyền sang
    public void setAuctionData(String plate, double finalBid, boolean isWinner) {
        lblPlate.setText("Biển số: " + plate);

        // Định dạng tiền tệ (VD: 50,000,000 VNĐ)
        DecimalFormat formatter = new DecimalFormat("#,###");
        lblBidAmount.setText(formatter.format(finalBid) + " VNĐ");

        // Đổi màu sắc và câu chữ phụ thuộc vào kết quả
        if (isWinner) {
            lblStatus.setText("ĐẤU GIÁ THÀNH CÔNG");
            lblStatus.setStyle("-fx-text-fill: #00b894;"); // Màu xanh lá
            lblSubStatus.setText("Chúc mừng bạn đã sở hữu biển số này!");
        } else {
            lblStatus.setText("ĐẤU GIÁ THẤT BẠI");
            lblStatus.setStyle("-fx-text-fill: #ff6b6b;"); // Màu đỏ hồng (khớp với nút Đăng xuất của Dashboard)
            lblSubStatus.setText("Rất tiếc, đã có người trả giá cao hơn bạn.");
        }
    }

    // Xử lý nút bấm quay lại (Dùng chung cho cả nút trên Header và dưới Thẻ)
    @FXML
    private void handleBackHome(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}