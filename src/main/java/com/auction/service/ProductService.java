package com.auction.service;

import com.auction.database.ItemDAO;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductService {

    // ==========================================
    // CHO USER: Chỉ lấy những xe "ĐÃ DUYỆT" và "CHƯA KẾT THÚC" từ DATABASE
    // ==========================================
    public static List<String[]> getApprovedProductsByType(String type) {
        // Lấy từ database - CHỈ lấy những xe đã duyệt và chưa kết thúc phiên
        List<String[]> allApproved = ItemDAO.getAllApprovedActiveItems();
        List<String[]> result = new ArrayList<>();

        for (String[] product : allApproved) {
            // Index [5] trong database format là vehicle_type
            if (product[5].equalsIgnoreCase(type)) {
                result.add(product);
            }
        }
        // Đã loại bỏ hoàn toàn vòng lặp DataStore legacy
        return result;
    }

    // ==========================================
    // CHO ADMIN: Lấy tất cả xe "CHỜ DUYỆT" từ DATABASE
    // ==========================================
    public static List<String[]> getPendingProducts() {
        // Gọi thẳng dữ liệu từ database, không dùng fallback
        return ItemDAO.getPendingItems();
    }

    // ==========================================
    // CHO ADMIN: Logic bấm nút "Duyệt Xe" (✔)
    // ==========================================
    public static boolean approveProduct(String plateToApprove) {
        // Chỉ thực hiện trên Database
        return ItemDAO.approveItem(plateToApprove);
    }

    // ==========================================
    // CHO ADMIN: Logic bấm nút "Từ chối Xe" (✖️)
    // ==========================================
    public boolean rejectProduct(String plateToReject) {
        return ItemDAO.rejectItem(plateToReject);
    }

    // ==========================================
    // CHO USER: Logic bấm nút "Đăng Ký Bán" - LƯU VÀO DATABASE
    // ==========================================
    // ProductService.java
    public boolean addNewProduct(String type, String plate, String province, String price,
                                 String name, String phone, String email, String address) {

        // BƯỚC 1: Vẫn kiểm tra plateExists như cũ để tối ưu (tránh gọi lệnh INSERT thừa)
        if (ItemDAO.plateExists(plate)) {
            return false;
        }

        // BƯỚC 2: Thực hiện thêm vào DB và xử lý Exception
        try {
            double priceDouble = Double.parseDouble(price);

            // Gọi hàm DAO (bây giờ hàm này có thể ném lỗi)
            ItemDAO.addItemFromSeller(plate, priceDouble, "Biển số xe đẹp", type, name, phone, email, address);

            return true; // Thành công

        } catch (SQLException e) {
            // ✅ Kiểm tra nếu lỗi là do trùng khóa (Duplicate Entry) trong MySQL
            if (e.getMessage().contains("Duplicate entry")) {
                System.out.println("⚠️ Biển số đã tồn tại (Lỗi trùng lặp từ DB)");
                return false;
            }

            // Với các lỗi DB khác (như mất kết nối), ném Runtime để UI biết có lỗi nghiêm trọng
            throw new RuntimeException("Lỗi database nghiêm trọng: " + e.getMessage(), e);

        } catch (NumberFormatException e) {
            System.out.println("❌ Lỗi định dạng giá: " + e.getMessage());
            return false;
        }
    }
}