package com.example.moderation.service;

import com.example.moderation.model.*;
import com.example.moderation.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModerationService {
    
    private final EnrichmentClientService enrichmentClient;
    private final ModerationRulesEngine rulesEngine;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${kafka.topics.output}")
    private String outputTopic;
    
    public ProcessingResult processEvent(CustomerRequestEvent event) {
        log.info("Processing event: {}, customer: {}", event.getEventId(), event.getCustomerId());
        
        try {
            EnrichmentData enrichmentData = enrichmentClient.getEnrichmentData(event);
            ModerationRulesEngine.ModerationEngineResult rulesResult = rulesEngine.evaluate(event, enrichmentData);
            saveProcessedEvent(event, rulesResult);
            
            if (rulesResult.isAllRulesPassed()) {
                publishToOutputTopic(event, enrichmentData);
                return ProcessingResult.builder()
                        .eventId(event.getEventId())
                        .status(ProcessingStatus.PUBLISHED)
                        .message("Event approved and published to Topic-2")
                        .processedAt(LocalDateTime.now())
                        .build();
            } else {
                return ProcessingResult.builder()
                        .eventId(event.getEventId())
                        .status(ProcessingStatus.REJECTED)
                        .message(rulesResult.getRejectionReason())
                        .rejectionDetails(rulesResult.getFailedRule() != null ? 
                                rulesResult.getFailedRule().getDetails() : null)
                        .processedAt(LocalDateTime.now())
                        .build();
            }
            
        } catch (Exception e) {
            log.error("Error processing event {}: {}", event.getEventId(), e.getMessage());
            saveErrorEvent(event, e.getMessage());
            return ProcessingResult.builder()
                    .eventId(event.getEventId())
                    .status(ProcessingStatus.ERROR)
                    .message("Processing error: " + e.getMessage())
                    .processedAt(LocalDateTime.now())
                    .build();
        }
    }
    
    @Async
    public CompletableFuture<ProcessingResult> processEventAsync(CustomerRequestEvent event) {
        return CompletableFuture.completedFuture(processEvent(event));
    }
    
    private ProcessedEvent saveProcessedEvent(CustomerRequestEvent event, 
                                               ModerationRulesEngine.ModerationEngineResult rulesResult) {
        ProcessedEvent.ProcessingResult result = rulesResult.isAllRulesPassed() 
                ? ProcessedEvent.ProcessingResult.PUBLISHED 
                : mapRejectionReason(rulesResult.getFailedRule());
        
        ProcessedEvent processedEvent = ProcessedEvent.builder()
                .eventId(event.getEventId())
                .customerId(event.getCustomerId())
                .category(event.getCategory())
                .result(result)
                .rejectionReason(rulesResult.isAllRulesPassed() ? null : rulesResult.getRejectionReason())
                .processedAt(LocalDateTime.now())
                .expireAt(LocalDateTime.now().plusDays(30))
                .build();
        
        return processedEventRepository.save(processedEvent);
    }
    
    private void saveErrorEvent(CustomerRequestEvent event, String errorMessage) {
        try {
            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(event.getEventId())
                    .customerId(event.getCustomerId())
                    .category(event.getCategory())
                    .result(ProcessedEvent.ProcessingResult.REJECTED_NO_DATA)
                    .rejectionReason("Processing error: " + errorMessage)
                    .processedAt(LocalDateTime.now())
                    .expireAt(LocalDateTime.now().plusDays(30))
                    .build());
        } catch (Exception e) {
            log.error("Failed to save error event: {}", e.getMessage());
        }
    }
    
    private ProcessedEvent.ProcessingResult mapRejectionReason(com.example.moderation.rules.RuleResult failedRule) {
        if (failedRule == null) return ProcessedEvent.ProcessingResult.REJECTED_NO_DATA;
        return switch (failedRule.getRuleName()) {
            case "DUPLICATE_EVENT_CHECK" -> ProcessedEvent.ProcessingResult.REJECTED_DUPLICATE;
            case "ACTIVE_REQUEST_CHECK" -> ProcessedEvent.ProcessingResult.REJECTED_ACTIVE_REQUEST;
            case "WORKING_HOURS_CHECK" -> ProcessedEvent.ProcessingResult.REJECTED_OUTSIDE_HOURS;
            default -> ProcessedEvent.ProcessingResult.REJECTED_NO_DATA;
        };
    }
    
    private void publishToOutputTopic(CustomerRequestEvent event, EnrichmentData enrichmentData) {
        ModerationResultEvent resultEvent = ModerationResultEvent.builder()
                .originalEventId(event.getEventId())
                .requestId(event.getRequestId())
                .customerId(event.getCustomerId())
                .category(event.getCategory())
                .subject(event.getSubject())
                .priority(event.getPriority())
                .status(ModerationResultEvent.ModerationStatus.APPROVED)
                .enrichmentData(enrichmentData)
                .processedAt(LocalDateTime.now())
                .build();
        
        kafkaTemplate.send(outputTopic, event.getCustomerId(), resultEvent)
                .whenComplete((result, ex) -> {
                    if (ex != null) log.error("Failed to publish to Topic-2: {}", ex.getMessage());
                });
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ProcessingResult {
        private String eventId;
        private ProcessingStatus status;
        private String message;
        private String rejectionDetails;
        private LocalDateTime processedAt;
    }
    
    public enum ProcessingStatus { PUBLISHED, REJECTED, ERROR }
}
