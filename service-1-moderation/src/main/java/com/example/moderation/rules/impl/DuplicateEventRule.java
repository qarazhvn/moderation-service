package com.example.moderation.rules.impl;

import com.example.moderation.model.CustomerRequestEvent;
import com.example.moderation.model.EnrichmentData;
import com.example.moderation.model.ProcessedEvent;
import com.example.moderation.repository.ProcessedEventRepository;
import com.example.moderation.rules.ModerationRule;
import com.example.moderation.rules.RuleResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Checks if event was already processed (idempotency)
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DuplicateEventRule implements ModerationRule {
    
    private final ProcessedEventRepository processedEventRepository;
    private static final String RULE_NAME = "DUPLICATE_EVENT_CHECK";
    
    @Override
    public RuleResult apply(CustomerRequestEvent event, EnrichmentData enrichmentData) {
        if (processedEventRepository.existsByEventId(event.getEventId())) {
            return RuleResult.rejected(RULE_NAME, "Event already processed");
        }
        return RuleResult.passed(RULE_NAME);
    }
    
    @Override
    public String getRuleName() { return RULE_NAME; }
    
    @Override
    public int getPriority() { return 1; }
}
