package com.auction.database;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ItemDAO - Quản lý biển số xe (items)
 * Đã chuẩn hóa: Căn lề code, xóa các dòng trống và chú thích thừa.
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
            pstmt.setDouble(3, startPrice); // current_price = start_price lúc đầu
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

        String sql =
                "SELECT * FROM items WHERE plate_number = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {

                    // =====================================
                    // TỰ CHUYỂN SANG AUCTION
                    // =====================================

                    String registrationEnd =
                            rs.getString("registration_end_time");

                    String auctionStatus =
                            rs.getString("auction_status");

                    if (registrationEnd != null &&
                            "REGISTRATION".equalsIgnoreCase(auctionStatus)) {

                        LocalDateTime now =
                                LocalDateTime.now();

                        LocalDateTime endTime =
                                Timestamp.valueOf(registrationEnd)
                                        .toLocalDateTime();

                        if (now.isAfter(endTime)) {

                            String updateSql =
                                    "UPDATE items " +
                                            "SET auction_status = 'AUCTION' " +
                                            "WHERE plate_number = ?";

                            try (PreparedStatement updateStmt =
                                         conn.prepareStatement(updateSql)) {

                                updateStmt.setString(1, plateNumber);

                                updateStmt.executeUpdate();

                                auctionStatus = "AUCTION";

                                System.out.println(
                                        "✅ Đã chuyển sang AUCTION: "
                                                + plateNumber
                                );
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

            System.out.println(
                    "❌ Lỗi getItemFullData: "
                            + e.getMessage()
            );
        }

        return null;
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
        String sql =
                "SELECT id, plate_number, start_price, current_price, description, vehicle_type, " +
                        "status, seller_name, seller_phone, seller_email, seller_address, " +
                        "registration_end_time, auction_start_time, auction_end_time, auction_status " +
                        "FROM items " +
                        "WHERE approval_status = 'APPROVED'";

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
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
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
            if (pstmt.executeUpdate() > 0) {
                System.out.println("✅ Cập nhật giá thành công");
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi updateCurrentPrice: " + e.getMessage());
        }
    }

    // =====================================================================
    // 4. QUẢN LÝ THỜI GIAN VÀ NGƯỜI THAM GIA ĐẤU GIÁ
    // =====================================================================

    public static boolean startRegistrationPhase(String plateNumber, int durationMinutes) {
        String sql = "UPDATE items SET " +
                "registration_start_time = NOW(), " +
                "registration_end_time = DATE_ADD(NOW(), INTERVAL ? MINUTE), " +
                "auction_start_time = DATE_ADD(NOW(), INTERVAL ? MINUTE), " +
                "auction_end_time = DATE_ADD(DATE_ADD(NOW(), INTERVAL ? MINUTE), INTERVAL 60 MINUTE), " +
                "registration_duration = ?, " +
                "auction_status = 'REGISTRATION' " +
                "WHERE plate_number = ? AND approval_status = 'APPROVED'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, durationMinutes);
            pstmt.setInt(2, durationMinutes);
            pstmt.setInt(3, durationMinutes);
            pstmt.setInt(4, durationMinutes);
            pstmt.setString(5, plateNumber);

            if (pstmt.executeUpdate() > 0) {
                System.out.println("✅ Bắt đầu giai đoạn đăng ký cho biển '" + plateNumber + "'");
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
            if (pstmt.executeUpdate() > 0) {
                System.out.println("✅ Tăng số người tham gia cho biển '" + plateNumber + "' thành công");
            }
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
                if (rs.next()) {
                    return rs.getInt("participant_count");
                }
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
                if (rs.next()) {
                    return Math.max(rs.getLong("remaining"), 0);
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi getAuctionTimeRemaining: " + e.getMessage());
        }
        return 0;
    }

    public static boolean finishAuction(int itemId, double finalPrice) {
        String sql = """
        UPDATE items
        SET auction_status = 'FINISHED',
            current_price = ?,
            auction_end_time = NOW()
        WHERE id = ?
    """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, finalPrice);
            ps.setInt(2, itemId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    public static void autoFinishAuction(String plateNumber) {

        String sql =
                "SELECT auction_end_time, auction_status, current_price " +
                        "FROM items WHERE plate_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    double currentPrice = rs.getDouble("current_price");

                    String endTimeStr =
                            rs.getString("auction_end_time");

                    String status =
                            rs.getString("auction_status");

                    if (endTimeStr != null &&
                            "AUCTION".equalsIgnoreCase(status)) {

                        LocalDateTime now =
                                LocalDateTime.now();

                        LocalDateTime endTime =
                                Timestamp.valueOf(endTimeStr)
                                        .toLocalDateTime();

                        if (now.isAfter(endTime)) {

                            int itemId = getItemIdByPlate(plateNumber);

                            if (itemId != -1) {
                                finishAuction(itemId, currentPrice);
                            }

                            System.out.println(
                                    "✅ AUTO FINISHED: "
                                            + plateNumber
                            );
                        }
                    }
                }
            }

        } catch (SQLException e) {

            System.out.println(
                    "❌ Lỗi autoFinishAuction: "
                            + e.getMessage()
            );
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
                if (rs.next()) {
                    return rs.getLong(1);
                }
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
        String sql = "SELECT id, plate_number, start_price, current_price, vehicle_type, seller_name, auction_status, auction_end_time " +
                "FROM items WHERE auction_status = 'FINISHED' ORDER BY auction_end_time DESC LIMIT 50";

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
        String sql = "SELECT DISTINCT i.id, i.plate_number, i.start_price, i.current_price, i.vehicle_type, i.auction_status, i.auction_end_time " +
                "FROM items i JOIN bids b ON i.id = b.item_id " +
                "WHERE i.auction_status = 'FINISHED' AND b.user_id = ? " +
                "ORDER BY i.auction_end_time DESC";

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
                            "N/A", // seller_name
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
    /**
     * Lấy các phiên đấu giá đang hoạt động để Admin giám sát
     * Trả về: [0]plate [1]vehicle_type [2]auction_status [3]current_price
     *         [4]registration_end_time [5]auction_end_time [6]participant_count
     */
    public static List<String[]> getAuctionsForMonitoring() {
        List<String[]> result = new ArrayList<>();
        String sql =
                "SELECT plate_number, vehicle_type, auction_status, current_price, " +
                        "       registration_end_time, auction_end_time, participant_count " +
                        "FROM items " +
                        "WHERE approval_status = 'APPROVED' " +
                        "  AND auction_status IN ('REGISTRATION', 'AUCTION') " +
                        "ORDER BY auction_end_time ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                result.add(new String[]{
                        rs.getString("plate_number"),                          // [0]
                        rs.getString("vehicle_type"),                          // [1]
                        rs.getString("auction_status"),                        // [2]
                        String.valueOf(rs.getDouble("current_price")),         // [3]
                        rs.getString("registration_end_time"),                 // [4]
                        rs.getString("auction_end_time"),                      // [5]
                        String.valueOf(rs.getInt("participant_count"))         // [6]
                });
            }
            System.out.println("✅ Đã tải " + result.size() + " phiên đang giám sát");

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
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            }

        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
            return false;
        }
    }
}