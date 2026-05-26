package com.auction.ui;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WebSocket Client cho Auction System UI
 * Kết nối tới server WebSocket để nhận realtime updates (Tích hợp Auto-Reconnect thông minh)
 */
public class AuctionWebSocketClient extends WebSocketClient {

    private static AuctionWebSocketClient instance;
    private static final String SERVER_URL = "wss://plateauction-production.up.railway.app";
    private final List<AuctionWebSocketListener> listeners = new CopyOnWriteArrayList<>();

    private boolean isConnected = false;

    // =========================================================================
    // CÁC BIẾN QUẢN LÝ RECONNECT THÔNG MINH
    // =========================================================================
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> reconnectTask = null;

    private int retryAttempt = 0;
    private final int MAX_DELAY_MS = 30000;    // Đợi tối đa 30 giây
    private final int INITIAL_DELAY_MS = 1000; // Khởi điểm đợi 1 giây

    // Cờ đánh dấu: Người dùng chủ động thoát hay do mất mạng?
    private final AtomicBoolean isIntentionalClose = new AtomicBoolean(false);

    public interface AuctionWebSocketListener {
        void onPriceUpdate(int itemId, double currentPrice, int bidderId, String bidderName, long bidTime);
        void onParticipantUpdate(int itemId, int participantCount);
        void onAuctionFinished(int itemId, double finalPrice, int winnerId, String winnerName);
        void onTimerExtended(int itemId, int newTimeRemaining);
        void onConnected();
        void onDisconnected();
        void onItemsReceived(JSONArray items);
        void onBidResponse(boolean isSuccess, String message);
    }

    private AuctionWebSocketClient(String serverUrl) throws URISyntaxException {
        super(new URI(serverUrl));
    }

    /**
     * Singleton - lấy hoặc tạo instance
     */
    public static synchronized AuctionWebSocketClient getInstance() {
        try {
            if (instance == null) {
                instance = new AuctionWebSocketClient(SERVER_URL);
                System.out.println("⏳ Đang kết nối WebSocket lên mây...");
                instance.connectBlocking();

                if (instance.isOpen()) {
                    System.out.println("✅ WebSocket instance created và đã kết nối!");
                } else {
                    System.out.println("❌ Kết nối WebSocket lần đầu thất bại!");
                    // Nếu lần đầu server chưa lên, tự động kích hoạt tiến trình thử lại ngầm
                    instance.triggerAutoReconnect();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return instance;
    }

    // =========================================================================
    // XỬ LÝ SỰ KIỆN WEBSOCKET
    // =========================================================================
    @Override
    public void onOpen(ServerHandshake handshake) {
        isConnected = true;
        System.out.println("✅ WebSocket connected");

        // Khi mạng có lại, dọn dẹp các tiến trình Reconnect đang chạy dở
        retryAttempt = 0;
        if (reconnectTask != null && !reconnectTask.isDone()) {
            reconnectTask.cancel(false);
        }

        for (AuctionWebSocketListener listener : listeners) {
            listener.onConnected();
        }
    }

    @Override
    public void onMessage(String message) {
        System.out.println("📩 WS MESSAGE: " + message);
        try {
            JSONObject json = new JSONObject(message);

            String type = json.optString("type", "");
            String action = json.optString("action", "");

            // =============================================================
            // LUỒNG 1: XỬ LÝ KẾT QUẢ REQUEST
            // =============================================================
            if ("GET_ITEMS_RESPONSE".equals(action)) {
                if ("success".equals(json.optString("status"))) {
                    JSONArray itemsArray = json.getJSONArray("items");
                    listeners.forEach(listener -> listener.onItemsReceived(itemsArray));
                }
            }
            else if ("PLACE_BID_RESPONSE".equals(action)) {
                boolean isSuccess = "success".equals(json.optString("status"));
                String msg = json.optString("message", "");
                listeners.forEach(listener -> listener.onBidResponse(isSuccess, msg));
            }

            // =============================================================
            // LUỒNG 2: XỬ LÝ REAL-TIME BROADCAST
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
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        isConnected = false;
        System.out.println("❌ WebSocket disconnected: " + reason);

        for (AuctionWebSocketListener listener : listeners) {
            listener.onDisconnected();
        }

        // KÍCH HOẠT AUTO-RECONNECT NẾU KHÔNG PHẢI DO USER BẤM THOÁT
        if (!isIntentionalClose.get()) {
            triggerAutoReconnect();
        }
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("⚠️ WebSocket Client error: " + ex.getMessage());
    }

    // =========================================================================
    // CÁC HÀM GỬI LỆNH VÀ ĐĂNG KÝ
    // =========================================================================
    public void subscribe(int itemId) {
        if (!isConnected) {
            System.err.println("❌ Không thể subscribe - WS chưa connect.");
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

    public void unsubscribe(int itemId) {
        if (isConnected) {
            JSONObject message = new JSONObject();
            message.put("action", "UNSUBSCRIBE");
            message.put("itemId", itemId);
            send(message.toString());
            System.out.println("📤 Gửi UNSUBSCRIBE itemId: " + itemId);
        }
    }

    public void addListener(AuctionWebSocketListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            System.out.println("✅ Listener added");
        }
    }

    public void removeListener(AuctionWebSocketListener listener) {
        listeners.remove(listener);
    }

    public boolean isConnected() {
        return isConnected;
    }

    // =========================================================================
    // THUẬT TOÁN AUTO-RECONNECT
    // =========================================================================

    /**
     * Dùng khi người dùng bấm nút Thoát/Back. Cấm hệ thống tự bật lại.
     */
    public void shutdownIntentionally() {
        isIntentionalClose.set(true);
        if (reconnectTask != null) reconnectTask.cancel(true);
        this.close();
    }

    /**
     * Tính toán thời gian chờ ngẫu nhiên và khôi phục mạng ngầm
     */
    private void triggerAutoReconnect() {
        if (reconnectTask != null && !reconnectTask.isDone()) {
            return; // Tránh tạo nhiều task đè lên nhau
        }

        retryAttempt++;
        long baseDelay = (long) (INITIAL_DELAY_MS * Math.pow(2, retryAttempt - 1));
        if (baseDelay > MAX_DELAY_MS) baseDelay = MAX_DELAY_MS;

        // Thêm Jitter để tránh tình trạng nhiều máy tính cùng tấn công Server lúc có mạng lại
        long minJitter = baseDelay / 2;
        long actualDelay = minJitter + (long) (Math.random() * (baseDelay - minJitter));

        System.out.println("♻️ Mất mạng! Thử kết nối lại lần " + retryAttempt + " sau " + actualDelay + "ms...");

        reconnectTask = scheduler.schedule(() -> {
            try {
                // Thư viện Java-WebSocket cung cấp sẵn hàm reconnectBlocking() chạy trên luồng ngầm
                reconnectBlocking();
            } catch (Exception e) {
                System.err.println("❌ Reconnect thất bại: " + e.getMessage());
            }
        }, actualDelay, TimeUnit.MILLISECONDS);
    }
}