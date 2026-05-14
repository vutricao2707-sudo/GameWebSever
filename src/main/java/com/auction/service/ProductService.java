package com.auction.service;

import com.auction.database.DataStore;
import com.auction.database.ItemDAO;

import java.util.ArrayList;
import java.util.List;

public class ProductService {

    // ==========================================
    // CHO USER: Chỉ lấy những xe "ĐÃ DUYỆT" từ DATABASE
    // ==========================================
    public List<String[]> getApprovedProductsByType(String type) {
        // Lấy từ database
        List<String[]> allApproved = ItemDAO.getAllApprovedItems();
        List<String[]> result = new ArrayList<>();

        for (String[] product : allApproved) {
            // Cấu trúc từ database: [0]id [1]plate [2]start_price [3]current_price
            // [4]description [5]vehicle_type [6]status [7]seller_name [8]seller_phone [9]seller_email
            if (product[5].equalsIgnoreCase(type)) {  // product[5] là vehicle_type
                result.add(product);
            }
        }

        // Fallback thêm dữ liệu từ DataStore nếu cần (legacy)
        for (String[] product : DataStore.products) {
            if (product[0].equalsIgnoreCase(type) && product[8].equals("APPROVED")) {
                result.add(product);
            }
        }

        return result;
    }

    // ==========================================
    // CHO ADMIN: Lấy tất cả xe "CHỜ DUYỆT" từ DATABASE
    // ==========================================
    public List<String[]> getPendingProducts() {
        // Lấy từ database (ưu tiên)
        List<String[]> pendingFromDB = ItemDAO.getPendingItems();
        if (!pendingFromDB.isEmpty()) {
            return pendingFromDB;
        }

        // Fallback từ DataStore nếu DB trống
        List<String[]> result = new ArrayList<>();
        for (String[] product : DataStore.products) {
            if (product[8].equals("PENDING")) {
                result.add(product);
            }
        }
        return result;
    }

    // ==========================================
    // CHO ADMIN: Logic bấm nút "Duyệt Xe" (✔) từ DATABASE
    // ==========================================
    public boolean approveProduct(String plateToApprove) {
        // Thử duyệt từ database trước
        if (ItemDAO.approveItem(plateToApprove)) {
            return true;
        }

        // Fallback vào DataStore nếu DB không có
        for (String[] product : DataStore.products) {
            if (product[1].equals(plateToApprove)) {
                product[8] = "APPROVED";
                return true;
            }
        }
        return false;
    }

    // ==========================================
    // CHO ADMIN: Logic bấm nút "Từ chối Xe" (✖️) vào DATABASE
    // ==========================================
    public boolean rejectProduct(String plateToReject) {
        return ItemDAO.rejectItem(plateToReject);
    }

    // ==========================================
    // CHO USER: Logic bấm nút "Đăng Ký Bán" - LƯU VÀO DATABASE
    // ==========================================
    public boolean addNewProduct(String type, String plate, String province, String price,
                                 String name, String phone, String email, String address) {

        // BƯỚC 1: Kiểm tra chống trùng lặp - kiểm tra DATABASE trước
        if (ItemDAO.plateExists(plate)) {
            return false;  // Biển số đã tồn tại
        }

        // BƯỚC 2: Kiểm tra chống trùng lặp trong DataStore (legacy)
        for (String[] product : DataStore.products) {
            if (product[1].equalsIgnoreCase(plate)) {
                return false;
            }
        }

        // BƯỚC 3: Nếu không trùng, thêm vào DATABASE với status PENDING
        try {
            double priceDouble = Double.parseDouble(price);
            ItemDAO.addItemFromSeller(plate, priceDouble, "Biển số xe đẹp", type, name, phone, email, address);
            return true;
        } catch (NumberFormatException e) {
            System.out.println("❌ Lỗi parse giá: " + e.getMessage());
            return false;
        }
    }
}