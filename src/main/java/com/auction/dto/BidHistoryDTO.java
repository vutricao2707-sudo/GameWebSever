package com.auction.dto;

import java.time.LocalDateTime;

/**
 * DTO for tracking bid history with timestamps
 * Used for displaying real-time price charts
 */
public class BidHistoryDTO {
    private int bidId;
    private double bidAmount;
    private LocalDateTime bidTime;
    private String bidderName;
    private String plateNumber;

    public BidHistoryDTO(int bidId, double bidAmount, LocalDateTime bidTime, String bidderName, String plateNumber) {
        this.bidId = bidId;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
        this.bidderName = bidderName;
        this.plateNumber = plateNumber;
    }

    // Getters
    public int getBidId() {
        return bidId;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public String getBidderName() {
        return bidderName;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    // Setters
    public void setBidAmount(double bidAmount) {
        this.bidAmount = bidAmount;
    }

    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }

    public void setBidderName(String bidderName) {
        this.bidderName = bidderName;
    }

    @Override
    public String toString() {
        return bidderName + " - " + String.format("%.0f", bidAmount) + " đ @ " + bidTime;
    }
}

