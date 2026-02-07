package com.example.moderation.service;

import com.example.moderation.model.CustomerRequestEvent;
import com.example.moderation.model.EnrichmentData;
import com.example.moderation.rules.ModerationRule;
import com.example.moderation.rules.RuleResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Chain of Responsibility pattern for moderation rules
@Slf4j
@Service
public class ModerationRulesEngine {
    
    private final List<ModerationRule> rules;
    
    public ModerationRulesEngine(List<ModerationRule> rules) {
        this.rules = rules.stream()
                .sorted(Comparator.comparingInt(ModerationRule::getPriority))
                .toList();
        log.info("Initialized with {} rules", this.rules.size());
    }
    
    public ModerationEngineResult evaluate(CustomerRequestEvent event, EnrichmentData enrichmentData) {
        log.debug("Evaluating rules for event: {}", event.getEventId());
        
        List<RuleResult> results = new ArrayList<>();
        boolean allPassed = true;
        RuleResult failedRule = null;
        
        for (ModerationRule rule : rules) {
            if (!rule.isEnabled()) continue;
            
            try {
                RuleResult result = rule.apply(event, enrichmentData);
                results.add(result);
                
                if (!result.isPassed()) {
                    allPassed = false;
                    failedRule = result;
                    break;
                }
            } catch (Exception e) {
                log.error("Rule {} error: {}", rule.getRuleName(), e.getMessage());
                RuleResult errorResult = RuleResult.rejected(rule.getRuleName(), "Rule error", e.getMessage());
                results.add(errorResult);
                allPassed = false;
                failedRule = errorResult;
                break;
            }
        }
        
        return ModerationEngineResult.builder()
                .eventId(event.getEventId())
                .allRulesPassed(allPassed)
                .ruleResults(results)
                .failedRule(failedRule)
                .rulesEvaluated(results.size())
                .totalRules(rules.size())
                .build();
    }
    
    public List<String> getRegisteredRules() {
        return rules.stream()
                .map(rule -> String.format("%s (priority: %d)", rule.getRuleName(), rule.getPriority()))
                .toList();
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ModerationEngineResult {
        private String eventId;
        private boolean allRulesPassed;
        private List<RuleResult> ruleResults;
        private RuleResult failedRule;
        private int rulesEvaluated;
        private int totalRules;
        
        public String getRejectionReason() {
            return failedRule != null ? failedRule.getRejectionReason() : null;
        }
    }
}
