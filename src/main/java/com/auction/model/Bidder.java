package com.auction.model;

public class Bidder extends User {
    private double balance;

    public Bidder(String userId, String userName, String fullName, String password,
                  String email, String phone, String address, double balance) {
        super(userId, userName, fullName, password, email, phone, address);
        this.balance = balance;
    }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}