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
        void onPriceUpdate(int itemId, double currentPrice, int bidderId, String bidderName);
        void onParticipantUpdate(int itemId, int participantCount);
        void onAuctionFinished(int itemId, double finalPrice, int winnerId, String winnerName);
        void onTimerExtended(int itemId, int newTimeRemaining);
        void onConnected();
        void onDisconnected();
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

                instance =
                        new AuctionWebSocketClient(
                                "ws://localhost:8081"
                        );

                instance.connect();

                System.out.println(
                        "✅ WebSocket instance created"
                );
            }

            // =========================================
            // MẤT KẾT NỐI → RECONNECT
            // =========================================

            else if (!instance.isConnected
                    && !instance.isReconnecting) {

                instance.isReconnecting = true;

                System.out.println(
                        "♻️ WebSocket reconnecting..."
                );

                instance.reconnect();
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
            String type = json.optString("type", "");

            if ("PRICE_UPDATE".equals(type)) {
                int itemId = json.getInt("itemId");
                double currentPrice = json.getDouble("currentPrice");
                int bidderId = json.getInt("bidderId");
                String bidderName = json.getString("bidderName");

                listeners.forEach(listener -> listener.onPriceUpdate(itemId, currentPrice, bidderId, bidderName));

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

        new Thread(() -> {

            try {

                int retry = 0;

                while (!isConnected && retry < 20) {

                    Thread.sleep(300);

                    retry++;
                }

                if (!isConnected) {

                    System.err.println(
                            "❌ Không thể subscribe - WS chưa connect"
                    );

                    return;
                }

                JSONObject message = new JSONObject();

                message.put("action", "SUBSCRIBE");
                message.put("itemId", itemId);

                send(message.toString());

                System.out.println(
                        "📤 SUBSCRIBED itemId = " + itemId
                );

            } catch (Exception e) {

                e.printStackTrace();
            }

        }).start();
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

