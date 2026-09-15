# Arkanzabel

Android-клиент для VPN-туннелирования на базе v2ray-engine. предоставляет доступ к заблокированным сервисам через конфигурации VLESS / ShadowSocks / WireGuard с автоматическим выбором оптимального профиля.

## Ключевые возможности

- **One-tap connect** — при входе автоматически пингуются все профили и выбирается самый быстрый
- **Автоматическая загрузка профилей** из внешних источников (подписки)
- **Ручной выбор сервера** с отображением ping, протокола (WebSocket / gRPC / KCP) и региона
- **Избранные профили** — добавление тапом для быстрого доступа
- **Per-app прокси** — настройка маршрутизации трафика по приложениям
- **Anti-DPI** — fragment/noise/keepalive/MUX-политика, fakedns, kill-switch
- **Happy Eyeballs** — параллельное подключение IPv4/IPv6

## Архитектура проекта

```
arkanzabel/
├── app/              # UI-слой: Jetpack Compose, навигация, экраны
│   └── src/main/java/com/thindie/rknzbl/
│       ├── MainActivity.kt
│       ├── application/          # Application class, DI
│       ├── domain/               # Доменные модели и репозитории
│       ├── appfeatures/          # Экраны (MVI: State/Command/Screen/StateSink)
│       └── error/                # Обработка ошибок
├── core/             # Общие типы, утилиты, навигация
│   └── src/main/java/com/thindie/engine/core/
│       ├── Router.kt             # Роутер-ориентированная навигация
│       ├── WorkState.kt          # Состояния работы (Loading/Success/Error)
│       ├── ScreenScope.kt        # Scope для экранов
│       └── ...
├── v2ray-engine/     # Сетевой слой: Xray-движок, конфигурация туннеля
│   └── src/main/java/com/thindie/engine/v2ray/
│       ├── ConfigAssembler.kt    # Сборка конфига Xray
│       ├── V2rayConfigManager.kt # Управление конфигурацией
│       └── ...
├── docs/             # Документация проекта
└── research/         # Исследования и заметки
```

### Паттерн MVI для экранов

Каждый экран состоит из:
- `*Route.kt` — определение маршрута
- `*Screen.kt` — UI-композиция (Jetpack Compose)
- `*State.kt` — модель состояния
- `*Command.kt` — sealed interface команд
- `*StateSink.kt` — обработка событий и переходов

Пример: `com.thindie.engine.core.ExampleScreen`

## Стек технологий

| Компонент | Версия |
|-----------|--------|
| Android Gradle Plugin | 8.13.2 |
| Kotlin | 2.3.20 |
| Jetpack Compose BOM | 2026.03.00 |
| Ktor Client | 3.4.2 |
| OKHttp | 5.3.2 |
| MMKV (хранилище) | 1.3.16 |
| Coroutines | 1.10.2 |
| minSdk | 24 |

## Сборка и запуск

```bash
# Сборка debug-версии
./gradlew app:assembleDebug

# Форматирование кода (ktlint)
./gradlew app:ktLintFormat

# Запуск unit-тестов
./gradlew buildWithTests
```

## Лицензия

См. файл [LICENSE](./LICENSE).
