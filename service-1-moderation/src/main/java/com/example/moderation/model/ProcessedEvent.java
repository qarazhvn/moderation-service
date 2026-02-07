package com.example.moderation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "processed_events")
public class ProcessedEvent {
    @Id
    private String eventId;
    
    @Indexed
    private String customerId;
    
    @Indexed
    private String category;
    
    private ProcessingResult result;
    private String rejectionReason;
    
    @Indexed
    private LocalDateTime processedAt;
    
    @Indexed(expireAfterSeconds = 2592000) // 30 days TTL
    private LocalDateTime expireAt;
    
    public enum ProcessingResult {
        PUBLISHED, REJECTED_DUPLICATE, REJECTED_ACTIVE_REQUEST, REJECTED_OUTSIDE_HOURS, REJECTED_NO_DATA
    }
}
