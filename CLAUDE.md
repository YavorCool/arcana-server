# arcana-server

Kotlin + Ktor бэкенд для ARCANA.

## Стек

- Kotlin 2.3.0, JVM 21
- Ktor 3.4.0 (Netty)
- Exposed 0.61.0 (ORM)
- PostgreSQL 17
- JWT (ktor-server-auth-jwt)
- kotlinx.serialization
- Docker (eclipse-temurin:21)

## Структура

```
src/main/kotlin/com/schlepping/arcana/
├── Application.kt    — main + module(), точка входа
├── Routing.kt        — configureRouting()
├── Security.kt       — configureSecurity(), JWT
├── Serialization.kt  — configureSerialization()
└── Monitoring.kt     — configureMonitoring()
```

`Application.module()` вызывает отдельные `configureX()` функции для каждого concern.

## Архитектурные конвенции

Слои: Routes → Service → Repository → LlmProvider

Целевая middleware chain (см. ROADMAP.md):
IP rate limit → JWT → Device rate limit → Attestation → Validation → Business limits → Model routing → LLM → Logging

## Exposed ORM

- Таблицы: `object XxxTable : Table("xxx")`
- Запросы через `transaction { }`
- `SchemaUtils.create()` на старте (Flyway запланирован на v2.0)

## Serialization

- kotlinx.serialization
- `@Serializable data class` для DTO
- Content negotiation через Ktor plugin

## Конфигурация

- `src/main/resources/application.yaml` — Ktor config + бизнес-настройки
- Секреты через environment variables (DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD)
- JWT: настройки в application.yaml, secret через env

## Сборка и запуск

```bash
./gradlew run                    # Запуск dev-сервера
./gradlew test                   # Тесты
./gradlew buildFatJar            # Fat JAR для деплоя
docker compose up -d             # PostgreSQL + приложение
```

## Ключевые файлы

- `src/main/kotlin/com/schlepping/arcana/*.kt` — исходный код
- `src/main/resources/application.yaml` — конфигурация
- `docker-compose.yml` — PostgreSQL 17 + приложение
- `Dockerfile` — multi-stage build (JDK 21 build → JRE 21 run)
- `gradle/libs.versions.toml` — версии зависимостей
