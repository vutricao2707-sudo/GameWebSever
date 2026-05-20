package com.auction.server;

import java.io.*;
import java.net.Socket;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import com.auction.service.ProductService;
import com.auction.database.BidDAO;
import com.auction.database.ItemDAO;
import com.auction.database.UserDAO;
import com.auction.model.User;

/**
 * 🤖 ClientHandler = "Người phục vụ" mỗi client
 *
 * Mỗi client (App JavaFX) kết nối → Server tạo 1 ClientHandler
 * ClientHandler chịu trách nhiệm:
 * ✓ Đọc command từ client
 * ✓ Xử lý command
 * ✓ Gửi response về client
 */
public class ClientHandler implements Runnable {
    private final Socket clientSocket;  // Cái "ống" để nói chuyện với client

    // ✨ TẠO INSTANCE
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    // 🏃 CHẠY THREAD
    @Override
    public void run() {
        try (Socket s = clientSocket;
             // InputStream = "Cái lỗ" để NHẬN dữ liệu từ client
             InputStream inputStream = s.getInputStream();
             // OutputStream = "Cái lỗ" để GỬI dữ liệu cho client
             OutputStream outputStream = s.getOutputStream()) {

            // CẤU HÌNH SOCKET
            s.setSoTimeout(10_000);  // Nếu đợi lâu hơn 10s mà ko có dữ liệu thì timeout

            System.out.println("✅ Client kết nối: " + s.getRemoteSocketAddress());

            // TẠO "CÁI ỐNG" ĐỂ ĐỌC/GHI DỮ LIỆU
            // BufferedReader = Đọc dữ liệu nhanh hơn, dễ dàng hơn
            BufferedReader br = new BufferedReader(
                new InputStreamReader(inputStream, "UTF-8")  // UTF-8 để hỗ trợ Tiếng Việt
            );

            // PrintWriter = Ghi dữ liệu, true = tự động flush (gửi ngay)
            PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(outputStream, "UTF-8"),
                true  // auto-flush: gửi ngay lập tức
            );

            // LẶP VÔ HẠN - LẮNG NGHE TỪ CLIENT
            String jsonCommand;  // Biến chứa command từ client (JSON format)

            while ((jsonCommand = br.readLine()) != null) {
                // Nhận được 1 dòng text từ client
                System.out.println(" Nhận command: " + jsonCommand);

                try {
                    // PARSE JSON (phân tích dữ liệu)
                    JSONObject request = new JSONObject(jsonCommand);
                    String action = request.getString("action");  // Lấy "action"

                    // CHỌN XỬ LÝ THEO ACTION
                    JSONObject response = new JSONObject();

                    if ("GET_ITEMS".equals(action)) {
                        // CLIENT MUỐN XEM DANH SÁCH BIỂN SỐ
                        handleGetItems(request, response);
                    }
                    else if ("PLACE_BID".equals(action)) {
                        // CLIENT MUỐN ĐẤU GIÁ
                        handlePlaceBid(request, response);
                    }
                    else if ("GET_PENDING_ITEMS".equals(action)) {
                        // ADMIN XEM DANH SÁCH CHỜ DUYỆT
                        handleGetPendingItems(request, response);
                    }
                    else if ("APPROVE_ITEM".equals(action)) {
                        // ADMIN DUYỆT BIỂN SỐ
                        handleApproveItem(request, response);
                    }
                    else {
                        // ACTION KHÔNG NHẬN DẠng
                        response.put("status", "error");
                        response.put("message", "Action không hợp lệ: " + action);
                    }

                    // GỬI RESPONSE VỀ CLIENT
                    String jsonResponse = response.toString();
                    System.out.println("Gửi response: " + jsonResponse);
                    pw.println(jsonResponse);

                } catch (Exception e) {
                    //LỖI KHI PARSE JSON HOẶC XỬ LÝ
                    System.err.println("Lỗi xử lý: " + e.getMessage());

                    // GỬI ERROR RESPONSE VỀ CLIENT
                    JSONObject errorResponse = new JSONObject();
                    errorResponse.put("status", "error");
                    errorResponse.put("message", "Lỗi server: " + e.getMessage());
                    pw.println(errorResponse.toString());
                }
            }

            // KẾT THÚC KẾT NỐI
            System.out.println("Client ngắt kết nối: " + s.getRemoteSocketAddress());

        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        }
    }

    /**
     *HANDLE GET_ITEMS
     * Client yêu cầu: xem danh sách biển số đã duyệt
     */
    private void handleGetItems(JSONObject request, JSONObject response) {
        try {
            String type = request.optString("type", "Car");  // Mặc định là "Car"

            // LẤY DANH SÁCH TỬ DATABASE
            List<String[]> items = ProductService.getApprovedProductsByType(type);

            // CONVERT THÀNH JSON
            JSONArray itemsArray = new JSONArray();
            for (String[] item : items) {
                JSONObject itemObj = new JSONObject();
                itemObj.put("id", item[0]);
                itemObj.put("plate_number", item[1]);
                itemObj.put("start_price", item[2]);
                itemObj.put("current_price", item[3]);
                itemObj.put("description", item[4]);
                itemObj.put("vehicle_type", item[5]);
                itemsArray.put(itemObj);
            }

            // TẠO RESPONSE
            response.put("status", "success");
            response.put("items", itemsArray);
            response.put("count", items.size());

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
    }

    /**
     * HANDLE PLACE_BID
     * Client muốn đấu giá (tăng giá)
     */
    private void handlePlaceBid(JSONObject request, JSONObject response) {
        try {
            int itemId = request.getInt("itemId");
            int userId = request.getInt("userId");  // Lấy user ID từ client
            double amount = request.getDouble("amount");
            long lastBid = BidDAO.getLastBidTimeForUser(itemId, userId);
            long now = System.currentTimeMillis() / 1000;

            if (now - lastBid < 60) {
                response.put("status", "error");
                response.put("message", "Phải chờ 60 giây giữa các lần đặt giá");
                return;
            }

            //  LƯU VÀO DATABASE (sử dụng saveBid cho validation + auto-extend)
            BidDAO.addBid(itemId, userId, amount);

            // Cập nhật giá hiện tại của item
            ItemDAO.updateCurrentPrice(itemId, amount);

            // ✅ BROADCAST giá mới qua WebSocket tới tất cả clients đang theo dõi
            AuctionWebSocketServer wsServer = AuctionWebSocketServer.getInstance();
            User user = UserDAO.getUserById(userId);
            String bidderName = (user != null) ? user.getFullName() : "Người dùng #" + userId;
            wsServer.broadcastPriceUpdate(itemId, amount, userId, bidderName);

            // ===================================================
            // ⏱️ AUTO-EXTEND TIMER (nếu bid trong 2 phút cuối)
            // ===================================================
            if (BidDAO.shouldExtendAuction(itemId)) {
                ItemDAO.extendAuctionTime(itemId, 2); // Extend 2 phút
                long newTimeRemaining = ItemDAO.getAuctionTimeRemainingSeconds(itemId);
                wsServer.broadcastTimerExtended(itemId, (int) newTimeRemaining);
                System.out.println("⏱️ AUTO-EXTENDED: Timer được kéo dài 2 phút → Broadcast TIMER_EXTENDED");
            }

            response.put("status", "success");
            response.put("message", "Đấu giá thành công");
            response.put("new_price", amount);

            System.out.println("✅ PLACE_BID: itemId=" + itemId + ", newPrice=" + amount + " - Broadcast to WebSocket");

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            System.err.println("❌ Lỗi PLACE_BID: " + e.getMessage());
        }
    }

    /**
     * HANDLE GET_PENDING_ITEMS (ADMIN)
     * Xem danh sách biển số chờ duyệt
     */
    private void handleGetPendingItems(JSONObject request, JSONObject response) {
        try {
            List<String[]> pendingItems = ProductService.getPendingProducts();

            JSONArray itemsArray = new JSONArray();
            for (String[] item : pendingItems) {
                JSONObject itemObj = new JSONObject();
                itemObj.put("id", item[0]);
                itemObj.put("plate_number", item[1]);
                itemObj.put("description", item[4]);
                itemObj.put("seller_name", item[7]);  // Tên người bán
                itemsArray.put(itemObj);
            }

            response.put("status", "success");
            response.put("pending_items", itemsArray);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
    }

    /**
     * HANDLE APPROVE_ITEM (ADMIN)
     * Duyệt 1 biển số
     */
    private void handleApproveItem(JSONObject request, JSONObject response) {
        try {
            String plateNumber = request.getString("plate_number");

            boolean success = ProductService.approveProduct(plateNumber);

            if (success) {
                response.put("status", "success");
                response.put("message", "Duyệt biển số thành công");
            } else {
                response.put("status", "error");
                response.put("message", "Không thể duyệt biển số này");
            }

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
    }
}