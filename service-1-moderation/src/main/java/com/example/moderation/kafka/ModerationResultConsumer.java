package com.example.moderation.kafka;

import com.example.moderation.model.ModerationResultEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ModerationResultConsumer {
    
    @KafkaListener(
            topics = "${kafka.topics.output}",
            groupId = "moderation-result-logger",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeResult(
            @Payload ModerationResultEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            org.springframework.kafka.support.Acknowledgment acknowledgment) {
        
        log.info("Result from Topic-2: eventId={}, customerId={}, status={}, partition={}, offset={}",
                event.getOriginalEventId(), event.getCustomerId(), event.getStatus(), partition, offset);
        acknowledgment.acknowledge();
    }
}
