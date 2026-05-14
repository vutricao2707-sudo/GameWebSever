package com.auction.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuditLogger {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void logAction(String username, String action, String target) {
        String time = LocalDateTime.now().format(formatter);
        System.out.println(String.format("[AUDIT] %s | User: %s | Action: %s | Target: %s",
                time, username, action, target));
        // Bạn có thể phát triển thêm hàm này để ghi thẳng vào bảng 'audit_logs' trong Database
    }
}