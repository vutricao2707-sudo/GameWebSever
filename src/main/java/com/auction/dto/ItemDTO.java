package com.auction.dto;

public class ItemDTO {
    private String vehicleType, plateNumber, province, formattedPrice;
    private String sellerName, sellerPhone, sellerEmail, sellerAddress;

    public static ItemDTO fromApprovedArray(String[] p) {
        if (p == null || p.length < 10) return null;
        ItemDTO dto = new ItemDTO();
        dto.plateNumber = p[1];
        dto.formattedPrice = p[2]; // start_price
        dto.vehicleType = p[5];
        dto.sellerName = p[7];
        dto.sellerPhone = p[8];
        dto.sellerEmail = p[9];
        dto.province = ""; // Tạm thời để trống hoặc query thêm nếu cần
        dto.sellerAddress = "";
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