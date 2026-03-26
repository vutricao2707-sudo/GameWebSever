package com.auction.model;

public class Vehicle extends Item {
    private String model;
    private String color;
    private double batteryCapacity;
    private String condition;

    public Vehicle(String itemId, String name, String description, double startingPrice,
                   String model, String color, double batteryCapacity, int rangePerCharge, String condition) {

        super(itemId, name, description, startingPrice);
        this.model = model;
        this.color = color;
        this.batteryCapacity = batteryCapacity;
        this.condition = condition;
    }

    public String getModel() {
        return model;
    }
    public void setModel(String model) {
        this.model = model;
    }

    public String getColor() {
        return color; }
    public void setColor(String color) {
        this.color = color;
    }

    public double getBatteryCapacity() {
        return batteryCapacity;
    }
    public void setBatteryCapacity(double batteryCapacity) {
        this.batteryCapacity = batteryCapacity;
    }



    public String getCondition() {
        return condition;
    }
    public void setCondition(String condition) {
        this.condition = condition;
    }

    // Ghi đè hàm toString để lúc in ra nhìn cho "Pro"
    @Override
    public String toString() {
        return "Xe: VinFast " + model + " (" + color + ") | Pin: " + batteryCapacity + " kWh | Tình trạng: " + condition;
    }
}