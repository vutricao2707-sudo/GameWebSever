package com.auction.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PasswordUtilTest {

    @Test
    public void testHashConsistency() {
        // Kịch bản 1: 2 Mật khẩu giống nhau phải tạo ra mã băm giống nhau
        String testPassword = "Password123@";
        String hash1 = PasswordUtil.hashPassword(testPassword);
        String hash2 = PasswordUtil.hashPassword(testPassword);

        assertEquals(hash1, hash2, "Cùng một mật khẩu phải tạo ra mã băm giống hệt nhau");
    }

    @Test
    public void testHashLength() {
        // Kịch bản 2: Kiểm tra độ dài chuẩn của thuật toán SHA-256
        String rawPassword = "admin";
        String hash = PasswordUtil.hashPassword(rawPassword);

        // SHA-256 luôn trả về chuỗi Hex dài 64 ký tự
        assertEquals(64, hash.length(), "Độ dài mã SHA-256 phải luôn là 64 ký tự");
    }

    @Test
    public void testDifferentPasswords() {
        // Kịch bản 3: Hai mật khẩu khác nhau phải ra mã băm khác nhau
        String hash1 = PasswordUtil.hashPassword("123456");
        String hash2 = PasswordUtil.hashPassword("1234567");

        assertNotEquals(hash1, hash2, "Hai mật khẩu khác nhau không được trùng mã băm");
    }
}