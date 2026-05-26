package com.auction.server;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

/**
 * WebSocket Server cho Auction System
 * Broadcast cập nhật giá, trạng thái đấu giá realtime tới tất cả clients
 */
public class AuctionWebSocketServer extends WebSocketServer {
    private static AuctionWebSocketServer instance;

    // Lưu trữ clients kết nối tới từng item (auction room)
    // Key: itemId, Value: List<WebSocket>
    private final Map<Integer, List<WebSocket>> itemSubscribers = new ConcurrentHashMap<>();

    // ==========================================
    // 1. THÊM HÀM NÀY ĐỂ LẤY PORT TỪ RAILWAY
    // ==========================================
    private static int getDynamicPort() {
        String portEnv = System.getenv("PORT");
        // Nếu chạy trên Railway (có PORT), thì dùng Port của nó.
        // Nếu chạy test ở IntelliJ (portEnv == null), thì vẫn dùng 8081.
        return (portEnv != null) ? Integer.parseInt(portEnv) : 8081;
    }

    // ==========================================
    // 2. SỬA LẠI HÀM KHỞI TẠO NÀY
    // ==========================================
    private AuctionWebSocketServer() {
        // Ép Server lắng nghe ở mọi IP (0.0.0.0) bằng Port động
        super(new InetSocketAddress("0.0.0.0", getDynamicPort()));
        System.out.println("🚀 WebSocket Server khởi tạo trên port " + getDynamicPort());
    }

    /**
     * Singleton pattern - dùng 1 instance duy nhất
     */
    public static synchronized AuctionWebSocketServer getInstance() {
        if (instance == null) {
            instance = new AuctionWebSocketServer();
        }
        return instance;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("✅ Client mới kết nối: " + conn.getRemoteSocketAddress());
    }

    /**
     * Nhận message từ client
     * Format: {"action": "SUBSCRIBE", "itemId": 123}
     *         {"action": "UNSUBSCRIBE", "itemId": 123}
     */
    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JSONObject json = new JSONObject(message);
            String action = json.optString("action", "");

            if ("SUBSCRIBE".equals(action)) {
                int itemId = json.getInt("itemId");
                List<WebSocket> subscribers = itemSubscribers.computeIfAbsent(itemId, k -> new CopyOnWriteArrayList<>());
                if (!subscribers.contains(conn)) {
                    subscribers.add(conn);
                }
                System.out.println("📌 Client subscribe itemId: " + itemId);

                // Gửi confirmation
                JSONObject response = new JSONObject();
                response.put("type", "SUBSCRIBED");
                response.put("itemId", itemId);
                conn.send(response.toString());

            } else if ("UNSUBSCRIBE".equals(action)) {
                int itemId = json.getInt("itemId");
                List<WebSocket> subscribers = itemSubscribers.get(itemId);
                if (subscribers != null) {
                    subscribers.remove(conn);
                }
                System.out.println("📍 Client unsubscribe itemId: " + itemId);
            } else if ("NEW_BID".equals(action)) {
                int itemId = json.getInt("itemId");
                double bidAmount = json.getDouble("bidAmount");
                int bidderId = json.getInt("bidderId");
                String bidderName = json.getString("bidderName");
                long bidTime = json.optLong("bidTime", System.currentTimeMillis());

                // Server nhận được lệnh, tiến hành "phát loa" cho toàn bộ phòng
                broadcastBidUpdate(itemId, bidAmount, bidderId, bidderName, bidTime);
                System.out.println("🔔 Server nhận được BID mới và đang Broadcast...");
            }

        } catch (Exception e) {
            System.err.println("❌ Lỗi xử lý WebSocket message: " + e.getMessage());
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("❌ Client ngắt kết nối: " + conn.getRemoteSocketAddress());
        // Xóa client khỏi tất cả subscribers
        itemSubscribers.values().forEach(list -> list.remove(conn));
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("⚠️ WebSocket error: " + ex.getMessage());
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("🌐 WebSocket Server đã khởi động!");
    }

    /**
     * Broadcast cập nhật giá cho tất cả clients đang theo dõi item này
     *
     * @param itemId ID sản phẩm
     * @param currentPrice Giá hiện tại
     * @param bidderId ID người đặt giá
     * @param bidderName Tên người đặt giá
     */
    public void broadcastPriceUpdate(int itemId, double currentPrice, int bidderId, String bidderName) {
        broadcastBidUpdate(itemId, currentPrice, bidderId, bidderName, System.currentTimeMillis());
    }

    /**
     * Broadcast bid mới cho tất cả clients đang ở cùng phòng đấu giá.
     */
    public void broadcastBidUpdate(int itemId, double bidAmount, int bidderId, String bidderName, long bidTime) {
        List<WebSocket> subscribers = itemSubscribers.get(itemId);
        if (subscribers != null && !subscribers.isEmpty()) {
            JSONObject message = new JSONObject();
            message.put("type", "PRICE_UPDATE");
            message.put("itemId", itemId);
            message.put("currentPrice", bidAmount);
            message.put("bidAmount", bidAmount);
            message.put("bidderId", bidderId);
            message.put("bidderName", bidderName);
            message.put("bidTime", bidTime);
            message.put("timestamp", bidTime);

            String payload = message.toString();
            subscribers.forEach(conn -> {
                try {
                    conn.send(payload);
                } catch (Exception e) {
                    System.err.println("Lỗi gửi PRICE_UPDATE: " + e.getMessage());
                }
            });

            System.out.println("📤 Broadcast PRICE_UPDATE itemId=" + itemId + " price=" + bidAmount + " -> " + subscribers.size() + " clients");
        }
    }

    /**
     * Broadcast cập nhật số người tham gia
     */
    public void broadcastParticipantUpdate(int itemId, int participantCount) {
        List<WebSocket> subscribers = itemSubscribers.get(itemId);
        if (subscribers != null && !subscribers.isEmpty()) {
            JSONObject message = new JSONObject();
            message.put("type", "PARTICIPANT_UPDATE");
            message.put("itemId", itemId);
            message.put("participantCount", participantCount);
            message.put("timestamp", System.currentTimeMillis());

            String payload = message.toString();
            subscribers.forEach(conn -> {
                try {
                    conn.send(payload);
                } catch (Exception e) {
                    System.err.println("Lỗi gửi PARTICIPANT_UPDATE: " + e.getMessage());
                }
            });
        }
    }

    /**
     * Broadcast khi phiên đấu giá kết thúc
     */
    public void broadcastAuctionFinished(int itemId, double finalPrice, int winnerId, String winnerName) {
        List<WebSocket> subscribers = itemSubscribers.get(itemId);
        if (subscribers != null && !subscribers.isEmpty()) {
            JSONObject message = new JSONObject();
            message.put("type", "AUCTION_FINISHED");
            message.put("itemId", itemId);
            message.put("finalPrice", finalPrice);
            message.put("winnerId", winnerId);
            message.put("winnerName", winnerName);
            message.put("timestamp", System.currentTimeMillis());

            String payload = message.toString();
            subscribers.forEach(conn -> {
                try {
                    conn.send(payload);
                } catch (Exception e) {
                    System.err.println("Lỗi gửi AUCTION_FINISHED: " + e.getMessage());
                }
            });
        }
    }

    /**
     * Broadcast timer extend khi có bid trong 2 phút cuối
     */
    public void broadcastTimerExtended(int itemId, int newTimeRemaining) {
        List<WebSocket> subscribers = itemSubscribers.get(itemId);
        if (subscribers != null && !subscribers.isEmpty()) {
            JSONObject message = new JSONObject();
            message.put("type", "TIMER_EXTENDED");
            message.put("itemId", itemId);
            message.put("newTimeRemaining", newTimeRemaining);
            message.put("timestamp", System.currentTimeMillis());

            String payload = message.toString();
            subscribers.forEach(conn -> {
                try {
                    conn.send(payload);
                } catch (Exception e) {
                    System.err.println("Lỗi gửi TIMER_EXTENDED: " + e.getMessage());
                }
            });
        }
    }
}


