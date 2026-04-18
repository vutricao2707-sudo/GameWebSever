package com.auction.model;

public class LicensePlate extends Item {
    private String plateNumber;
    private String vehicleType;

    public LicensePlate(String id, String name, String description, double startingPrice,
                        String plateNumber, String vehicleType) {
        super(id, name, description, startingPrice);
        this.plateNumber = plateNumber;
        this.vehicleType = vehicleType;
    }

    public String getPlateNumber() {
        return plateNumber;
    }
    public String getVehicleType() {
        return vehicleType;
    }

    @Override
    public String toString() {
        return getName() + " | Biển: " + plateNumber + " | Loại xe: " + vehicleType;
    }
}