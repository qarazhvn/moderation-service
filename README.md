# Moderation Service

Сервис модерации обращений клиентов на Spring Boot.

## Что это

REST API + Kafka Consumer для модерации клиентских обращений. События принимаются из Kafka Topic-1, проходят через движок бизнес-правил и публикуются в Topic-2. Данные обогащения получаются из отдельного сервиса через REST. Метаданные хранятся в MongoDB для идемпотентности.

## Стек

- Java 17, Spring Boot 3
- Apache Kafka
- MongoDB
- Redis
- Docker

## Быстрый старт

```bash
git clone https://github.com/qarazhvn/moderation-service
cd moderation-service
docker compose up --build
```

После запуска:

- **Swagger UI (Moderation):** http://localhost:8080/swagger-ui.html
- **Swagger UI (Enrichment):** http://localhost:8081/swagger-ui.html
- **Kafka UI:** http://localhost:8090
- **Mongo Express:** http://localhost:8091 (admin / admin)
- **Redis Commander:** http://localhost:8092

## Особенности

- **Событийная архитектура** — Kafka для асинхронной обработки событий
- **Идемпотентность** — повторное событие с тем же ID не обрабатывается дважды
- **Движок правил** — Chain of Responsibility для расширяемой модерации
- **Retry-механизм** — автоматические повторные попытки при сбоях
- **SOLID** — чистая архитектура с разделением ответственности

