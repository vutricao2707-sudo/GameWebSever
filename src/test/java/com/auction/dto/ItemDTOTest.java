package com.auction.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ItemDTOTest {

    @Test
    public void testFromApprovedArray_ValidData() {
        // Kịch bản 1: Mảng dữ liệu chuẩn (đầy đủ 15 phần tử như trong DAO)
        String[] mockData = {
                "1", "30K-99999", "50000000", "65000000", "Mô tả", "Ô tô", "ACTIVE",
                "Vũ Trí Đạt", "0987654321", "vutridat@gmail.com", "Hà Nội",
                "2026-06-01 10:00:00", "2026-06-01 11:00:00", "AUCTION", "2026-06-01 12:00:00"
        };

        ItemDTO dto = ItemDTO.fromApprovedArray(mockData);

        // Kiểm tra xem Object có được tạo thành công không
        assertNotNull(dto, "DTO không được null khi mảng hợp lệ");

        // Kiểm tra bóc tách dữ liệu cơ bản
        assertEquals(1, dto.getId(), "ID phải đúng là 1");
        assertEquals("30K-99999", dto.getPlateNumber(), "Biển số phải map đúng");
        assertEquals(50000000.0, dto.getStartPrice(), "Giá khởi điểm phải ép kiểu đúng sang double");

        // Kiểm tra format giá tiền (phải có dấu phẩy)
        assertEquals("65,000,000", dto.getFormattedPrice(), "Giá trị phải được format có dấu phẩy ngăn cách");

        // Kiểm tra thông tin người bán
        assertEquals("Vũ Trí Đạt", dto.getSellerName());
    }

    @Test
    public void testFromApprovedArray_InsufficientData() {
        // Kịch bản 2: Mảng dữ liệu bị thiếu (length < 11)
        String[] shortData = {"1", "30K-99999"};

        ItemDTO dto = ItemDTO.fromApprovedArray(shortData);

        // Kiểm tra xem hàm có trả về null khi dữ liệu không đủ không
        assertNull(dto, "DTO phải null khi mảng dữ liệu không đủ phần tử");
    }

    @Test
    public void testFromApprovedArray_NumberFormatException() {
        // Kịch bản 3: Dữ liệu giá tiền bị sai định dạng (có chữ cái)
        String[] corruptedData = {
                "1", "30K-99999", "năm mươi triệu", "65000000a", "Mô tả", "Ô tô", "ACTIVE",
                "Nguyễn Văn A", "0987654321", "a@gmail.com", "Hà Nội",
                "", "", "WAITING", ""
        };

        ItemDTO dto = ItemDTO.fromApprovedArray(corruptedData);

        assertNotNull(dto, "Vẫn tạo được Object dù giá tiền bị lỗi");

        // Kiểm tra xem khi có lỗi NumberFormatException, formattedPrice sẽ lấy chuỗi gốc
        assertEquals("năm mươi triệu", dto.getFormattedPrice(), "Khi lỗi NumberFormatException, formatPrice sẽ lấy chuỗi gốc");
    }
}