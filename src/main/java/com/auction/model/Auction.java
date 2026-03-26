package com.auction.model;

import java.util.List;

public class Auction {
    private Item item;
    private Seller seller;
    private List<Bid> history;
    private double currentHighestBid;
    private boolean status;
}
