package com.auction.model;
public class LicensePlate extends Item {
    private String plateNumber;
    private String province;

    public LicensePlate(String itemId, String name, String description, double startingPrice,
                        String plateNumber, String province) {
        super(itemId, name, description, startingPrice);
        this.plateNumber = plateNumber;
        this.province = province;
    }
    public String getPlateNumber() {
        return plateNumber;
    }
    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public String getProvince() {
        return province;
    }
    public void setProvince(String province) {
        this.province = province;
    }

    @Override
    public String toString() {
        return "Biển số: [" + plateNumber + "] | Tỉnh: " + province + " | Giá KĐ: $" + getStartingPrice();
    }
}
