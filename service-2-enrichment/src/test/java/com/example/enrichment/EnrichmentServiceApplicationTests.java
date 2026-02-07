package com.example.enrichment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Тест запуска контекста приложения.
 */
@SpringBootTest
@ActiveProfiles("test")
class EnrichmentServiceApplicationTests {

    @Test
    void contextLoads() {
        // Проверяет, что Spring контекст загружается без ошибок
    }
}
