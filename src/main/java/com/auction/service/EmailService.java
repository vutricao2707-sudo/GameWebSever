package com.auction.service;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ✅ THÊM MỚI: Dịch vụ gửi email thông báo cho người dùng
 * Sử dụng Gmail SMTP (yêu cầu app password)
 * Cách setup: Bật 2FA trên Gmail, tạo App Password, sử dụng nó ở đây
 */
public class EmailService {
    private static final Logger logger = Logger.getLogger(EmailService.class.getName());
    private static final String SENDER_EMAIL = "your-email@gmail.com"; // Đổi thành email của bạn
    private static final String APP_PASSWORD = "your-app-password"; // Đổi thành app password

    /**
     * Gửi email thông báo khi biển số được duyệt
     */
    public static void sendPlateApprovedNotification(String recipientEmail, String plateNumber, double startingPrice) {
        String subject = "✅ Biển số được duyệt - " + plateNumber;
        String body = String.format("""
                Xin chào,
                
                Biển số %s của bạn đã được duyệt thành công!
                
                Thông tin:
                - Biển số: %s
                - Giá khởi điểm: %,.0f đ
                - Trạng thái: Sẵn sàng bán đấu giá
                
                Hãy đăng nhập vào hệ thống để xem chi tiết.
                
                Cảm ơn bạn!
                """, plateNumber, plateNumber, startingPrice);
        
        sendEmail(recipientEmail, subject, body);
    }

    /**
     * Gửi email thông báo khi bị outbid
     */
    public static void sendOutbidNotification(String recipientEmail, String plateNumber, double newBidAmount) {
        String subject = "⚠️ Bạn đã bị outbid - " + plateNumber;
        String body = String.format("""
                Xin chào,
                
                Biển số %s bạn đang theo dõi vừa được ai đó đặt giá cao hơn!
                
                Mức giá mới: %,.0f đ
                
                Bạn vẫn có cơ hội đặt giá lại. Hãy quay lại sàn để tiếp tục!
                
                Cảm ơn bạn!
                """, plateNumber, newBidAmount);
        
        sendEmail(recipientEmail, subject, body);
    }

    /**
     * Gửi email thông báo khi thắng cuộc
     */
    public static void sendWinnerNotification(String recipientEmail, String plateNumber, double finalBidAmount) {
        String subject = "🎉 Bạn đã thắng cuộc - " + plateNumber;
        String body = String.format("""
                Xin chào,
                
                Chúc mừng! Bạn đã thắng cuộc cho biển số %s!
                
                Chi tiết:
                - Biển số: %s
                - Giá cuối cùng: %,.0f đ
                - Trạng thái: Chờ thanh toán
                
                Vui lòng hoàn thành quá trình thanh toán trong vòng 24 giờ.
                
                Cảm ơn bạn!
                """, plateNumber, plateNumber, finalBidAmount);
        
        sendEmail(recipientEmail, subject, body);
    }

    /**
     * Phương thức chung để gửi email
     * (Hiện tại để demo - cần thêm JavaMail library nếu muốn chạy thực)
     */
    private static void sendEmail(String recipientEmail, String subject, String body) {
        try {
            // Kiểm tra cấu hình
            if (SENDER_EMAIL.equals("your-email@gmail.com")) {
                logger.warning("⚠️ EmailService chưa được cấu hình! Vui lòng thêm email và app password.");
                logger.info("📧 [DEMO EMAIL] To: " + recipientEmail + " | Subject: " + subject);
                return;
            }

            // TODO: Sau khi thêm dependency javax.mail, uncomment đoạn này
            /*
            Properties props = new Properties();
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");

            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);
            message.setText(body, "utf-8");

            Transport.send(message);
            logger.info("✅ Email gửi thành công đến: " + recipientEmail);
            */

            // Hiện tại để demo
            logger.info("📧 [DEMO EMAIL SENT] To: " + recipientEmail);
            logger.info("Subject: " + subject);
            logger.info("Body preview: " + body.substring(0, Math.min(100, body.length())) + "...");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Lỗi gửi email", e);
        }
    }
}

