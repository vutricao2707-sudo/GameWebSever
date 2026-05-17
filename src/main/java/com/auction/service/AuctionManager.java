package com.auction.service;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.*;

import java.util.ArrayList;
import java.util.List;

public class AuctionManager implements IAuctionService {

    private List<Auction> auctions;
    private List<User> users;
    private static AuctionManager instance;

    private AuctionManager() {
        this.auctions = new ArrayList<>();
        this.users    = new ArrayList<>();
        // Không có tài khoản mẫu hardcoded.
        // Xác thực người dùng được thực hiện hoàn toàn qua UserDAO + database.
    }

    public static AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    // QUẢN LÝ NGƯỜI DÙNG
    public void registerUser(User user) {
        users.add(user);
    }

    public User login(String username, String password) {
        for (User u : users) {
            if (u.getUserName().equals(username) && u.getPassword().equals(password)) {
                return u;
            }
        }
        return null;
    }

    private Auction findAuctionById(String auctionId) {
        for (Auction a : auctions) {
            if (a.getAuctionId().equals(auctionId)) {
                return a;
            }
        }
        return null;
    }

    @Override
    public Auction createAuction(Item item, Seller seller) {
        String newId = "AUC-" + (auctions.size() + 1);
        Auction newAuction = new Auction(newId, item, seller);
        auctions.add(newAuction);
        return newAuction;
    }

    @Override
    public void placeBid(String auctionId, Bidder bidder, double bidAmount)
            throws InvalidBidException, AuctionClosedException {

        Auction auction = findAuctionById(auctionId);

        if (auction == null) {
            throw new InvalidBidException("Lỗi: Không tìm thấy mã phiên đấu giá này!");
        }

        if (!auction.isOpen()) {
            throw new AuctionClosedException("Phiên đấu giá " + auctionId + " đã gõ búa kết thúc. Bạn không thể đặt thêm!");
        }

        double currentHighest = auction.getItem().getStartingPrice();
        List<BidTransaction> history = auction.getBids();

        if (!history.isEmpty()) {
            currentHighest = history.get(history.size() - 1).getAmount();
        }

        if (bidAmount <= currentHighest) {
            throw new InvalidBidException("Lỗi: Giá của bạn đưa ra ($" + bidAmount
                    + ") phải cao hơn mức giá đỉnh hiện tại ($" + currentHighest + ")");
        }

        BidTransaction newBidTransaction = new BidTransaction(bidder, bidAmount);
        auction.getBids().add(newBidTransaction);
        
        // ✅ THÊM MỚI: Cập nhật thời gian bid cuối cùng và check auto-extend
        auction.setLastBidTime(System.currentTimeMillis());
        auction.checkAndAutoExtend();
    }

    @Override
    public Auction closeAuction(String auctionId) throws AuctionClosedException {
        Auction auction = findAuctionById(auctionId);
        if (auction == null) {
            return null;
        }
        if (!auction.isOpen()) {
            throw new AuctionClosedException("Phiên này đã đóng từ trước rồi!");
        }
        auction.setOpen(false);
        return auction;
    }

    @Override
    public List<Auction> getActiveAuctions() {
        List<Auction> activeList = new ArrayList<>();
        for (Auction a : auctions) {
            if (a.isOpen()) {
                activeList.add(a);
            }
        }
        return activeList;
    }
}