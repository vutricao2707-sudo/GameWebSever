package com.auction.dto;

public class ItemDTO {
    private String vehicleType, plateNumber, province, formattedPrice;
    private String sellerName, sellerPhone, sellerEmail, sellerAddress;

    // Chuyển đổi từ mảng String (DB) sang đối tượng DTO (Giao diện)
    public static ItemDTO fromApprovedArray(String[] p) {
        // Kiểm tra an toàn: Nếu mảng rỗng hoặc thiếu các cột cơ bản (ít nhất 11 cột từ 0-10)
        if (p == null || p.length < 11) {
            System.out.println("⚠️ Cảnh báo: Dữ liệu mảng không đủ độ dài để tạo DTO");
            return null;
        }

        ItemDTO dto = new ItemDTO();
        dto.plateNumber = p[1]; // Index 1: Biển số

        // FIX: Format giá tiền có dấu phẩy phân cách hàng nghìn
        try {
            double rawPrice = Double.parseDouble(p[2]); // Index 2: Giá khởi điểm
            dto.formattedPrice = String.format("%,.0f", rawPrice); // Ví dụ: 50.000.000
        } catch (NumberFormatException e) {
            dto.formattedPrice = p[2];
        }

        dto.vehicleType = p[5]; // Index 5: Loại xe

        // Kiểm tra an toàn cho các thông tin người bán (tránh NullPointer)
        dto.sellerName    = (p.length > 7 && p[7] != null) ? p[7] : "N/A";
        dto.sellerPhone   = (p.length > 8 && p[8] != null) ? p[8] : "N/A";
        dto.sellerEmail   = (p.length > 9 && p[9] != null) ? p[9] : "N/A";
        dto.sellerAddress = (p.length > 10 && p[10] != null) ? p[10] : "N/A";

        dto.province = ""; // Có thể bổ sung logic cắt chuỗi địa chỉ để lấy tỉnh sau

        return dto;
    }

    // Các hàm Getter
    public String getVehicleType() { return vehicleType; }
    public String getPlateNumber() { return plateNumber; }
    public String getProvince() { return province; }
    public String getFormattedPrice() { return formattedPrice; }
    public String getSellerName() { return sellerName; }
    public String getSellerPhone() { return sellerPhone; }
    public String getSellerEmail() { return sellerEmail; }
    public String getSellerAddress() { return sellerAddress; }
}