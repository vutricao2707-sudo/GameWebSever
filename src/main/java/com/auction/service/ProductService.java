package com.auction.service;

import com.auction.data.DataStore;
import java.util.ArrayList;
import java.util.List;

public class ProductService {

    // ==========================================
    // CHO USER: Chỉ lấy những xe "ĐÃ DUYỆT"
    // ==========================================
    public List<String[]> getApprovedProductsByType(String type) {
        List<String[]> result = new ArrayList<>();
        for (String[] product : DataStore.products) {
            // Điều kiện lọc: Đúng loại xe (Ô tô/Xe máy) VÀ trạng thái phải là APPROVED
            if (product[0].equalsIgnoreCase(type) && product[8].equals("APPROVED")) {
                result.add(product);
            }
        }
        return result;
    }

    // ==========================================
    // CHO ADMIN: Lấy tất cả xe "CHỜ DUYỆT"
    // ==========================================
    public List<String[]> getPendingProducts() {
        List<String[]> result = new ArrayList<>();
        for (String[] product : DataStore.products) {
            // Lọc ra những xe có chữ PENDING
            if (product[8].equals("PENDING")) {
                result.add(product);
            }
        }
        return result;
    }

    // ==========================================
    // CHO ADMIN: Logic bấm nút "Duyệt Xe" (✔)
    // ==========================================
    public boolean approveProduct(String plateToApprove) {
        for (String[] product : DataStore.products) {
            if (product[1].equals(plateToApprove)) {
                product[8] = "APPROVED"; // Sửa trạng thái trong kho
                return true; // Duyệt thành công
            }
        }
        return false; // Không tìm thấy biển số này
    }

    // ==========================================
    // CHO USER: Logic bấm nút "Đăng Ký Bán"
    // ==========================================
    public void addNewProduct(String type, String plate, String province, String price,
                              String name, String phone, String email, String address) {

        // Mặc định xe mới nhét vào kho sẽ bị gán mác "PENDING"
        String[] newProduct = {type, plate, province, price, name, phone, email, address, "PENDING"};
        DataStore.products.add(newProduct);
    }
}