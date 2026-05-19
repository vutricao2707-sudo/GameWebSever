package com.auction.util;

import com.auction.model.User;

/**
 * SessionManager - Singleton để quản lý session người dùng hiện tại
 * Sử dụng enum pattern để đảm bảo thread-safe
 */
public enum SessionManager {
    INSTANCE;

    private static User currentUser;

    /**
     * Getter singleton - để tương thích với Singleton pattern gọi
     */
    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public void logout() {
        this.currentUser = null;
    }
}