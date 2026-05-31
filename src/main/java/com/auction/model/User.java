package com.auction.model;

public abstract class User {
    private String userId;
    private String userName;
    private String fullName;
    private String password;
    private String email;
    private String phone;
    private String address;


    public User(String userId, String userName, String fullName
            , String password, String email, String phone, String address) {
        this.userId = userId;
        this.userName = userName;
        this.fullName = fullName;
        this.password = password;
        this.email = email;
        this.phone = phone;
        this.address = address;
    }

    public int getId() {
        try {
            if (this.userId != null && !this.userId.isEmpty()) {
                return Integer.parseInt(this.userId);
            }
        } catch (NumberFormatException e) {
            System.out.println("⚠️ Cảnh báo: userId không phải là số hợp lệ (" + this.userId + ")");
        }
        return 0;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}