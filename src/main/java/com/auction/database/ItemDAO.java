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
    public static void addItemFromSeller(String plateNumber, double startPrice, String description,
                                         String vehicleType, String sellerName, String sellerPhone,
                                         String sellerEmail, String sellerAddress) {
        String sql = "INSERT INTO items (plate_number, start_price, current_price, description, vehicle_type, status, " +
                     "approval_status, seller_name, seller_phone, seller_email, seller_address) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);
            pstmt.setDouble(2, startPrice);
            pstmt.setDouble(3, startPrice);  // current_price = start_price lúc đầu
            pstmt.setString(4, description);
            pstmt.setString(5, vehicleType);
            pstmt.setString(6, "ACTIVE");  // status = ACTIVE
            pstmt.setString(7, "PENDING"); // approval_status = PENDING (chờ duyệt)
            pstmt.setString(8, sellerName);
            pstmt.setString(9, sellerPhone);
            pstmt.setString(10, sellerEmail);
            pstmt.setString(11, sellerAddress);

            pstmt.executeUpdate();
            System.out.println("✅ Thêm item (chờ duyệt) biển '" + plateNumber + "' thành công");

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }


    // Lấy item theo plate_number - trả về ResultSet
    public static ResultSet getItemByPlate(String plateNumber) {
        String sql = "SELECT * FROM items WHERE plate_number = ?";

        try {
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, plateNumber);
            return pstmt.executeQuery();

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
            return null;
        }
    }
    public static String[] getItemDataByPlate(String plateNumber) {
        String sql = "SELECT * FROM items WHERE plate_number = ?";
        // Dùng try-with-resources để tự động đóng Connection, PreparedStatement và ResultSet
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, plateNumber);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new String[]{
                            rs.getString("id"),
                            rs.getString("plate_number"),
                            rs.getString("start_price"),
                            rs.getString("vehicle_type"),
                            rs.getString("approval_status")
                    };
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
        return null;
    }

    // Lấy tất cả items - trả về List
    public static List<String[]> getAllApprovedItems() {
        List<String[]> result = new ArrayList<>();
        String sql = "SELECT id, plate_number, start_price, current_price, description, vehicle_type, status, " +
                     "seller_name, seller_phone, seller_email FROM items " +
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
                    rs.getString("seller_email")
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
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
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
            }
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }
}

