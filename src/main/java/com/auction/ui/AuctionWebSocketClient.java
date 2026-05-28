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

public class AuctionWebSocketClient extends WebSocketClient {

    private static AuctionWebSocketClient instance;
    private static final String SERVER_URL = "wss://plateauction-production.up.railway.app";
    private final List<AuctionWebSocketListener> listeners = new CopyOnWriteArrayList<>();

    private boolean isConnected = false;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> reconnectTask = null;

    private int retryAttempt = 0;
    private final int MAX_DELAY_MS = 30000;
    private final int INITIAL_DELAY_MS = 1000;

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

    public static synchronized AuctionWebSocketClient getInstance() {
        try {
            if (instance == null) {
                instance = new AuctionWebSocketClient(SERVER_URL);
                System.out.println("⏳ Đang kết nối WebSocket lên mây...");
                // Ép Client phải kiên nhẫn chờ Railway thức dậy tối đa 10 giây
                instance.connectBlocking(10, java.util.concurrent.TimeUnit.SECONDS);
                if (instance.isOpen()) {
                    System.out.println("✅ WebSocket instance created và đã kết nối!");
                } else {
                    System.out.println("❌ Kết nối WebSocket lần đầu thất bại!");
                    instance.triggerAutoReconnect();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return instance;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        isConnected = true;
        System.out.println("✅ WebSocket connected");

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

        if (!isIntentionalClose.get()) {
            triggerAutoReconnect();
        }
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("⚠️ WebSocket Client error: " + ex.getMessage());
    }

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
        }
    }

    public void removeListener(AuctionWebSocketListener listener) {
        listeners.remove(listener);
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void shutdownIntentionally() {
        isIntentionalClose.set(true);
        if (reconnectTask != null) reconnectTask.cancel(true);
        this.close();
    }

    private void triggerAutoReconnect() {
        if (reconnectTask != null && !reconnectTask.isDone()) return;

        retryAttempt++;
        long baseDelay = (long) (INITIAL_DELAY_MS * Math.pow(2, retryAttempt - 1));
        if (baseDelay > MAX_DELAY_MS) baseDelay = MAX_DELAY_MS;

        long minJitter = baseDelay / 2;
        long actualDelay = minJitter + (long) (Math.random() * (baseDelay - minJitter));

        System.out.println("♻️ Mất mạng! Thử kết nối lại lần " + retryAttempt + " sau " + actualDelay + "ms...");

        reconnectTask = scheduler.schedule(() -> {
            try {
                // Thử nối lại nhẹ nhàng
                boolean success = reconnectBlocking();
                if (!success) {
                    triggerAutoReconnect();
                }
            } catch (Exception e) {
                System.err.println("❌ Thư viện văng lỗi " + e.getClass().getSimpleName() + ". Bật khiên Tái sinh!");
                // Nếu thư viện văng IllegalStateException, dùng tuyệt chiêu Tái sinh hoàn toàn
                triggerHardReconnect();
            }
        }, actualDelay, TimeUnit.MILLISECONDS);
    }

    // TUYỆT CHIÊU CUỐI: Tái sinh hoàn toàn Client khi thư viện bị đơ
    private void triggerHardReconnect() {
        System.out.println("🔥 Kích hoạt Hard Reconnect (Tạo Client mới tinh)...");
        try {
            AuctionWebSocketClient newClient = new AuctionWebSocketClient(SERVER_URL);

            // Chuyển toàn bộ "người nghe" sang client mới
            newClient.listeners.addAll(this.listeners);
            newClient.retryAttempt = this.retryAttempt;

            // Ép con trỏ Singleton trỏ sang bản thể mới
            instance = newClient;

            // Ép Client phải kiên nhẫn chờ Railway thức dậy tối đa 10 giây
            instance.connectBlocking(10, java.util.concurrent.TimeUnit.SECONDS);
            if (!newClient.isOpen()) {
                newClient.triggerAutoReconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}