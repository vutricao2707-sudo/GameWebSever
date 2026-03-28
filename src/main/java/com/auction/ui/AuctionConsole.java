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

    private static final Seller currentSeller = new Seller("S01", "Cường Vy Vinfast ", "cuongvy@vinfast.vn", "123", "090123456", 5.0);
    private static final Bidder currentBidder = new Bidder("B01", "Vũ Trí Thấp", "vutrithap@gmail.com", "123", "091999999", 5000000000.0);

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

    private static double inputDouble(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Double.parseDouble(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println(" Lỗi: Bạn vừa nhập chữ. Vui lòng chỉ nhập số!");
            }
        }
    }

    private static int inputInt(String prompt){
        while (true){
            try{
                System.out.print(prompt);
                return Integer.parseInt(scanner.nextLine());

            } catch (NumberFormatException e){
                System.out.println(" Lỗi: Bạn vừa nhập chữ. Vui lòng chỉ nhập số!");
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
            double price = inputDouble("Giá khởi điểm (VNĐ): ");
            System.out.print("Dòng xe: "); String model = scanner.nextLine();
            System.out.print("Màu sắc: "); String color = scanner.nextLine();
            double battery = inputDouble("Dung lượng pin: ");
            int condition = inputInt("Độ mới (%): ");

            Vehicle v = new Vehicle(id, name, desc, price, model, color, battery, condition);

            manager.createAuction(v, currentSeller);
            System.out.println(">>> Đã đăng bán thành công!");
        } catch (Exception e) {
            System.out.println("Lỗi: " + e.getMessage());
        }
    }

    private static void viewAuctions() {
        System.out.println("\n========== DANH SÁCH XE ĐANG ĐẤU GIÁ ==========");
        List<Auction> activeList = manager.getActiveAuctions();

        if (activeList.isEmpty()) {
            System.out.println(" Hiện tại không có phiên đấu giá nào đang mở.");
        }
        else {
            for (Auction a : activeList) {
                System.out.println(" MÃ PHIÊN: " + a.getAuctionId());
                System.out.println(" Người bán: " + a.getSeller().getUsername() + " (Uy tín: " + a.getSeller().getRating() + "⭐)");

                System.out.println(" Thông tin xe: " + a.getItem().toString());

                System.out.println(" Giá khởi điểm: " + String.format("%,.0f", a.getItem().getStartingPrice()) + " VNĐ");

                // Logic tính giá cao nhất hiện tại (Cực kỳ quan trọng để người sau biết đường trả giá)
                List<BidTransaction> history = a.getBids();
                if (history.isEmpty()) {
                    System.out.println(" Tình trạng: Chưa có ai thả giá. Hãy là người đầu tiên!");
                }
                else {
                    double currentHighest = history.get(history.size() - 1).getAmount();
                    System.out.println(" Giá ĐỈNH hiện tại: " + String.format("%,.0f", currentHighest) + " VNĐ (" + history.size() + " lượt đấu)");
                }
                System.out.println("-----------------------------------------------");
            }
        }
    }

    private static void placeBid() {
        try {
            System.out.print("ID phiên: "); String auctionId = scanner.nextLine();
            double amount = inputDouble("Giá trả: ");
            manager.placeBid(auctionId, currentBidder, amount);
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