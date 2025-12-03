package com.longtapcode.identity_service.service;

import com.longtapcode.identity_service.dto.event.BookingConfirmedEvent;
import com.longtapcode.identity_service.dto.event.SeatInfoEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.from:Cinema Booking <noreply@cinema.com>}")
    private String fromEmail;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${app.email.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @KafkaListener(
            topics = "${spring.kafka.topics.booking-confirmed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleBookingConfirmed(BookingConfirmedEvent event, Acknowledgment acknowledgment) {
        log.info("📧 Received BookingConfirmedEvent - BookingId: {}, Email: {}",
                event.getBookingId(), event.getUserEmail());

        try {
            if (!emailEnabled) {
                log.warn("Email sending is disabled. Skipping email for booking: {}", event.getBookingId());
                acknowledgment.acknowledge();
                return;
            }

            // Send booking confirmation email
            sendBookingConfirmationEmail(event);

            // Acknowledge message
            acknowledgment.acknowledge();
            log.info("✅ Email sent successfully for booking: {}", event.getBookingId());

        } catch (Exception e) {
            log.error("❌ Failed to send email for booking: {}", event.getBookingId(), e);
            // Don't acknowledge - message will be retried
            throw new RuntimeException("Email sending failed", e);
        }
    }

    /**
     * Send booking confirmation email
     */
    public void sendBookingConfirmationEmail(BookingConfirmedEvent event) throws MessagingException {
        log.info("Preparing email for booking: {}", event.getBookingId());

        // Prepare template variables
        Map<String, Object> variables = buildEmailVariables(event);

        // Generate HTML content from template
        String htmlContent = templateEngine.process("email/booking-confirmation",
                createContext(variables));

        // Create and send email
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(event.getUserEmail());
        helper.setSubject("🎬 Booking Confirmation - " + event.getMovieTitle());
        helper.setText(htmlContent, true);

        mailSender.send(message);
        log.info("Email sent to: {}", event.getUserEmail());
    }

    /**
     * Build template variables
     */
    private Map<String, Object> buildEmailVariables(BookingConfirmedEvent event) {
        Map<String, Object> variables = new HashMap<>();

        // User info
        variables.put("userName", event.getUserName());
        variables.put("userEmail", event.getUserEmail());

        // Booking info
        variables.put("bookingId", event.getBookingId());
        variables.put("orderId", event.getOrderId());
        variables.put("bookingDate", formatDateTime(event.getBookingDate()));
        variables.put("totalPrice", formatPrice(event.getTotalPrice()));
        variables.put("paymentMethod", event.getPaymentMethod());
        variables.put("transactionId", event.getTransactionId());

        // Show info
        variables.put("movieTitle", event.getMovieTitle());
        variables.put("showDateTime", formatDateTime(event.getShowDateTime()));
        variables.put("showDate", formatDate(event.getShowDateTime()));
        variables.put("showTimeOnly", formatTime(event.getShowDateTime()));
        // Seats
        variables.put("seats", event.getSeats());
        variables.put("seatCount", event.getSeats().size());
        variables.put("seatNumbers", event.getSeats().stream()
                .map(SeatInfoEvent::getSeatNumber)
                .reduce((a, b) -> a + ", " + b)
                .orElse("N/A"));

        // Links
        variables.put("viewBookingUrl", frontendUrl + "/my-bookings");
        variables.put("homeUrl", frontendUrl);

        // QR Code (optional - for future)
        variables.put("qrCodeUrl", generateQrCodeUrl(event.getOrderId()));

        return variables;
    }

    /**
     * Create Thymeleaf context
     */
    private Context createContext(Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return context;
    }

    /**
     * Format helpers
     */
    private String formatPrice(Long cents) {
        if (cents == null) return "$0.00";
        BigDecimal dollars = BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return "$" + dollars.toString();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' hh:mm a");
        return dateTime.format(formatter);
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
        return dateTime.format(formatter);
    }

    private String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
        return dateTime.format(formatter);
    }

    private String generateQrCodeUrl(String orderId) {
        return "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + orderId;
    }
}