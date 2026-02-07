package com.example.moderation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationResultEvent {
    private String originalEventId;
    private String requestId;
    private String customerId;
    private String category;
    private String subject;
    private CustomerRequestEvent.Priority priority;
    private ModerationStatus status;
    private EnrichmentData enrichmentData;
    private LocalDateTime processedAt;
    
    public enum ModerationStatus { APPROVED, ENRICHED, READY_FOR_PROCESSING }
}
