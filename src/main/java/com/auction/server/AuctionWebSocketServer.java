package com.auction.server;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

/**
 * WebSocket Server cho Auction System
 * Broadcast cập nhật giá, trạng thái đấu giá realtime tới tất cả clients
 * (Bản tối ưu chống rò rỉ bộ nhớ - Anti Memory Leak)
 */
public class AuctionWebSocketServer extends WebSocketServer {
    private static AuctionWebSocketServer instance;

    // TỐI ƯU 1: Dùng Set thay cho List để tránh trùng lặp và tăng tốc độ xóa
    // Key: itemId, Value: Set<WebSocket>
    private final Map<Integer, Set<WebSocket>> itemSubscribers = new ConcurrentHashMap<>();

    // TỐI ƯU 2: Thêm Reverse Index để dọn dẹp siêu tốc mà không cần quét toàn bộ
    // Key: WebSocket, Value: Set<itemId>
    private final Map<WebSocket, Set<Integer>> clientSubscriptions = new ConcurrentHashMap<>();

    // TỐI ƯU 3: Bộ quét rác định kỳ (Scheduled Sweeper)
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    // ==========================================
    // LẤY PORT TỪ RAILWAY
    // ==========================================
    private static int getDynamicPort() {
        String portEnv = System.getenv("PORT");
        return (portEnv != null) ? Integer.parseInt(portEnv) : 8081;
    }

    // ==========================================
    // KHỞI TẠO SERVER
    // ==========================================
    private AuctionWebSocketServer() {
        super(new InetSocketAddress("0.0.0.0", getDynamicPort()));

        // Tự động gửi Ping mỗi 30 giây để Railway không ngắt kết nối
        this.setConnectionLostTimeout(30);

        System.out.println("🚀 WebSocket Server khởi tạo trên port " + getDynamicPort());

        // Khởi động bộ quét rác ngầm
        startCleanupTask();
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

    // ==========================================
    // CÁC HÀM QUẢN LÝ VÀ DỌN DẸP KẾT NỐI
    // ==========================================

    /**
     * Kiểm tra xem connection còn sống không
     */
    private boolean isUsable(WebSocket conn) {
        return conn != null && conn.isOpen() && !conn.isClosing() && !conn.isClosed();
    }

    /**
     * Dọn dẹp tập trung: Xóa Client khỏi hệ thống và dọn phòng rỗng
     */
    private void cleanupClient(WebSocket conn) {
        if (conn == null) return;

        Set<Integer> itemIds = clientSubscriptions.remove(conn);
        if (itemIds != null) {
            for (Integer itemId : itemIds) {
                Set<WebSocket> subscribers = itemSubscribers.get(itemId);
                if (subscribers != null) {
                    subscribers.remove(conn);
                    // Xóa luôn phòng nếu không còn ai xem (Chống đầy RAM)
                    if (subscribers.isEmpty()) {
                        itemSubscribers.remove(itemId);
                    }
                }
            }
        }

        try {
            if (!conn.isClosed()) conn.close();
        } catch (Exception ignored) {}
    }

    /**
     * Tự động quét và dọn dẹp các kết nối chết mỗi 30 giây
     */
    private void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                for (WebSocket conn : clientSubscriptions.keySet()) {
                    if (!isUsable(conn)) {
                        cleanupClient(conn);
                    }
                }
            } catch (Exception e) {
                System.err.println("Lỗi trong quá trình cleanup: " + e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    // ==========================================
    // XỬ LÝ SỰ KIỆN WEBSOCKET
    // ==========================================

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("✅ Client mới kết nối: " + conn.getRemoteSocketAddress());
        // Khởi tạo sẵn profile cho client mới
        clientSubscriptions.computeIfAbsent(conn, k -> ConcurrentHashMap.newKeySet());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // MÀNG LỌC BẢO VỆ - Bỏ qua các HTTP Headers (Tin nhắn rác) từ Railway
        if (message == null || !message.trim().startsWith("{")) {
            System.out.println("⚠️ Bỏ qua message rác (Không phải JSON): " + message);
            return;
        }

        try {
            JSONObject json = new JSONObject(message);
            String action = json.optString("action", "");

            if ("SUBSCRIBE".equals(action)) {
                int itemId = json.getInt("itemId");

                // Đăng ký 2 chiều
                itemSubscribers.computeIfAbsent(itemId, k -> ConcurrentHashMap.newKeySet()).add(conn);
                clientSubscriptions.computeIfAbsent(conn, k -> ConcurrentHashMap.newKeySet()).add(itemId);

                System.out.println("📌 Client subscribe itemId: " + itemId);

                // Gửi confirmation
                JSONObject response = new JSONObject();
                response.put("type", "SUBSCRIBED");
                response.put("itemId", itemId);
                conn.send(response.toString());

            } else if ("UNSUBSCRIBE".equals(action)) {
                int itemId = json.getInt("itemId");

                // Xóa đăng ký 2 chiều
                Set<WebSocket> subs = itemSubscribers.get(itemId);
                if (subs != null) {
                    subs.remove(conn);
                    if (subs.isEmpty()) itemSubscribers.remove(itemId);
                }

                Set<Integer> items = clientSubscriptions.get(conn);
                if (items != null) items.remove(itemId);

                System.out.println("📍 Client unsubscribe itemId: " + itemId);

            } else if ("NEW_BID".equals(action)) {
                int itemId = json.getInt("itemId");
                double bidAmount = json.getDouble("bidAmount");
                int bidderId = json.getInt("bidderId");
                String bidderName = json.getString("bidderName");
                long bidTime = json.optLong("bidTime", System.currentTimeMillis());

                broadcastBidUpdate(itemId, bidAmount, bidderId, bidderName, bidTime);
                System.out.println("🔔 Server nhận được BID mới và đang Broadcast...");
            }

        } catch (Exception e) {
            System.err.println("❌ Lỗi xử lý WebSocket message: " + e.getMessage());
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("❌ Client ngắt kết nối: " + conn.getRemoteSocketAddress() + " - Lý do: " + reason);
        cleanupClient(conn); // Gọi hàm dọn dẹp tập trung
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("⚠️ WebSocket error: " + ex.getMessage());
        // Nếu lỗi xảy ra và connection đã sập, dọn dẹp ngay
        if (conn != null && !conn.isOpen()) {
            cleanupClient(conn);
        }
    }

    @Override
    public void onStart() {
        System.out.println("🌐 WebSocket Server đã khởi động!");
    }

    // ==========================================
    // CÁC HÀM BROADCAST
    // ==========================================

    public void broadcastPriceUpdate(int itemId, double currentPrice, int bidderId, String bidderName) {
        broadcastBidUpdate(itemId, currentPrice, bidderId, bidderName, System.currentTimeMillis());
    }

    public void broadcastBidUpdate(int itemId, double bidAmount, int bidderId, String bidderName, long bidTime) {
        Set<WebSocket> subscribers = itemSubscribers.get(itemId);
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

            safeBroadcast(subscribers, message.toString());
            System.out.println("📤 Broadcast PRICE_UPDATE itemId=" + itemId + " price=" + bidAmount + " -> " + subscribers.size() + " clients");
        }
    }

    public void broadcastParticipantUpdate(int itemId, int participantCount) {
        Set<WebSocket> subscribers = itemSubscribers.get(itemId);
        if (subscribers != null && !subscribers.isEmpty()) {
            JSONObject message = new JSONObject();
            message.put("type", "PARTICIPANT_UPDATE");
            message.put("itemId", itemId);
            message.put("participantCount", participantCount);
            message.put("timestamp", System.currentTimeMillis());

            safeBroadcast(subscribers, message.toString());
        }
    }

    public void broadcastAuctionFinished(int itemId, double finalPrice, int winnerId, String winnerName) {
        Set<WebSocket> subscribers = itemSubscribers.get(itemId);
        if (subscribers != null && !subscribers.isEmpty()) {
            JSONObject message = new JSONObject();
            message.put("type", "AUCTION_FINISHED");
            message.put("itemId", itemId);
            message.put("finalPrice", finalPrice);
            message.put("winnerId", winnerId);
            message.put("winnerName", winnerName);
            message.put("timestamp", System.currentTimeMillis());

            safeBroadcast(subscribers, message.toString());
        }
    }

    public void broadcastTimerExtended(int itemId, int newTimeRemaining) {
        Set<WebSocket> subscribers = itemSubscribers.get(itemId);
        if (subscribers != null && !subscribers.isEmpty()) {
            JSONObject message = new JSONObject();
            message.put("type", "TIMER_EXTENDED");
            message.put("itemId", itemId);
            message.put("newTimeRemaining", newTimeRemaining);
            message.put("timestamp", System.currentTimeMillis());

            safeBroadcast(subscribers, message.toString());
        }
    }

    /**
     * TỐI ƯU 4: Hàm gửi tin nhắn an toàn
     * Tự động phát hiện và tiêu diệt các Client bị "chết mạng" trong lúc đang gửi tin
     */
    private void safeBroadcast(Set<WebSocket> subscribers, String payload) {
        for (WebSocket conn : subscribers) {
            if (!isUsable(conn)) {
                cleanupClient(conn);
                continue;
            }
            try {
                conn.send(payload);
            } catch (Exception e) {
                // Gửi lỗi (ví dụ đứt cáp giữa chừng) -> Dọn dẹp ngay
                cleanupClient(conn);
            }
        }
    }
}