package com.auction.data;

import java.util.ArrayList;
import java.util.List;

public class DataStore {
    // [0] Loại xe  [1] Biển số  [2] Tỉnh thành  [3] Giá tiền
    // [4] Họ tên   [5] SĐT      [6] Email        [7] Địa chỉ
    // [8] Trạng thái (PENDING / APPROVED)
    public static List<String[]> products = new ArrayList<>();
    // Không có dữ liệu mẫu — mọi dữ liệu đều lấy từ DATABASE
}