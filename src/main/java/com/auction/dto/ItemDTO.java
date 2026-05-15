package com.auction.dto;

public class ItemDTO {

    private String vehicleType;
    private String plateNumber;
    private String province;
    private String formattedPrice;

    private String sellerName;
    private String sellerPhone;
    private String sellerEmail;
    private String sellerAddress;

    // =========================
    // THÊM FIELD THỜI GIAN
    // =========================
    private String registrationEndTime;

    // =========================================================
    // CHUYỂN TỪ ARRAY DATABASE -> DTO
    // =========================================================
    public static ItemDTO fromApprovedArray(String[] p) {

        if (p == null || p.length < 11) {
            System.out.println("⚠️ Dữ liệu không đủ để tạo ItemDTO");
            return null;
        }

        ItemDTO dto = new ItemDTO();

        // =========================
        // THÔNG TIN CƠ BẢN
        // =========================

        dto.plateNumber = p[1];

        try {
            double rawPrice = Double.parseDouble(p[2]);

            dto.formattedPrice =
                    String.format("%,.0f", rawPrice);

        } catch (NumberFormatException e) {

            dto.formattedPrice = p[2];
        }

        dto.vehicleType = p[5];

        // =========================
        // THÔNG TIN NGƯỜI BÁN
        // =========================

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

        // =========================
        // THỜI GIAN ĐĂNG KÝ
        // =========================

        dto.registrationEndTime =
                (p.length > 11 && p[11] != null)
                        ? p[11]
                        : "";

        dto.province = "";

        return dto;
    }

    // =========================================================
    // GETTERS
    // =========================================================

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

    // =========================
    // GETTER MỚI
    // =========================

    public String getRegistrationEndTime() {
        return registrationEndTime;
    }
}