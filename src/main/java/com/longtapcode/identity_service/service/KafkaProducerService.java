package com.longtapcode.identity_service.service;

import com.longtapcode.identity_service.dto.event.BookingConfirmedEvent;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KafkaProducerService {

    final KafkaTemplate<String, BookingConfirmedEvent> kafkaTemplate;

    @Value("${spring.kafka.topics.booking-confirmed}")
    String bookingConfirmedTopic;

    public void publishBookingConfirmed(BookingConfirmedEvent event){
        log.info("Publishing BookingConfirmedEvent to Kafka - BookingId: {}, OrderId: {}",event.getBookingId(), event.getOrderId());

        try{
            CompletableFuture<SendResult<String, BookingConfirmedEvent>> future =
                    kafkaTemplate.send(bookingConfirmedTopic, event.getOrderId(), event);

            future.whenComplete((result, ex)->{
                if(ex == null){
                    log.info("✅ Event published successfully - Topic: {}, Partition: {}, Offset: {}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }else{
                    log.error("❌ Failed to publish event - BookingId: {}, Error: {}",
                            event.getBookingId(), ex.getMessage(), ex);
                }
            });
        }catch(Exception e){
            log.error("❌ Exception while publishing event - BookingId: {}", event.getBookingId(), e);
        }
    }
}
