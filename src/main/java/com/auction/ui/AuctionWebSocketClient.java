package com.auction.ui;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * WebSocket Client cho Auction System UI
 * Kết nối tới server WebSocket để nhận realtime updates
 */
public class AuctionWebSocketClient extends WebSocketClient {
    private static AuctionWebSocketClient instance;
    private final List<AuctionWebSocketListener> listeners = new CopyOnWriteArrayList<>();
    private boolean isConnected = false;
    private boolean isReconnecting = false;
    public interface AuctionWebSocketListener {
        void onPriceUpdate(int itemId, double currentPrice, int bidderId, String bidderName, long bidTime);
        void onParticipantUpdate(int itemId, int participantCount);
        void onAuctionFinished(int itemId, double finalPrice, int winnerId, String winnerName);
        void onTimerExtended(int itemId, int newTimeRemaining);
        void onConnected();
        void onDisconnected();
        void onItemsReceived(org.json.JSONArray items); // Để hứng danh sách xe
        void onBidResponse(boolean isSuccess, String message); // Để hứng thông báo đặt giá thành công/thất bại
    }

    private AuctionWebSocketClient(String serverUrl) throws URISyntaxException {
        super(new URI(serverUrl));
    }

    /**
     * Singleton - lấy hoặc tạo instance
     */
    public static synchronized AuctionWebSocketClient getInstance() {
        try {
            // =========================================
            // CHƯA CÓ INSTANCE
            // =========================================
            if (instance == null) {
                instance = new AuctionWebSocketClient("wss://plateauction-production.up.railway.app");

                System.out.println("⏳ Đang kết nối WebSocket lên mây...");
                // 👇 ĐÃ SỬA THÀNH connectBlocking() ĐỂ ÉP CHỜ KẾT NỐI
                boolean connected = instance.connectBlocking();

                if (connected) {
                    System.out.println("✅ WebSocket instance created và đã kết nối!");
                } else {
                    System.out.println("❌ Kết nối WebSocket thất bại!");
                }
            }
            // =========================================
            // MẤT KẾT NỐI → RECONNECT
            // =========================================
            else if (!instance.isConnected && !instance.isReconnecting) {
                instance.isReconnecting = true;
                System.out.println("♻️ WebSocket reconnecting...");

                // 👇 ĐÃ SỬA THÀNH reconnectBlocking()
                instance.reconnectBlocking();
                System.out.println("✅ Đã reconnect xong!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return instance;
    }

    @Override
    public void onClose(
            int code,
            String reason,
            boolean remote
    ) {

        isConnected = false;

        isReconnecting = false;

        System.out.println(
                "❌ WebSocket disconnected: " + reason
        );

        for (AuctionWebSocketListener listener : listeners) {
            listener.onDisconnected();
        }
    }
    @Override
    public void onOpen(ServerHandshake handshake) {

        isConnected = true;

        isReconnecting = false;

        System.out.println(
                "✅ WebSocket connected"
        );

        for (AuctionWebSocketListener listener : listeners) {

            listener.onConnected();
        }
    }
    /**
     * Nhận message từ server
     */
    @Override
    public void onMessage(String message) {
        System.out.println("📩 WS MESSAGE: " + message);
        try {
            JSONObject json = new JSONObject(message);

            // Server có thể gửi "type" (cho realtime broadcast) hoặc "action" (cho trả kết quả request)
            String type = json.optString("type", "");
            String action = json.optString("action", "");

            // =============================================================
            // LUỒNG 1: XỬ LÝ KẾT QUẢ REQUEST (Thay thế cho TCP Socket cũ)
            // =============================================================
            if ("GET_ITEMS_RESPONSE".equals(action)) {
                if ("success".equals(json.optString("status"))) {
                    org.json.JSONArray itemsArray = json.getJSONArray("items");
                    // Đẩy danh sách xe ra cho Giao diện hiển thị
                    listeners.forEach(listener -> listener.onItemsReceived(itemsArray));
                }
            }
            else if ("PLACE_BID_RESPONSE".equals(action)) {
                boolean isSuccess = "success".equals(json.optString("status"));
                String msg = json.optString("message", "");
                // Báo cho Giao diện biết đặt giá thành công hay lỗi (để hiện Popup Alert)
                listeners.forEach(listener -> listener.onBidResponse(isSuccess, msg));
            }

            // =============================================================
            // LUỒNG 2: XỬ LÝ REAL-TIME BROADCAST (Code cũ của bạn giữ nguyên)
            // =============================================================
            else if ("PRICE_UPDATE".equals(type)) {
                int itemId = json.getInt("itemId");
                double currentPrice = json.getDouble("currentPrice");
                int bidderId = json.getInt("bidderId");
                String bidderName = json.getString("bidderName");
                long bidTime = json.optLong("bidTime", json.optLong("timestamp", System.currentTimeMillis()));

                listeners.forEach(listener -> listener.onPriceUpdate(itemId, currentPrice, bidderId, bidderName, bidTime));

            } else if ("PARTICIPANT_UPDATE".equals(type)) {
                int itemId = json.getInt("itemId");
                int participantCount = json.getInt("participantCount");

                listeners.forEach(listener -> listener.onParticipantUpdate(itemId, participantCount));

            } else if ("AUCTION_FINISHED".equals(type)) {
                int itemId = json.getInt("itemId");
                double finalPrice = json.getDouble("finalPrice");
                int winnerId = json.getInt("winnerId");
                String winnerName = json.getString("winnerName");

                listeners.forEach(listener -> listener.onAuctionFinished(itemId, finalPrice, winnerId, winnerName));

            } else if ("TIMER_EXTENDED".equals(type)) {
                int itemId = json.getInt("itemId");
                int newTimeRemaining = json.getInt("newTimeRemaining");

                listeners.forEach(listener -> listener.onTimerExtended(itemId, newTimeRemaining));

            } else if ("SUBSCRIBED".equals(type)) {
                System.out.println("📌 Subscribed tới itemId: " + json.getInt("itemId"));
            }

        } catch (Exception e) {
            System.err.println("❌ Lỗi xử lý WebSocket message: " + e.getMessage());
            e.printStackTrace(); // Thêm dòng này để in chi tiết lỗi nếu có
        }
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("⚠️ WebSocket Client error: " + ex.getMessage());
        ex.printStackTrace();
    }

    /**
     * Subscribe tới updates của 1 item
     */
    public void subscribe(int itemId) {
        if (!isConnected) {
            System.err.println("❌ Không thể subscribe - WS chưa connect. Vui lòng kiểm tra lại mạng!");
            return;
        }

        try {
            JSONObject message = new JSONObject();
            message.put("action", "SUBSCRIBE");
            message.put("itemId", itemId);

            send(message.toString());
            System.out.println("📤 SUBSCRIBED itemId = " + itemId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Unsubscribe từ updates của 1 item
     */
    public void unsubscribe(int itemId) {
        if (isConnected) {
            JSONObject message = new JSONObject();
            message.put("action", "UNSUBSCRIBE");
            message.put("itemId", itemId);
            send(message.toString());
            System.out.println("📤 Gửi UNSUBSCRIBE itemId: " + itemId);
        }
    }

    /**
     * Đăng ký listener để nhận updates
     */
    public void addListener(AuctionWebSocketListener listener) {

        if (!listeners.contains(listener)) {

            listeners.add(listener);

            System.out.println(
                    "✅ Listener added"
            );
        }
    }
    /**
     * Bỏ đăng ký listener
     */
    public void removeListener(AuctionWebSocketListener listener) {
        listeners.remove(listener);
    }

    public boolean isConnected() {
        return isConnected;
    }
}

