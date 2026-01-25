package com.longtapcode.identity_service.configuration;

import com.longtapcode.identity_service.listener.RedisExpiredListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisConfig {
    @Bean
    public StringRedisTemplate redisTemplate(RedisConnectionFactory factory){
        return new StringRedisTemplate(factory);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisExpiredListener expiredListener
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        container.addMessageListener(
                expiredListener,
                new PatternTopic("__keyevent@*__:expired")
        );

        return container;
    }

}
