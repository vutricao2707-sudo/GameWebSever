package com.auction.data;

import java.util.ArrayList;
import java.util.List;

public class DataStore {
    // Cấu trúc mảng chuẩn:
    // [0]: Loại xe (Ô tô/Xe máy)
    // [1]: Biển số
    // [2]: Tỉnh thành
    // [3]: Giá tiền
    // [4]: Họ tên
    // [5]: SĐT
    // [6]: Email
    // [7]: Địa chỉ
    // [8]: TRẠNG THÁI ("PENDING" hoặc "APPROVED") <--- THÊM CÁI NÀY

    public static List<String[]> products = new ArrayList<>();

    // Nạp sẵn vài dữ liệu mẫu khi app vừa bật lên
    static {
        // Xe này đã được duyệt -> Sẽ hiện ở Dashboard của User
        products.add(new String[]{"Ô tô", "30K-888.88", "Hà Nội", "150.000.000", "Vũ Trí Cao", "0987654321", "cao@email.com", "Hà Nội", "APPROVED"});

        // Xe này mới đăng, đang chờ duyệt -> Sẽ hiện ở bảng của Admin
        products.add(new String[]{"Ô tô", "51G-123.45", "TP.HCM", "45.000.000", "Trần Văn B", "0123456789", "b@email.com", "Quận 1", "PENDING"});

        products.add(new String[]{"Xe máy", "29A-999.99", "Hà Nội", "300.000.000", "Lê Thị C", "0999888777", "c@email.com", "Đống Đa", "PENDING"});
    }
}