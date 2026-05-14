package com.auction.util;

public class InputSanitizer {
    public static String sanitize(String input) {
        if (input == null) return "";
        // Cắt khoảng trắng 2 đầu và loại bỏ các thẻ HTML cơ bản
        return input.trim().replaceAll("<[^>]*>", "");
    }
}