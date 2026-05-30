package com.auction.database;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import com.auction.util.PasswordUtil;

//Tự động tạo các bảng khi app khởi động

public class DatabaseInitializer {

    public static void initDatabase() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // BẢNG 1: USERS - Lưu người dùng
            String createUsers = "CREATE TABLE IF NOT EXISTS users (" +
                    "  id INT PRIMARY KEY AUTO_INCREMENT," +
                    "  username VARCHAR(50) NOT NULL UNIQUE," +
                    "  password VARCHAR(255) NOT NULL," +
                    "  email VARCHAR(100)," +
                    "  full_name VARCHAR(100)," +
                    "  user_type VARCHAR(20)," +
                    "  phone VARCHAR(15)," +
                    "  address TEXT," +
                    "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
            stmt.execute(createUsers);
            System.out.println("✅ Bảng users sẵn sàng");


            // BẢNG 2: ITEMS - Lưu biển số xe
            String createItems = "CREATE TABLE IF NOT EXISTS items (" +
                    "  id INT PRIMARY KEY AUTO_INCREMENT," +
                    "  plate_number VARCHAR(20) NOT NULL UNIQUE," +
                    "  start_price DECIMAL(15,2)," +
                    "  current_price DECIMAL(15,2)," +
                    "  description TEXT," +
                    "  vehicle_type VARCHAR(50)," +
                    "  status VARCHAR(20)," +
                    "  approval_status VARCHAR(20) DEFAULT 'PENDING'," +
                    "  seller_name VARCHAR(100)," +
                    "  seller_phone VARCHAR(15)," +
                    "  seller_email VARCHAR(100)," +
                    "  seller_address TEXT," +
                    "  registration_duration INT DEFAULT 1440," +
                    "  registration_start_time DATETIME," +
                    "  registration_end_time DATETIME," +
                    "  auction_start_time DATETIME," +
                    "  auction_end_time DATETIME," +
                    "  participant_count INT DEFAULT 0," +
                    "  auction_status VARCHAR(20) DEFAULT 'WAITING'" +
                    ")";
            stmt.execute(createItems);
            System.out.println("✅ Bảng items sẵn sàng");


            // BẢNG 3: AUCTIONS - Lưu phiên đấu giá
            String createAuctions = "CREATE TABLE IF NOT EXISTS auctions (" +
                    "  id INT PRIMARY KEY AUTO_INCREMENT," +
                    "  item_id INT NOT NULL," +
                    "  seller_id INT NOT NULL," +
                    "  winner_id INT," +
                    "  is_open BOOLEAN DEFAULT TRUE," +
                    "  start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "  end_time DATETIME," +
                    "  final_price DECIMAL(15,2)," +
                    "  FOREIGN KEY (item_id) REFERENCES items(id)," +
                    "  FOREIGN KEY (seller_id) REFERENCES users(id)," +
                    "  FOREIGN KEY (winner_id) REFERENCES users(id)" +
                    ")";
            stmt.execute(createAuctions);
            System.out.println("✅ Bảng auctions sẵn sàng");


            // BẢNG 4: BIDS - Lưu lịch sử đặt giá
            String createBids = "CREATE TABLE IF NOT EXISTS bids (" +
                    "  id INT PRIMARY KEY AUTO_INCREMENT," +
                    "  item_id INT NOT NULL," +
                    "  user_id INT NOT NULL," +
                    "  bid_amount DECIMAL(15,2) NOT NULL," +
                    "  bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "  FOREIGN KEY (item_id) REFERENCES items(id)," +
                    "  FOREIGN KEY (user_id) REFERENCES users(id)" +
                    ")";
            stmt.execute(createBids);
            System.out.println("✅ Bảng bids sẵn sàng");


            String hashedAdminPassword = PasswordUtil.hashPassword("Admin123@");

            String seedAdmin =
                    "INSERT IGNORE INTO users " +
                            "(username, password, email, full_name, user_type, phone, address) " +
                            "VALUES " +
                            "('adminhethong', '" + hashedAdminPassword + "', 'Admin123@', " +
                            "'QUẢN TRỊ VIÊN HỆ THỐNG', 'Admin', '0379131455', 'Hà Nội')";
            stmt.execute(seedAdmin);

            System.out.println("\n✅✅✅ Database khởi tạo thành công! ✅✅✅\n");

        } catch (SQLException e) {
            System.out.println("❌ Lỗi: " + e.getMessage());
        }
    }
}