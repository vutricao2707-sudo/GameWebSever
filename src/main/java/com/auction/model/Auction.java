package com.auction.model;

import java.util.ArrayList;
import java.util.List;

public class Auction {
    String auctionId;
    private Item item;
    private Seller seller;
    private List<BidTransaction> bidTransactions;
    private boolean isOpen;

    public Auction(String auctionId, Item item, Seller seller) {
        this.auctionId = auctionId;
        this.item = item;
        this.seller = seller;
        this.bidTransactions = new ArrayList<>();
        this.isOpen = true;
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


}
