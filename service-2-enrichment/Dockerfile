# Dockerfile для Service-2 (Enrichment Service)
# Многоэтапная сборка для оптимизации размера образа

# Этап 1: Сборка приложения
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Копируем pom файлы для кэширования зависимостей
COPY pom.xml .
COPY service-1-moderation/pom.xml service-1-moderation/
COPY service-2-enrichment/pom.xml service-2-enrichment/

# Загружаем зависимости (кэшируется если pom не изменился)
RUN mvn dependency:go-offline -pl service-2-enrichment -am -B

# Копируем исходный код
COPY service-2-enrichment/src service-2-enrichment/src

# Собираем приложение
RUN mvn package -pl service-2-enrichment -am -DskipTests -B

# Этап 2: Runtime образ
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Создаём пользователя для безопасности
RUN addgroup -S spring && adduser -S spring -G spring

# Копируем собранный jar
COPY --from=builder /app/service-2-enrichment/target/*.jar app.jar

# Переключаемся на непривилегированного пользователя
USER spring:spring

# Порт приложения
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Запуск приложения
ENTRYPOINT ["java", "-jar", "app.jar"]
