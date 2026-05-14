package com.auction.dto;

public class ItemDTO {
    private String vehicleType, plateNumber, province, formattedPrice;
    private String sellerName, sellerPhone, sellerEmail, sellerAddress;

    public static ItemDTO fromApprovedArray(String[] p) {
        // SỬA: Đổi điều kiện từ 10 thành 11 vì mảng p bây giờ có 11 phần tử (do mới thêm address)
        if (p == null || p.length < 11) return null;

        ItemDTO dto = new ItemDTO();
        dto.plateNumber = p[1];

        // SỬA (Vấn đề 2): Format giá tiền từ "150000000.0" thành "150,000,000"
        try {
            double rawPrice = Double.parseDouble(p[2]); // Ép chuỗi về số thực
            dto.formattedPrice = String.format("%,.0f", rawPrice); // Format số có dấu phẩy
        } catch (NumberFormatException e) {
            dto.formattedPrice = p[2]; // Đề phòng lỗi ép kiểu thì gán lại chuỗi gốc
        }

        dto.vehicleType = p[5];
        dto.sellerName = p[7];
        dto.sellerPhone = p[8];
        dto.sellerEmail = p[9];
        dto.province = "";

        // SỬA (Vấn đề 1): Đọc địa chỉ người bán từ vị trí p[10]
        dto.sellerAddress = p[10];

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