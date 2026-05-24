package com.auction.database;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ItemDAO — Quản lý biển số xe (items)
 * Đã bổ sung: getFullMonitoringData() cho Admin giám sát realtime
 */
public class ItemDAO {

    // =====================================================================
    // 1. THÊM MỚI BIỂN SỐ (ITEMS)
    // =====================================================================

    public static void addItem(String plateNumber, double startPrice, String description,
                               String vehicleType, String status) {
        String sql = "INSERT INTO items (plate_number, start_price, current_price, description, vehicle_type, status) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);
            pstmt.setDouble(2, startPrice);
            pstmt.setDouble(3, startPrice);
            pstmt.setString(4, description);
            pstmt.setString(5, vehicleType);
            pstmt.setString(6, status);
            pstmt.executeUpdate();
            System.out.println("✅ Thêm item biển '" + plateNumber + "' thành công");

        } catch (SQLException e) {
            System.out.println("❌ Lỗi thêm item: " + e.getMessage());
        }
    }

    public static void addItemFromSeller(String plateNumber, double startPrice, String description,
                                         String vehicleType, String sellerName, String sellerPhone,
                                         String sellerEmail, String sellerAddress) throws SQLException {

        String sql = "INSERT INTO items (plate_number, start_price, current_price, description, vehicle_type, status, " +
                "approval_status, seller_name, seller_phone, seller_email, seller_address) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);
            pstmt.setDouble(2, startPrice);
            pstmt.setDouble(3, startPrice);
            pstmt.setString(4, description);
            pstmt.setString(5, vehicleType);
            pstmt.setString(6, "ACTIVE");
            pstmt.setString(7, "PENDING");
            pstmt.setString(8, sellerName);
            pstmt.setString(9, sellerPhone);
            pstmt.setString(10, sellerEmail);
            pstmt.setString(11, sellerAddress);
            pstmt.executeUpdate();
            System.out.println("✅ Thêm item (chờ duyệt) thành công");
        }
    }

    // =====================================================================
    // 2. TRUY VẤN DỮ LIỆU BIỂN SỐ
    // =====================================================================

    public static String[] getItemByPlate(String plateNumber) {
        String sql = "SELECT * FROM items WHERE plate_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new String[]{
                            String.valueOf(rs.getInt("id")),
                            rs.getString("plate_number"),
                            String.valueOf(rs.getDouble("start_price")),
                            String.valueOf(rs.getDouble("current_price")),
                            rs.getString("description"),
                            rs.getString("vehicle_type"),
                            rs.getString("status"),
                            rs.getString("approval_status")
                    };
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi getItemByPlate: " + e.getMessage());
        }
        return null;
    }

    public static String[] getItemFullData(String plateNumber) {
        String sql = "SELECT * FROM items WHERE plate_number = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp registrationEndTs = rs.getTimestamp("registration_end_time");
                    String auctionStatus = rs.getString("auction_status");

                    // Tự động chuyển REGISTRATION → AUCTION nếu đã hết giờ đăng ký
                    if (registrationEndTs != null && "REGISTRATION".equalsIgnoreCase(auctionStatus)) {

                        LocalDateTime now = LocalDateTime.now();
                        LocalDateTime endTime = registrationEndTs.toLocalDateTime();

                        if (now.isAfter(endTime)) {

                            String updateSql =
                                    "UPDATE items " +
                                            "SET auction_status = 'AUCTION' " +
                                            "WHERE plate_number = ? " +
                                            "AND auction_status = 'REGISTRATION'";

                            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {

                                updateStmt.setString(1, plateNumber);

                                int updated = updateStmt.executeUpdate();

                                if (updated > 0) {
                                    auctionStatus = "AUCTION";
                                    System.out.println("✅ Đã chuyển sang AUCTION: " + plateNumber);
                                }
                            }
                        }
                    }

                    return new String[]{
                            String.valueOf(rs.getInt("id")),           // [0]
                            rs.getString("plate_number"),              // [1]
                            rs.getString("start_price"),               // [2]
                            rs.getString("current_price"),             // [3]
                            rs.getString("description"),               // [4]
                            rs.getString("vehicle_type"),              // [5]
                            rs.getString("seller_name"),               // [6]
                            rs.getString("registration_start_time"),   // [7]
                            rs.getString("registration_end_time"),     // [8]
                            auctionStatus,                             // [9]
                            String.valueOf(rs.getInt("participant_count")) // [10]
                    };
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi getItemFullData: " + e.getMessage());
        }
        return null;
    }

    /** Lấy auction_status mới nhất từ DB (tránh dùng cache ItemDTO cũ) */
    public static String getAuctionStatus(String plateNumber) {
        String sql = "SELECT auction_status, registration_end_time FROM items WHERE plate_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("auction_status");
                    Timestamp regEndTs = rs.getTimestamp("registration_end_time");

                    // Tự động chuyển REGISTRATION → AUCTION nếu đã hết giờ
                    if ("REGISTRATION".equalsIgnoreCase(status) && regEndTs != null) {

                        LocalDateTime now = LocalDateTime.now();
                        LocalDateTime endTime = regEndTs.toLocalDateTime();

                        if (now.isAfter(endTime)) {

                            boolean updated = updateAuctionStatus(plateNumber, "AUCTION");

                            if (updated) {
                                return "AUCTION";
                            }
                        }
                    }
                    return status;
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi getAuctionStatus: " + e.getMessage());
        }
        return "WAITING";
    }

    public static boolean updateAuctionStatus(String plateNumber, String newStatus) {
        String sql = "UPDATE items SET auction_status = ? WHERE plate_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setString(2, plateNumber);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("❌ Lỗi updateAuctionStatus: " + e.getMessage());
        }
        return false;
    }

    public static List<String[]> getAllApprovedItems() {
        List<String[]> result = new ArrayList<>();
        String sql = "SELECT id, plate_number, start_price, current_price, description, vehicle_type, status, " +
                "seller_name, seller_phone, seller_email, seller_address, registration_end_time " +
                "FROM items WHERE approval_status = 'APPROVED' AND status = 'ACTIVE'";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                result.add(new String[]{
                        String.valueOf(rs.getInt("id")),
                        rs.getString("plate_number"),
                        String.valueOf(rs.getDouble("start_price")),
                        String.valueOf(rs.getDouble("current_price")),
                        rs.getString("description"),
                        rs.getString("vehicle_type"),
                        rs.getString("status"),
                        rs.getString("seller_name"),
                        rs.getString("seller_phone"),
                        rs.getString("seller_email"),
                        rs.getString("seller_address"),
                        rs.getString("registration_end_time")
                });
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi getAllApprovedItems: " + e.getMessage());
        }
        return result;
    }

    public static List<String[]> getAllApprovedActiveItems() {
        List<String[]> result = new ArrayList<>();
        String sql = "SELECT id, plate_number, start_price, current_price, description, vehicle_type, " +
                "status, seller_name, seller_phone, seller_email, seller_address, " +
                "registration_end_time, auction_start_time, auction_status, auction_end_time " +
                "FROM items " +
                "WHERE approval_status = 'APPROVED' AND status = 'ACTIVE' AND auction_status != 'FINISHED'";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                result.add(new String[]{
                        String.valueOf(rs.getInt("id")),              // [0]
                        rs.getString("plate_number"),                 // [1]
                        String.valueOf(rs.getDouble("start_price")), // [2]
                        String.valueOf(rs.getDouble("current_price")), // [3]
                        rs.getString("description"),                  // [4]
                        rs.getString("vehicle_type"),                 // [5]
                        rs.getString("status"),                       // [6]
                        rs.getString("seller_name"),                  // [7]
                        rs.getString("seller_phone"),                 // [8]
                        rs.getString("seller_email"),                 // [9]
                        rs.getString("seller_address"),               // [10]
                        rs.getString("registration_end_time"),        // [11]
                        rs.getString("auction_start_time"),           // [12]
                        rs.getString("auction_status"),               // [13]
                        rs.getString("auction_end_time")              // [14]
                });
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi getAllApprovedActiveItems: " + e.getMessage());
        }
        return result;
    }

    public static List<String[]> getPendingItems() {
        List<String[]> result = new ArrayList<>();
        String sql = "SELECT id, plate_number, start_price, vehicle_type, seller_name, " +
                "seller_phone, seller_email, seller_address, approval_status " +
                "FROM items WHERE approval_status = 'PENDING'";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                result.add(new String[]{
                        String.valueOf(rs.getInt("id")),
                        rs.getString("plate_number"),
                        String.valueOf(rs.getDouble("start_price")),
                        rs.getString("vehicle_type"),
                        rs.getString("seller_name"),
                        rs.getString("seller_phone"),
                        rs.getString("seller_email"),
                        rs.getString("seller_address"),
                        rs.getString("approval_status")
                });
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi getPendingItems: " + e.getMessage());
        }
        return result;
    }

    public static boolean plateExists(String plateNumber) {
        String sql = "SELECT COUNT(*) FROM items WHERE plate_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi plateExists: " + e.getMessage());
        }
        return false;
    }

    // =====================================================================
    // 3. CẬP NHẬT TRẠNG THÁI VÀ GIÁ
    // =====================================================================

    public static boolean approveItem(String plateNumber) {
        String sql = "UPDATE items SET approval_status = 'APPROVED' WHERE plate_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);
            if (pstmt.executeUpdate() > 0) {
                System.out.println("✅ Duyệt biển '" + plateNumber + "' thành công");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi approveItem: " + e.getMessage());
        }
        return false;
    }

    public static boolean rejectItem(String plateNumber) {
        String sql = "UPDATE items SET approval_status = 'REJECTED' WHERE plate_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);
            if (pstmt.executeUpdate() > 0) {
                System.out.println("✅ Từ chối biển '" + plateNumber + "' thành công");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi rejectItem: " + e.getMessage());
        }
        return false;
    }

    public static void updateCurrentPrice(String plateNumber, double newPrice) {
        String sql = "UPDATE items SET current_price = ? WHERE plate_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, newPrice);
            pstmt.setString(2, plateNumber);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("❌ Lỗi updateCurrentPrice: " + e.getMessage());
        }
    }

    public static void updateCurrentPrice(int itemId, double newPrice) {
        String sql = "UPDATE items SET current_price = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, newPrice);
            pstmt.setInt(2, itemId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("❌ Lỗi updateCurrentPrice(id): " + e.getMessage());
        }
    }

    public static boolean hideSelectedItems(List<String> plates) {
        if (plates == null || plates.isEmpty()) return false;
        String sql = "UPDATE items SET status = 'INACTIVE' WHERE plate_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (String plate : plates) {
                pstmt.setString(1, plate);
                pstmt.addBatch();
            }
            int[] results = pstmt.executeBatch();
            return results.length > 0;
        } catch (SQLException e) {
            System.out.println("❌ Lỗi hideSelectedItems: " + e.getMessage());
            return false;
        }
    }

    // =====================================================================
    // 4. QUẢN LÝ THỜI GIAN VÀ NGƯỜI THAM GIA
    // =====================================================================

    public static boolean startRegistrationPhase(String plateNumber, int regMinutes, int aucMinutes) {
        String sql = "UPDATE items SET " +
                "registration_start_time = NOW(), " +
                "registration_end_time = DATE_ADD(NOW(), INTERVAL ? MINUTE), " +
                "auction_start_time = DATE_ADD(NOW(), INTERVAL ? MINUTE), " +
                "auction_end_time = DATE_ADD(DATE_ADD(NOW(), INTERVAL ? MINUTE), INTERVAL ? MINUTE), " +
                "registration_duration = ?, " +
                "auction_status = 'REGISTRATION' " +
                "WHERE plate_number = ? AND approval_status = 'APPROVED'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, regMinutes);
            pstmt.setInt(2, regMinutes);
            pstmt.setInt(3, regMinutes);
            pstmt.setInt(4, aucMinutes);
            pstmt.setInt(5, regMinutes);
            pstmt.setString(6, plateNumber);

            if (pstmt.executeUpdate() > 0) {
                System.out.println("✅ Đã setup thời gian cho biển '" + plateNumber + "'");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi startRegistrationPhase: " + e.getMessage());
        }
        return false;
    }

    public static void incrementParticipantCount(String plateNumber) {
        String sql = "UPDATE items SET participant_count = participant_count + 1 WHERE plate_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("❌ Lỗi incrementParticipantCount: " + e.getMessage());
        }
    }

    public static int getParticipantCount(String plateNumber) {
        String sql = "SELECT participant_count FROM items WHERE plate_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("participant_count");
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi getParticipantCount: " + e.getMessage());
        }
        return 0;
    }

    public static long getAuctionTimeRemaining(String plateNumber) {
        String sql = "SELECT UNIX_TIMESTAMP(auction_end_time) - UNIX_TIMESTAMP(NOW()) AS remaining " +
                "FROM items WHERE plate_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return Math.max(rs.getLong("remaining"), 0);
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi getAuctionTimeRemaining: " + e.getMessage());
        }
        return 0;
    }

    public static boolean finishAuction(int itemId, double finalPrice) {
        String sql = "UPDATE items SET auction_status = 'FINISHED', current_price = ?, " +
                "auction_end_time = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, finalPrice);
            ps.setInt(2, itemId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("❌ Lỗi finishAuction: " + e.getMessage());
            return false;
        }
    }

    public static void autoFinishAuction(String plateNumber) {
        String sql = "SELECT auction_end_time, auction_status, current_price FROM items WHERE plate_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    double currentPrice = rs.getDouble("current_price");
                    Timestamp endTimeTs = rs.getTimestamp("auction_end_time");
                    String status = rs.getString("auction_status");

                    if (endTimeTs != null && "AUCTION".equalsIgnoreCase(status)) {

                        LocalDateTime now = LocalDateTime.now();
                        LocalDateTime endTime = endTimeTs.toLocalDateTime();

                        if (now.isAfter(endTime)) {

                            int itemId = getItemIdByPlate(plateNumber);

                            if (itemId != -1) {

                                boolean finished = finishAuction(itemId, currentPrice);

                                if (finished) {
                                    System.out.println("✅ AUTO FINISHED: " + plateNumber);
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi autoFinishAuction: " + e.getMessage());
        }
    }

    public static long getLastBidTime(String plateNumber, int userId) {
        String sql = "SELECT UNIX_TIMESTAMP(bid_time) FROM bids " +
                "WHERE item_id = (SELECT id FROM items WHERE plate_number = ?) " +
                "AND user_id = ? ORDER BY bid_time DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi getLastBidTime: " + e.getMessage());
        }
        return 0;
    }

    // =====================================================================
    // 5. LẤY KẾT QUẢ ĐẤU GIÁ
    // =====================================================================

    public static List<String[]> getFinishedAuctions() {
        List<String[]> result = new ArrayList<>();
        String sql = "SELECT id, plate_number, start_price, current_price, vehicle_type, seller_name, " +
                "auction_status, auction_end_time FROM items " +
                "WHERE auction_status = 'FINISHED' ORDER BY auction_end_time DESC LIMIT 50";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                result.add(new String[]{
                        String.valueOf(rs.getInt("id")),
                        rs.getString("plate_number"),
                        String.valueOf(rs.getDouble("start_price")),
                        String.valueOf(rs.getDouble("current_price")),
                        rs.getString("vehicle_type"),
                        rs.getString("seller_name"),
                        rs.getString("auction_status"),
                        rs.getString("auction_end_time")
                });
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi getFinishedAuctions: " + e.getMessage());
        }
        return result;
    }

    public static List<String[]> getFinishedAuctionsByUserId(int userId) {
        List<String[]> results = new ArrayList<>();
        String sql = "SELECT DISTINCT i.id, i.plate_number, i.start_price, i.current_price, i.vehicle_type, " +
                "i.auction_status, i.auction_end_time FROM items i JOIN bids b ON i.id = b.item_id " +
                "WHERE i.auction_status = 'FINISHED' AND b.user_id = ? ORDER BY i.auction_end_time DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new String[]{
                            String.valueOf(rs.getInt("id")),
                            rs.getString("plate_number"),
                            String.valueOf(rs.getDouble("start_price")),
                            String.valueOf(rs.getDouble("current_price")),
                            rs.getString("vehicle_type"),
                            "N/A",
                            rs.getString("auction_status"),
                            rs.getString("auction_end_time")
                    });
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi getFinishedAuctionsByUserId: " + e.getMessage());
        }
        return results;
    }

    // =====================================================================
    // 6. ADMIN GIÁM SÁT REALTIME — DỮ LIỆU ĐẦY ĐỦ
    // =====================================================================

    /**
     * Lấy dữ liệu đầy đủ cho trang giám sát Admin.
     * Trả về: [0]id [1]plate [2]vehicle_type [3]auction_status [4]current_price
     *         [5]auction_end_time [6]registration_end_time [7]participant_count
     *         [8]highest_bidder_name [9]highest_bid_amount
     *
     * Bao gồm cả phiên FINISHED trong 24 giờ qua để Admin xem kết quả.
     */
    public static List<String[]> getFullMonitoringData() {
        List<String[]> result = new ArrayList<>();
        String sql =
                "SELECT i.id, i.plate_number, i.vehicle_type, i.auction_status, " +
                        "       i.current_price, i.auction_end_time, i.registration_end_time, " +
                        "       i.participant_count, " +
                        "       (SELECT u.full_name FROM bids b " +
                        "        JOIN users u ON b.user_id = u.id " +
                        "        WHERE b.item_id = i.id " +
                        "        ORDER BY b.bid_amount DESC LIMIT 1) AS highest_bidder, " +
                        "       (SELECT b.bid_amount FROM bids b " +
                        "        WHERE b.item_id = i.id " +
                        "        ORDER BY b.bid_amount DESC LIMIT 1) AS highest_bid " +
                        "FROM items i " +
                        "WHERE i.approval_status = 'APPROVED' " +
                        "  AND i.auction_status IN ('REGISTRATION', 'AUCTION', 'FINISHED') " +
                        "  AND (i.auction_status != 'FINISHED' " +
                        "       OR i.auction_end_time >= DATE_SUB(NOW(), INTERVAL 24 HOUR)) " +
                        "ORDER BY " +
                        "  CASE i.auction_status " +
                        "    WHEN 'AUCTION' THEN 1 " +
                        "    WHEN 'REGISTRATION' THEN 2 " +
                        "    WHEN 'FINISHED' THEN 3 " +
                        "  END, " +
                        "  i.auction_end_time ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String highestBid = rs.getString("highest_bid");
                result.add(new String[]{
                        String.valueOf(rs.getInt("id")),             // [0] id
                        rs.getString("plate_number"),                // [1] biển số
                        rs.getString("vehicle_type"),                // [2] loại xe
                        rs.getString("auction_status"),              // [3] trạng thái
                        String.valueOf(rs.getDouble("current_price")), // [4] giá hiện tại
                        rs.getString("auction_end_time"),            // [5] thời gian kết thúc đấu giá
                        rs.getString("registration_end_time"),       // [6] thời gian kết thúc đăng ký
                        String.valueOf(rs.getInt("participant_count")), // [7] người tham gia
                        rs.getString("highest_bidder") != null ? rs.getString("highest_bidder") : "Chưa có", // [8]
                        highestBid != null ? highestBid : "0"        // [9] giá cao nhất
                });
            }
            System.out.println("✅ Monitoring data: " + result.size() + " phiên");

        } catch (SQLException e) {
            System.out.println("❌ Lỗi getFullMonitoringData: " + e.getMessage());
        }
        return result;
    }

    /**
     * Lấy thông tin người thắng cuộc của 1 phiên.
     * Trả về: [0]user_id [1]full_name [2]bid_amount
     */
    public static String[] getWinnerByPlate(String plateNumber) {
        String sql = "SELECT b.user_id, u.full_name, b.bid_amount " +
                "FROM bids b JOIN users u ON b.user_id = u.id " +
                "WHERE b.item_id = (SELECT id FROM items WHERE plate_number = ?) " +
                "ORDER BY b.bid_amount DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new String[]{
                            String.valueOf(rs.getInt("user_id")),
                            rs.getString("full_name"),
                            String.valueOf(rs.getDouble("bid_amount"))
                    };
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi getWinnerByPlate: " + e.getMessage());
        }
        return null;
    }

    /**
     * Lấy số tiền cao nhất mà 1 user đã đặt trong phiên (để thông báo kết quả).
     */
    public static double getUserHighestBid(String plateNumber, int userId) {
        String sql = "SELECT MAX(bid_amount) FROM bids " +
                "WHERE item_id = (SELECT id FROM items WHERE plate_number = ?) AND user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi getUserHighestBid: " + e.getMessage());
        }
        return 0;
    }

    public static List<String[]> getAuctionsForMonitoring() {
        List<String[]> result = new ArrayList<>();
        String sql =
                "SELECT plate_number, vehicle_type, auction_status, current_price, " +
                        "registration_end_time, auction_end_time, participant_count " +
                        "FROM items " +
                        "WHERE approval_status = 'APPROVED' " +
                        "AND auction_status IN ('REGISTRATION', 'AUCTION') " +
                        "ORDER BY auction_end_time ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                result.add(new String[]{
                        rs.getString("plate_number"),
                        rs.getString("vehicle_type"),
                        rs.getString("auction_status"),
                        String.valueOf(rs.getDouble("current_price")),
                        rs.getString("registration_end_time"),
                        rs.getString("auction_end_time"),
                        String.valueOf(rs.getInt("participant_count"))
                });
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi getAuctionsForMonitoring: " + e.getMessage());
        }
        return result;
    }

    public static int getItemIdByPlate(String plate) {
        String sql = "SELECT id FROM items WHERE plate_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, plate);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException e) {
        System.out.println("❌ Lỗi getItemIdByPlate: " + e.getMessage());
    }
        return -1;
    }

    public static boolean updateStatus(int itemId, String status) {
        String sql = "UPDATE items SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, itemId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("❌ Lỗi updateStatus: " + e.getMessage());
            return false;
        }
    }

    // =====================================================================
    // 7. AUTO-EXTEND TIMER
    // =====================================================================

    public static boolean extendAuctionTime(int itemId, int minutes) {
        String sql = "UPDATE items SET auction_end_time = DATE_ADD(auction_end_time, INTERVAL ? MINUTE) " +
                "WHERE id = ? AND auction_status = 'AUCTION'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, minutes);
            pstmt.setInt(2, itemId);
            if (pstmt.executeUpdate() > 0) {
                System.out.println("⏱️ AUTO-EXTEND: item " + itemId + " kéo dài " + minutes + " phút");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi extendAuctionTime: " + e.getMessage());
        }
        return false;
    }

    public static long getAuctionTimeRemainingSeconds(int itemId) {
        String sql = "SELECT UNIX_TIMESTAMP(auction_end_time) - UNIX_TIMESTAMP(NOW()) AS remaining " +
                "FROM items WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, itemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return Math.max(rs.getLong("remaining"), 0);
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi getAuctionTimeRemainingSeconds: " + e.getMessage());
        }
        return 0;
    }

    public static boolean updateCurrentPriceIfHigher(
            String plateNumber,
            double oldPrice,
            double newPrice
    ) {

        String sql =
                "UPDATE items " +
                        "SET current_price = ? " +
                        "WHERE plate_number = ? " +
                        "AND current_price = ? " +
                        "AND ? > current_price";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, newPrice);
            ps.setString(2, plateNumber);
            ps.setDouble(3, oldPrice);
            ps.setDouble(4, newPrice);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {

            System.out.println("❌ Lỗi updateCurrentPriceIfHigher: " + e.getMessage());
            return false;
        }
    }

    public static double getCurrentPrice(String plateNumber) {
        String sql = "SELECT current_price FROM items WHERE plate_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, plateNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("current_price");
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi getCurrentPrice: " + e.getMessage());
        }
        return 0;
    }
    public static boolean extendAuctionTime(
            Connection conn,
            int itemId,
            int minutes
    ) {

        String sql =
                "UPDATE items " +
                        "SET auction_end_time = DATE_ADD(auction_end_time, INTERVAL ? MINUTE) " +
                        "WHERE id = ? AND auction_status = 'AUCTION'";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, minutes);
            pstmt.setInt(2, itemId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {

            System.out.println("❌ Lỗi extendAuctionTime(conn): " + e.getMessage());
            return false;
        }
    }
    /**
     * Dùng để cập nhật trực tiếp auction_status.
     * Rất quan trọng khi phiên chuyển từ REGISTRATION -> AUCTION -> FINISHED
     */
    public static boolean setAuctionStatus(int itemId, String status) {
        String sql = "UPDATE items SET auction_status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, itemId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("❌ Lỗi setAuctionStatus: " + e.getMessage());
        }
        return false;
    }

    /**
     * Lấy trạng thái của item theo ID thay vì theo biển số.
     * Dùng cho AuctionSyncManager để check trạng thái liên tục.
     */
    public static String getAuctionStatusById(int itemId) {
        String sql = "SELECT auction_status FROM items WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("auction_status");
        } catch (SQLException e) {
            System.out.println("❌ Lỗi getAuctionStatusById: " + e.getMessage());
        }
        return "UNKNOWN";
    }

    /**
     * Tăng số lượng người tham gia an toàn bằng Transaction.
     * Đảm bảo không bị mất dữ liệu khi 2 người cùng vào phòng 1 lúc.
     */
    public static void incrementParticipantCount(int itemId) {
        String sql = "UPDATE items SET participant_count = participant_count + 1 WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("❌ Lỗi incrementParticipantCount(id): " + e.getMessage());
        }
    }
}