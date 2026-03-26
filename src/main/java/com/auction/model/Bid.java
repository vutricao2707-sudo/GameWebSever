package com.auction.model;

import java.time.LocalDateTime;

public class Bid {
    private Bidder bidder;
    private double amount;
    private LocalDateTime timestamp;

    public Bidder getBidder() {
        return bidder;
    }

    public void setBidder(Bidder bidder) {
        this.bidder = bidder;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Bid(Bidder bidder, double amount) {
        this.bidder = bidder;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();

    }
    @Override
    public String toString() {
        return bidder.getUsername() + " đặt giá: $" + amount + " lúc " + timestamp;
    }
}
