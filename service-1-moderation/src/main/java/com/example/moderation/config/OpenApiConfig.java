package com.example.moderation.config;

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
                        .title("Moderation Service API")
                        .version("1.0.0")
                        .description("""
                                API для сервиса модерации обращений клиентов.
                                
                                ## Функционал
                                
                                - Приём событий об обращениях клиентов из Kafka
                                - Обогащение данных через внешний сервис
                                - Применение бизнес-правил модерации
                                - Публикация результатов в Kafka
                                
                                ## Правила модерации
                                
                                1. **Идемпотентность**: Повторная обработка события не создаёт дубликатов
                                2. **Активные обращения**: Блокировка при наличии активного обращения в категории
                                3. **Рабочее время**: Фильтрация по рабочим часам для определённых категорий
                                
                                ## Тестирование
                                
                                Используйте endpoints для отправки тестовых событий и проверки результатов.
                                """)
                        .contact(new Contact()
                                .name("Development Team")
                                .email("dev@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development"),
                        new Server().url("http://service-1:8080").description("Docker Network")
                ));
    }
}
