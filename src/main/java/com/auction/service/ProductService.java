package com.auction.service;

import com.auction.model.DataStore;

import java.util.ArrayList;
import java.util.List;

/**
 * ProductService: Tầng Service chịu trách nhiệm duy nhất về dữ liệu sản phẩm.
 * Controller KHÔNG được phép đụng vào DataStore trực tiếp nữa.
 * Mọi thao tác đọc/ghi sản phẩm đều phải đi qua class này.
 */
public class ProductService {

    /**
     * Lấy danh sách sản phẩm đã được lọc theo loại xe.
     *
     * @param type Loại xe cần lọc, ví dụ: "Ô tô" hoặc "Xe máy"
     * @return Danh sách các sản phẩm khớp với loại xe yêu cầu
     */
    public List<String[]> getProductsByType(String type) {
        List<String[]> result = new ArrayList<>();

        // Logic lọc được chuyển từ DashboardController sang đây
        boolean isCarTab = type.equals("Ô tô");

        for (String[] p : DataStore.products) {
            if (p[0] == null) continue;

            boolean matchCar  = isCarTab  && (p[0].equals("Ô tô")  || p[0].contains("tô")  || p[0].contains("to"));
            boolean matchBike = !isCarTab && (p[0].equals("Xe máy") || p[0].contains("máy") || p[0].contains("may"));

            if (matchCar || matchBike) {
                result.add(p);
            }
        }

        return result;
    }

    /**
     * Thêm một sản phẩm mới vào kho dữ liệu.
     * Hàm này sẽ được RegisterSellController dùng ở bước refactor tiếp theo.
     *
     * @param product Mảng dữ liệu sản phẩm theo thứ tự:
     *                [0] loại xe, [1] biển số, [2] tỉnh, [3] giá,
     *                [4] họ tên, [5] sđt, [6] email, [7] địa chỉ
     */
    public void addProduct(String[] product) {
        DataStore.products.add(product);
    }
}