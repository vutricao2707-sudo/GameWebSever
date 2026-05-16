package com.auction.dto;

public class ItemDTO {

    // =========================================================
    // FIELD CƠ BẢN
    // =========================================================

    private int id;

    private double startPrice;
    private double currentPrice;

    private String vehicleType;
    private String plateNumber;
    private String province;

    private String formattedPrice;

    // =========================================================
    // THÔNG TIN NGƯỜI BÁN
    // =========================================================

    private String sellerName;
    private String sellerPhone;
    private String sellerEmail;
    private String sellerAddress;
    private String auctionStartTime;
    private String auctionStatus;
    private String auctionEndTime;
    // =========================================================
    // THỜI GIAN & TRẠNG THÁI ĐẤU GIÁ
    // =========================================================

    private String registrationEndTime;

    public static ItemDTO fromApprovedArray(String[] p) {

        if (p == null || p.length < 11) {
            System.out.println("⚠️ Dữ liệu không đủ để tạo ItemDTO");
            return null;
        }

        ItemDTO dto = new ItemDTO();

        // =====================================================
        // THÔNG TIN CƠ BẢN
        // =====================================================

        dto.id = Integer.parseInt(p[0]);

        dto.plateNumber = p[1];

        try {

            dto.startPrice = Double.parseDouble(p[2]);

            dto.currentPrice = Double.parseDouble(p[3]);

            dto.formattedPrice =
                    String.format("%,.0f", dto.currentPrice);

        } catch (NumberFormatException e) {

            dto.formattedPrice = p[2];
        }

        dto.vehicleType = p[5];

        // =====================================================
        // THÔNG TIN NGƯỜI BÁN
        // =====================================================

        dto.sellerName =
                (p.length > 7 && p[7] != null)
                        ? p[7]
                        : "N/A";

        dto.sellerPhone =
                (p.length > 8 && p[8] != null)
                        ? p[8]
                        : "N/A";

        dto.sellerEmail =
                (p.length > 9 && p[9] != null)
                        ? p[9]
                        : "N/A";

        dto.sellerAddress =
                (p.length > 10 && p[10] != null)
                        ? p[10]
                        : "N/A";

        // =====================================================
        // THỜI GIAN
        // =====================================================

        dto.registrationEndTime =
                (p.length > 11 && p[11] != null)
                        ? p[11]
                        : "";

        dto.auctionStartTime =
                (p.length > 12 && p[12] != null)
                        ? p[12]
                        : "";

        dto.auctionStatus =
                (p.length > 13 && p[13] != null)
                        ? p[13]
                        : "WAITING";

        dto.auctionEndTime =
                (p.length > 14 && p[14] != null)
                        ? p[14]
                        : "";

        dto.province = "";

        return dto;
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public int getId() {
        return id;
    }

    public double getStartPrice() {
        return startPrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public String getProvince() {
        return province;
    }

    public String getFormattedPrice() {
        return formattedPrice;
    }

    public String getSellerName() {
        return sellerName;
    }

    public String getSellerPhone() {
        return sellerPhone;
    }

    public String getSellerEmail() {
        return sellerEmail;
    }

    public String getSellerAddress() {
        return sellerAddress;
    }

    public String getRegistrationEndTime() {
        return registrationEndTime;
    }
    public String getAuctionStartTime() {
        return auctionStartTime;
    }
    public String getAuctionStatus() {
        return auctionStatus;
    }
    public String getAuctionEndTime() {
        return auctionEndTime;
    }
}