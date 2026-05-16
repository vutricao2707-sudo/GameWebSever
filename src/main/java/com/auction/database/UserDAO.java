package com.auction.database;

import com.auction.model.User;
import com.auction.model.Seller;
import com.auction.model.Bidder;
import com.auction.util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO = Data Access Object
 * Đã chuẩn hóa code: Cắt khoảng trắng dư thừa, đưa username về chữ thường để chống lỗi.
 */
public class UserDAO {

    // =========================================================================
    // PHẦN MỚI: Kiểm tra username bị cấm và username đã tồn tại
    // =========================================================================

    private static final String[] RESERVED_USERNAMES = {
            "vpa_admin", "admin", "administrator", "root", "system"
    };

    public static boolean isReservedUsername(String username) {
        String cleanUsername = username.trim().toLowerCase(); // Cải tiến: Chuẩn hóa chữ
        for (String reserved : RESERVED_USERNAMES) {
            if (reserved.equals(cleanUsername)) {
                return true;
            }
        }
        return false;
    }

    public static boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username.trim().toLowerCase()); // Cải tiến: Tránh lỗi dư dấu cách
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi kiểm tra username: " + e.getMessage());
        }
        return false;
    }

    // =========================================================================
    // PHẦN GIỮ NGUYÊN TỪ CODE CŨ (Đã chuẩn hóa định dạng)
    // =========================================================================

    public static boolean addUser(String username, String password, String email,
                                  String fullName, String userType, String phone, String address) {

        String hashedPassword = PasswordUtil.hashPassword(password);
        String sql = "INSERT INTO users (username, password, email, full_name, user_type, phone, address) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username.trim().toLowerCase());
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, email);
            pstmt.setString(4, fullName);
            pstmt.setString(5, userType);
            pstmt.setString(6, phone);
            pstmt.setString(7, address);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("✅ Thêm user '" + username + "' thành công");
                return true;
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
        return false;
    }

    public static boolean addUser(String username, String password, String email,
                                  String fullName, String userType, String phone) {
        return addUser(username, password, email, fullName, userType, phone, "");
    }

    public static User getUserByUsernameAndPassword(String username, String password) {
        String hashedPassword = PasswordUtil.hashPassword(password);
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username.trim().toLowerCase());
            pstmt.setString(2, hashedPassword);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String userId   = String.valueOf(rs.getInt("id"));
                String fullName = rs.getString("full_name");
                String email    = rs.getString("email");
                String phone    = rs.getString("phone");
                String address  = rs.getString("address");
                String userType = rs.getString("user_type");

                // Cải tiến: Dùng equalsIgnoreCase để tránh lỗi chữ hoa/thường (vd: "seller" hay "Seller" đều nhận)
                if ("Seller".equalsIgnoreCase(userType) || "Admin".equalsIgnoreCase(userType)) {
                    return new Seller(userId, username, fullName, hashedPassword, email, phone, address, 5.0);
                } else {
                    return new Bidder(userId, username, fullName, hashedPassword, email, phone, address, 0.0);
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi lấy thông tin user: " + e.getMessage());
        }
        return null;
    }

    public static void findUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username.trim().toLowerCase());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                System.out.println("\n📌 Thông tin user:");
                System.out.println("  ID: "       + rs.getInt("id"));
                System.out.println("  Username: " + rs.getString("username"));
                System.out.println("  Email: "    + rs.getString("email"));
                System.out.println("  Tên: "      + rs.getString("full_name"));
                System.out.println("  Loại: "     + rs.getString("user_type"));
                System.out.println("  SĐT: "      + rs.getString("phone") + "\n");
            } else {
                System.out.println("❌ Không tìm thấy user '" + username + "'");
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }

    public static void getAllUsers() {
        String sql = "SELECT id, username, email, full_name, user_type FROM users";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n📌 Danh sách users:");
            while (rs.next()) {
                System.out.println("  " + rs.getInt("id") + ". " + rs.getString("username") +
                        " (" + rs.getString("user_type") + ")");
            }
            System.out.println();

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }

    public static void deleteUser(String username) {
        String sql = "DELETE FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username.trim().toLowerCase());
            int rowsDeleted = pstmt.executeUpdate();

            if (rowsDeleted > 0) {
                System.out.println("✅ Xóa user '" + username + "' thành công");
            } else {
                System.out.println("❌ Không tìm thấy user '" + username + "'");
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }

    public static boolean checkLogin(String username, String password) {
        String hashedPassword = PasswordUtil.hashPassword(password);
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username.trim().toLowerCase());
            pstmt.setString(2, hashedPassword);

            ResultSet rs = pstmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lấy toàn bộ danh sách user cho Admin xem (Đã xóa các dòng trắng thừa)
     */
    public static List<String[]> getAllUsersForAdmin() {
        List<String[]> users = new ArrayList<>();
        String sql = "SELECT id, username, full_name, email, phone, address, user_type, created_at " +
                "FROM users WHERE username != 'vpa_admin' ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(new String[]{
                        String.valueOf(rs.getInt("id")),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getString("user_type"),
                        rs.getString("created_at")
                });
            }
            System.out.println("✅ Đã tải " + users.size() + " tài khoản cho Admin");

        } catch (SQLException e) {
            System.out.println("❌ Lỗi getAllUsersForAdmin: " + e.getMessage());
        }
        return users;
    }

    /**
     * Xóa user theo ID (Đã xóa các dòng trắng thừa và tối ưu khối try-catch)
     */
    public static boolean deleteUserById(int userId) {
        String deleteBids = "DELETE FROM bids WHERE user_id = ?";
        String deleteUser = "DELETE FROM users WHERE id = ? AND username != 'vpa_admin'";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pBids = conn.prepareStatement(deleteBids);
                 PreparedStatement pUser = conn.prepareStatement(deleteUser)) {

                pBids.setInt(1, userId);
                pBids.executeUpdate();

                pUser.setInt(1, userId);
                int rows = pUser.executeUpdate();

                if (rows > 0) {
                    conn.commit();
                    System.out.println("✅ Đã xóa user ID " + userId);
                    return true;
                } else {
                    conn.rollback();
                    System.out.println("❌ Không tìm thấy user");
                    return false;
                }

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.out.println("❌ Lỗi deleteUserById: " + e.getMessage());
            return false;
        }
    }
}