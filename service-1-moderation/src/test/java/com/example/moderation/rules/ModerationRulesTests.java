package com.example.moderation.rules;

import com.example.moderation.config.ModerationRulesConfig;
import com.example.moderation.model.CustomerRequestEvent;
import com.example.moderation.model.EnrichmentData;
import com.example.moderation.rules.impl.ActiveRequestRule;
import com.example.moderation.rules.impl.WorkingHoursRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit тесты для правил модерации.
 * 
 * Тестируют бизнес-логику правил в изоляции.
 */
class ModerationRulesTests {
    
    private ModerationRulesConfig config;
    
    @BeforeEach
    void setUp() {
        config = new ModerationRulesConfig();
        config.setWorkingHoursCategories(List.of("BILLING", "COMPLAINT"));
        config.setWorkingHoursStart(LocalTime.of(9, 0));
        config.setWorkingHoursEnd(LocalTime.of(18, 0));
        config.setAllowMultipleActiveCategories(List.of("GENERAL_INQUIRY"));
        config.setMaxActiveRequestsPerCategory(1);
        config.setWorkingHoursCheckEnabled(true);
        config.setActiveRequestsCheckEnabled(true);
    }
    
    // ============================================
    // Тесты для WorkingHoursRule
    // ============================================
    
    @Test
    @DisplayName("WorkingHoursRule: должен пропустить событие в рабочее время")
    void workingHoursRule_shouldPassDuringWorkingHours() {
        WorkingHoursRule rule = new WorkingHoursRule(config);
        
        // Понедельник, 10:00 - рабочее время
        CustomerRequestEvent event = createEvent("BILLING", 
                LocalDateTime.of(2026, 2, 2, 10, 0)); // Понедельник
        
        RuleResult result = rule.apply(event, null);
        
        assertTrue(result.isPassed());
    }
    
    @Test
    @DisplayName("WorkingHoursRule: должен отклонить событие вне рабочего времени")
    void workingHoursRule_shouldRejectOutsideWorkingHours() {
        WorkingHoursRule rule = new WorkingHoursRule(config);
        
        // Понедельник, 22:00 - нерабочее время
        CustomerRequestEvent event = createEvent("BILLING", 
                LocalDateTime.of(2026, 2, 2, 22, 0));
        
        RuleResult result = rule.apply(event, null);
        
        assertFalse(result.isPassed());
        assertEquals("WORKING_HOURS_CHECK", result.getRuleName());
    }
    
    @Test
    @DisplayName("WorkingHoursRule: должен отклонить событие в выходной день")
    void workingHoursRule_shouldRejectOnWeekend() {
        WorkingHoursRule rule = new WorkingHoursRule(config);
        
        // Суббота, 12:00 - выходной
        CustomerRequestEvent event = createEvent("BILLING", 
                LocalDateTime.of(2026, 2, 7, 12, 0)); // Суббота
        
        RuleResult result = rule.apply(event, null);
        
        assertFalse(result.isPassed());
    }
    
    @Test
    @DisplayName("WorkingHoursRule: должен пропустить категорию не из списка")
    void workingHoursRule_shouldPassNonRestrictedCategory() {
        WorkingHoursRule rule = new WorkingHoursRule(config);
        
        // TECHNICAL_SUPPORT не в списке ограниченных категорий
        CustomerRequestEvent event = createEvent("TECHNICAL_SUPPORT", 
                LocalDateTime.of(2026, 2, 7, 22, 0)); // Суббота, ночь
        
        RuleResult result = rule.apply(event, null);
        
        assertTrue(result.isPassed());
    }
    
    // ============================================
    // Тесты для ActiveRequestRule
    // ============================================
    
    @Test
    @DisplayName("ActiveRequestRule: должен пропустить если нет активных обращений")
    void activeRequestRule_shouldPassWhenNoActiveRequests() {
        ActiveRequestRule rule = new ActiveRequestRule(config);
        
        CustomerRequestEvent event = createEvent("BILLING", LocalDateTime.now());
        EnrichmentData enrichment = createEnrichmentWithoutActiveRequests();
        
        RuleResult result = rule.apply(event, enrichment);
        
        assertTrue(result.isPassed());
    }
    
    @Test
    @DisplayName("ActiveRequestRule: должен отклонить если есть активное обращение в той же категории")
    void activeRequestRule_shouldRejectWhenActiveRequestExists() {
        ActiveRequestRule rule = new ActiveRequestRule(config);
        
        CustomerRequestEvent event = createEvent("BILLING", LocalDateTime.now());
        EnrichmentData enrichment = createEnrichmentWithActiveRequest("BILLING");
        
        RuleResult result = rule.apply(event, enrichment);
        
        assertFalse(result.isPassed());
        assertEquals("ACTIVE_REQUEST_CHECK", result.getRuleName());
    }
    
    @Test
    @DisplayName("ActiveRequestRule: должен пропустить если активное обращение в другой категории")
    void activeRequestRule_shouldPassWhenActiveRequestInDifferentCategory() {
        ActiveRequestRule rule = new ActiveRequestRule(config);
        
        CustomerRequestEvent event = createEvent("BILLING", LocalDateTime.now());
        EnrichmentData enrichment = createEnrichmentWithActiveRequest("TECHNICAL_SUPPORT");
        
        RuleResult result = rule.apply(event, enrichment);
        
        assertTrue(result.isPassed());
    }
    
    @Test
    @DisplayName("ActiveRequestRule: должен пропустить категорию из списка разрешенных")
    void activeRequestRule_shouldPassForAllowedMultipleCategory() {
        ActiveRequestRule rule = new ActiveRequestRule(config);
        
        // GENERAL_INQUIRY разрешает множественные обращения
        CustomerRequestEvent event = createEvent("GENERAL_INQUIRY", LocalDateTime.now());
        EnrichmentData enrichment = createEnrichmentWithActiveRequest("GENERAL_INQUIRY");
        
        RuleResult result = rule.apply(event, enrichment);
        
        assertTrue(result.isPassed());
    }
    
    @Test
    @DisplayName("ActiveRequestRule: должен пропустить если данные обогащения недоступны")
    void activeRequestRule_shouldPassWhenEnrichmentNotAvailable() {
        ActiveRequestRule rule = new ActiveRequestRule(config);
        
        CustomerRequestEvent event = createEvent("BILLING", LocalDateTime.now());
        EnrichmentData enrichment = EnrichmentData.builder()
                .customerId("CUST-001")
                .dataAvailable(false)
                .build();
        
        RuleResult result = rule.apply(event, enrichment);
        
        assertTrue(result.isPassed());
    }
    
    // ============================================
    // Вспомогательные методы
    // ============================================
    
    private CustomerRequestEvent createEvent(String category, LocalDateTime timestamp) {
        return CustomerRequestEvent.builder()
                .eventId("EVT-001")
                .customerId("CUST-001")
                .requestId("REQ-001")
                .category(category)
                .subject("Test subject")
                .priority(CustomerRequestEvent.Priority.MEDIUM)
                .timestamp(timestamp)
                .build();
    }
    
    private EnrichmentData createEnrichmentWithoutActiveRequests() {
        return EnrichmentData.builder()
                .customerId("CUST-001")
                .customerName("Test Customer")
                .customerLevel(EnrichmentData.CustomerLevel.REGULAR)
                .activeRequests(List.of())
                .dataAvailable(true)
                .build();
    }
    
    private EnrichmentData createEnrichmentWithActiveRequest(String category) {
        return EnrichmentData.builder()
                .customerId("CUST-001")
                .customerName("Test Customer")
                .customerLevel(EnrichmentData.CustomerLevel.REGULAR)
                .activeRequests(List.of(
                        EnrichmentData.ActiveRequest.builder()
                                .requestId("REQ-ACTIVE-001")
                                .category(category)
                                .subject("Active request")
                                .status(EnrichmentData.RequestStatus.OPEN)
                                .createdAt(LocalDateTime.now().minusDays(1))
                                .build()
                ))
                .dataAvailable(true)
                .build();
    }
}
