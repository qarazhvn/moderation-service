package com.example.moderation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;
import java.util.List;

/**
 * Конфигурация бизнес-правил модерации.
 * 
 * Вынесение правил в конфигурацию позволяет:
 * 1. Изменять правила без перекомпиляции
 * 2. Использовать разные настройки для разных окружений
 * 3. Легко тестировать с различными конфигурациями
 * 
 * Принципы SOLID:
 * - Open/Closed: Добавление новых правил не требует изменения существующего кода
 * - Single Responsibility: Класс отвечает только за хранение конфигурации правил
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "moderation.rules")
public class ModerationRulesConfig {
    
    /**
     * Категории, для которых проверяется рабочее время
     */
    private List<String> workingHoursCategories;
    
    /**
     * Начало рабочего времени (по умолчанию 09:00)
     */
    private LocalTime workingHoursStart = LocalTime.of(9, 0);
    
    /**
     * Конец рабочего времени (по умолчанию 18:00)
     */
    private LocalTime workingHoursEnd = LocalTime.of(18, 0);
    
    /**
     * Категории, для которых разрешены множественные активные обращения
     */
    private List<String> allowMultipleActiveCategories;
    
    /**
     * Максимальное количество активных обращений на клиента (по умолчанию 1)
     */
    private int maxActiveRequestsPerCategory = 1;
    
    /**
     * Включить/выключить проверку рабочего времени
     */
    private boolean workingHoursCheckEnabled = true;
    
    /**
     * Включить/выключить проверку активных обращений
     */
    private boolean activeRequestsCheckEnabled = true;
}
