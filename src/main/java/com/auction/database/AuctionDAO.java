package com.auction.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {

    // =========================
    // 1. TẠO PHIÊN ĐẤU GIÁ
    // =========================
    public static boolean createAuction(int itemId, int sellerId) {

        String sql = """
    INSERT INTO auctions (item_id, seller_id, start_time, is_open)
    VALUES (?, ?, NOW(), 1)
""";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, itemId);
            ps.setInt(2, sellerId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("❌ createAuction error: " + e.getMessage());
            return false;
        }
    }

    // =========================
    // 2. LẤY PHIÊN ĐANG MỞ
    // =========================
    public static List<String[]> getOpenAuctions() {

        String sql = "SELECT * FROM auctions WHERE is_open = 1";

        List<String[]> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                list.add(new String[]{
                        String.valueOf(rs.getInt("id")),
                        String.valueOf(rs.getInt("item_id")),
                        String.valueOf(rs.getInt("seller_id")),
                        String.valueOf(rs.getInt("winner_id")),
                        rs.getString("start_time"),
                        rs.getString("end_time"),
                        String.valueOf(rs.getDouble("final_price")),
                        String.valueOf(rs.getBoolean("is_open"))
                });
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    // =========================
    // 3. ĐÓNG PHIÊN ĐẤU GIÁ
    // =========================
    public static boolean closeAuction(int auctionId, int winnerId, double finalPrice) {

        String sql = """
        UPDATE auctions
        SET is_open = 0,
            winner_id = ?,
            final_price = ?,
            end_time = NOW()
        WHERE id = ?
    """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, winnerId);
            ps.setDouble(2, finalPrice);
            ps.setInt(3, auctionId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("❌ closeAuction error: " + e.getMessage());
            return false;
        }
    }

    // =========================
    // 4. LẤY CHI TIẾT PHIÊN
    // =========================
    public static ResultSet getAuctionDetails(int auctionId) throws SQLException {

        String sql = """
            SELECT *
            FROM auctions
            WHERE id = ?
        """;

        Connection conn = DatabaseConnection.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);

        ps.setInt(1, auctionId);

        return ps.executeQuery();
    }
}