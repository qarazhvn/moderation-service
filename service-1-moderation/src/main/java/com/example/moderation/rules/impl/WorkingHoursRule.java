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

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

// Checks if request is within working hours for specific categories
@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class WorkingHoursRule implements ModerationRule {
    
    private final ModerationRulesConfig rulesConfig;
    private static final String RULE_NAME = "WORKING_HOURS_CHECK";
    
    @Override
    public RuleResult apply(CustomerRequestEvent event, EnrichmentData enrichmentData) {
        if (!rulesConfig.isWorkingHoursCheckEnabled()) return RuleResult.passed(RULE_NAME);
        
        List<String> restrictedCategories = rulesConfig.getWorkingHoursCategories();
        if (restrictedCategories == null || !restrictedCategories.contains(event.getCategory())) {
            return RuleResult.passed(RULE_NAME);
        }
        
        LocalDateTime eventTime = event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now();
        
        if (!isWithinWorkingHours(eventTime)) {
            return RuleResult.rejected(RULE_NAME, "Request received outside working hours",
                    String.format("Time: %s, Working hours: %s-%s", 
                            eventTime.toLocalTime(),
                            rulesConfig.getWorkingHoursStart(),
                            rulesConfig.getWorkingHoursEnd()));
        }
        
        return RuleResult.passed(RULE_NAME);
    }
    
    private boolean isWithinWorkingHours(LocalDateTime dateTime) {
        DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) return false;
        
        LocalTime time = dateTime.toLocalTime();
        return !time.isBefore(rulesConfig.getWorkingHoursStart()) 
                && !time.isAfter(rulesConfig.getWorkingHoursEnd());
    }
    
    @Override
    public String getRuleName() { return RULE_NAME; }
    
    @Override
    public int getPriority() { return 3; }
    
    @Override
    public boolean isEnabled() { return rulesConfig.isWorkingHoursCheckEnabled(); }
}
