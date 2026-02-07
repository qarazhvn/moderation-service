package com.example.moderation.kafka;

import com.example.moderation.model.CustomerRequestEvent;
import com.example.moderation.service.ModerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerRequestEventConsumer {
    
    private final ModerationService moderationService;
    
    @KafkaListener(
            topics = "${kafka.topics.input}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload CustomerRequestEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        log.info("Received: eventId={}, customerId={}, partition={}, offset={}", 
                event.getEventId(), event.getCustomerId(), partition, offset);
        
        try {
            ModerationService.ProcessingResult result = moderationService.processEvent(event);
            log.info("Processed: eventId={}, status={}", result.getEventId(), result.getStatus());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing eventId={}: {}", event.getEventId(), e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }
}
