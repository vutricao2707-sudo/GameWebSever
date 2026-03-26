package com.auction.model;

public class Seller extends User {
    private double rating;

    public Seller(String id, String username, String password, String email, double rating) {
        super(id, username, password, email);
        this.rating = rating;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }
}
