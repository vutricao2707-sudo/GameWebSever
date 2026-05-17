package com.auction.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction {
    private String auctionId;
    private Item item;
    private Seller seller;
    private List<BidTransaction> bidTransactions;
    private boolean isOpen;
    
    // Auto-Extend Timer Properties
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int durationSeconds = 3600; // Mặc định 60 phút
    private long lastBidTime; // Thời gian bid cuối cùng
    private static final int AUTO_EXTEND_THRESHOLD = 120; // 2 phút cuối
    private static final int AUTO_EXTEND_SECONDS = 120; // Kéo dài 2 phút

    public Auction(String auctionId, Item item, Seller seller) {
        this.auctionId = auctionId;
        this.item = item;
        this.seller = seller;
        this.bidTransactions = new ArrayList<>();
        this.isOpen = true;
        this.startTime = LocalDateTime.now();
        this.endTime = this.startTime.plusSeconds(durationSeconds);
        this.lastBidTime = System.currentTimeMillis();
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
    }

    public List<BidTransaction> getBids() {
        return bidTransactions;
    }

    public void setBids(List<BidTransaction> bidTransactions) {
        this.bidTransactions = bidTransactions;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        isOpen = open;
    }

    // ===== Auto-Extend Timer Methods =====
    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
        this.endTime = this.startTime.plusSeconds(durationSeconds);
    }

    public long getLastBidTime() {
        return lastBidTime;
    }

    public void setLastBidTime(long lastBidTime) {
        this.lastBidTime = lastBidTime;
    }

    /**
     * Kiểm tra xem có cần auto-extend không:
     * Nếu bid trong 2 phút cuối, tự động kéo dài thêm 2 phút
     */
    public void checkAndAutoExtend() {
        long timeRemaining = getTimeRemainingSeconds();
        
        // Nếu còn dưới 2 phút và có bid mới lần này, auto-extend
        if (timeRemaining < AUTO_EXTEND_THRESHOLD && timeRemaining > 0) {
            LocalDateTime now = LocalDateTime.now();
            long millisSinceLastBid = System.currentTimeMillis() - lastBidTime;
            
            // Nếu bid được đặt gần đây (trong 10 giây), extend
            if (millisSinceLastBid < 10000) {
                this.endTime = this.endTime.plusSeconds(AUTO_EXTEND_SECONDS);
            }
        }
    }

    /**
     * Lấy số giây còn lại cho phiên đấu giá
     */
    public long getTimeRemainingSeconds() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endTime)) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.SECONDS.between(now, endTime);
    }

    /**
     * Kiểm tra xem phiên có hết hạn không
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endTime);
    }

}

