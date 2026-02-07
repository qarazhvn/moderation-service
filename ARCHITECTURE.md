# Архитектура проекта: Сервис Модерации

## 📊 Общая схема системы

```
                                    ┌─────────────────────────────────────────────────────────────┐
                                    │                      DOCKER COMPOSE                         │
                                    │                                                             │
┌──────────────┐                    │  ┌─────────────┐         ┌─────────────┐                   │
│   Клиент     │────────────────────│─>│  KAFKA      │────────>│  SERVICE-1  │                   │
│  (Swagger)   │                    │  │  Topic-1    │         │  Moderation │                   │
└──────────────┘                    │  │             │         │  :8080      │                   │
                                    │  │ customer-   │         └──────┬──────┘                   │
                                    │  │ requests-   │                │                          │
                                    │  │ topic       │                │ REST API                 │
                                    │  └─────────────┘                ▼                          │
                                    │                          ┌─────────────┐                   │
                                    │  ┌─────────────┐         │  SERVICE-2  │                   │
                                    │  │  KAFKA      │<────────│  Enrichment │                   │
                                    │  │  Topic-2    │         │  :8081      │                   │
                                    │  │             │         └──────┬──────┘                   │
                                    │  │ moderation- │                │                          │
                                    │  │ results-    │                ▼                          │
                                    │  │ topic       │         ┌─────────────┐                   │
                                    │  └─────────────┘         │   REDIS     │                   │
                                    │                          │   :6379     │                   │
                                    │  ┌─────────────┐         │  Данные     │                   │
                                    │  │  MONGODB    │         │  клиентов   │                   │
                                    │  │  :27017     │         └─────────────┘                   │
                                    │  │  История    │                                           │
                                    │  │  событий    │                                           │
                                    │  └─────────────┘                                           │
                                    └─────────────────────────────────────────────────────────────┘
```

---

## 🗄️ Хранилища данных

### 1. Apache Kafka — Брокер сообщений

| Элемент | Значение |
|---------|----------|
| **Порт** | 9092 (внутри Docker), 29092 (снаружи) |
| **Конфигурация** | `docker-compose.yml` строки 28-52 |
| **UI** | http://localhost:8090 (Kafka UI) |

#### Топики:

| Топик | Назначение | Кто пишет | Кто читает |
|-------|------------|-----------|------------|
| `customer-requests-topic` | Входящие обращения | Клиент/API | Service-1 |
| `moderation-results-topic` | Одобренные события | Service-1 | Внешние системы |

#### Где настроено:
```
service-1-moderation/src/main/resources/application.yaml
├── kafka.topics.input: customer-requests-topic
└── kafka.topics.output: moderation-results-topic

service-1-moderation/src/main/java/com/example/moderation/
├── config/KafkaConfig.java          # Конфигурация Kafka
└── kafka/CustomerRequestEventConsumer.java  # Consumer из Topic-1
```

---

### 2. MongoDB — База данных событий

| Элемент | Значение |
|---------|----------|
| **Порт** | 27017 |
| **База данных** | `moderation_db` |
| **Коллекция** | `processed_events` |
| **Конфигурация** | `docker-compose.yml` строки 70-84 |
| **UI** | http://localhost:8091 (Mongo Express, admin/admin) |

#### Что хранится:

```javascript
// Коллекция: processed_events
{
  "_id": "ObjectId(...)",
  "eventId": "EVT-001",           // Уникальный ID события
  "customerId": "CUST-001",       // ID клиента
  "category": "TECHNICAL_SUPPORT", // Категория обращения
  "result": "PUBLISHED",          // Результат модерации
  "rejectionReason": null,        // Причина отклонения (если есть)
  "processedAt": "2026-02-07T10:00:00",
  "expireAt": "2026-03-09T10:00:00"  // TTL 30 дней
}
```

#### Возможные значения `result`:
| Значение | Описание |
|----------|----------|
| `PUBLISHED` | Одобрено, отправлено в Topic-2 |
| `REJECTED_DUPLICATE` | Отклонено — дубликат |
| `REJECTED_ACTIVE_REQUEST` | Отклонено — есть активное обращение |
| `REJECTED_OUTSIDE_HOURS` | Отклонено — вне рабочего времени |
| `REJECTED_NO_DATA` | Отклонено — нет данных/ошибка |

#### Где настроено:
```
service-1-moderation/src/main/resources/application.yaml
├── spring.data.mongodb.uri: mongodb://mongodb:27017/moderation_db
└── spring.data.mongodb.database: moderation_db

service-1-moderation/src/main/java/com/example/moderation/
├── model/ProcessedEvent.java           # Модель документа
├── repository/ProcessedEventRepository.java  # Репозиторий
└── rules/impl/DuplicateEventRule.java   # Проверка дубликатов
```

---

### 3. Redis — Кэш данных клиентов

| Элемент | Значение |
|---------|----------|
| **Порт** | 6379 |
| **TTL** | 3600 секунд (1 час) |
| **Конфигурация** | `docker-compose.yml` строки 100-115 |
| **UI** | http://localhost:8092 (Redis Commander) |

#### Что хранится:

```javascript
// Ключ: customer:{customerId}
{
  "customerId": "CUST-001",
  "customerName": "Иван Иванов",
  "customerEmail": "ivan@example.com",
  "customerPhone": "+7-999-123-45-67",
  "customerLevel": "VIP",           // NEW, REGULAR, VIP, PREMIUM
  "registeredAt": "2025-01-15T10:00:00",
  "activeRequests": [               // Текущие активные обращения
    {
      "requestId": "REQ-001",
      "category": "BILLING",
      "subject": "Вопрос по счёту",
      "status": "IN_PROGRESS",      // OPEN, IN_PROGRESS, PENDING, RESOLVED, CLOSED
      "createdAt": "2026-02-05T10:00:00"
    }
  ]
}
```

#### Где настроено:
```
service-2-enrichment/src/main/resources/application.yaml
├── spring.data.redis.host: redis
└── spring.data.redis.port: 6379

service-2-enrichment/src/main/java/com/example/enrichment/
├── config/RedisConfig.java           # Конфигурация Redis
├── model/CustomerData.java           # Модель данных
├── repository/CustomerRepository.java # Репозиторий
└── service/EnrichmentService.java    # Бизнес-логика
```

---

## 🔧 Service-1: Moderation Service (порт 8080)

### Структура кода:

```
service-1-moderation/src/main/java/com/example/moderation/
│
├── ModerationServiceApplication.java    # Точка входа
│
├── config/
│   ├── KafkaConfig.java                 # Настройка Kafka Consumer/Producer
│   ├── WebClientConfig.java             # HTTP клиент к Service-2
│   ├── ModerationRulesConfig.java       # Параметры правил модерации
│   └── OpenApiConfig.java               # Swagger документация
│
├── controller/
│   └── ModerationController.java        # REST API для тестирования
│       ├── POST /api/v1/moderation/send-to-kafka
│       ├── POST /api/v1/moderation/process-direct
│       ├── GET  /api/v1/moderation/events
│       ├── GET  /api/v1/moderation/events/{eventId}
│       ├── DELETE /api/v1/moderation/events/{eventId}
│       └── GET  /api/v1/moderation/statistics
│
├── kafka/
│   └── CustomerRequestEventConsumer.java  # Чтение из Topic-1
│
├── model/
│   ├── CustomerRequestEvent.java        # Входящее событие из Kafka
│   ├── EnrichmentData.java              # Данные от Service-2
│   ├── ModerationResultEvent.java       # Исходящее событие в Topic-2
│   └── ProcessedEvent.java              # Документ MongoDB
│
├── repository/
│   └── ProcessedEventRepository.java    # MongoDB репозиторий
│
├── rules/
│   ├── ModerationRule.java              # Интерфейс правила
│   ├── RuleResult.java                  # Результат проверки
│   └── impl/
│       ├── DuplicateEventRule.java      # Приоритет 1: Проверка дубликатов
│       ├── ActiveRequestRule.java       # Приоритет 2: Активные обращения
│       └── WorkingHoursRule.java        # Приоритет 3: Рабочее время
│
├── service/
│   ├── ModerationService.java           # Главный сервис обработки
│   ├── ModerationRulesEngine.java       # Движок правил (Chain of Responsibility)
│   └── EnrichmentClientService.java     # REST клиент к Service-2
│
└── exception/
    └── ...                              # Обработка ошибок
```

### Swagger UI: http://localhost:8080/swagger-ui.html

---

## 🔧 Service-2: Enrichment Service (порт 8081)

### Структура кода:

```
service-2-enrichment/src/main/java/com/example/enrichment/
│
├── EnrichmentServiceApplication.java    # Точка входа
│
├── config/
│   ├── RedisConfig.java                 # Настройка Redis
│   └── OpenApiConfig.java               # Swagger документация
│
├── controller/
│   └── EnrichmentController.java        # REST API
│       ├── GET  /api/v1/enrichment/customer/{customerId}
│       ├── POST /api/v1/enrichment/customer
│       ├── PUT  /api/v1/enrichment/customer/{customerId}
│       ├── DELETE /api/v1/enrichment/customer/{customerId}
│       ├── GET  /api/v1/enrichment/customers
│       ├── POST /api/v1/enrichment/customer/{id}/requests
│       ├── DELETE /api/v1/enrichment/customer/{id}/requests/{reqId}
│       └── POST /api/v1/enrichment/setup-test-data
│
├── dto/
│   ├── CustomerRequest.java             # DTO для создания клиента
│   └── EnrichmentResponse.java          # DTO ответа
│
├── model/
│   └── CustomerData.java                # Redis модель
│
├── repository/
│   └── CustomerRepository.java          # Redis репозиторий
│
├── service/
│   └── EnrichmentService.java           # Бизнес-логика
│
└── exception/
    └── ...                              # Обработка ошибок
```

### Swagger UI: http://localhost:8081/swagger-ui.html

---

## 📁 Конфигурационные файлы

### docker-compose.yml — главный файл инфраструктуры

| Строки | Сервис | Порт | Назначение |
|--------|--------|------|------------|
| 12-26 | Zookeeper | 2181 | Координатор Kafka |
| 28-52 | Kafka | 9092, 29092 | Брокер сообщений |
| 54-68 | Kafka UI | 8090 | Мониторинг Kafka |
| 70-84 | MongoDB | 27017 | База данных событий |
| 86-98 | Mongo Express | 8091 | UI для MongoDB |
| 100-115 | Redis | 6379 | Кэш данных клиентов |
| 117-125 | Redis Commander | 8092 | UI для Redis |
| 127-155 | Service-1 | 8080 | Сервис модерации |
| 157-180 | Service-2 | 8081 | Сервис обогащения |

### application.yaml — настройки сервисов

**Service-1** (`service-1-moderation/src/main/resources/application.yaml`):
- Подключение к MongoDB
- Подключение к Kafka (consumer + producer)
- URL Service-2
- Конфигурация правил модерации

**Service-2** (`service-2-enrichment/src/main/resources/application.yaml`):
- Подключение к Redis

---

## 🔄 Поток обработки события

```
1. Событие приходит в Topic-1 (customer-requests-topic)
                    │
                    ▼
2. CustomerRequestEventConsumer читает событие
                    │
                    ▼
3. ModerationService.processEvent() запускается
                    │
                    ├──> EnrichmentClientService запрашивает данные клиента
                    │         │
                    │         └──> Service-2 → Redis → возврат CustomerData
                    │
                    ▼
4. ModerationRulesEngine.evaluate() проверяет правила
                    │
        ┌───────────┼───────────┐
        ▼           ▼           ▼
   DuplicateRule  ActiveRule  WorkingHoursRule
   (MongoDB)      (Redis)     (config)
        │           │           │
        └───────────┴───────────┘
                    │
                    ▼
5. Результат сохраняется в MongoDB (processed_events)
                    │
                    ▼
6. Если все правила прошли:
   └──> ModerationResultEvent публикуется в Topic-2
        (moderation-results-topic)
```

---

## 🌐 Доступные UI интерфейсы

| Сервис | URL | Логин/Пароль |
|--------|-----|--------------|
| Service-1 Swagger | http://localhost:8080/swagger-ui.html | - |
| Service-2 Swagger | http://localhost:8081/swagger-ui.html | - |
| Kafka UI | http://localhost:8090 | - |
| Mongo Express | http://localhost:8091 | admin / admin |
| Redis Commander | http://localhost:8092 | - |

---

## 📋 Быстрый справочник: Где что?

| Вопрос | Ответ |
|--------|-------|
| Где настроен Kafka? | `docker-compose.yml` + `service-1-moderation/.../config/KafkaConfig.java` |
| Где потребляются события из Topic-1? | `service-1-moderation/.../kafka/CustomerRequestEventConsumer.java` |
| Где публикуются события в Topic-2? | `service-1-moderation/.../service/ModerationService.java` метод `publishToOutputTopic()` |
| Где настроен MongoDB? | `docker-compose.yml` + `service-1-moderation/.../application.yaml` |
| Где сохраняются события в MongoDB? | `service-1-moderation/.../service/ModerationService.java` метод `saveProcessedEvent()` |
| Где проверяются дубликаты? | `service-1-moderation/.../rules/impl/DuplicateEventRule.java` |
| Где настроен Redis? | `docker-compose.yml` + `service-2-enrichment/.../config/RedisConfig.java` |
| Где хранятся данные клиентов? | `service-2-enrichment/.../model/CustomerData.java` → Redis |
| Где правила модерации? | `service-1-moderation/.../rules/impl/` |
| Где движок правил? | `service-1-moderation/.../service/ModerationRulesEngine.java` |
