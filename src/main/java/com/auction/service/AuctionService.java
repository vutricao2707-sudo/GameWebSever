package com.auction.service;

import com.auction.database.ItemDAO;

public class AuctionService {

    // Bắt đầu giai đoạn đăng ký
    public boolean startRegistration(int itemId, int minutes) {
        return ItemDAO.startRegistrationPhase(itemId, minutes);
    }

    // Chuyển sang đấu giá
    public boolean startAuction(int itemId) {
        return ItemDAO.updateStatus(itemId, "AUCTION");
    }

    // Kết thúc đấu giá
    public boolean finishAuction(int itemId, double finalPrice) {
        return ItemDAO.finishAuction(itemId, finalPrice);
    }

    // Lấy danh sách monitor
    public Object getMonitoringData() {
        return ItemDAO.getAuctionsForMonitoring();
    }
}