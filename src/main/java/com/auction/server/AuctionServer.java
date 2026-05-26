package com.auction.server;

public class AuctionServer {

    public static void main(String[] args) {
        System.out.println("⏳ Đang chuẩn bị khởi động hệ thống Đấu Giá (WebSocket)...");

        // 1. KHỞI ĐỘNG WEBSOCKET SERVER
        AuctionWebSocketServer wsServer = AuctionWebSocketServer.getInstance();
        wsServer.start();
        System.out.println("✅ Đã gửi lệnh khởi động WebSocket Server!");

        // 2. SHUTDOWN HOOK (Bắt sự kiện khi tắt Server để đóng kết nối an toàn)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("⚠️ Hệ thống đang tắt, tiến hành ngắt kết nối WebSocket...");
            try {
                wsServer.stop();
                System.out.println("✅ WebSocket đã dừng an toàn.");
            } catch (Exception e) {
                System.err.println("❌ Lỗi khi tắt WebSocket: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }));
    }
}