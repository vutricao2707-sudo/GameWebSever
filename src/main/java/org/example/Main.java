package org.example;


import com.auction.model.*;
import com.auction.service.AuctionManager;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- KHỞI ĐỘNG SÀN ĐẤU GIÁ XE VINFAST ---");

        AuctionManager manager = AuctionManager.getInstance();

        Seller seller = new Seller("S01", "Vo Phuong Vy", "vophuongvy@gmail.com", "123", "0123456789", 5.0);
        Bidder bidder1 = new Bidder("B01", "Vo Ta Le Cuong", "cuonggay@@gmail.com", "234", "0234567890", 5000000000.0 );
        Bidder bidder2 = new Bidder("B02", "Pham Xuan An", "xuanan@gmail.com", "345", "0212345678", 9000000000.0);

        Vehicle vf8 = new Vehicle("V-001", "VinFast VF 8", "Xe lướt đi chợ", 850000000.0, "VF 8", "Cam", 87.7, 99);
        LicensePlate plate = new LicensePlate("P-001", "Biển Ngũ Quý", "Biển siêu VIP", 40000000.0, "30K-999.99", "Hà Nội");

        try {
            Auction auction1 = manager.createAuction(vf8, seller);
            System.out.println("\n[THÔNG BÁO] Vừa mở phiên: " + auction1.getAuctionId() + " bán xe " + vf8.getModel());

            manager.placeBid(auction1.getAuctionId(), bidder1, 860000000.0);
            System.out.println("-> " + bidder1.getUsername() + " trả giá: 860 triệu");

            manager.placeBid(auction1.getAuctionId(), bidder2, 900000000.0);
            System.out.println("-> " + bidder2.getUsername() + " khô máu: 900 triệu");


            manager.closeAuction(auction1.getAuctionId());
            System.out.println("\n[THÔNG BÁO] Đã gõ búa chốt phiên " + auction1.getAuctionId() + "!");

            System.out.println("\n[TEST LỖI ] Ông B01 cố chấp ném thêm tiền vào phiên đã đóng...");
            manager.placeBid(auction1.getAuctionId(), bidder1, 950000000.0);
        } catch (Exception e){
            System.err.println("HỆ THỐNG TỪ CHỐI: " + e.getMessage());
        }
    }
}