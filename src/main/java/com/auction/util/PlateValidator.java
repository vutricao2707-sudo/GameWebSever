package com.auction.util;

public class PlateValidator {
    // Chấp nhận định dạng: 2 số + 1 chữ cái + 1 số/chữ + dấu gạch/chấm (tùy chọn) + 4/5 số. VD: 29A-12345
    private static final String PLATE_REGEX = "^[1-9][0-9][A-Z][A-Z0-9][-.]?\\d{4,5}$";

    public static boolean isValidPlate(String plate) {
        if (plate == null || plate.trim().isEmpty()) return false;
        return plate.toUpperCase().replaceAll("\\s+", "").matches(PLATE_REGEX);
    }
}