package com.longtapcode.identity_service.configuration;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Channel Interceptor: Chặn ở tầng STOMP message.
 *
 * Đây là lớp bảo vệ thứ 2, chạy sau WebSocketAuthInterceptor.
 * Nó có thể chặn từng loại STOMP command:
 *   - CONNECT    : Lúc client gửi lệnh kết nối STOMP
 *   - SUBSCRIBE  : Lúc client đăng ký theo dõi một topic
 *   - SEND       : Lúc client gửi message lên /app/...
 *   - DISCONNECT : Lúc client ngắt kết nối
 */
@Component
@Slf4j
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    /**
     * Chạy TRƯỚC KHI message được gửi vào channel.
     * Return message → Tiếp tục xử lý
     * Return null    → Message bị chặn, không đi tiếp
     * Throw exception → Báo lỗi về client
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message; // Không phải STOMP message, bỏ qua
        }

        StompCommand command = accessor.getCommand();

        // Chỉ log để theo dõi các STOMP command
        // Bạn có thể thêm logic kiểm tra quyền tại đây
        if (StompCommand.CONNECT.equals(command)) {
            // Lấy userId đã được lưu vào session attributes từ WebSocketAuthInterceptor
            Object userId = accessor.getSessionAttributes() != null
                    ? accessor.getSessionAttributes().get("userId")
                    : null;
            log.info("STOMP CONNECT from userId: {}", userId);
        }

        if (StompCommand.SUBSCRIBE.equals(command)) {
            String destination = accessor.getDestination();
            Object userId = accessor.getSessionAttributes() != null
                    ? accessor.getSessionAttributes().get("userId")
                    : null;
            log.debug("STOMP SUBSCRIBE: userId={} → {}", userId, destination);

            // Ví dụ: Nếu muốn chỉ user đã đăng nhập mới subscribe được
            // thì uncomment đoạn dưới:
            //
            // Boolean authenticated = (Boolean) accessor.getSessionAttributes().get("authenticated");
            // if (!Boolean.TRUE.equals(authenticated)) {
            //     throw new MessagingException("Unauthorized: Please login to subscribe to seat updates");
            // }
        }

        if (StompCommand.DISCONNECT.equals(command)) {
            Object userId = accessor.getSessionAttributes() != null
                    ? accessor.getSessionAttributes().get("userId")
                    : null;
            log.info("STOMP DISCONNECT from userId: {}", userId);
        }

        return message; // Cho phép message đi tiếp
    }
}
