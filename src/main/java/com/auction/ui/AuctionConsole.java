package com.auction.ui;

import com.auction.model.*;
import com.auction.service.AuctionManager;
import com.auction.exception.InvalidBidException;
import com.auction.exception.AuctionClosedException;

import java.util.List;
import java.util.Scanner;

public class AuctionConsole {
    private static final Scanner scanner = new Scanner(System.in);
    private static final AuctionManager manager = AuctionManager.getInstance();

    public static void main(String[] args) {
        while (true) {
            printMenu();
            try {
                int choice = Integer.parseInt(scanner.nextLine());
                switch (choice) {
                    case 1 -> registerVehicle();
                    case 2 -> viewAuctions();
                    case 3 -> placeBid();
                    case 4 -> closeAuction();
                    case 0 -> {
                        System.out.println("Cảm ơn bạn đã sử dụng sàn đấu giá VinFast!");
                        System.exit(0);
                    }
                    default -> System.out.println("Lựa chọn không hợp lệ!");
                }
            } catch (NumberFormatException e) {
                System.out.println("Lỗi: Vui lòng nhập số!");
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n=== SÀN ĐẤU GIÁ VINFAST ===");
        System.out.println("1. Đăng bán xe mới");
        System.out.println("2. Xem danh sách xe đang đấu giá");
        System.out.println("3. Đặt giá cho một chiếc xe");
        System.out.println("4. Chốt phiên đấu giá");
        System.out.println("0. Thoát");
        System.out.print("Mời bạn chọn chức năng: ");
    }

    private static void registerVehicle() {
        System.out.println("\n--- ĐĂNG BÁN XE VINFAST ---");
        try {
            System.out.print("Nhập ID xe: "); String id = scanner.nextLine();
            System.out.print("Tên xe: "); String name = scanner.nextLine();
            System.out.print("Mô tả: "); String desc = scanner.nextLine();
            System.out.print("Giá khởi điểm: "); double price = Double.parseDouble(scanner.nextLine());
            System.out.print("Dòng xe: "); String model = scanner.nextLine();
            System.out.print("Màu sắc: "); String color = scanner.nextLine();
            System.out.print("Dung lượng pin: "); double battery = Double.parseDouble(scanner.nextLine());
            System.out.print("Độ mới (%): "); int condition = Integer.parseInt(scanner.nextLine());

            Vehicle v = new Vehicle(id, name, desc, price, model, color, battery, condition);
            Seller seller = new Seller("S01", "Hoàng Vin", "hoang@vinfast.vn", "123", "090", 5.0);

            manager.createAuction(v, seller);
            System.out.println(">>> Đã đăng bán thành công!");
        } catch (Exception e) {
            System.out.println("Lỗi: " + e.getMessage());
        }
    }

    private static void viewAuctions() {
        System.out.println("\n--- DANH SÁCH ĐANG MỞ ---");
        List<Auction> activeList = manager.getActiveAuctions();
        if (activeList.isEmpty()) {
            System.out.println("Trống.");
        } else {
            activeList.forEach(a -> System.out.println("ID: " + a.getAuctionId() + " | " + a.getItem().getName()));
        }
    }

    private static void placeBid() {
        try {
            System.out.print("ID phiên: "); String auctionId = scanner.nextLine();
            System.out.print("Giá trả: "); double amount = Double.parseDouble(scanner.nextLine());
            Bidder bidder = new Bidder("B01", "Khách", "bid@mail.com", "123", "091", 500000000);
            manager.placeBid(auctionId, bidder, amount);
            System.out.println("Thành công!");
        } catch (InvalidBidException | AuctionClosedException e) {
            System.out.println("Lỗi: " + e.getMessage());
        }
    }

    private static void closeAuction() {
        try {
            System.out.print("ID phiên cần chốt: "); String id = scanner.nextLine();
            manager.closeAuction(id);
            System.out.println("Đã chốt phiên!");
        } catch (AuctionClosedException e) {
            System.out.println("Lỗi: " + e.getMessage());
        }
    }
}