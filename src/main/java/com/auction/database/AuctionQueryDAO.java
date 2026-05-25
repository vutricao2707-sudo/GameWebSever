package com.auction.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionQueryDAO {

    private static final Logger logger = Logger.getLogger(AuctionQueryDAO.class.getName());

    public static long getAuctionTimeRemaining(int itemId) {
        String sql = "SELECT GREATEST(UNIX_TIMESTAMP(auction_end_time) - UNIX_TIMESTAMP(NOW()), 0) AS remaining FROM items WHERE id = ? AND auction_status = 'AUCTION'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("remaining");
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "[AuctionQueryDAO] getAuctionTimeRemaining error itemId=" + itemId, e);
        }
        return 0L;
    }

    public static double getCurrentMaxBid(int itemId) {
        String sql = "SELECT COALESCE(MAX(bid_amount), 0) AS max_bid FROM bids WHERE item_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("max_bid");
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "[AuctionQueryDAO] getCurrentMaxBid error itemId=" + itemId, e);
        }
        return 0.0;
    }

    public static LeaderInfo getCurrentLeader(int itemId) {
        String sql = "SELECT b.user_id, b.bid_amount, u.full_name FROM bids b JOIN users u ON b.user_id = u.id WHERE b.item_id = ? AND b.bid_amount = (SELECT MAX(bid_amount) FROM bids WHERE item_id = ?) ORDER BY b.bid_time DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.setInt(2, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new LeaderInfo(rs.getInt("user_id"), rs.getString("full_name"), rs.getDouble("bid_amount"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "[AuctionQueryDAO] getCurrentLeader error itemId=" + itemId, e);
        }
        return null;
    }

    public static List<BidRecord> getBidHistory(int itemId) {
        List<BidRecord> result = new ArrayList<>();
        String sql = "SELECT b.id, b.bid_amount, b.bid_time, u.id AS user_id, u.full_name FROM bids b JOIN users u ON b.user_id = u.id WHERE b.item_id = ? ORDER BY b.bid_time ASC LIMIT 100";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new BidRecord(rs.getInt("id"), rs.getInt("user_id"), rs.getString("full_name"), rs.getDouble("bid_amount"), rs.getTimestamp("bid_time").toLocalDateTime()));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "[AuctionQueryDAO] getBidHistory error itemId=" + itemId, e);
        }
        return result;
    }

    public static int getBidCount(int itemId) {
        String sql = "SELECT COUNT(*) FROM bids WHERE item_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "[AuctionQueryDAO] getBidCount error itemId=" + itemId, e);
        }
        return 0;
    }

    public static FinishResult finishAuction(int itemId, int winnerId, double finalPrice) {
        String sqlItem = "UPDATE items SET auction_status = 'FINISHED', current_price = ? WHERE id = ? AND auction_status = 'AUCTION'";
        String sqlAuction = "UPDATE auctions SET is_open = 0, winner_id = ?, final_price = ?, end_time = NOW() WHERE item_id = ? AND is_open = 1";
        String sqlResult = "INSERT IGNORE INTO auction_results (item_id, winner_id, final_price) VALUES (?, ?, ?)";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int itemRows;
            try (PreparedStatement ps = conn.prepareStatement(sqlItem)) {
                ps.setDouble(1, finalPrice);
                ps.setInt(2, itemId);
                itemRows = ps.executeUpdate();
            }

            if (itemRows == 0) {
                conn.rollback();
                return FinishResult.alreadyFinished(itemId);
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlAuction)) {
                ps.setInt(1, winnerId > 0 ? winnerId : 0);
                ps.setDouble(2, finalPrice);
                ps.setInt(3, itemId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlResult)) {
                ps.setInt(1, itemId);
                if (winnerId > 0) ps.setInt(2, winnerId);
                else ps.setNull(2, Types.INTEGER);
                ps.setDouble(3, finalPrice);
                ps.executeUpdate();
            }

            conn.commit();
            return FinishResult.success(itemId, winnerId, finalPrice);
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { }
            }
            return FinishResult.failed(itemId, e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ex) { }
                try { conn.close(); } catch (SQLException ex) { }
            }
        }
    }

    public static AuctionSnapshot getAuctionSnapshot(int itemId) {
        String sql = "SELECT current_price, auction_status, registration_end_time, auction_end_time, GREATEST(UNIX_TIMESTAMP(CASE WHEN auction_status = 'REGISTRATION' THEN registration_end_time WHEN auction_status = 'AUCTION' THEN auction_end_time ELSE NULL END) - UNIX_TIMESTAMP(NOW()), 0) AS remaining_seconds FROM items WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new AuctionSnapshot(itemId, rs.getDouble("current_price"), rs.getString("auction_status"), rs.getLong("remaining_seconds"), rs.getString("registration_end_time"), rs.getString("auction_end_time"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "[AuctionQueryDAO] getAuctionSnapshot error itemId=" + itemId, e);
        }
        return null;
    }

    public static List<ExpiredAuction> getExpiredAuctions() {
        List<ExpiredAuction> result = new ArrayList<>();
        String sql = "SELECT id, plate_number, current_price FROM items WHERE auction_status = 'AUCTION' AND auction_end_time IS NOT NULL AND auction_end_time <= NOW()";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new ExpiredAuction(rs.getInt("id"), rs.getString("plate_number"), rs.getDouble("current_price")));
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "[AuctionQueryDAO] getExpiredAuctions error", e);
        }
        return result;
    }

    public static void createIndexesIfNeeded() {
        String[] indexes = {
                "CREATE INDEX IF NOT EXISTS idx_items_auction_status_end ON items (auction_status, auction_end_time)",
                "CREATE INDEX IF NOT EXISTS idx_bids_item_amount ON bids (item_id, bid_amount DESC)",
                "CREATE INDEX IF NOT EXISTS idx_bids_item_user_time ON bids (item_id, user_id, bid_time DESC)"
        };
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String ddl : indexes) {
                try { stmt.execute(ddl); } catch (SQLException e) { }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "[AuctionQueryDAO] createIndexes error", e);
        }
    }

    public static class FinishResult {
        public final boolean success;
        public final boolean alreadyFinished;
        public final int itemId;
        public final int winnerId;
        public final double finalPrice;
        public final String errorMessage;

        private FinishResult(boolean success, boolean alreadyFinished, int itemId, int winnerId, double finalPrice, String errorMessage) {
            this.success = success; this.alreadyFinished = alreadyFinished; this.itemId = itemId; this.winnerId = winnerId; this.finalPrice = finalPrice; this.errorMessage = errorMessage;
        }
        static FinishResult success(int itemId, int winnerId, double finalPrice) { return new FinishResult(true, false, itemId, winnerId, finalPrice, null); }
        static FinishResult alreadyFinished(int itemId) { return new FinishResult(false, true, itemId, 0, 0, "Already finished"); }
        static FinishResult failed(int itemId, String msg) { return new FinishResult(false, false, itemId, 0, 0, msg); }
    }

    public static class LeaderInfo {
        public final int userId;
        public final String fullName;
        public final double bidAmount;
        public LeaderInfo(int userId, String fullName, double bidAmount) { this.userId = userId; this.fullName = fullName; this.bidAmount = bidAmount; }
    }

    public static class BidRecord {
        public final int bidId;
        public final int userId;
        public final String bidderName;
        public final double amount;
        public final java.time.LocalDateTime bidTime;
        public BidRecord(int bidId, int userId, String bidderName, double amount, java.time.LocalDateTime bidTime) { this.bidId = bidId; this.userId = userId; this.bidderName = bidderName; this.amount = amount; this.bidTime = bidTime; }
    }

    public static class AuctionSnapshot {
        public final int itemId;
        public final double currentPrice;
        public final String auctionStatus;
        public final long remainingSeconds;
        public final String registrationEndTime;
        public final String auctionEndTime;

        public AuctionSnapshot(int itemId, double currentPrice, String auctionStatus, long remainingSeconds, String registrationEndTime, String auctionEndTime) {
            this.itemId = itemId; this.currentPrice = currentPrice; this.auctionStatus = auctionStatus; this.remainingSeconds = remainingSeconds; this.registrationEndTime = registrationEndTime; this.auctionEndTime = auctionEndTime;
        }

        public boolean isExpired() { return remainingSeconds <= 0 && ("AUCTION".equalsIgnoreCase(auctionStatus) || "REGISTRATION".equalsIgnoreCase(auctionStatus)); }
    }

    public static class ExpiredAuction {
        public final int itemId;
        public final String plateNumber;
        public final double currentPrice;
        public ExpiredAuction(int itemId, String plateNumber, double currentPrice) { this.itemId = itemId; this.plateNumber = plateNumber; this.currentPrice = currentPrice; }
    }
}