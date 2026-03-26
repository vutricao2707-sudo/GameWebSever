package com.auction.model;

public class Electronics extends Item{
    private double warrantyPeriod;

    public Electronics(String itemId, String name, String description, double startingPrice, double warrantyPeriod) {
        super(itemId, name, description, startingPrice);
        this.warrantyPeriod = warrantyPeriod;
    }


}
