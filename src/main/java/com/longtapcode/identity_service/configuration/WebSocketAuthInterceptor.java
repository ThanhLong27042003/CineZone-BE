package com.longtapcode.identity_service.configuration;

import java.util.List;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Interceptor chặn HTTP handshake trước khi nâng cấp thành WebSocket.
 *
 * Luồng hoạt động:
 *   Client → GET /ws?token=<jwt> → [Interceptor này] → Nếu hợp lệ → Kết nối WS mở
 *
 * Lưu ý: WebSocket handshake là một HTTP request, nhưng nhiều browser/client
 * KHÔNG cho phép set custom header (Authorization) khi dùng SockJS + browser.
 * Vì vậy, cách phổ biến nhất là truyền token qua query parameter: /ws?token=xxx
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final CustomJwtDecoder customJwtDecoder;

    /**
     * Chạy TRƯỚC khi handshake diễn ra.
     * Return true  → Cho phép nâng cấp thành WebSocket
     * Return false → Từ chối, client nhận lỗi 403
     */
    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        // Lấy token từ query param: /ws?token=eyJhbG...
        List<String> tokenParams = request.getURI().getQuery() != null
                ? List.of(request.getURI().getQuery().split("&"))
                : List.of();

        String token = tokenParams.stream()
                .filter(param -> param.startsWith("token="))
                .map(param -> param.substring("token=".length()))
                .findFirst()
                .orElse(null);

        // Không có token → Từ chối (cho endpoint public không cần auth thì bỏ phần này)
        if (token == null || token.isBlank()) {
            log.warn("WebSocket handshake rejected: No token provided from {}", request.getRemoteAddress());
            // Return true để vẫn cho phép kết nối (vì ghế xem có thể không cần login)
            // Nếu muốn bắt buộc auth, đổi thành return false;
            attributes.put("userId", "anonymous");
            return true;
        }

        // Verify JWT bằng CustomJwtDecoder đã có sẵn
        try {
            var jwt = customJwtDecoder.decode(token);
            String userId = jwt.getSubject(); // sub claim trong JWT = userId

            // Lưu userId vào WebSocket session attributes để dùng sau
            attributes.put("userId", userId);
            attributes.put("authenticated", true);

            log.info("WebSocket handshake accepted for userId: {} from {}", userId, request.getRemoteAddress());
            return true;

        } catch (Exception e) {
            log.warn("WebSocket handshake rejected: Invalid JWT token. Error: {}", e.getMessage());
            return false; // Từ chối kết nối
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // Không cần xử lý gì sau handshake
    }
}
