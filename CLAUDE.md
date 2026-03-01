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
├── Application.kt        — main + module(), точка входа
├── Routing.kt            — configureRouting(), сборка всех роутов
├── plugins/
│   ├── Database.kt       — configureDatabase(), подключение к PostgreSQL
│   ├── Security.kt       — configureSecurity(), JWT
│   ├── Serialization.kt  — configureSerialization()
│   └── Monitoring.kt     — configureMonitoring()
├── db/                   — определения таблиц Exposed
│   ├── DeviceTables.kt
│   ├── ReadingTables.kt
│   └── UsageTables.kt
└── auth/                 — feature: аутентификация
    ├── AuthModels.kt     — DTO + доменные модели
    ├── AuthRepository.kt — работа с БД
    ├── AuthService.kt    — бизнес-логика, JWT генерация
    └── AuthRoutes.kt     — эндпоинты /api/v1/auth/*
```

`Application.module()` вызывает отдельные `configureX()` функции для каждого concern.

## Архитектурные конвенции

Слои: Routes → Service → Repository → LlmProvider

- Service работает с доменными моделями, НЕ с `ResultRow` и таблицами из `db/`
- Repository маппит `ResultRow` → доменные модели и возвращает их наверх

Целевая middleware chain (см. ROADMAP.md):
IP rate limit → JWT → Device rate limit → Attestation → Validation → Business limits → Model routing → LLM → Logging

## Стиль кода Kotlin

- **Корутины везде.** Ktor — корутиновый фреймворк. Все IO-операции должны быть `suspend`. Exposed: `newSuspendedTransaction {}` вместо `transaction {}`. Единственное исключение: `SchemaUtils.create()` на старте.
- **Идиоматичный Kotlin:** `?.let {} ?: ...`, scope functions (`let`, `also`, `apply`, `run`), expression body для коротких функций, trailing commas
- **Не писать Java на Kotlin:** избегать if/else где работает `?.let`/`?:`, избегать явных null-чеков где работает safe call chain

## Exposed ORM

- Таблицы: `object XxxTable : Table("xxx")`
- Запросы через `newSuspendedTransaction { }` (suspend, переключается на Dispatchers.IO)
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

### Testing — TDD

Проект следует TDD-подходу: **тесты пишутся до реализации**.

- **Порядок:** Test → Red → Implement → Green → Refactor
- **Service-тесты:** для каждого Service — свой `FakeRepository` (пример: `FakeDailyCardRepository`). Тестируем бизнес-логику без БД
- **Route-тесты:** через `testApplication` + JWT + fake providers (пример: `DailyCardRoutesTest`)
- **Конвенция:** каждый milestone включает тесты на свои Service-классы. Тесты — часть Definition of Done задачи, не отдельная задача
