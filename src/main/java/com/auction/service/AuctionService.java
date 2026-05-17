package com.auction.service;
import com.auction.database.ItemDAO;
public class AuctionService {

    public boolean startRegistration(int itemId, int minutes) {
        return ItemDAO.startRegistrationPhase(String.valueOf(itemId), minutes);
    }

    public boolean startAuction(int itemId) {
        return ItemDAO.updateStatus(itemId, "AUCTION");
    }

    public boolean finishAuction(int itemId, double finalPrice) {
        return ItemDAO.finishAuction(itemId, finalPrice);
    }

    public Object getMonitoringData() {
        return ItemDAO.getAuctionsForMonitoring();
    }
}