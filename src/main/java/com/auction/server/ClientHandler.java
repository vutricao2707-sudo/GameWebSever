package com.auction.server;

import java.io.IOException;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (Socket s = clientSocket;
             var in = s.getInputStream();
             var out = s.getOutputStream()) {
            s.setSoTimeout(10_000); // timeout 10s cho read
            System.out.println("Xử lý client " + s.getRemoteSocketAddress());
            // đọc/ghi với in/out (BufferedReader/PrintWriter hoặc DataInputStream...)
            // Ví dụ demo: chờ 5s (thay bằng logic thực tế)
            Thread.sleep(5000);
        } catch (IOException e) {
            System.err.println("I/O error với client: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}