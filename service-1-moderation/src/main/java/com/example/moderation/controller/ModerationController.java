package com.example.moderation.controller;

import com.example.moderation.model.CustomerRequestEvent;
import com.example.moderation.model.ProcessedEvent;
import com.example.moderation.repository.ProcessedEventRepository;
import com.example.moderation.service.ModerationRulesEngine;
import com.example.moderation.service.ModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/moderation")
@RequiredArgsConstructor
@Tag(name = "Moderation API", description = "API для модерации обращений клиентов")
public class ModerationController {
    
    private final ModerationService moderationService;
    private final ModerationRulesEngine rulesEngine;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${kafka.topics.input}")
    private String inputTopic;
    
    @PostMapping("/send-to-kafka")
    @Operation(summary = "Отправить событие в Kafka")
    public ResponseEntity<Map<String, Object>> sendToKafka(@Valid @RequestBody CustomerRequestEvent event) {
        if (event.getEventId() == null || event.getEventId().isEmpty()) event.setEventId(UUID.randomUUID().toString());
        if (event.getTimestamp() == null) event.setTimestamp(LocalDateTime.now());
        
        kafkaTemplate.send(inputTopic, event.getCustomerId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) log.error("Failed to send: {}", ex.getMessage());
                    else log.info("Sent to Kafka. EventId: {}", event.getEventId());
                });
        
        return ResponseEntity.accepted().body(Map.of(
                "status", "SENT_TO_KAFKA", "eventId", event.getEventId(), 
                "topic", inputTopic, "timestamp", LocalDateTime.now()));
    }
    
    @PostMapping("/process-direct")
    @Operation(summary = "Прямая обработка события")
    public ResponseEntity<ModerationService.ProcessingResult> processDirect(@Valid @RequestBody CustomerRequestEvent event) {
        if (event.getEventId() == null || event.getEventId().isEmpty()) event.setEventId(UUID.randomUUID().toString());
        if (event.getTimestamp() == null) event.setTimestamp(LocalDateTime.now());
        log.info("Direct processing. EventId: {}", event.getEventId());
        return ResponseEntity.ok(moderationService.processEvent(event));
    }
    
    @PostMapping("/test-event")
    @Operation(summary = "Создать тестовое событие")
    public ResponseEntity<ModerationService.ProcessingResult> createTestEvent(
            @RequestParam String customerId, @RequestParam String category,
            @RequestParam(defaultValue = "MEDIUM") CustomerRequestEvent.Priority priority) {
        CustomerRequestEvent event = CustomerRequestEvent.builder()
                .eventId(UUID.randomUUID().toString()).customerId(customerId)
                .requestId("REQ-" + System.currentTimeMillis()).category(category)
                .subject("Test request").description("Test request via API")
                .priority(priority).timestamp(LocalDateTime.now()).build();
        return ResponseEntity.ok(moderationService.processEvent(event));
    }
    
    @GetMapping("/events/{eventId}")
    @Operation(summary = "Получить обработанное событие")
    public ResponseEntity<ProcessedEvent> getProcessedEvent(@PathVariable String eventId) {
        return processedEventRepository.findByEventId(eventId)
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/events")
    @Operation(summary = "Получить все обработанные события")
    public ResponseEntity<List<ProcessedEvent>> getAllProcessedEvents() {
        return ResponseEntity.ok(processedEventRepository.findAll());
    }
    
    @GetMapping("/statistics")
    @Operation(summary = "Получить статистику")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(Map.of(
                "totalProcessed", processedEventRepository.count(),
                "published", processedEventRepository.countByResult(ProcessedEvent.ProcessingResult.PUBLISHED),
                "rejectedDuplicate", processedEventRepository.countByResult(ProcessedEvent.ProcessingResult.REJECTED_DUPLICATE),
                "rejectedActiveRequest", processedEventRepository.countByResult(ProcessedEvent.ProcessingResult.REJECTED_ACTIVE_REQUEST),
                "rejectedOutsideHours", processedEventRepository.countByResult(ProcessedEvent.ProcessingResult.REJECTED_OUTSIDE_HOURS),
                "registeredRules", rulesEngine.getRegisteredRules(),
                "timestamp", LocalDateTime.now()));
    }
    
    @DeleteMapping("/events/{eventId}")
    @Operation(summary = "Удалить обработанное событие")
    public ResponseEntity<Map<String, String>> deleteProcessedEvent(@PathVariable String eventId) {
        if (processedEventRepository.existsByEventId(eventId)) {
            processedEventRepository.deleteById(eventId);
            return ResponseEntity.ok(Map.of("status", "DELETED", "eventId", eventId));
        }
        return ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/events")
    @Operation(summary = "Очистить все события")
    public ResponseEntity<Map<String, Object>> clearAllEvents() {
        long count = processedEventRepository.count();
        processedEventRepository.deleteAll();
        return ResponseEntity.ok(Map.of("status", "CLEARED", "deletedCount", count));
    }
    
    @GetMapping("/health")
    @Operation(summary = "Проверка состояния")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "moderation-service", 
                "timestamp", LocalDateTime.now(), "rulesCount", rulesEngine.getRegisteredRules().size()));
    }
}
