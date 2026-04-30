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
    public boolean addNewProduct(String type, String plate, String province, String price,
                                 String name, String phone, String email, String address) {

        // BƯỚC 4: Kiểm tra chống trùng lặp
        // Duyệt qua toàn bộ xe đang có trong kho DataStore
        for (String[] product : DataStore.products) {
            // Index [1] chính là vị trí lưu Biển số. Dùng equalsIgnoreCase để không phân biệt hoa/thường
            if (product[1].equalsIgnoreCase(plate)) {
                return false; // Phát hiện trùng -> Lập tức từ chối, báo lỗi (false)
            }
        }

        // Nếu đi qua hết vòng lặp mà không bị trùng, thì mới cho phép thêm mới
        String[] newProduct = {type, plate, province, price, name, phone, email, address, "PENDING"};
        DataStore.products.add(newProduct);

        return true; // Báo cáo thêm thành công
    }
}