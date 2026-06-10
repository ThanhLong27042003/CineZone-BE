package com.longtapcode.identity_service.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // Lớp 1: Chặn ở HTTP handshake → Verify JWT
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    // Lớp 2: Chặn ở STOMP channel → Kiểm tra quyền từng command
    private final WebSocketChannelInterceptor webSocketChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Prefix cho các message từ server → client
        config.enableSimpleBroker("/topic");

        // Prefix cho các message từ client → server
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                // Đăng ký Lớp 1: Verify JWT ngay tại handshake
                .addInterceptors(webSocketAuthInterceptor)
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Đăng ký Lớp 2: Channel interceptor cho từng STOMP command
        registration.interceptors(webSocketChannelInterceptor);
    }
}
