package com.auction.database;

import java.sql.*;

/**
 * AuctionDAO - Quản lý phiên đấu giá (auctions)
 */
public class AuctionDAO {

    // Tạo phiên đấu giá mới
    public static void createAuction(int itemId, int sellerId) {
        // 💣 ĐÃ GỠ BOM: Thay vì INSERT bảng auctions, chỉ cần UPDATE trạng thái trong bảng items
        String sql = "UPDATE items SET auction_status = 'AUCTION' WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);
            // Lưu ý: sellerId được giữ lại ở tham số hàm để không làm lỗi code chỗ khác,
            // nhưng không cần đưa vào câu lệnh SQL này nữa.

            pstmt.executeUpdate();
            System.out.println("✅ Tạo phiên đấu giá thành công");

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }


    // Lấy tất cả phiên đấu giá đang mở
    public static void getOpenAuctions() {
        // 💣 ĐÃ GỠ BOM: Query thẳng từ bảng items
        String sql = "SELECT id, plate_number, current_price FROM items WHERE auction_status = 'AUCTION'";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n📌 Phiên đấu giá đang mở:");
            while (rs.next()) {
                System.out.println("  Phiên #" + rs.getInt("id") +
                        " | Biển: " + rs.getString("plate_number") +
                        " | Giá: " + rs.getDouble("current_price"));
            }
            System.out.println();

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }


    // Đóng phiên đấu giá (khi hết giờ)
    public static void closeAuction(int auctionId, int winnerId, double finalPrice) {
        String sql = "UPDATE items SET auction_status = 'FINISHED', current_price = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, finalPrice);
            pstmt.setInt(2, auctionId);
            // Lưu ý: winnerId được giữ nguyên ở tham số để code cũ không báo lỗi.

            pstmt.executeUpdate();
            System.out.println("✅ Đóng phiên đấu giá thành công");

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }


    // Lấy chi tiết phiên đấu giá
    public static void getAuctionDetails(int auctionId) {
        // 💣 ĐÃ GỠ BOM: Lấy dữ liệu từ bảng items
        String sql = "SELECT id, plate_number, current_price, auction_status FROM items WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, auctionId); // auctionId truyền vào chính là id của item
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                System.out.println("\n📌 Chi tiết phiên:");
                System.out.println("  Phiên #: " + rs.getInt("id"));
                System.out.println("  Biển: " + rs.getString("plate_number"));
                System.out.println("  Giá hiện tại: " + rs.getDouble("current_price"));
                System.out.println("  Trạng thái: " + rs.getString("auction_status") + "\n");
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }
}