package com.example.moderation.rules.impl;

import com.example.moderation.config.ModerationRulesConfig;
import com.example.moderation.model.CustomerRequestEvent;
import com.example.moderation.model.EnrichmentData;
import com.example.moderation.rules.ModerationRule;
import com.example.moderation.rules.RuleResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

// Checks if customer has active requests in same category
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class ActiveRequestRule implements ModerationRule {
    
    private final ModerationRulesConfig rulesConfig;
    private static final String RULE_NAME = "ACTIVE_REQUEST_CHECK";
    
    @Override
    public RuleResult apply(CustomerRequestEvent event, EnrichmentData enrichmentData) {
        if (!rulesConfig.isActiveRequestsCheckEnabled()) return RuleResult.passed(RULE_NAME);
        if (enrichmentData == null || !enrichmentData.isDataAvailable()) return RuleResult.passed(RULE_NAME);
        
        List<String> allowedCategories = rulesConfig.getAllowMultipleActiveCategories();
        if (allowedCategories != null && allowedCategories.contains(event.getCategory())) {
            return RuleResult.passed(RULE_NAME);
        }
        
        List<EnrichmentData.ActiveRequest> activeRequests = enrichmentData.getActiveRequests();
        if (activeRequests == null || activeRequests.isEmpty()) return RuleResult.passed(RULE_NAME);
        
        long sameCategoryCount = activeRequests.stream()
                .filter(req -> req.getCategory().equalsIgnoreCase(event.getCategory()))
                .filter(req -> isActiveStatus(req.getStatus()))
                .count();
        
        int maxActive = rulesConfig.getMaxActiveRequestsPerCategory();
        if (sameCategoryCount >= maxActive) {
            return RuleResult.rejected(RULE_NAME, "Active request already exists in this category",
                    String.format("Found %d active request(s)", sameCategoryCount));
        }
        
        return RuleResult.passed(RULE_NAME);
    }
    
    private boolean isActiveStatus(EnrichmentData.RequestStatus status) {
        return status == EnrichmentData.RequestStatus.OPEN 
                || status == EnrichmentData.RequestStatus.IN_PROGRESS
                || status == EnrichmentData.RequestStatus.PENDING;
    }
    
    @Override
    public String getRuleName() { return RULE_NAME; }
    
    @Override
    public int getPriority() { return 2; }
    
    @Override
    public boolean isEnabled() {
        return rulesConfig.isActiveRequestsCheckEnabled();
    }
}
