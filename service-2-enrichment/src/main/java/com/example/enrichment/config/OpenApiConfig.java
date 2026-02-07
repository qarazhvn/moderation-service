package com.example.enrichment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Enrichment Service API")
                        .version("1.0.0")
                        .description("""
                                API для сервиса обогащения данных клиентов.
                                
                                ## Функционал
                                
                                - Хранение данных клиентов в Redis кэше
                                - Предоставление расширенной информации по REST API
                                - Управление активными обращениями клиентов
                                
                                ## Интеграция
                                
                                Этот сервис вызывается Service-1 (Moderation Service) 
                                для обогащения данных обращений клиентов.
                                
                                ## Тестирование
                                
                                1. Используйте `/setup-test-data` для создания тестовых клиентов
                                2. Получите данные клиента через `/customer/{customerId}`
                                3. Добавляйте/удаляйте активные обращения для тестирования правил
                                """)
                        .contact(new Contact()
                                .name("Development Team")
                                .email("dev@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Local Development"),
                        new Server().url("http://service-2:8081").description("Docker Network")
                ));
    }
}
