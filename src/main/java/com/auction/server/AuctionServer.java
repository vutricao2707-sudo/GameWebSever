package com.auction.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class AuctionServer {
    private static final int PORT = 8080;
    private static volatile boolean running = true;

    public static void main(String[] args) {
        System.out.println("Khởi động AuctionServer trên port " + PORT + "...");

        // Thread pool: số luồng có thể điều chỉnh theo nhu cầu
        ExecutorService pool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 2,
                new ThreadFactory() {
                    private int count = 0;
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "client-handler-" + (++count));
                        t.setDaemon(false);
                        return t;
                    }
                });

        // Shutdown hook để dừng server mượt khi nhận SIGINT/SIGTERM
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown hook: dừng server...");
            running = false;
            try {
                pool.shutdown();
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }));

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            serverSocket.setReuseAddress(true);
            serverSocket.setSoTimeout(1000); // kiểm tra flag running mỗi 1s
            System.out.println("Server sẵn sàng lắng nghe kết nối!");

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client mới: " + clientSocket.getInetAddress().getHostAddress() +
                            ":" + clientSocket.getPort());
                    // submit cho pool thay vì tạo thread tay
                    pool.submit(new ClientHandler(clientSocket));
                } catch (SocketTimeoutException ste) {
                    // timeout để kiểm tra biến running, tiếp tục vòng lặp
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Lỗi accept(): " + e.getMessage());
                    } else {
                        System.out.println("Server đang dừng, accept kết thúc.");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi khởi động Server: " + e.getMessage());
        } finally {
            // đảm bảo shutdown pool nếu chưa làm
            pool.shutdownNow();
        }

        System.out.println("Server đã dừng.");
    }
}