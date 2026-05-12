package com.auction.config;

import java.io.*;
import java.util.Properties;

/**
 * ============================================
 * ConfigLoader CLASS
 * ============================================
 * 
 * Công dụng: Đọc file database.properties
 * 
 * Vì sao cần class này?
 * - Mỗi người có password MySQL khác nhau
 * - Muốn code chung, nhưng config riêng
 * - Đọc từ file properties để mỗi người config riêng
 * 
 * ============================================
 */
public class ConfigLoader {
    
    private static Properties properties = null;
    
    /**
     * Đọc file database.properties
     * 
     * Giải thích từng dòng:
     * 1. Nếu đã load rồi, trả về (ko load lại)
     * 2. Tạo Properties object (giống Map, lưu key-value)
     * 3. Mở file database.properties
     * 4. Load toàn bộ nội dung vào Properties
     * 5. Return properties để code dùng
     */
    public static Properties loadProperties() {
        
        if (properties != null) {
            // Đã load rồi, ko load lại
            return properties;
        }
        
        properties = new Properties();
        
        try {
            // Tìm file database.properties trong resources/
            InputStream inputStream = ConfigLoader.class
                    .getClassLoader()
                    .getResourceAsStream("database.properties");
            
            if (inputStream == null) {
                System.err.println("❌ database.properties không tìm thấy!");
                System.err.println("   Hãy tạo file: src/main/resources/database.properties");
                return null;
            }
            
            // Load toàn bộ nội dung file vào properties
            properties.load(inputStream);
            System.out.println("✅ Config loaded successfully!");
            
            inputStream.close();
            
        } catch (IOException e) {
            System.err.println("❌ Lỗi đọc file config: " + e.getMessage());
            return null;
        }
        
        return properties;
    }
    
    /**
     * Lấy giá trị từ file properties
     * 
     * Ví dụ:
     * String password = ConfigLoader.getProperty("db.password");
     */
    public static String getProperty(String key) {
        if (properties == null) {
            loadProperties();
        }
        return properties.getProperty(key);
    }
    
    /**
     * Lấy giá trị với giá trị mặc định
     * 
     * Nếu key ko tồn tại, trả về defaultValue
     */
    public static String getProperty(String key, String defaultValue) {
        if (properties == null) {
            loadProperties();
        }
        return properties.getProperty(key, defaultValue);
    }
}

/*
 * ============================================
 * VÍ DỤ CÁCH DÙNG:
 * ============================================
 * 
 * String url = ConfigLoader.getProperty("db.url");
 * String username = ConfigLoader.getProperty("db.username");
 * String password = ConfigLoader.getProperty("db.password");
 * 
 * ============================================
 */

