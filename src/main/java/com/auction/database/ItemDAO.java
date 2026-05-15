package com.auction.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ItemDAO - Quản lý biển số xe (items)
 */
public class ItemDAO {

    // Thêm item (biển số xe) mới
    public static void addItem(String plateNumber, double startPrice, String description,
                               String vehicleType, String status) {
        String sql = "INSERT INTO items (plate_number, start_price, current_price, description, vehicle_type, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);
            pstmt.setDouble(2, startPrice);
            pstmt.setDouble(3, startPrice);  // current_price = start_price lúc đầu
            pstmt.setString(4, description);
            pstmt.setString(5, vehicleType);
            pstmt.setString(6, status);

            pstmt.executeUpdate();
            System.out.println("✅ Thêm item biển '" + plateNumber + "' thành công");

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }

    // Thêm item mới từ người bán (với thông tin seller)
    // ItemDAO.java
    public static void addItemFromSeller(String plateNumber, double startPrice, String description,
                                         String vehicleType, String sellerName, String sellerPhone,
                                         String sellerEmail, String sellerAddress) throws SQLException {
        // ✅ Thêm 'throws SQLException' để báo cho Service biết là hàm này có thể gây lỗi DB

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


    // Lấy item theo plate_number - trả về ResultSet
    // Lấy thông tin chi tiết của 1 biển số (Đã tối ưu chống rò rỉ bộ nhớ)
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
    // Lấy tất cả items - trả về List
    public static List<String[]> getAllApprovedItems() {
        List<String[]> result = new ArrayList<>();
        // SỬA: Thêm cột seller_address vào câu query
        String sql = "SELECT id, plate_number, start_price, current_price, description, vehicle_type, status, " +
                "seller_name, seller_phone, seller_email, seller_address FROM items " +
                "WHERE approval_status = 'APPROVED' AND status = 'ACTIVE'";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String[] item = {
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
                        rs.getString("seller_address") // SỬA: Lấy thêm địa chỉ từ CSDL (Đây sẽ là p[10])
                };
                result.add(item);
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }

        return result;
    }

    // Lấy tất cả items chờ duyệt (PENDING)
    public static List<String[]> getPendingItems() {
        List<String[]> result = new ArrayList<>();
        String sql = "SELECT id, plate_number, start_price, vehicle_type, seller_name, " +
                     "seller_phone, seller_email, seller_address, approval_status FROM items " +
                     "WHERE approval_status = 'PENDING'";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String[] item = {
                    String.valueOf(rs.getInt("id")),
                    rs.getString("plate_number"),
                    String.valueOf(rs.getDouble("start_price")),
                    rs.getString("vehicle_type"),
                    rs.getString("seller_name"),
                    rs.getString("seller_phone"),
                    rs.getString("seller_email"),
                    rs.getString("seller_address"),
                    rs.getString("approval_status")
                };
                result.add(item);
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }

        return result;
    }

    // Duyệt biển số (thay đổi từ PENDING thành APPROVED)
    public static boolean approveItem(String plateNumber) {
        String sql = "UPDATE items SET approval_status = 'APPROVED' WHERE plate_number = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);
            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("✅ Duyệt biển '" + plateNumber + "' thành công");
                return true;
            } else {
                System.out.println("❌ Không tìm thấy biển '" + plateNumber + "'");
                return false;
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
            return false;
        }
    }

    // Từ chối biển số (xóa hoặc đánh dấu REJECTED)
    public static boolean rejectItem(String plateNumber) {
        String sql = "UPDATE items SET approval_status = 'REJECTED' WHERE plate_number = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);
            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("✅ Từ chối biển '" + plateNumber + "' thành công");
                return true;
            } else {
                System.out.println("❌ Không tìm thấy biển '" + plateNumber + "'");
                return false;
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
            return false;
        }
    }

    // Kiểm tra biển số đã tồn tại chưa
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
            System.out.println("❌ Lỗi: " + e.getMessage());
        }

        return false;
    }

    // Cập nhật giá hiện tại của item
    public static void updateCurrentPrice(String plateNumber, double newPrice) {
        String sql = "UPDATE items SET current_price = ? WHERE plate_number = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, newPrice);
            pstmt.setString(2, plateNumber);

            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("✅ Cập nhật giá thành công");
            } else {
                System.out.println("❌ Không tìm thấy biển số để cập nhật");
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }

    // =====================================================================
    // PHẦN MỚI: QUẢN LÝ THỜI GIAN VÀ NGƯỜI THAM GIA
    // =====================================================================

    // Bắt đầu giai đoạn đăng ký (khi admin duyệt biển số)
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

            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("✅ Bắt đầu giai đoạn đăng ký cho biển '" + plateNumber + "'");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
        return false;
    }

    // Tăng số người tham gia
    public static void incrementParticipantCount(String plateNumber) {
        String sql = "UPDATE items SET participant_count = participant_count + 1 WHERE plate_number = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);

            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("✅ Tăng số người tham gia cho biển '" + plateNumber + "' thành công");
            } else {
                System.out.println("❌ Không tìm thấy biển số '" + plateNumber + "'");
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }

    // Lấy số người tham gia
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
            System.out.println("❌ Lỗi: " + e.getMessage());
        }

        return 0;
    }

    // Lấy thời gian đấu giá còn lại (seconds)
    public static long getAuctionTimeRemaining(String plateNumber) {
        String sql = "SELECT UNIX_TIMESTAMP(auction_end_time) - UNIX_TIMESTAMP(NOW()) AS remaining " +
                "FROM items WHERE plate_number = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    long remaining = rs.getLong("remaining");

                    // Không cho trả về số âm
                    return Math.max(remaining, 0);
                }
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi getAuctionTimeRemaining: " + e.getMessage());
        }

        return 0;
    }
    // Hoàn thành phiên đấu giá
    public static void finishAuction(String plateNumber) {
        String sql = "UPDATE items SET auction_status = 'FINISHED', status = 'INACTIVE' " +
                "WHERE plate_number = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);

            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("✅ Phiên đấu giá kết thúc cho biển '" + plateNumber + "'");
            } else {
                System.out.println("❌ Không tìm thấy biển '" + plateNumber + "'");
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }
    // Lấy tất cả items chỉ những cái ACTIVE (chưa kết thúc) cho Dashboard
    public static List<String[]> getAllApprovedActiveItems() {
        List<String[]> result = new ArrayList<>();
        String sql = "SELECT id, plate_number, start_price, current_price, description, vehicle_type, status, " +
                "seller_name, seller_phone, seller_email, seller_address, auction_status " +
                "FROM items WHERE approval_status = 'APPROVED' AND status = 'ACTIVE' AND auction_status != 'FINISHED'";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String[] item = {
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
                        rs.getString("auction_status")
                };
                result.add(item);
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }

        return result;
    }

    // Kiểm tra lần đặt giá cuối cùng của user (để tính cooldown)
    public static long getLastBidTime(String plateNumber, int userId) {
        String sql = "SELECT UNIX_TIMESTAMP(bid_time) FROM bids " +
                "WHERE auction_id = (SELECT id FROM auctions WHERE item_id = " +
                "(SELECT id FROM items WHERE plate_number = ?)) " +
                "AND bidder_id = ? ORDER BY bid_time DESC LIMIT 1";

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
            System.out.println("❌ Lỗi: " + e.getMessage());
        }

        return 0;
    }
    public static String[] getItemFullData(String plateNumber) {
        // Câu lệnh này lấy đầy đủ 11-12 cột từ bảng items
        String sql = "SELECT * FROM items WHERE plate_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, plateNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new String[]{
                            String.valueOf(rs.getInt("id")),           // [0]
                            rs.getString("plate_number"),             // [1]
                            rs.getString("start_price"),              // [2]
                            rs.getString("current_price"),            // [3]
                            rs.getString("description"),              // [4]
                            rs.getString("vehicle_type"),             // [5]
                            rs.getString("seller_name"),              // [6]
                            rs.getString("registration_start_time"),   // [7]
                            rs.getString("registration_end_time"),     // [8]
                            rs.getString("auction_status"),           // [9]
                            String.valueOf(rs.getInt("participant_count")) // [10]
                    };
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi getItemFullData: " + e.getMessage());
        }
        return null;
    }

    // ===== PHƯƠNG THỨC MỚI: Lấy kết quả đấu giá =====

    // Lấy tất cả items đã kết thúc phiên (FINISHED)
    public static List<String[]> getFinishedAuctions() {
        List<String[]> result = new ArrayList<>();
        String sql = "SELECT id, plate_number, start_price, current_price, vehicle_type, seller_name, auction_status, auction_end_time " +
                "FROM items WHERE auction_status = 'FINISHED' " +
                "ORDER BY auction_end_time DESC LIMIT 50";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String[] item = {
                        String.valueOf(rs.getInt("id")),
                        rs.getString("plate_number"),
                        String.valueOf(rs.getDouble("start_price")),
                        String.valueOf(rs.getDouble("current_price")),
                        rs.getString("vehicle_type"),
                        rs.getString("seller_name"),
                        rs.getString("auction_status"),
                        rs.getString("auction_end_time")
                };
                result.add(item);
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }

        return result;
    }
}

