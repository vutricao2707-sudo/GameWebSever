package com.auction.model;

import java.time.LocalDateTime;

/**
 * Watchlist - Model để lưu trữ wishlist/watchlist của người dùng
 * Cho phép người dùng "theo dõi" biển số chưa mở đấu giá
 */
public class Watchlist {
    private int id;
    private int userId;
    private String plateNumber;
    private LocalDateTime addedDate;
    private boolean notified;

    /**
     * Constructor cho thêm mới watchlist (từ service)
     */
    public Watchlist(int userId, String plateNumber) {
        this.userId = userId;
        this.plateNumber = plateNumber;
        this.addedDate = LocalDateTime.now();
        this.notified = false;
    }

    /**
     * Constructor cho fetch từ database (đầy đủ)
     */
    public Watchlist(int id, int userId, String plateNumber, LocalDateTime addedDate, boolean notified) {
        this.id = id;
        this.userId = userId;
        this.plateNumber = plateNumber;
        this.addedDate = addedDate;
        this.notified = notified;
    }

    // ===== Getters & Setters =====
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public LocalDateTime getAddedDate() {
        return addedDate;
    }

    public void setAddedDate(LocalDateTime addedDate) {
        this.addedDate = addedDate;
    }

    public boolean isNotified() {
        return notified;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    @Override
    public String toString() {
        return "Watchlist{" +
                "id=" + id +
                ", userId=" + userId +
                ", plateNumber='" + plateNumber + '\'' +
                ", addedDate=" + addedDate +
                ", notified=" + notified +
                '}';
    }
}



