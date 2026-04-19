package com.auction.ui;

import com.auction.model.*;
import com.auction.service.AuctionManager;
import com.auction.exception.InvalidBidException;
import com.auction.exception.AuctionClosedException;

import java.util.List;
import java.util.Scanner;

public class AuctionConsole {
    // 1. Tài nguyên dùng chung
    private static final Scanner scanner = new Scanner(System.in);
    private static final AuctionManager manager = AuctionManager.getInstance();

    // 2. Giả lập tài khoản đăng nhập
    private static final Seller currentSeller = new Seller(
            "S01", "admin", "Nguyễn Văn Admin", "123456",
            "admin@gmail.com", "0123456789", "Hà Nội", 5.0
    );
    private static final Bidder currentBidder = new Bidder(
            "B01", "ronaldat", "Nguyễn Thành Đạt", "123456",
            "dat@vnu.edu.vn", "0919999999", "Cầu Giấy, Hà Nội", 5000000000.0
    );

    // 3. Main
    public static void main(String[] args) {
        while (true) {
            printMenu();
            try {
                int choice = Integer.parseInt(scanner.nextLine());
                switch (choice) {
                    case 1 -> registerLicensePlate();
                    case 2 -> viewAuctions();
                    case 3 -> placeBid();
                    case 4 -> closeAuction();
                    case 0 -> {
                        System.out.println("\nCảm ơn bạn đã sử dụng Cổng Đấu Giá. Tạm biệt!");
                        System.exit(0);
                    }
                    default -> System.out.println("Lựa chọn không hợp lệ (0-4)!");
                }
            } catch (NumberFormatException e) {
                System.out.println("Lỗi: Vui lòng chỉ nhập số!");
            }
        }
    }

    // 4. Hàm bắt lỗi nhập liệu
    private static double inputDouble(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Double.parseDouble(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Lỗi: Bạn vừa nhập chữ. Vui lòng chỉ nhập số!");
            }
        }
    }

    // 5. Menu
    private static void printMenu() {
        System.out.println("\n=============================================");
        System.out.println("    CỔNG ĐẤU GIÁ BIỂN SỐ XE    ");
        System.out.println("=============================================");
        System.out.println("1. Đăng tải biển số mới lên sàn");
        System.out.println("2. Xem danh sách biển số đang đấu giá");
        System.out.println("3. Tham gia trả giá (Đấu giá)");
        System.out.println("4. Chốt phiên đấu giá");
        System.out.println("0. Thoát");
        System.out.print("Mời bạn chọn chức năng: ");
    }

    // 6. Các hàm kết nối.

    private static void registerLicensePlate() {
        System.out.println("\n--- ĐĂNG TẢI BIỂN SỐ ---");
        try {
            System.out.print("Nhập ID (VD: LP-01): "); String id = scanner.nextLine();
            System.out.print("Tên biển (VD: Ngũ quý 9): "); String name = scanner.nextLine();
            System.out.print("Mô tả: "); String desc = scanner.nextLine();
            double price = inputDouble("Giá khởi điểm (VNĐ): ");
            System.out.print("Số biển (VD: 30K-999.99): "); String plateNumber = scanner.nextLine();
            System.out.print("Loại xe: "); String vehicleType = scanner.nextLine();

            LicensePlate lp = new LicensePlate(id, name, desc, price, plateNumber, vehicleType);
            Auction newAuction = manager.createAuction(lp, currentSeller);

            System.out.println("\n Thành công! Mã phiên đấu giá: " + newAuction.getAuctionId());
        } catch (Exception e) {
            System.out.println("Lỗi: " + e.getMessage());
        }
    }

    private static void viewAuctions() {
        System.out.println("\n========== SÀN GIAO DỊCH ==========");
        List<Auction> activeList = manager.getActiveAuctions();

        if (activeList.isEmpty()) {
            System.out.println("Sàn đang trống.");
        }
        else {
            for (Auction a : activeList) {
                System.out.println("PHIÊN: " + a.getAuctionId() + " | Bán bởi: " + a.getSeller().getFullName());
                System.out.println(a.getItem().toString());
                System.out.println("Khởi điểm: " + String.format("%,.0f", a.getItem().getStartingPrice()) + " VNĐ");

                List<BidTransaction> history = a.getBids();
                if (history.isEmpty()) {
                    System.out.println("Tình trạng: Chưa có ai trả giá.");
                } else {
                    double currentHighest = history.get(history.size() - 1).getAmount();
                    System.out.println("Giá ĐỈNH: " + String.format("%,.0f", currentHighest) + " VNĐ (" + history.size() + " lượt)");
                }
                System.out.println("--------------------------------------");
            }
        }
    }

    private static void placeBid() {
        System.out.println("\n--- TRẢ GIÁ ---");
        try {
            System.out.print("Nhập ID phiên (VD: AUC-1): "); String auctionId = scanner.nextLine();
            double amount = inputDouble("Mức giá (VNĐ): ");

            manager.placeBid(auctionId, currentBidder, amount);
            System.out.println("Người dùng " + currentBidder.getFullName() + " trả: " + String.format("%,.0f", amount));
        } catch (InvalidBidException | AuctionClosedException e) {
            System.out.println("Lỗi: " + e.getMessage());
        }
    }

    private static void closeAuction() {
        System.out.println("\n--- CHỐT PHIÊN ---");
        try {
            System.out.print("Nhập ID phiên cần chốt: "); String id = scanner.nextLine();

            Auction closedAuction = manager.closeAuction(id);
            if (closedAuction == null) {
                System.out.println("Lỗi: Không tìm thấy phiên!");
            } else {
                List<BidTransaction> history = closedAuction.getBids();
                if (!history.isEmpty()) {
                    BidTransaction winBid = history.get(history.size() - 1);
                    System.out.println(" Thắng cuộc: " + winBid.getBidder().getFullName() + " - " + String.format("%,.0f", winBid.getAmount()) + " VNĐ");
                } else {
                    System.out.println("️ Phiên không có người mua.");
                }
            }
        } catch (AuctionClosedException e) {
            System.out.println(" Lỗi: " + e.getMessage());
        }
    }
}