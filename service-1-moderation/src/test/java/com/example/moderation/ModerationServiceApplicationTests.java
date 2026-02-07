package com.example.moderation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Тест запуска контекста приложения.
 */
@SpringBootTest
@ActiveProfiles("test")
class ModerationServiceApplicationTests {

    @Test
    void contextLoads() {
        // Проверяет, что Spring контекст загружается без ошибок
    }
}
