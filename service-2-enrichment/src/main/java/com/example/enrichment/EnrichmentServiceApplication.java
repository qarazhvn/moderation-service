package com.example.enrichment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Главный класс приложения Service-2 (Enrichment Service).
 * 
 * Сервис предоставляет расширенную информацию о клиентах
 * через REST API, используя Redis для кэширования данных.
 * 
 * Принципы SOLID:
 * - Single Responsibility: Только обогащение данных
 * - Dependency Inversion: Зависимость от абстракций (Redis Repository)
 */
@SpringBootApplication
public class EnrichmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnrichmentServiceApplication.class, args);
    }
}
