package com.auction.model;

public class Seller extends User {
    private double rating;

    public Seller(String id, String username, String email, String password, String phoneNumber, double rating) {
        super(id, username, email, password, phoneNumber);
        this.rating = rating;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    @Override
    public String toString() {
        return super.toString() + " | Rating: " + this.rating;
    }
}
