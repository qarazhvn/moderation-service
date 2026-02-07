package com.example.moderation.rules;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Результат применения правила модерации.
 * 
 * Использует паттерн Value Object для неизменяемого представления результата.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleResult {
    
    /**
     * Прошло ли событие проверку правила
     */
    private boolean passed;
    
    /**
     * Название правила
     */
    private String ruleName;
    
    /**
     * Причина отклонения (если не прошло)
     */
    private String rejectionReason;
    
    /**
     * Дополнительные детали
     */
    private String details;
    
    /**
     * Создает успешный результат проверки.
     */
    public static RuleResult passed(String ruleName) {
        return RuleResult.builder()
                .passed(true)
                .ruleName(ruleName)
                .build();
    }
    
    /**
     * Создает неуспешный результат проверки.
     */
    public static RuleResult rejected(String ruleName, String reason) {
        return RuleResult.builder()
                .passed(false)
                .ruleName(ruleName)
                .rejectionReason(reason)
                .build();
    }
    
    /**
     * Создает неуспешный результат с деталями.
     */
    public static RuleResult rejected(String ruleName, String reason, String details) {
        return RuleResult.builder()
                .passed(false)
                .ruleName(ruleName)
                .rejectionReason(reason)
                .details(details)
                .build();
    }
}
