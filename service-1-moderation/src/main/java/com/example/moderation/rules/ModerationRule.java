package com.example.moderation.rules;

import com.example.moderation.model.CustomerRequestEvent;
import com.example.moderation.model.EnrichmentData;

/**
 * Интерфейс для правил модерации.
 * 
 * Паттерн Strategy: Каждое правило реализует единый интерфейс,
 * что позволяет легко добавлять новые правила без изменения существующего кода.
 * 
 * Принципы SOLID:
 * - Interface Segregation: Минимальный интерфейс с одной ответственностью
 * - Open/Closed: Новые правила добавляются как новые классы
 * - Liskov Substitution: Все реализации взаимозаменяемы
 * - Dependency Inversion: Зависимость от абстракции, не от реализации
 */
public interface ModerationRule {
    
    /**
     * Применяет правило модерации к событию.
     * 
     * @param event событие обращения клиента
     * @param enrichmentData расширенные данные о клиенте
     * @return результат применения правила
     */
    RuleResult apply(CustomerRequestEvent event, EnrichmentData enrichmentData);
    
    /**
     * Возвращает название правила для логирования.
     * 
     * @return название правила
     */
    String getRuleName();
    
    /**
     * Возвращает приоритет правила (меньше = выше приоритет).
     * Правила с высоким приоритетом проверяются первыми.
     * 
     * @return приоритет правила
     */
    default int getPriority() {
        return 100;
    }
    
    /**
     * Проверяет, включено ли правило.
     * 
     * @return true если правило активно
     */
    default boolean isEnabled() {
        return true;
    }
}
