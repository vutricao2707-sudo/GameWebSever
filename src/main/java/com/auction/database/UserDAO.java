package com.auction.database;

import com.auction.model.User;
import com.auction.model.Seller;
import com.auction.model.Bidder;
import com.auction.util.PasswordUtil;
import java.sql.*;

/**
 * DAO = Data Access Object
 * File này chứa các hàm để query bảng users
 */
public class UserDAO {

    // =========================================================================
    // PHẦN MỚI: Kiểm tra username bị cấm và username đã tồn tại
    // =========================================================================

    private static final String[] RESERVED_USERNAMES = {
            "vpa_admin", "admin", "administrator", "root", "system"
    };

    public static boolean isReservedUsername(String username) {
        for (String reserved : RESERVED_USERNAMES) {
            if (reserved.equalsIgnoreCase(username.trim())) {
                return true;
            }
        }
        return false;
    }

    public static boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username.trim());
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
    // PHẦN GIỮ NGUYÊN TỪ CODE CŨ
    // =========================================================================

    public static boolean addUser(String username, String password, String email,
                                  String fullName, String userType, String phone, String address) {

        // ✅ THÊM DÒNG NÀY: Mã hóa mật khẩu đầu vào trước khi lưu vào database
        String hashedPassword = PasswordUtil.hashPassword(password);

        String sql = "INSERT INTO users (username, password, email, full_name, user_type, phone, address) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            // ✅ SỬA DÒNG NÀY: Lưu chuỗi đã mã hóa thay vì mật khẩu gốc
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
        // ✅ THÊM DÒNG NÀY: Mã hóa mật khẩu đầu vào
        String hashedPassword = PasswordUtil.hashPassword(password);

        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            // ✅ SỬA DÒNG NÀY: Tìm kiếm bằng mật khẩu đã mã hóa
            pstmt.setString(2, hashedPassword);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String userId   = String.valueOf(rs.getInt("id"));
                String fullName = rs.getString("full_name");
                String email    = rs.getString("email");
                String phone    = rs.getString("phone");
                String address  = rs.getString("address");
                String userType = rs.getString("user_type");

                // Lưu ý: Truyền hashedPassword vào constructor để đối tượng User giữ chuỗi an toàn
                if ("Seller".equals(userType) || "Admin".equals(userType)) {
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

            pstmt.setString(1, username);
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

            pstmt.setString(1, username);
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
        // ✅ THÊM DÒNG NÀY: Mã hóa mật khẩu người dùng vừa nhập
        String hashedPassword = PasswordUtil.hashPassword(password);

        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            // ✅ SỬA DÒNG NÀY: Đem chuỗi đã mã hóa đi so sánh với Database
            pstmt.setString(2, hashedPassword);

            ResultSet rs = pstmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
            return false;
        }
    }
}