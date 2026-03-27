package com.auction.model;

public class Bidder extends User {
    private double balance;

    public Bidder(String id, String username, String email, String password, String phoneNumber, double balance) {
        super(id, username, email, password, phoneNumber);
        this.balance = balance;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return super.toString() + " | Balance: " + this.balance ;
    }
}
