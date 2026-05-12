package com.auction.database;

import java.io.InputStream;
import java.util.Properties;

/**
 * ConfigLoader - Đọc file database.properties
 * 
 * Giải thích:
 * - Đọc file database.properties từ resources folder
 * - Lưu vào Properties object (giống HashMap)
 * - Code gọi ConfigLoader.getProperty("db.password") để lấy password
 * - Mỗi máy có file .properties khác → password khác → không hard-code
 */
public class ConfigLoader {
    
    private static final Properties properties = new Properties();
    
    // Load file 1 lần khi app start
    static {
        try (InputStream input = ConfigLoader.class.getClassLoader()
                .getResourceAsStream("database.properties")) {
            
            if (input == null) {
                System.out.println("❌ Lỗi: Không tìm thấy database.properties");
                System.out.println("   → Copy file database.properties.example thành database.properties");
                System.out.println("   → Sửa password trong file đó");
            } else {
                properties.load(input);
                System.out.println("✅ Đã load config từ database.properties");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Lỗi đọc database.properties: " + e.getMessage());
        }
    }
    
    
    // Lấy giá trị từ properties
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}


