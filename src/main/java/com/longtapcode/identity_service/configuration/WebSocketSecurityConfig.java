package com.longtapcode.identity_service.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

/**
 * Cấu hình bảo mật riêng cho WebSocket Message (Spring Security 6 chuẩn).
 *
 * @EnableWebSocketSecurity tự động:
 *   1. Thêm CSRF protection cho STOMP (Cross-Site WebSocket Hijacking)
 *   2. Đăng ký AuthorizationManager để kiểm tra quyền từng message
 *
 * Đây là cách CHUẨN của Spring Security 6, thay thế cho
 * AbstractSecurityWebSocketMessageBrokerConfigurer (đã deprecated).
 */
@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * Định nghĩa quyền cho từng loại STOMP message.
     *
     * Tương tự như authorizeHttpRequests() nhưng cho WebSocket:
     *   - simpSubscribeDestMatchers → Kiểm tra quyền khi SUBSCRIBE
     *   - simpDestMatchers          → Kiểm tra quyền khi SEND (Client → Server)
     *   - simpTypeMatchers          → Kiểm tra theo loại message (CONNECT, DISCONNECT...)
     */
    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder messages) {

        messages
                // ─── CONNECT / DISCONNECT: Cho phép tất cả ─────────────────────────
                // (Việc verify JWT đã được xử lý trong WebSocketAuthInterceptor rồi)
                .simpTypeMatchers(SimpMessageType.CONNECT, SimpMessageType.DISCONNECT)
                .permitAll()

                // ─── SUBSCRIBE: Ai cũng có thể xem sơ đồ ghế (không cần đăng nhập) ─
                .simpSubscribeDestMatchers("/topic/show/**")
                .permitAll()

                // ─── SEND: Client gửi lên /app/... → Phải đăng nhập ─────────────────
                // Ví dụ: Client gửi yêu cầu hold ghế qua STOMP SEND
                .simpDestMatchers("/app/**")
                .authenticated()

                // ─── Mọi message khác: Từ chối ────────────────────────────────────────
                .anyMessage()
                .denyAll();

        return messages.build();
    }
}
