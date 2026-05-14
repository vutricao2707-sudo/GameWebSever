package com.auction.util;

import com.auction.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseHealthCheck {
    public static boolean isDatabaseAlive() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return conn.isValid(2); // Timeout 2 giây
        } catch (SQLException e) {
            System.err.println("❌ Database Health Check Failed: Mất kết nối MySQL!");
            return false;
        }
    }
}