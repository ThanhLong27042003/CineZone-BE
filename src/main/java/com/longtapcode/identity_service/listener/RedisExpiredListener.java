package com.longtapcode.identity_service.listener;

import com.longtapcode.identity_service.constant.SeatInstanceStatus;
import com.longtapcode.identity_service.dto.response.SeatUpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RedisExpiredListener implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();

        System.out.println("Seat auto released by TTL: " + expiredKey);

        if (!expiredKey.startsWith("hold:")) return;

        String[] parts = expiredKey.split(":");
        Long showId = Long.parseLong(parts[1]);
        String seatNumber = parts[2];

        Set<String> paymentKeys = redisTemplate.keys("paypal_metadata:*");
        if (paymentKeys != null) {
            for (String key : paymentKeys) {
                Map<Object, Object> meta = redisTemplate.opsForHash().entries(key);

                String seats = (String) meta.get("seats");
                String paypalOrderId = (String) meta.get("paypalOrderId");

                if (seats != null && Arrays.asList(seats.split(",")).contains(seatNumber)) {

                    // 🔥 ĐÁNH DẤU PAYMENT SESSION BỊ HỦY
                    String paymentSessionKey = "payment_session:" + paypalOrderId;
                    redisTemplate.opsForValue().set(paymentSessionKey, "EXPIRED");
                }
            }
        }
        SeatUpdateResponse msg = SeatUpdateResponse.builder()
                .showId(showId)
                .seatNumber(seatNumber)
                .status(SeatInstanceStatus.AVAILABLE.getStatus())
                .expiresAt(0L)
                .build();

        messagingTemplate.convertAndSend(
                "/topic/show/" + showId,
                msg
        );
    }
}

