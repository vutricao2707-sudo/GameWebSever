package com.auction.util;

import com.auction.model.User;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;

    // Private constructor để chặn việc tạo thêm Session mới (Chuẩn Singleton)
    private SessionManager() {}

    // Lấy instance duy nhất của SessionManager để dùng chung cho toàn bộ App
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void logout() {
        this.currentUser = null;
    }
}