package com.auction.database;

import com.auction.model.User;
import com.auction.model.Seller;
import com.auction.model.Bidder;
import com.auction.util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    //Kiểm tra username bị cấm và username đã tồn tại
    private static final String[] RESERVED_USERNAMES = {
            "adminhethong", "admin", "administrator", "root", "system"
    };

    public static boolean isReservedUsername(String username) {
        String cleanUsername = username.trim().toLowerCase();
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
            pstmt.setString(1, username.trim().toLowerCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi kiểm tra username: " + e.getMessage());
        }
        return false;
    }

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

    public static User getUserByUsernameAndPassword(String username, String password) {

        String hashedPassword = PasswordUtil.hashPassword(password);

        String sql =
                "SELECT * FROM users " +
                        "WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(
                    1,
                    username.trim().toLowerCase()
            );

            pstmt.setString(
                    2,
                    hashedPassword
            );
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {

                    String userId =
                            String.valueOf(rs.getInt("id"));

                    String fullName =
                            rs.getString("full_name");

                    String email =
                            rs.getString("email");

                    String phone =
                            rs.getString("phone");

                    String address =
                            rs.getString("address");

                    String userType =
                            rs.getString("user_type");

                    if ("Seller".equalsIgnoreCase(userType)
                            || "Admin".equalsIgnoreCase(userType)) {

                        return new Seller(
                                userId,
                                username,
                                fullName,
                                hashedPassword,
                                email,
                                phone,
                                address,
                                5.0
                        );

                    } else {

                        return new Bidder(
                                userId,
                                username,
                                fullName,
                                hashedPassword,
                                email,
                                phone,
                                address,
                                0.0
                        );
                    }
                }
            }

        } catch (SQLException e) {

            System.out.println(
                    "❌ Lỗi lấy thông tin user: "
                            + e.getMessage()
            );
        }

        return null;
    }

    public static boolean checkLogin(String username, String password) {

        String hashedPassword =
                PasswordUtil.hashPassword(password);

        String sql =
                "SELECT * FROM users " +
                        "WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(
                    1,
                    username.trim().toLowerCase()
            );

            pstmt.setString(
                    2,
                    hashedPassword
            );
            System.out.println("USERNAME: " + username);

            System.out.println(
                    "HASHED: " + hashedPassword
            );
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {

            System.out.println(
                    "❌ Lỗi checkLogin: "
                            + e.getMessage()
            );

            return false;
        }
    }

    //Lấy toàn bộ danh sách user cho Admin xem
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

    //Xóa user theo ID
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
    public static String getRoleByUsername(String username) {
        String sql = "SELECT user_type FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username.trim().toLowerCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("user_type");
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi lấy quyền: " + e.getMessage());
        }
        return "USER";
    }

    //Lấy User theo ID
    public static User getUserById(int userId) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String username = rs.getString("username");
                String fullName = rs.getString("full_name");
                String password = rs.getString("password");
                String email = rs.getString("email");
                String phone = rs.getString("phone");
                String address = rs.getString("address");
                String userType = rs.getString("user_type");

                if ("Seller".equalsIgnoreCase(userType) || "Admin".equalsIgnoreCase(userType)) {
                    return new Seller(
                            String.valueOf(userId),
                            username,
                            fullName,
                            password,
                            email,
                            phone,
                            address,
                            5.0
                    );
                } else {
                    return new Bidder(
                            String.valueOf(userId),
                            username,
                            fullName,
                            password,
                            email,
                            phone,
                            address,
                            0.0
                    );
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Lỗi getUserById: " + e.getMessage());
        }
        return null;
    }
}