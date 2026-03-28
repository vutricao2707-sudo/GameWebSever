package com.auction.model;

public class Vehicle extends Item {
    private String model;
    private String color;
    private double batteryCapacity;
    private int conditionPercentage;

    public Vehicle(String itemId, String name, String description, double startingPrice,
                   String model, String color, double batteryCapacity, int conditionPercentage) {

        super(itemId, name, description, startingPrice);
        this.model = model;
        this.color = color;
        this.batteryCapacity = batteryCapacity;
        this.conditionPercentage = conditionPercentage;
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


    public int getConditionPercentage() {
        return conditionPercentage;
    }

    public void setConditionPercentage(int conditionPercentage) {
        this.conditionPercentage = conditionPercentage;
    }

    @Override
    public String toString() {
        return "VinFast " + model + " (" + color + ") | Pin: " + batteryCapacity + " kWh | Độ mới: " + conditionPercentage + "%";    }
}