package com.auction.service;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.Auction;
import com.auction.model.Bidder;
import com.auction.model.Item;
import com.auction.model.Seller;

import java.util.List;

public interface IAuctionService {
    Auction createAuction(Item item, Seller seller);

    void placeBid(String auctionId, Bidder bidder, double bidAmount) throws InvalidBidException, AuctionClosedException;

    Auction closeAuction(String auctionId) throws AuctionClosedException;

    List<Auction> getActiveAuctions();
}
