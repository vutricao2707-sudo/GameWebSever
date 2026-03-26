package com.auction.model;

import java.util.ArrayList;
import java.util.List;

public class Auction {
    String auctionId;
    private Item item;
    private Seller seller;
    private List<Bid> bids;
    private boolean isOpen;


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

    public List<Bid> getBids() {
        return bids;
    }

    public void setBids(List<Bid> bids) {
        this.bids = bids;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        isOpen = open;
    }

    public Auction(String auctionId, Item item, Seller seller) {
        this.auctionId = auctionId;
        this.item = item;
        this.seller = seller;
        this.bids = new ArrayList<>();
        this.isOpen = true;
    }
}
