package com.auction.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PlateValidatorTest {

    @Test
    public void testValidPlates() {
        // Kịch bản 1: Biển số đúng định dạng
        // Chấp nhận định dạng: 2 số + 1 chữ cái + 1 số/chữ + dấu gạch/chấm (tùy chọn) + 4/5 số.
        assertTrue(PlateValidator.isValidPlate("29A1-12345"), "Biển xe máy chuẩn phải hợp lệ");
        assertTrue(PlateValidator.isValidPlate("30AA.55555"), "Biển ô tô có dấu chấm phải hợp lệ");
        assertTrue(PlateValidator.isValidPlate(" 51B2-1234  "), "Biển có khoảng trắng thừa 2 đầu vẫn phải hợp lệ vì đã xử lý trim()");
    }

    @Test
    public void testInvalidPlates() {
        // Kịch bản 2: Biển số sai định dạng
        assertFalse(PlateValidator.isValidPlate("ABC-12345"), "Sai mã vùng (không phải số) phải bị từ chối");
        assertFalse(PlateValidator.isValidPlate("01A1-12345"), "Mã vùng bắt đầu bằng số 0 phải bị từ chối");
        assertFalse(PlateValidator.isValidPlate("29A1-12"), "Thiếu số cuối phải bị từ chối");
    }

    @Test
    public void testNullOrEmpty() {
        // Kịch bản 3: Đầu vào nguy hiểm (null hoặc rỗng)
        assertFalse(PlateValidator.isValidPlate(null), "Giá trị null phải trả về false");
        assertFalse(PlateValidator.isValidPlate(""), "Chuỗi rỗng phải trả về false");
        assertFalse(PlateValidator.isValidPlate("   "), "Chuỗi chỉ chứa khoảng trắng phải trả về false");
    }
}