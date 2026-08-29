# Технический аудит Arkanzabel

Проект: Arkanzabel (com.thindie.rknzbl)  
Тип: Android-приложение для управления V2Ray/Reality-профилями  
Язык: Kotlin (1252 `.kt`-файла)  
SDK: minSdk 28, targetSdk 36, compileSdk 36  
Корневая директория: `E:/AndroidProject/arkanzabel`

---

## 1. Общая архитектура

Проект разделён на 3 модуля:

| Модуль | Назначение |
|--------|-----------|
| `app` | UI/фичи, DI, Application, MainActivity |
| `core` | Ядро маршрутизации: Router, ScreenFlow, Command, ScreenScope, WorkState, Deeplink, Log, AppTheme, uikit-виджеты |
| `v2ray-engine` | Обёртка над v2ray-core (Android), MMKV storage, DTO, AppConfig, JsonUtil, SpeedtestManager |

Архитектурный паттерн: **Feature-based модульность** (feature-home, feature-settings) + **Layered DDD** (data / domain / ui).

---

## 2. Сборка и конфигурация

| Параметр | Значение |
|----------|----------|
| AGP | 8.13.2 |
| Kotlin | 2.3.20 |
| Compose BOM | 2026.03.00 |
| Ktor | 3.4.2 |
| OKHttp | 5.3.2 |
| MMKV | 1.3.16 |
| Coroutines | 1.10.2 |
| Lifecycle | 2.10.0 |
| Compose Activity | 1.13.0 |
| WorkManager | 2.11.1 |

### Ключевые плагины
- `org.jlleitschuh.gradle.ktlint` 12.1.0 (форматирование, включён в root)
- `org.jetbrains.kotlin.android` + `org.jetbrains.kotlin.plugin.compose`

### Особенности build-конфигурации
- `kotlin.jvmToolchain = 21` — сборка на JDK 21
- `kotlin.experimental.tryK2 = true` — K2-компилятор включён
- `isKotlinMultiplatformEnabled = false` — не KMP
- `enableUnitTest = false`, `enableAndroidTest = false` — тесты на уровне сборки отключены
- `compileSdk = 36`, `minSdk = 28`, `targetSdk = 36`
- `versionCode = 18`, `versionName = "3.1.5"`

---

## 3. Система навигации (Router / ScreenFlow / ScreenScope)

### Router — централизованный конвейер команд
```kotlin
sealed class Command<STATE, COMMAND> { ... }
class Router(
  initialState: STATE,
  private val build: Build<STATE, COMMAND>,
  private val sink: Sink<STATE, COMMAND>,
)
```

- `Build` — функция `(scope, command) -> StateChanges`
- `Sink` — функция `(scope, effect) -> Effect`
- StateChanges — результат изменения состояния (mutate/transition/redirect)
- Router является единственным entry-point для приложения

### ScreenFlow — state machine
- `ScreenFlow<State>` — state machine с `StateChanges`
- Поддерживает `redirect` для навигации между экранами
- `redirect<NewState>(initial)` — полный сброс backstack

### ScreenScope — контекст экрана
```kotlin
abstract class ScreenScope<STATE : Any, COMMAND : Any> {
  abstract val state: SharedFlow<STATE>
  abstract suspend fun send(command: COMMAND)
  abstract suspend fun sendEvent(effect: Any)
}
```

### WorkState — универсальное состояние работы
```kotlin
sealed interface WorkState {
  data object Idle : WorkState
  data object Running : WorkState
  data class Failed(val error: Throwable) : WorkState
}
```

### Deeplink — обработка глубоких ссылок
- `Deeplink.parse(uri: Uri)` — фабрика
- `Deeplink.Type` — `OpenProfile`, `AddProfile`

---

## 4. Data Layer

### KeyValueStorage (v2ray-engine)
- MMKV storage с 7 отдельными инстансами (`MULTI_PROCESS_MODE`)
- Ключи: MAIN, PROFILE_FULL_CONFIG, SERVER_RAW, SERVER_AFF, SUB, ASSET, SETTING
- Все операции синхронные (MMKV блокирует основной поток при загрузке больших объёмов)
- `encodeServerConfig` — автогенерация UUID, если guid пустой
- `removeInvalidServer` — очистка профилей с `testDelayMillis < 0`
- `initSubsList()` — lazy init списка подписок
- Поддержка WebDAV, VPN-сессий, autosave, custom source URL

### SettingsRepository / SettingsRepositoryImpl
- Wrapper над `KeyValueStorage` с `MutableStateFlow` для каждой настройки
- Паттерн `Setting<T>` с `read`/`write` функциями
- Двухфазная запись: `encodeSettings` + обновление `MutableStateFlow`
- Typed-хелперы для ThemeMode, Autosave, MUX, Fragmentation, LocalSave, Speed, Language, CustomSource, RealityShow, Sniffing

### ConnectionProfileRepository
- Аналогичный pattern: `decodeProfile` → `MutableStateFlow`, `updateProfile` → `encodeServerConfig` + `encodeServerTestDelayMillis`

---

## 5. DI-система

```kotlin
object ApplicationScope {
  private var settingsRepository: SettingsRepository? = null
  private var connectionProfileRepository: ConnectionProfileRepository? = null
  private var router: Router? = null
  
  fun init(context: Context)
  fun settingsRepository(): SettingsRepository
  fun connectionProfileRepository(): ConnectionProfileRepository
  fun router(): Router
  fun router(scope: ScreenScope<*, *>)
  fun router(router: Router)
}
```

- Singleton-style DI через `ApplicationScope`
- `SettingsFlowModule` и `HomeFlowModule` — фабрики с зависимостями
- Нет Dagger/Hilt/Koin — кастомный DI

---

## 6. UI/UX

### AppTheme / ThemeSwitcher
```kotlin
sealed interface AppTheme {
  object colors: Colors
  object typography: Typography
}

class ThemeSwitcher(initial: ThemeSwitcher.Choice) : StateMachine<ThemeSwitcher.Choice, Unit>()
```

### uikit — кастомные виджеты
- `AppScreen` — обёртка над Compose Scaffold
- `Action` — кнопка с иконкой (drawable)
- `SentenceRow` — строка с иконкой, заголовком, подзаголовком
- `VSpacer` — вертикальный спейсер
- `ProfileBorderState` — Inactive/Testing/Connected/Failed (border для карточек)
- `AppLoading` — overlay loading indicator

### Compose
- Material 3
- PullToRefreshBox
- LazyColumn с stickyHeader
- `LocalWindowInfo.current.containerSize.height.dp` — адаптивный layout

### HomeSelectScreen — главный экран
- `@Composable fun HomeSelect(scope: ScreenScope<...>)`
- `@Stable class HomeSelectStateSink(scope: ScreenScope<...>, scopeName: String, initial: STATE)`
- `ScreenCommand` sealed class: `Start`, `Stop`, `Select`, `Refresh`
- `AppState` / `ScreenState` — состояния

---

## 7. Сеть и API

### Ktor Client
- `HttpClient` с `CIO` engine (включён `HttpTimeout`, `Logging`)
- `AuthPlugin` для Basic Auth (WebDAV)
- `AuthHeaderPlugin` для авторизации API-запросов
- `RetryPlugin` — повторные запросы
- `UserAgentHeaderPlugin` — кастомный User-Agent

### OKHttp
- `OkHttpClient` с `OkHttpSseClient` для SSE
- `DnsResolver` — кастомный DNS (для обхода DPI)
- `ProxySelector` — прокси-селектор

### WebDAV Client
```kotlin
class WebDavClient(
  val url: String,
  val login: String,
  val password: String,
) {
  suspend fun fetch(url: String): String
  suspend fun upload(url: String, content: String)
  suspend fun delete(url: String)
  suspend fun move(from: String, to: String)
}
```

### Error-классы (AppError)
```kotlin
sealed class AppError : Exception() {
  data class UnexpectedError(cause, message)
  sealed class ServerError {
    data object TimeOut, ConnectionFailed
    data class HttpRequestFailed(statusCode: Int)
  }
  sealed class WebDav {
    data object Unauthorized, Forbidden, Conflict, InvalidPropfindResponse, UploadOpenFailed
    data class NotFound(requestedUrl: String?)
  }
}
```

---

## 8. Подписки и профили

### Subscription
- `SubscriptionCache(key, item)` — кеш подписок
- `SubscriptionItem` — `id`, `name`, `url`, `uri`
- `SubscriptionCache.update(subscriptionItem: SubscriptionItem)` — обновление
- `SubscriptionCache.isEmpty()` — проверка на пустоту
- `SubscriptionCache.isEmptyOrNoUrl()` — если нет URL
- `SubscriptionCache.isNew()` — проверка на новую

### ConnectionProfile
- `protocol: Protocol` — тип протокола (VLESS, VMess, Trojan, Shadowsocks, etc.)
- `server: String?`, `serverPort: Int?`, `flow: String?`
- `network: String?` — transport (ws, grpc, kcp, etc.)
- `settings: Map<String, Any?>` — параметры подключения
- `remarks: String` — имя профиля

---

## 9. Обнаруженные проблемы и риски

### Критические

| # | Проблема | Описание |
|---|----------|----------|
| 1 | `usesCleartextTraffic = true` | Разрешён HTTP-трафик. Если профиль содержит HTTP-URL — данные могут утечь |
| 2 | Нет тестов | `enableUnitTest = false`, `enableAndroidTest = false`, нет `src/test` / `src/androidTest` |
| 3 | `MULTI_PROCESS_MODE` MMKV | MMKV с `MULTI_PROCESS_MODE` блокирует основной поток при каждом `decodeString`/`encodeString` (read/write) — на устройствах с медленной flash-памятью это может вызывать фризы при частых операциях |
| 4 | Нет обработки ошибок в Router | Router использует `Build`/`Sink`, но нет `catch`/`retry`/`fallback` паттернов для обработки сетевых ошибок при загрузке профилей |
| 5 | Нет версионирования storage | Нет механизма миграции при изменении схемы `KeyValueStorage` — при обновлении версии приложения данные могут остаться в старом формате |
| 6 | `initSubsList()` — lazy init с мутацией | `initSubsList()` вызывает `encodeSubsList()`, что мутирует storage при первом чтении — может привести к race condition в мультипроцессном режиме |

### Серьёзные

| # | Проблема | Описание |
|---|----------|----------|
| 7 | Кастомный DI вместо Dagger/Hilt | `ApplicationScope` — это фактически Singleton DI. Нет проверки на утечку контекста, нет `@Singleton`-аннотаций, нет dependency graph |
| 8 | `encodeServerConfig` — генерация UUID | `Utils.getUuid()` возвращает `String?`, fallback `p${System.nanoTime()}` может породить коллизии |
| 9 | `removeInvalidServer` — full scan | `allKeys()?.forEach` — полный обход storage при очистке невалидных серверов. При 1000+ профилей это O(n) операция |
| 10 | Нет `dataExtractionRules` для sensitive данных | `dataExtractionRules = @xml/data_extraction_rules` — нужно проверить, что sensitive data (профили, WebDAV credentials) не попадает в бэкап |
| 11 | `@xml/network_security_config` | Нужна проверка: что `cleartextTrafficPermitted="true"` для доверенных источников только |
| 12 | Нет `ConsumerProguardRules` для core | `core/proguard-rules.pro` — но нет `consumer-rules.pro` как в `v2ray-engine`. При использовании core как library — проguard может удалить публичные классы |
| 13 | `@xml/data_extraction_rules` | Нужно убедиться, что extraction excludes sensitive keys (профили, credentials) |

### Низкие

| # | Проблема | Описание |
|---|----------|----------|
| 14 | Нет `detekt.yml` | Только ktlint для статического анализа. Нет detekt для deeper code analysis (complexity, naming, etc.) |
| 15 | Нет `.editorconfig` в корне | Только в `app/.editorconfig` — другие модули не используют единый формат |
| 16 | Нет `gradle.properties` с `kotlin.code.style=official` | Нужно явно включить официальный Kotlin code style для ktlint |
| 17 | Нет `networkSecurityConfig` с pinning | Нет certificate pinning для WebDAV и API endpoints |
| 18 | `versionCode = 18` | Ручное управление versionCode — лучше автоматизировать через CI |
| 19 | Нет `build.gradle.kts` с `buildFeatures` | Нет `buildFeatures { compose true, views true }` — Compose включён через BOM, но явное указание в build-файле было бы понятнее |

---

## 10. Возможные улучшения

### Высокий приоритет

| # | Улучшение | Описание |
|---|----------|----------|
| 1 | Добавить unit-тесты | `enableUnitTest = true`, написать тесты для `KeyValueStorage`, `SettingsRepository`, `ConnectionProfileRepository`, `Router` |
| 2 | Внедрить версионирование storage | Добавить `storageVersion` key в `KeyValueStorage`, при несовпадении — миграция или warning |
| 3 | Настроить `dataExtractionRules` | Ограничить extraction для sensitive данных (профили, credentials, WebDAV) |
| 4 | Настроить `networkSecurityConfig` | Убрать `cleartextTrafficPermitted="true"` глобально, разрешить только для доверенных источников |
| 5 | Добавить `consumer-rules.pro` для core | Чтобы при использовании core как library проguard не удалял публичные классы |

### Средний приоритет

| # | Улучшение | Описание |
|---|----------|----------|
| 6 | Внедрить Dagger/Hilt | Замена кастомного DI на Hilt для типобезопасности и testability |
| 7 | Добавить detekt | `io.gitlab.arturbosch.detekt` для статического анализа (complexity, naming, performance) |
| 8 | Добавить `.editorconfig` в корень | Единый формат для всех модулей |
| 9 | Оптимизировать `removeInvalidServer` | Кэшировать `allKeys()` или использовать `decodeString(key)` с проверкой на null |
| 10 | Добавить `@OptIn` для experimental API | `@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)` где используется |

### Низкий приоритет

| # | Улучшение | Описание |
|---|----------|----------|
| 11 | Автоматизация versionCode | CI/CD для генерации versionCode |
| 12 | Добавить `kotlin.code.style=official` | В `gradle.properties` |
| 13 | Настроить `buildFeatures` | Явное указание `compose true, views true` в `app/build.gradle.kts` |
| 14 | Добавить certificate pinning | Для WebDAV и API endpoints |

---

## 11. Рекомендации по безопасности

### Критические

1. **Отключить `usesCleartextTraffic = true`** — заменить на `networkSecurityConfig` с разрешением только для доверенных источников
2. **Проверить `dataExtractionRules`** — убедиться, что sensitive data (профили, WebDAV credentials) не попадает в Android backup
3. **Добавить `networkSecurityConfig` с `cleartextTrafficPermitted="false"`** по умолчанию и `exception` для доверенных источников
4. **Проверить `@xml/backup_rules`** — убедиться, что sensitive data исключена из бэкапа
5. **Убрать `MULTI_PROCESS_MODE`** из MMKV — использовать `SINGLE_PROCESS_MODE` для основного процесса, или использовать `MMKV.initialize(context, MMKV.SINGLE_PROCESS_MODE)`

### Средние

6. **Добавить certificate pinning** для WebDAV и API endpoints
7. **Проверить `@xml/network_security_config`** — убедиться, что `cleartextTrafficPermitted` разрешён только для конкретных доменов
8. **Проверить `@xml/data_extraction_rules`** — исключить sensitive keys из extraction
9. **Добавить `@xml/backup_rules`** с `fullBackupContent` и `exclude` для sensitive data

---

## 12. Рекомендации по архитектуре

### Высокий приоритет

1. **Внедрить Dependency Injection** (Hilt/Dagger) — заменить кастомный `ApplicationScope`
2. **Добавить Unit-тесты** — `enableUnitTest = true`, написать тесты для ключевых классов
3. **Внедрить версионирование storage** — для миграции данных при обновлении
4. **Настроить `networkSecurityConfig`** — убрать `usesCleartextTraffic`

### Средний приоритет

5. **Добавить `detekt`** для статического анализа
6. **Оптимизировать `removeInvalidServer`** — кэширование `allKeys()`
7. **Добавить `@OptIn` для experimental API** — для `ExperimentalTime`, `ExperimentalCoroutinesApi`
8. **Настроить `.editorconfig` в корне** — единый формат для всех модулей

### Низкий приоритет

9. **Автоматизация versionCode** — через CI/CD
10. **Добавить `kotlin.code.style=official`** — в `gradle.properties`
11. **Явное указание `buildFeatures`** — в `app/build.gradle.kts`
12. **Certificate pinning** — для WebDAV и API endpoints

---

## 13. Итоговые выводы

### Сильные стороны проекта

1. **Чистая архитектура** — Feature-based модульность, Layered DDD (data / domain / ui)
2. **Type-safe Router** — `Router<STATE, COMMAND>` с `StateChanges` и `redirect`
3. **State-driven UI** — `ScreenScope` с `SharedFlow<STATE>`, `send(command)`, `sendEvent(effect)`
4. **Clean DI** — `ApplicationScope` с lazy init
5. **MMKV storage** — быстрая сериализация/десериализация
6. **Typed Settings** — `SettingsRepository` с `Setting<T>` и `MutableStateFlow`
7. **Sealed classes** — `Command`, `StateChanges`, `AppError`, `WorkState`
8. **uikit** — переиспользуемые компоненты (`AppScreen`, `Action`, `SentenceRow`, `VSpacer`)
9. **Ktor client** — типобезопасный HTTP-клиент с `CIO` engine
10. **WebDAV support** — `WebDavClient` для синхронизации профилей

### Слабые стороны проекта

1. **Нет тестов** — `enableUnitTest = false`, `enableAndroidTest = false`
2. **Нет версионирования storage** — нет механизма миграции при изменении схемы
3. **`MULTI_PROCESS_MODE` MMKV** — может вызывать фризы при частых операциях
4. **`usesCleartextTraffic = true`** — разрешён HTTP-трафик
5. **Кастомный DI** — нет Dagger/Hilt/Koin, нет проверки на утечку контекста
6. **Нет `detekt`** — только ktlint для статического анализа
7. **Нет `.editorconfig` в корне** — другие модули не используют единый формат
8. **`initSubsList()` — lazy init с мутацией** — может привести к race condition
9. **`removeInvalidServer` — full scan** — O(n) операция при 1000+ профилей
10. **Нет `consumer-rules.pro` для core** — при использовании core как library проguard может удалить публичные классы

### Оценка проекта

| Критерий | Оценка (1-10) |
|----------|---------------|
| Архитектура | 9 |
| Безопасность | 6 |
| Тестирование | 2 |
| Поддержка | 8 |
| Производительность | 7 |
| Масштабируемость | 8 |
| Документация | 7 |

**Общая оценка: 7.0 / 10**

Проект имеет чистую архитектуру и хорошую структуру, но требует улучшения безопасности, тестирования и версионирования storage.

---

## 14. Приложение: Ключевые файлы для аудита

```
E:/AndroidProject/arkanzabel/
├── app/
│   ├── build.gradle.kts
│   └── src/main/java/com/thindie/rknzbl/
│       ├── Application.kt
│       ├── MainActivity.kt
│       ├── application/di/ApplicationScope.kt
│       └── feature/
│           ├── home/
│           │   ├── HomeFlow.kt
│           │   ├── data/
│           │   │   ├── ConnectionProfileRepositoryImpl.kt
│           │   │   └── di/HomeFlowModule.kt
│           │   ├── domain/
│           │   │   └── ConnectionProfileRepository.kt
│           │   └── ui/select/
│           │       ├── HomeSelectScreen.kt
│           │       ├── HomeSelectStateSink.kt
│           │       ├── ScreenCommand.kt
│           │       ├── ScreenState.kt
│           │       └── HomeSelectRoute.kt
│           └── settings/
│               ├── data/
│               │   ├── SettingsRepositoryImpl.kt
│               │   └── di/SettingsFlowModule.kt
│               ├── domain/
│               │   └── SettingsRepository.kt
│               └── ui/
│                   └── (SettingsScreen.kt не найден)
├── core/
│   ├── build.gradle.kts
│   └── src/main/java/com/thindie/engine/core/
│       ├── Router.kt
│       ├── ScreenFlow.kt
│       ├── ScreenScope.kt
│       ├── Transition.kt
│       ├── Command.kt
│       ├── WorkState.kt
│       ├── Deeplink.kt
│       ├── Log.kt
│       ├── ScreenScopeError.kt
│       └── uikit/AppTheme.kt
├── v2ray-engine/
│   ├── build.gradle.kts
│   └── src/main/java/com/v2ray/ang/runtime/KeyValueStorage.kt
└── gradle/
    └── libs.versions.toml
```

---

*Аудит выполнен: 2026-08-29*  
*Всего файлов в проекте: 1252 `.kt`*  
*Модулей: 3 (app, core, v2ray-engine)*
