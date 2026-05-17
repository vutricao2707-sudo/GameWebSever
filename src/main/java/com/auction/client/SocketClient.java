package com.auction.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SocketClient - client đơn giản kết nối đến AuctionServer.
 * Cải tiến:
 * - Singleton thread-safe (Initialization-on-demand holder idiom)
 * - Listener callback để UI cập nhật an toàn
 * - Quản lý thread listener, timeout đọc, đóng tài nguyên an toàn
 */
public class SocketClient {
    // Lazy-loaded thread-safe singleton
    private SocketClient() {}

    private static class Holder {
        private static final SocketClient INSTANCE = new SocketClient();
    }

    public static SocketClient getInstance() {
        return Holder.INSTANCE;
    }

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean running = false;
    private Thread listenerThread;

    // Dùng CopyOnWriteArrayList để an toàn khi notify trong nhiều thread
    private final CopyOnWriteArrayList<MessageListener> listeners = new CopyOnWriteArrayList<>();

    // Listener interface để UI có thể đăng ký
    public interface MessageListener {
        void onMessageReceived(String message);
        void onConnectionLost();
    }

    public void addMessageListener(MessageListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeMessageListener(MessageListener listener) {
        listeners.remove(listener);
    }

    /**
     * Kết nối tới server.
     * @param host hostname (ví dụ "localhost")
     * @param port port (ví dụ 8080)
     * @return true nếu kết nối thành công
     */
    public boolean connect(String host, int port) {
        disconnect(); // đảm bảo ngắt kết nối trước nếu có
        try {
            socket = new Socket(host, port);
            socket.setSoTimeout(10_000); // timeout read 10s (tùy chỉnh nếu cần)

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            running = true;
            listenerThread = new Thread(this::listenForMessages, "SocketClient-Listener");
            listenerThread.setDaemon(true);
            listenerThread.start();

            System.out.println("Kết nối tới " + host + ":" + port + " thành công!");
            return true;
        } catch (IOException e) {
            System.err.println("Lỗi kết nối Socket: " + e.getMessage());
            running = false;
            cleanupStreams();
            return false;
        }
    }

    // Vòng lặp lắng nghe từ server
    private void listenForMessages() {
        try {
            String serverMessage;
            while (running && (serverMessage = in.readLine()) != null) {
                // In log cho dev
                System.out.println("Server: " + serverMessage);
                // Thông báo tới tất cả listeners
                for (MessageListener l : listeners) {
                    try {
                        l.onMessageReceived(serverMessage);
                    } catch (Exception ex) {
                        // Bắt lỗi của listener để không làm dừng thread chính
                        System.err.println("Listener error: " + ex.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Lỗi I/O khi lắng nghe server: " + e.getMessage());
            } else {
                // đang disconnect bình thường
            }
        } finally {
            running = false;
            notifyConnectionLost();
            cleanupStreams();
        }
    }

    private void notifyConnectionLost() {
        for (MessageListener l : listeners) {
            try {
                l.onConnectionLost();
            } catch (Exception ex) {
                System.err.println("Listener error onConnectionLost: " + ex.getMessage());
            }
        }
    }

    /**
     * Gửi 1 dòng text (JSON hoặc text) tới server.
     * Protocol ở server phải tương thích (line-based).
     */
    public void sendMessage(String message) {
        if (out != null && !out.checkError() && isConnected()) {
            out.println(message);
        } else {
            System.err.println("Không thể gửi: chưa kết nối hoặc luồng lỗi");
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed() && running;
    }

    /**
     * Ngắt kết nối và dọn dẹp tài nguyên.
     */
    public void disconnect() {
        running = false;
        cleanupStreams();

        // đợi listener thread kết thúc ngắn (không block lâu)
        if (listenerThread != null && listenerThread.isAlive()) {
            try {
                listenerThread.join(2_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Đóng streams & socket an toàn
    private void cleanupStreams() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException ignored) {}
        if (out != null) {
            out.close();
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
        in = null;
        out = null;
        socket = null;
    }
}