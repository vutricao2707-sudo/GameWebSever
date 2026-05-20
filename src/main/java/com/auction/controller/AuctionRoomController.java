package com.auction.controller;

import com.auction.database.BidDAO;
import com.auction.database.ItemDAO;
import com.auction.database.WatchlistDAO;
import com.auction.ui.AuctionWebSocketClient;
import com.auction.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionRoomController implements AuctionWebSocketClient.AuctionWebSocketListener {

    private static final Logger logger = Logger.getLogger(AuctionRoomController.class.getName());

    // --- WebSocket Client ---
    private AuctionWebSocketClient wsClient;
    private int currentItemId = -1;

    // --- Các thành phần FXML ---
    @FXML private Label lblMinutes, lblSeconds;
    @FXML private Label lblPlateTop, lblPlateBottom;
    @FXML private VBox liveHistoryContainer;
    @FXML private Label lblCurrentPrice, lblStepMultiplier;
    @FXML private Button btnPlaceBid;
    @FXML private Button btnWatchlist;  // Nút theo dõi
    @FXML private Label lblParticipants;  // Hiển thị số người tham gia
    @FXML private LineChart<Number, Number> priceHistoryChart;  // Biểu đồ lịch sử giá
    @FXML private Label lblBidCount;  // Số lần đặt giá

    // --- Biến lưu trữ Logic Đấu Giá ---
    private double currentPrice;                   // Giá khởi điểm từ database
    private final double stepValue = 5000000.0;    // Bước giá cố định 5 triệu
    private int stepMultiplier = 1;                // Hệ số nhân (1, 2, 3...)
    private String currentPlateNumber;             // Lưu biển số hiện tại
    private int currentParticipants = 0;           // Số người tham gia

    // --- Biến Đồng hồ ---
    private int timeRemainingSeconds = 60 * 60;   // Đếm ngược 60 phút
    private Timeline countdownTimeline;
    private final DecimalFormat formatter = new DecimalFormat("#,###");

    // --- Biến Cooldown (1 phút sau mỗi lần đặt giá) ---
    private long lastBidTime = 0;
    private static final long BID_COOLDOWN_SECONDS = 60; // 1 phút

    @FXML
    public void initialize() {
        logger.info("📍 [AuctionRoom] Bắt đầu initialize controller");
        
        try {
            // Check FXML elements null
            if (lblMinutes == null) {
                logger.log(Level.SEVERE, "lblMinutes là null - FXML không load đúng");
                return;
            }
            if (lblSeconds == null) {
                logger.log(Level.SEVERE, "lblSeconds là null - FXML không load đúng");
                return;
            }
            if (lblCurrentPrice == null) {
                logger.log(Level.SEVERE, "lblCurrentPrice là null - FXML không load đúng");
                return;
            }
            if (lblStepMultiplier == null) {
                logger.log(Level.SEVERE, "lblStepMultiplier là null - FXML không load đúng");
                return;
            }
            if (btnPlaceBid == null) {
                logger.log(Level.SEVERE, "btnPlaceBid là null - FXML không load đúng");
                return;
            }
            
            logger.info("✅ [AuctionRoom] Tất cả FXML elements loaded");

            // Initialize UI
            lblMinutes.setText("60");
            lblSeconds.setText("00");
            logger.info("✅ [AuctionRoom] Timer UI initialized");

            updatePricingUI();
            logger.info("✅ [AuctionRoom] Pricing UI updated");
            
            updateParticipantCount();
            logger.info("✅ [AuctionRoom] Participant count updated");

            // ========================= Fix Memory Leak =========================
            javafx.application.Platform.runLater(() -> {
                try {
                    Stage stage = (Stage) lblMinutes.getScene().getWindow();
                    if (stage != null) {
                        stage.setOnCloseRequest(event -> {
                            logger.info("🛑 [AuctionRoom] Window closing - stopping timelines");
                            stopAllTimelines();
                            
                            // Disconnect WebSocket
                            if (wsClient != null && currentItemId != -1) {
                                try {
                                    wsClient.unsubscribe(currentItemId);
                                    wsClient.removeListener(this);
                                    logger.info("🔌 [AuctionRoom] WebSocket unsubscribed on window close");
                                } catch (Exception eWs) {
                                    logger.log(Level.WARNING, "Error unsubscribing WebSocket", eWs);
                                }
                            }
                        });
                        logger.info("✅ [AuctionRoom] Window close handler registered");
                    }
                } catch (Exception eStage) {
                    logger.log(Level.WARNING, "Error setting window close handler", eStage);
                }
            });
            
            logger.info("✅ [AuctionRoom] Initialize completed");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi trong initialize", e);
        }
    }

    // =========================================================================
    // LOGIC ĐỒNG HỒ ĐẾM NGƯỢC
    // =========================================================================
    private void startCountdown() {
        logger.info("📍 [AuctionRoom] startCountdown() called");
        
        try {
            if (countdownTimeline != null) {
                logger.info("ℹ️  [AuctionRoom] Stopping existing countdown timeline");
                countdownTimeline.stop();
            }
            
            if (lblMinutes == null || lblSeconds == null) {
                logger.log(Level.SEVERE, "lblMinutes hoặc lblSeconds là null");
                return;
            }
            
            countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
                try {
                    if (timeRemainingSeconds > 0) {
                        timeRemainingSeconds--;
                        int minutes = timeRemainingSeconds / 60;
                        int seconds = timeRemainingSeconds % 60;

                        // Format luôn có 2 chữ số (VD: 05:09)
                        lblMinutes.setText(String.format("%02d", minutes));
                        lblSeconds.setText(String.format("%02d", seconds));
                    } else {
                        logger.info("⏰ [AuctionRoom] Countdown finished");
                        countdownTimeline.stop();
                        
                        try {
                            if (btnPlaceBid != null) {
                                btnPlaceBid.setText("PHIÊN ĐẤU GIÁ KẾT THÚC");
                                btnPlaceBid.setStyle("-fx-background-color: #334155; -fx-text-fill: #94a3b8; -fx-background-radius: 12; -fx-font-size: 16px; -fx-font-weight: bold;");
                                btnPlaceBid.setDisable(true);
                            }

                            // Finish auction in database
                            if (currentPlateNumber != null && !currentPlateNumber.isEmpty()) {
                                int itemId = ItemDAO.getItemIdByPlate(currentPlateNumber);
                                if (itemId != -1) {
                                    ItemDAO.finishAuction(itemId, currentPrice);
                                    logger.info("✅ [AuctionRoom] Auction finished in database");
                                }
                            }
                        } catch (Exception eFinish) {
                            logger.log(Level.WARNING, "Error finishing auction", eFinish);
                        }
                    }
                } catch (Exception eTimer) {
                    logger.log(Level.WARNING, "Error in countdown timer", eTimer);
                }
            }));
            
            countdownTimeline.setCycleCount(Timeline.INDEFINITE);
            countdownTimeline.play();
            logger.info("✅ [AuctionRoom] Countdown started");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in startCountdown", e);
        }
    }

    // =========================================================================
    // LOGIC TĂNG GIẢM BƯỚC GIÁ
    // =========================================================================
    @FXML
    private void handleIncreaseStep() {
        logger.info("📍 [AuctionRoom] handleIncreaseStep() called");
        try {
            stepMultiplier++;
            updatePricingUI();
            logger.info("✅ [AuctionRoom] Step increased to: " + stepMultiplier);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in handleIncreaseStep", e);
        }
    }

    @FXML
    private void handleDecreaseStep() {
        logger.info("📍 [AuctionRoom] handleDecreaseStep() called");
        try {
            if (stepMultiplier > 1) {
                stepMultiplier--;
                updatePricingUI();
                logger.info("✅ [AuctionRoom] Step decreased to: " + stepMultiplier);
            } else {
                logger.info("ℹ️  [AuctionRoom] Step multiplier already at minimum (1)");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in handleDecreaseStep", e);
        }
    }

    private void updatePricingUI() {
        logger.info("📍 [AuctionRoom] updatePricingUI() called - Price: " + currentPrice);
        
        try {
            if (lblStepMultiplier == null) {
                logger.log(Level.SEVERE, "lblStepMultiplier là null");
                return;
            }
            if (lblCurrentPrice == null) {
                logger.log(Level.SEVERE, "lblCurrentPrice là null");
                return;
            }
            if (btnPlaceBid == null) {
                logger.log(Level.SEVERE, "btnPlaceBid là null");
                return;
            }
            
            lblStepMultiplier.setText(String.valueOf(stepMultiplier));
            lblCurrentPrice.setText(formatter.format(currentPrice) + " đ");

            double nextBid = currentPrice + (stepValue * stepMultiplier);
            btnPlaceBid.setText("ĐẶT GIÁ: " + formatter.format(nextBid) + " đ");
            
            logger.info("✅ [AuctionRoom] Pricing UI updated - Next bid: " + nextBid);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in updatePricingUI", e);
        }
    }

    // =========================================================================
    // LOGIC BẤM NÚT "ĐẶT GIÁ" - CÓ COOLDOWN
    // =========================================================================
    @FXML
    private void handlePlaceBid() {
        logger.info("📍 [AuctionRoom] handlePlaceBid() called");
        
        try {
            // ====================== Check Item Status ======================
            if (currentPlateNumber == null || currentPlateNumber.isEmpty()) {
                logger.log(Level.WARNING, "Current plate number là null");
                showAlert("Lỗi", "Biển số không xác định");
                return;
            }

            String[] itemData = null;
            try {
                itemData = ItemDAO.getItemFullData(currentPlateNumber);
            } catch (Exception eDb) {
                logger.log(Level.SEVERE, "Error getting item data", eDb);
                showAlert("Lỗi", "Không thể kiểm tra trạng thái auction");
                return;
            }

            if (itemData != null && itemData.length >= 10) {
                String auctionStatus = itemData[9];

                if ("FINISHED".equalsIgnoreCase(auctionStatus)) {
                    logger.info("ℹ️  [AuctionRoom] Auction đã kết thúc");
                    if (btnPlaceBid != null) {
                        btnPlaceBid.setDisable(true);
                        btnPlaceBid.setText("PHIÊN ĐẤU GIÁ ĐÃ KẾT THÚC");
                    }
                    showAlert("Phiên đã kết thúc", "Không thể đặt giá nữa.");
                    return;
                }
            }

            // ====================== Client Cooldown ======================
            long currentTime = System.currentTimeMillis() / 1000;
            long timeSinceLastBid = currentTime - lastBidTime;

            if (timeSinceLastBid < BID_COOLDOWN_SECONDS) {
                long remainingTime = BID_COOLDOWN_SECONDS - timeSinceLastBid;
                logger.info("⏳ [AuctionRoom] Client cooldown - còn " + remainingTime + "s");
                
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Chờ một chút");
                alert.setHeaderText(null);
                alert.setContentText("Bạn phải chờ " + remainingTime + " giây nữa mới được đặt giá!");
                alert.showAndWait();
                return;
            }

            // ====================== Get User ======================
            if (SessionManager.INSTANCE.getCurrentUser() == null) {
                logger.log(Level.WARNING, "Current user là null");
                showAlert("Lỗi", "Bạn chưa đăng nhập");
                return;
            }

            int userId = SessionManager.INSTANCE.getCurrentUser().getId();
            logger.info("ℹ️  [AuctionRoom] User ID: " + userId);

            // ====================== Get Latest Price ======================
            double latestPrice;
            try {
                latestPrice = ItemDAO.getCurrentPrice(currentPlateNumber);
                logger.info("✅ [AuctionRoom] Latest price from DB: " + latestPrice);
            } catch (Exception ePriceDb) {
                logger.log(Level.SEVERE, "Error getting current price", ePriceDb);
                showAlert("Lỗi", "Không thể lấy giá hiện tại: " + ePriceDb.getMessage());
                return;
            }

            double newBid = latestPrice + (stepValue * stepMultiplier);
            logger.info("ℹ️  [AuctionRoom] New bid: " + newBid);

            // ====================== Save Bid ======================
            boolean bidSaved;
            try {
                bidSaved = BidDAO.saveBid(currentPlateNumber, userId, newBid);
                logger.info(bidSaved ? "✅ [AuctionRoom] Bid saved" : "❌ [AuctionRoom] Bid rejected by server");
            } catch (Exception eSaveBid) {
                logger.log(Level.SEVERE, "Error saving bid", eSaveBid);
                showAlert("Lỗi", "Không thể lưu lệnh đặt giá: " + eSaveBid.getMessage());
                return;
            }

            if (!bidSaved) {
                logger.info("⏳ [AuctionRoom] Server cooldown triggered");
                
                long dbLastBidTime = 0;
                try {
                    dbLastBidTime = BidDAO.getLastBidTime(currentPlateNumber, userId);
                } catch (Exception eLastBid) {
                    logger.log(Level.WARNING, "Error getting last bid time", eLastBid);
                }

                long remainingTime = (dbLastBidTime + 60) - (System.currentTimeMillis() / 1000);

                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Chờ một chút");
                alert.setHeaderText(null);
                alert.setContentText("⏳ Server chặn spam.\nBạn phải chờ " + Math.max(1, remainingTime) + " giây nữa.");
                alert.showAndWait();
                return;
            }

            // ====================== Atomic Update ======================
            boolean updated;
            try {
                updated = ItemDAO.updateCurrentPriceIfHigher(currentPlateNumber, latestPrice, newBid);
                logger.info(updated ? "✅ [AuctionRoom] Price updated in DB" : "❌ [AuctionRoom] Price update failed - someone bid first");
            } catch (Exception eUpdate) {
                logger.log(Level.SEVERE, "Error updating price", eUpdate);
                showAlert("Lỗi", "Không thể cập nhật giá: " + eUpdate.getMessage());
                return;
            }

            // ====================== Handle Update Failure ======================
            if (!updated) {
                logger.info("⚠️  [AuctionRoom] Người khác đặt trước - refresh price");
                
                try {
                    refreshCurrentPrice();
                } catch (Exception eRefresh) {
                    logger.log(Level.WARNING, "Error refreshing price", eRefresh);
                }

                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Đặt giá thất bại");
                alert.setHeaderText(null);
                alert.setContentText("⚠️ Đã có người đặt giá trước bạn.\nGiá đã được cập nhật.");
                alert.showAndWait();
                return;
            }

            // ====================== Success ======================
            logger.info("✅ [AuctionRoom] Bid placed successfully!");
            
            currentPrice = newBid;
            stepMultiplier = 1;

            try {
                updatePricingUI();
            } catch (Exception eUi) {
                logger.log(Level.WARNING, "Error updating UI", eUi);
            }

            lastBidTime = currentTime;

            try {
                addHistoryRow(formatter.format(newBid), currentPlateNumber + " - Bạn");
            } catch (Exception eHistory) {
                logger.log(Level.WARNING, "Error adding history row", eHistory);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi không mong đợi trong handlePlaceBid", e);
            showAlert("Lỗi hệ thống", "Có lỗi xảy ra: " + e.getMessage());
        }
    }

    /**
     * Xử lý nút "Theo dõi" - Thêm/Xóa item khỏi watchlist
     */
    @FXML
    private void handleWatchlist() {
        logger.info("📍 [AuctionRoom] handleWatchlist() called");
        
        try {
            if (SessionManager.INSTANCE.getCurrentUser() == null) {
                logger.log(Level.WARNING, "User chưa đăng nhập");
                showAlert("Chưa đăng nhập", "Bạn cần đăng nhập để theo dõi biển số");
                return;
            }

            if (currentPlateNumber == null || currentPlateNumber.isEmpty()) {
                logger.log(Level.WARNING, "Current plate number là null");
                showAlert("Lỗi", "Không tìm thấy biển số");
                return;
            }

            int userId = SessionManager.INSTANCE.getCurrentUser().getId();
            int itemId = currentItemId;

            if (itemId == -1) {
                try {
                    itemId = ItemDAO.getItemIdByPlate(currentPlateNumber);
                } catch (Exception eDb) {
                    logger.log(Level.SEVERE, "Error getting item ID", eDb);
                    showAlert("Lỗi", "Không thể lấy item ID");
                    return;
                }
                
                if (itemId == -1) {
                    logger.log(Level.WARNING, "Item ID still -1");
                    showAlert("Lỗi", "Không tìm thấy item trong database");
                    return;
                }
            }

            // Kiểm tra xem đã theo dõi chưa
            boolean isWatched;
            try {
                isWatched = WatchlistDAO.isInWatchlist(userId, itemId);
                logger.info("ℹ️  [AuctionRoom] Watchlist status: " + (isWatched ? "watched" : "not watched"));
            } catch (Exception eWatchlist) {
                logger.log(Level.SEVERE, "Error checking watchlist", eWatchlist);
                showAlert("Lỗi", "Không thể kiểm tra watchlist");
                return;
            }

            if (isWatched) {
                // Xóa khỏi watchlist
                boolean removed;
                try {
                    removed = WatchlistDAO.removeFromWatchlist(userId, itemId);
                } catch (Exception eRemove) {
                    logger.log(Level.SEVERE, "Error removing from watchlist", eRemove);
                    showAlert("Lỗi", "Không thể xóa khỏi watchlist");
                    return;
                }
                
                if (removed) {
                    btnWatchlist.setStyle("-fx-background-color: rgba(59, 130, 246, 0.3); -fx-border-color: #3b82f6; -fx-background-radius: 20; -fx-border-radius: 20; -fx-cursor: hand; -fx-padding: 6 15; -fx-font-size: 14; -fx-font-weight: bold;");
                    btnWatchlist.setText("⭐ Theo dõi");
                    showAlert("Đã xóa", "Biển số đã được xóa khỏi danh sách theo dõi");
                    logger.info("✅ [AuctionRoom] Removed from watchlist: " + currentPlateNumber);
                }
            } else {
                // Thêm vào watchlist
                boolean added;
                try {
                    added = WatchlistDAO.addToWatchlist(userId, itemId);
                } catch (Exception eAdd) {
                    logger.log(Level.SEVERE, "Error adding to watchlist", eAdd);
                    showAlert("Lỗi", "Không thể thêm vào watchlist");
                    return;
                }
                
                if (added) {
                    btnWatchlist.setStyle("-fx-background-color: rgba(234, 179, 8, 0.3); -fx-border-color: #eab308; -fx-background-radius: 20; -fx-border-radius: 20; -fx-cursor: hand; -fx-padding: 6 15; -fx-font-size: 14; -fx-font-weight: bold;");
                    btnWatchlist.setText("⭐ Bỏ theo dõi");
                    showAlert("Thành công", "Biển số đã được thêm vào danh sách theo dõi");
                    logger.info("✅ [AuctionRoom] Added to watchlist: " + currentPlateNumber);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi không mong đợi trong handleWatchlist", e);
            showAlert("Lỗi hệ thống", "Có lỗi xảy ra: " + e.getMessage());
        }
    }

    /**
      Cập nhật trạng thái button theo dõi khi load trang
     */
    private void updateWatchlistButtonState() {
        logger.info("📍 [AuctionRoom] updateWatchlistButtonState() called");
        
        try {
            if (btnWatchlist == null) {
                logger.log(Level.WARNING, "btnWatchlist là null");
                return;
            }
            
            if (SessionManager.INSTANCE.getCurrentUser() == null || currentItemId == -1) {
                logger.info("⚠️  [AuctionRoom] Watchlist button disabled - no user or item");
                btnWatchlist.setDisable(true);
                return;
            }

            int userId = SessionManager.INSTANCE.getCurrentUser().getId();
            boolean isWatched = false;
            
            try {
                isWatched = WatchlistDAO.isInWatchlist(userId, currentItemId);
                logger.info("ℹ️  [AuctionRoom] Watchlist status: " + (isWatched ? "watched" : "not watched"));
            } catch (Exception eWatchlist) {
                logger.log(Level.WARNING, "Error checking watchlist status", eWatchlist);
                return;
            }

            if (isWatched) {
                btnWatchlist.setStyle("-fx-background-color: rgba(234, 179, 8, 0.3); -fx-border-color: #eab308; -fx-background-radius: 20; -fx-border-radius: 20; -fx-cursor: hand; -fx-padding: 6 15; -fx-font-size: 14; -fx-font-weight: bold;");
                btnWatchlist.setText("⭐ Bỏ theo dõi");
            } else {
                btnWatchlist.setStyle("-fx-background-color: rgba(59, 130, 246, 0.3); -fx-border-color: #3b82f6; -fx-background-radius: 20; -fx-border-radius: 20; -fx-cursor: hand; -fx-padding: 6 15; -fx-font-size: 14; -fx-font-weight: bold;");
                btnWatchlist.setText("⭐ Theo dõi");
            }
            
            logger.info("✅ [AuctionRoom] Watchlist button updated");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in updateWatchlistButtonState", e);
        }
    }

    // =====================================================================
    // PHẦN MỚI: QUẢN LÝ SỐ NGƯỜI THAM GIA
    // =====================================================================
    private void updateParticipantCount() {
        logger.info("📍 [AuctionRoom] updateParticipantCount() called");
        
        try {
            if (currentPlateNumber == null || currentPlateNumber.isEmpty()) {
                logger.log(Level.WARNING, "Current plate number là null");
                return;
            }
            
            if (lblParticipants == null) {
                logger.log(Level.WARNING, "lblParticipants là null");
                return;
            }
            
            try {
                currentParticipants = ItemDAO.getParticipantCount(currentPlateNumber);
                lblParticipants.setText("👥 " + currentParticipants + " người tham gia");
                logger.info("✅ [AuctionRoom] Participant count: " + currentParticipants);
            } catch (Exception eDb) {
                logger.log(Level.WARNING, "Error getting participant count", eDb);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in updateParticipantCount", e);
        }
    }

    // =========================================================================
    // TRUYỀN DỮ LIỆU VÀ ĐIỀU HƯỚNG
    // =========================================================================

    // Hàm này sẽ được gọi từ Dashboard truyền sang tất cả thông tin từ database
    public void setAuctionRoomData(String plate, double startingPrice) {
        logger.info("📍 [AuctionRoom] setAuctionRoomData() - Plate: " + plate + ", Start price: " + startingPrice);
        System.out.println("🔍 DEBUG: Received plate = '" + plate + "'");
        System.out.println("🔍 DEBUG: Received startingPrice = " + startingPrice);
        
        try {
            // ====================== Validate Input ======================
            if (plate == null || plate.trim().isEmpty()) {
                logger.log(Level.SEVERE, "Plate number là null hoặc rỗng");
                System.out.println("❌ Plate is null or empty!");
                showAlert("Lỗi", "Biển số không hợp lệ");
                return;
            }
            if (startingPrice <= 0) {
                logger.log(Level.WARNING, "Starting price không hợp lệ: " + startingPrice);
                showAlert("Lỗi", "Giá khởi điểm không hợp lệ");
                return;
            }
            
            // ====================== Get Item Data ======================
            String[] itemData = null;
            try {
                itemData = ItemDAO.getItemFullData(plate);
                logger.info("✅ [AuctionRoom] Lấy itemData từ database");
            } catch (Exception eDb) {
                logger.log(Level.SEVERE, "Lỗi khi query database", eDb);
                showAlert("Lỗi database", "Không thể lấy dữ liệu item: " + eDb.getMessage());
                return;
            }
            
            if (itemData == null) {
                logger.log(Level.SEVERE, "itemData là null - item không tồn tại");
                showAlert("Lỗi", "Không tìm thấy item trong database");
                return;
            }
            
            if (itemData.length < 10) {
                logger.log(Level.SEVERE, "itemData không đủ dữ liệu - length: " + itemData.length);
                showAlert("Lỗi", "Dữ liệu item bị lỗi");
                return;
            }

            String auctionStatus = itemData[9];
            logger.info("ℹ️  [AuctionRoom] Auction status: " + auctionStatus);

            // ====================== Display Plate Number (TRƯỚC khi check status) ======================
            try {
                if (lblPlateTop == null) {
                    logger.log(Level.SEVERE, "lblPlateTop là null");
                    showAlert("Lỗi UI", "Các thành phần giao diện chưa được khởi tạo");
                    return;
                }
                if (lblPlateBottom == null) {
                    logger.log(Level.SEVERE, "lblPlateBottom là null");
                    showAlert("Lỗi UI", "Các thành phần giao diện chưa được khởi tạo");
                    return;
                }

                String plateTop = "";
                String plateBottom = "";

                if (plate.contains("-")) {
                    // Biển số có dấu "-" như "36-C1"
                    String[] parts = plate.split("-");
                    plateTop = (parts.length > 0) ? parts[0].trim() : "";
                    plateBottom = (parts.length > 1) ? parts[1].trim() : "";
                } else {
                    // Biển số không có dấu "-" như "36C1"
                    // Lấy 2 ký tự đầu làm phần trên, phần còn lại làm phần dưới
                    if (plate.length() >= 2) {
                        plateTop = plate.substring(0, 2);
                        plateBottom = plate.substring(2);
                    } else {
                        plateTop = plate;
                        plateBottom = "";
                    }
                }
                
                System.out.println("🔍 DEBUG: plateTop = '" + plateTop + "', plateBottom = '" + plateBottom + "'");
                lblPlateTop.setText(plateTop);
                lblPlateBottom.setText(plateBottom);
                System.out.println("✅ DEBUG: Labels updated successfully");
                logger.info("✅ [AuctionRoom] Biển số displayed: " + plateTop + " - " + plateBottom);
            } catch (Exception ePlate) {
                logger.log(Level.SEVERE, "Lỗi display plate", ePlate);
                showAlert("Lỗi", "Không thể hiển thị biển số");
                return;
            }

            // ====================== Check Auction Status ======================
            if ("REGISTRATION".equalsIgnoreCase(auctionStatus)) {
                logger.info("⏳ [AuctionRoom] Phiên chưa bắt đầu - REGISTRATION");
                showAlert("Phiên chưa bắt đầu", "Hiện vẫn đang trong giai đoạn đăng ký tham gia đấu giá.");
                return;
            }

            if ("FINISHED".equalsIgnoreCase(auctionStatus)) {
                logger.info("🏁 [AuctionRoom] Phiên đã kết thúc");
                showAlert("Phiên đã kết thúc", "Phiên đấu giá này đã kết thúc.");
                return;
            }
            
            if ("WAITING".equalsIgnoreCase(auctionStatus)) {
                logger.info("⏳ [AuctionRoom] Phiên đang chờ - WAITING");
                showAlert("Đang chờ", "Phiên này đang chờ khai mạc.");
                return;
            }

            logger.info("✅ [AuctionRoom] Auction status valid - tiếp tục");

            // ====================== Clean Old Session ======================
            try {
                if (wsClient != null && currentItemId != -1) {
                    wsClient.unsubscribe(currentItemId);
                    wsClient.removeListener(this);
                    logger.info("🧹 [AuctionRoom] Clean old websocket session");
                }
                stopAllTimelines();
                logger.info("🧹 [AuctionRoom] Old timelines stopped");
            } catch (Exception eClean) {
                logger.log(Level.WARNING, "Error cleaning old session", eClean);
            }

            // ====================== Set New Data ======================
            this.currentPlateNumber = plate;
            this.currentPrice = startingPrice;
            
            try {
                this.currentItemId = ItemDAO.getItemIdByPlate(plate);
                logger.info("✅ [AuctionRoom] Current item ID: " + currentItemId);
            } catch (Exception eItemId) {
                logger.log(Level.SEVERE, "Error getting item ID", eItemId);
                currentItemId = -1;
            }

            if (currentItemId == -1) {
                logger.log(Level.WARNING, "Không tìm được item ID cho plate: " + plate);
                showAlert("Lỗi", "Không thể xác định item ID");
                return;
            }

            // ====================== Update Watchlist Status ======================
            try {
                updateWatchlistButtonState();
                logger.info("✅ [AuctionRoom] Watchlist button updated");
            } catch (Exception eWatchlist) {
                logger.log(Level.WARNING, "Error updating watchlist button", eWatchlist);
            }

            // ====================== Get Auction Time ======================
            try {
                long remaining = ItemDAO.getAuctionTimeRemaining(plate);
                if (remaining > 0) {
                    this.timeRemainingSeconds = (int) remaining;
                    logger.info("✅ [AuctionRoom] Time remaining from DB: " + remaining + " seconds");
                } else {
                    logger.log(Level.WARNING, "Lấy time remaining trả về: " + remaining);
                    this.timeRemainingSeconds = 60 * 60; // Default 60 minutes
                    logger.info("⚠️  [AuctionRoom] Using default time: 60 minutes");
                }
            } catch (Exception eTime) {
                logger.log(Level.SEVERE, "Error getting auction time", eTime);
                this.timeRemainingSeconds = 60 * 60;
                logger.info("⚠️  [AuctionRoom] Using default time due to error");
            }

            // ====================== Update UI ======================
            try {
                updatePricingUI();
                logger.info("✅ [AuctionRoom] Pricing UI updated");
            } catch (Exception ePricing) {
                logger.log(Level.SEVERE, "Error updating pricing UI", ePricing);
            }

            try {
                updateParticipantCount();
                logger.info("✅ [AuctionRoom] Participant count updated");
            } catch (Exception eParticipants) {
                logger.log(Level.SEVERE, "Error updating participant count", eParticipants);
            }

            // ====================== Load Price Chart ======================
            try {
                loadPriceHistoryChart();
                logger.info("✅ [AuctionRoom] Price history chart loaded");
            } catch (Exception eChart) {
                logger.log(Level.WARNING, "Error loading price chart", eChart);
            }

            // ====================== Start Countdown ======================
            try {
                startCountdown();
                logger.info("✅ [AuctionRoom] Countdown started");
            } catch (Exception eCountdown) {
                logger.log(Level.SEVERE, "Error starting countdown", eCountdown);
            }

            // ====================== WebSocket Setup ======================
            try {
                if (wsClient == null) {
                    wsClient = AuctionWebSocketClient.getInstance();
                    logger.info("✅ [AuctionRoom] WebSocket client instance retrieved");
                }
                
                if (wsClient != null) {
                    wsClient.addListener(this);
                    wsClient.subscribe(currentItemId);
                    logger.info("✅ [AuctionRoom] Subscribed to WebSocket item: " + currentItemId);
                } else {
                    logger.log(Level.WARNING, "WebSocket client là null sau getInstance()");
                }
            } catch (Exception eWs) {
                logger.log(Level.WARNING, "Error setting up WebSocket", eWs);
                showAlert("Cảnh báo", "Không thể kết nối WebSocket - bạn vẫn có thể sử dụng tính năng cơ bản");
            }
            
            logger.info("✅ [AuctionRoom] setAuctionRoomData() completed successfully");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi không mong đợi trong setAuctionRoomData", e);
            showAlert("Lỗi hệ thống", "Có lỗi xảy ra: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        logger.info("📍 [AuctionRoom] handleBack() called");
        
        try {
            // ✅ Dừng tất cả đồng hồ và luồng chạy ngầm khi thoát
            stopAllTimelines();
            logger.info("✅ [AuctionRoom] Timelines stopped");

            // ✅ Unsubscribe từ WebSocket khi rời khỏi phòng đấu giá
            if (wsClient != null && currentItemId != -1) {
                try {
                    wsClient.unsubscribe(currentItemId);
                    wsClient.removeListener(this);
                    logger.info("🔌 [AuctionRoom] Unsubscribed từ WebSocket item: " + currentItemId);
                } catch (Exception eWs) {
                    logger.log(Level.WARNING, "Error unsubscribing WebSocket", eWs);
                }
            }

            try {
                Parent newRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/Dashboard.fxml")));
                Scene currentScene = ((Node) event.getSource()).getScene();
                if (currentScene != null) {
                    currentScene.setRoot(newRoot);
                    logger.info("✅ [AuctionRoom] Navigated back to Dashboard");
                } else {
                    logger.log(Level.SEVERE, "Current scene là null");
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Lỗi khi tải trang Dashboard", e);
                showAlert("Lỗi", "Không thể quay lại trang chủ: " + e.getMessage());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi không mong đợi trong handleBack", e);
            showAlert("Lỗi hệ thống", "Có lỗi xảy ra: " + e.getMessage());
        }
    }
    private void showAlert(String title, String message) {
        logger.info("📍 [AuctionRoom] showAlert() - Title: " + title);
        
        try {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
            logger.info("✅ [AuctionRoom] Alert shown");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error showing alert", e);
            System.err.println("❌ Error showing alert: " + title + " - " + message);
            System.err.println(e.getMessage());
        }
    }
    private void stopAllTimelines() {
        logger.info("📍 [AuctionRoom] stopAllTimelines() called");
        
        try {
            if (countdownTimeline != null) {
                countdownTimeline.stop();
                logger.info("🛑 [AuctionRoom] Countdown stopped");
            } else {
                logger.info("ℹ️  [AuctionRoom] countdownTimeline already null");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error stopping countdownTimeline", e);
        }
    }

    // =====================================================================
    // PRICE HISTORY CHART - Biểu đồ lịch sử giá real-time
    // =====================================================================

    /**
     * Load lịch sử giá từ database và vẽ biểu đồ
     * Called when entering auction room or when new bid is placed via WebSocket
     */
    private void loadPriceHistoryChart() {
        logger.info("📍 [AuctionRoom] loadPriceHistoryChart() called");
        
        try {
            if (currentItemId == -1) {
                logger.log(Level.WARNING, "Current item ID là -1");
                return;
            }
            
            if (priceHistoryChart == null) {
                logger.log(Level.WARNING, "priceHistoryChart là null");
                return;
            }

            // Lấy dữ liệu từ database
            List<String[]> bidHistory;
            int bidCount;
            
            try {
                bidHistory = BidDAO.getBidHistoryForChart(currentItemId);
                bidCount = BidDAO.getBidCount(currentItemId);
                logger.info("✅ [AuctionRoom] Fetched " + bidCount + " bids from database");
            } catch (Exception eDb) {
                logger.log(Level.SEVERE, "Error fetching bid history", eDb);
                return;
            }

            // Cập nhật label số lần đặt giá
            if (lblBidCount != null) {
                try {
                    lblBidCount.setText(bidCount + " lần đặt");
                } catch (Exception eLbl) {
                    logger.log(Level.WARNING, "Error updating bid count label", eLbl);
                }
            }

            // Clear chart cũ
            try {
                priceHistoryChart.getData().clear();
            } catch (Exception eClear) {
                logger.log(Level.WARNING, "Error clearing chart", eClear);
            }

            if (bidHistory.isEmpty()) {
                logger.info("ℹ️  [AuctionRoom] Chưa có bids nào cho item này");
                return;
            }

            try {
                // Tạo series dữ liệu
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.setName("Mức giá");

                // Thêm dữ liệu vào series
                for (int i = 0; i < bidHistory.size(); i++) {
                    try {
                        String[] bidData = bidHistory.get(i);
                        if (bidData == null || bidData.length < 4) {
                            logger.log(Level.WARNING, "Bid data bị lỗi tại index " + i);
                            continue;
                        }
                        
                        double bidAmount = Double.parseDouble(bidData[1]);
                        String bidderName = bidData[3];

                        // X axis: Thứ tự đặt giá (1, 2, 3...)
                        // Y axis: Mức giá
                        XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(i + 1, bidAmount);

                        // Thêm tooltip
                        dataPoint.setNode(new javafx.scene.shape.Circle(3) {
                            {
                                setStyle("-fx-fill: #38bdf8; -fx-cursor: hand;");
                            }
                        });

                        javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(
                                bidderName + " - " + String.format("%.0f", bidAmount) + " đ"
                        );
                        javafx.scene.control.Tooltip.install(dataPoint.getNode(), tooltip);

                        series.getData().add(dataPoint);
                    } catch (Exception eBidPoint) {
                        logger.log(Level.WARNING, "Error processing bid point " + i, eBidPoint);
                    }
                }

                // Thêm series vào chart
                priceHistoryChart.getData().add(series);

                // Styling
                if (series.getNode() != null) {
                    series.getNode().setStyle("-fx-stroke: #38bdf8; -fx-stroke-width: 2;");
                }

                logger.info("✅ [AuctionRoom] Đã tải biểu đồ giá với " + bidCount + " bids");
            } catch (Exception eChart) {
                logger.log(Level.SEVERE, "Error building chart", eChart);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi trong loadPriceHistoryChart", e);
        }
    }

    /**
     * Update chart khi có bid mới từ WebSocket
     * Called từ onPriceUpdate listener
     */
    private void updatePriceHistoryChart() {
        javafx.application.Platform.runLater(this::loadPriceHistoryChart);
    }

    // =====================================================================
    // WEBSOCKET LISTENER IMPLEMENTATIONS
    // =====================================================================

    @Override
    public void onPriceUpdate(int itemId, double currentPrice, int bidderId, String bidderName) {
        logger.info("📍 [WebSocket] onPriceUpdate - ItemID: " + itemId + ", Price: " + currentPrice + ", Bidder: " + bidderName);
        
        try {
            if (itemId != currentItemId) {
                logger.info("ℹ️  [WebSocket] Ignoring price update for different item");
                return;
            }

            javafx.application.Platform.runLater(() -> {
                try {
                    this.currentPrice = currentPrice;
                    updatePricingUI();
                    addHistoryRow(formatter.format(currentPrice), bidderName);
                    updatePriceHistoryChart();
                    logger.info("✅ [WebSocket] Price updated from WebSocket: " + currentPrice);
                } catch (Exception eUpdate) {
                    logger.log(Level.WARNING, "Error updating UI from WebSocket", eUpdate);
                }
            });
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in onPriceUpdate", e);
        }
    }

    @Override
    public void onParticipantUpdate(int itemId, int participantCount) {
        logger.info("📍 [WebSocket] onParticipantUpdate - ItemID: " + itemId + ", Count: " + participantCount);
        
        try {
            if (itemId != currentItemId) {
                logger.info("ℹ️  [WebSocket] Ignoring participant update for different item");
                return;
            }

            javafx.application.Platform.runLater(() -> {
                try {
                    currentParticipants = participantCount;
                    if (lblParticipants != null) {
                        lblParticipants.setText("👥 " + participantCount + " người tham gia");
                        logger.info("✅ [WebSocket] Participant count updated: " + participantCount);
                    }
                } catch (Exception eUpdate) {
                    logger.log(Level.WARNING, "Error updating participant count", eUpdate);
                }
            });
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in onParticipantUpdate", e);
        }
    }

    @Override
    public void onAuctionFinished(int itemId, double finalPrice, int winnerId, String winnerName) {
        logger.info("📍 [WebSocket] onAuctionFinished - ItemID: " + itemId + ", Winner: " + winnerName + ", Price: " + finalPrice);
        
        try {
            if (itemId != currentItemId) {
                logger.info("ℹ️  [WebSocket] Ignoring finish event for different item");
                return;
            }

            javafx.application.Platform.runLater(() -> {
                try {
                    if (btnPlaceBid != null) {
                        btnPlaceBid.setDisable(true);
                        btnPlaceBid.setText("PHIÊN ĐẤU GIÁ ĐÃ KẾT THÚC");
                        btnPlaceBid.setStyle("-fx-background-color: #334155; -fx-text-fill: #94a3b8; -fx-background-radius: 12; -fx-font-size: 16px; -fx-font-weight: bold;");
                    }

                    // Dừng countdown
                    if (countdownTimeline != null) {
                        countdownTimeline.stop();
                    }

                    String message = "Phiên đấu giá kết thúc!\n\n" +
                            "Giá cuối cùng: " + formatter.format(finalPrice) + " đ\n" +
                            "Người thắng: " + winnerName;
                    showAlert("Phiên đấu giá kết thúc", message);
                    logger.info("✅ [WebSocket] Auction finished - Winner: " + winnerName);
                } catch (Exception eFinish) {
                    logger.log(Level.WARNING, "Error handling auction finish", eFinish);
                }
            });
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in onAuctionFinished", e);
        }
    }

     @Override
    public void onTimerExtended(int itemId, int newTimeRemaining) {
        logger.info("📍 [WebSocket] onTimerExtended - ItemID: " + itemId + ", Time: " + newTimeRemaining);
        
        try {
            if (itemId != currentItemId) {
                logger.info("ℹ️  [WebSocket] Ignoring timer event for different item");
                return;
            }

            javafx.application.Platform.runLater(() -> {
                try {
                    this.timeRemainingSeconds = newTimeRemaining;

                    // Display user-friendly message
                    long minutes = newTimeRemaining / 60;
                    long seconds = newTimeRemaining % 60;
                    String newTimeStr = String.format("%02d:%02d", minutes, seconds);

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("⏱️ Timer Kéo Dài!");
                    alert.setHeaderText(null);
                    alert.setContentText("🎯 Có bid trong 2 phút cuối!\n\n✨ Timer được tự động kéo dài thêm 2 phút.\n\n" +
                            "Thời gian còn lại: " + newTimeStr);
                    alert.showAndWait();
                    logger.info("✅ [WebSocket] Timer extended - New time: " + newTimeRemaining + "s");
                } catch (Exception eTimer) {
                    logger.log(Level.WARNING, "Error updating timer", eTimer);
                }
            });
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in onTimerExtended", e);
        }
    }

    @Override
    public void onConnected() {
        logger.info("✅ [WebSocket] UI connected");
    }

    @Override
    public void onDisconnected() {
        logger.info("❌ [WebSocket] UI disconnected");
    }
    private void refreshCurrentPrice() {
        logger.info("📍 [AuctionRoom] refreshCurrentPrice() called");
        
        try {
            if (currentPlateNumber == null || currentPlateNumber.isEmpty()) {
                logger.log(Level.WARNING, "Current plate number là null");
                return;
            }

            double latestPrice = 0;
            try {
                latestPrice = ItemDAO.getCurrentPrice(currentPlateNumber);
                logger.info("✅ [AuctionRoom] Refreshed price: " + latestPrice);
            } catch (Exception eDb) {
                logger.log(Level.SEVERE, "Error getting current price", eDb);
                return;
            }

            currentPrice = latestPrice;

            try {
                updatePricingUI();
            } catch (Exception eUi) {
                logger.log(Level.WARNING, "Error updating pricing UI", eUi);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in refreshCurrentPrice", e);
        }
    }
    private void addHistoryRow(String price, String bidderName) {
        logger.info("📍 [AuctionRoom] addHistoryRow() - Bidder: " + bidderName + ", Price: " + price);
        
        try {
            if (liveHistoryContainer == null) {
                logger.log(Level.WARNING, "liveHistoryContainer là null");
                return;
            }

            if (price == null || price.isEmpty() || bidderName == null || bidderName.isEmpty()) {
                logger.log(Level.WARNING, "Price hoặc bidderName là null/rỗng");
                return;
            }

            HBox row = new HBox();
            row.setSpacing(10);

            Label lblBidder = new Label(bidderName);
            lblBidder.setStyle(
                    "-fx-text-fill: white; " +
                            "-fx-font-size: 14px;"
            );

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label lblPrice = new Label(price + " đ");
            lblPrice.setStyle(
                    "-fx-text-fill: #38bdf8; " +
                            "-fx-font-size: 14px; " +
                            "-fx-font-weight: bold;"
            );

            row.getChildren().addAll(
                    lblBidder,
                    spacer,
                    lblPrice
            );

            liveHistoryContainer.getChildren().add(0, row);

            // Giữ tối đa 20 dòng history
            if (liveHistoryContainer.getChildren().size() > 20) {
                try {
                    liveHistoryContainer.getChildren().remove(
                            liveHistoryContainer.getChildren().size() - 1
                    );
                } catch (Exception eRemove) {
                    logger.log(Level.WARNING, "Error removing old history row", eRemove);
                }
            }

            logger.info("✅ [AuctionRoom] History row added");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in addHistoryRow", e);
        }
    }
}