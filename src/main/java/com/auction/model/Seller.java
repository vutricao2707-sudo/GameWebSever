package com.auction.model;

public class Seller extends User {
    private double rating;

    public Seller(String userId, String userName, String fullName, String password,
                  String email, String phone, String address, double rating) {
        super(userId, userName, fullName, password, email, phone, address);
        this.rating = rating;
    }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
}