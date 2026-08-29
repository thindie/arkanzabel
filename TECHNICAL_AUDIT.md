# Технический аудит Arkanzabel (v2)

> **Версия документа:** 2.0 — переработанная версия аудита от `902b063`.
> Все утверждения сверены с кодом построчно; ссылки вида `файл:строка` актуальны на дату правки.
> Раздел «Исправления относительно версии 1» — в конце документа.

## 1. Общая оценка

**7/10 (взвешенно).** Продукт с сильным сетевым слоем и слабым хранением секретов.

| Критерий | Вес | Балл | Комментарий |
|---|---|---|---|
| Архитектура и модульность | 25% | 8/10 | Чистое разделение app / core / v2ray-engine, DI-модули, Flow-ориентированность |
| Сетевой слой / anti-DPI | 30% | 9/10 | Fragment/noise/keepalive/MUX-политика, fakedns, kill-switch — уровень v2rayNG+ |
| Безопасность данных (хранилище, бэкап) | 20% | 4/10 | MMKV plaintext + `allowBackup=true` с пустыми правилами |
| Тестирование | 10% | 3/10 | Один тест-файл в репозитории; критичный runtimebuilder не покрыт |
| Сборка и гигиена | 15% | 6/10 | Актуальные версии, ktlint; мёртвые proguard-правила, неиспользуемый OKHttp |

Взвешенная сумма: `8·0.25 + 9·0.30 + 4·0.20 + 3·0.10 + 6·0.15 = 6.85 ≈ 7/10`.
(Версия 1 документа давала `(9+6+2+8+7+8+7)/7 = 6.7` при заявленных «7.0» — арифметика не сходилась.)

## 2. Структура проекта (подтверждено)

- **Модули** (`settings.gradle.kts`): `:app`, `:core`, `:v2ray-engine`.
- **1252 `.kt`-файла** (без `build/`).
- **Версии** (`gradle/libs.versions.toml`): AGP 8.13.2, Kotlin 2.3.20, Compose BOM 2026.03.00, Ktor 3.4.2, OKHttp 5.3.2, MMKV 1.3.16, Coroutines 1.10.2.
- **app** (`app/build.gradle.kts`): minSdk **24**, versionCode **1** / versionName **"1.0"**, `jvmTarget = JVM_17` (toolchain не задан), `buildFeatures { compose; buildConfig }`, в release `isMinifyEnabled = false`.
- **Тесты:** единственный файл — `core/src/test/java/com/thindie/engine/core/RouterScopeTest.kt`. В `app` и `v2ray-engine` тестовых директорий нет.
- **Линтеры:** ktlint (задача `ktLintFormat`); detekt не подключён. `.editorconfig` есть в корне и в `app/`; `kotlin.code.style=official` задан в `gradle.properties`.

## 3. Критические проблемы

### C1. Секреты профилей хранятся в plaintext (MMKV)
7 инстансов MMKV в `MULTI_PROCESS_MODE` (`KeyValueStorage.kt:41-52`): MAIN, PROFILE_FULL_CONFIG, SERVER_RAW, SERVER_AFF, SUB, ASSET, SETTING — каталог `files/mmkv/`. В них без шифрования лежат полные конфиги серверов (ключи VLESS/SS/WireGuard, SNI, UUID), подписки и их токены. Любая физическая потеря устройства или root = полный дамп аккаунтов.
**Рекомендация:** зашифровать чувствительные поля (AES-GCM с ключом из Android Keystore) либо минимум — исключить каталог `mmkv/` из бэкапов и device-transfer (см. C2).

### C2. `allowBackup="true"` + пустые правила бэкапа
`app/src/main/AndroidManifest.xml:16`: `android:allowBackup="true"`. Файлы `backup_rules.xml` / `data_extraction_rules.xml` — чистый шаблон Android Studio, все строки закомментированы. Ответ на вопрос «что уйдёт в бэкап?» уже известен: по умолчанию всё, включая `files/mmkv/` (C1) и `hev-socks5-tunnel.yaml`.
**Рекомендация:** `allowBackup="false"` либо явные `<exclude domain="file" path="mmkv"/>` + `<exclude ... path="hev-socks5-tunnel.yaml"/>`.

## 4. Серьёзные проблемы

### S1. Глобальный cleartext + доверие user-CA
Единственный исходный network-security config — `v2ray-engine/src/main/res/xml/network_security_config.xml` (у модуля app своего nsc нет): `cleartextTrafficPermitted="true"` глобально и `<certificates src="user" />`. Плюс `usesCleartextTraffic="true"` (`app/.../AndroidManifest.xml:26`).
Для VPN-клиента доверие user-CA — осознанный выбор (MITM-прокси, корпоративные сертификаты), но он **не задокументирован** ни комментарием в коде, ни в README. Риск: пользователь не понимает, что любой установленный CA видит весь трафик приложения до туннеля.
**Рекомендация:** комментарий в манифесте/конфиге + пункт в описании приложения; рассмотреть `cleartextTrafficPermitted="false"` для доменов API (подписки), оставив cleartext только там, где это необходимо.

### S2. DNS-утечка при предрезолвинге сервера
`PREF_OUTBOUND_DOMAIN_RESOLVE_METHOD` по умолчанию `"1"` (`ConfigAssembler.kt:52`, второй вызов — `V2rayConfigManager.kt:647`). При значении `"1"` срабатывает `DomainResolveStep.resolveOutboundDomainsToHosts()` → `HttpUtil.resolveHostToIP()` → **`InetAddress.getAllByName(host)`** (`HttpUtil.kt:55`) — системный DNS. Домен VPN-сервера уходит в резолв **до поднятия туннеля**, т.е. локальный провайдер/DPI видит, к какому серверу пользователь собирается подключаться.
**Рекомендация:** по умолчанию отключать предрезолвинг (метод `"0"`) или резолвить через уже установленный SOCKS-инбанд Xray; как минимум — явно предупреждать в UI о последствиях метода `"1"`.

### S3. Дублирование `AppError`
Две независимые sealed-иерархии: `app/src/main/java/com/thindie/rknzbl/error/AppError.kt:3` и `v2ray-engine/src/main/java/com/v2ray/ang/error/AppError.kt:5`. Ошибки движка не типизированы на уровне app — маппинг строками.
**Рекомендация:** единая модель ошибок в `core`, адаптеры на границе модулей.

### S4. Ktor без единой обработки HTTP-ошибок
HTTP-клиент (`ConnectionProfileRepositoryImpl.kt`) — голый `HttpClient(CIO)` + `HttpTimeout`: нет `expectSuccess()`, нет плагинов Logging/Retry/Auth, WebDAV-запросы (fetch/upload/delete) инлайном в репозитории. Ошибки 401/403/404 разбираются ad-hoc по кодам ответов.
**Рекомендация:** `expectSuccess()` + типизированные исключения на границе; вынести WebDAV-операции в отдельный клиент с тестами.

## 5. Средние проблемы

| # | Проблема | Где | Комментарий |
|---|---|---|---|
| M1 | `initSubsList()` мутирует storage при чтении | `KeyValueStorage.kt:~198-202` | Пустой список → запись в storage; чтение с побочным эффектом |
| M2 | Полный обход `allKeys()` при удалении невалидного сервера | `KeyValueStorage.kt:~167-175` | MMKV держит ключи в памяти — дёшево, но это code smell; лучше индекс по профилю |
| M3 | Fallback UUID = `"p${System.nanoTime()}"` | `KeyValueStorage.kt:89` | Предсказуемый идентификатор при ошибке генерации |
| M4 | Конфиг туннеля пишется без прав 600 и логируется | `TProxyService.kt:~50-57` | `hev-socks5-tunnel.yaml` в `filesDir` с дефолтными правами; активный `Log.d` выводит содержимое конфига. Рекомендация: `setReadable(true,false)` + убрать лог конфига |
| M5 | Нативные артефакты вне git | `v2ray-engine/libs/.gitignore:2` (`*`) | `libv2ray.aar`, `.so` не в VCS, контрольных сумм нет — сборка нерепродуцируема из репозитория. Рекомендация: фиксировать SHA-256 артефактов в файле рядом |
| M6 | Мёртвая конфигурация сборки | `app/build.gradle.kts` (release) | `isMinifyEnabled = false`, при этом proguard/consumer-rules существуют, но пусты. Либо включить R8, либо удалить правила |
| M7 | OKHttp 5.3.2 объявлен, но не используется | `libs.versions.toml` vs источники | Гrep по `okhttp3` в app/core/v2ray-engine пуст — лишний вес APK и поверхность обновлений |

## 6. Что версия 1 аудита пропустила (дополнение)

### 6.1 Нативный слой
- Прямой режим: TUN-fd передаётся в Xray напрямую — `Libv2ray.initCoreEnv(assetPath, deviceId)` → `coreController.startLoop(json, tunFd)` (`V2RayNativeManager.kt`).
- HEV-режим (proxy-only): `TProxyService` загружает `System.loadLibrary("hev-socks5-tunnel")`, пишет `files/hev-socks5-tunnel.yaml` и запускает userspace TUN→SOCKS5 (`Tun2SocksControl.kt`: «v2rayNG parity»), Xray подключается к локальному SOCKS-инбанду. Режим **выключен по умолчанию** (комментарий `SettingsManager.kt:268`): без bundled `.so` используется прямой TUN-fd → core.
- Kill-switch: `V2RayVpnService.onRevoke()` (`:88`) — при отзыве разрешения VPN трафик блокируется; MTU из настроек (`:265`); bypass для самого приложения и выбранных пакетов через `addDisallowedApplication` (`:341, 349, 361`).

### 6.2 DNS внутри Xray
`DnsConfigStep.kt`: включён `fakedns` (`:20`) — нерезолвлённые домены получают фейковые IP и резолвятся внутри туннеля; раздельные пулы domestic/remote DNS-серверов (`:89-110`); поддержка host-replacements. Это закрывает утечку пользовательских доменов, но **не** закрывает S2 (предрезолвинг адреса самого сервера).

## 7. Сильные стороны (в версии 1 недооценены)

### 7.1 Anti-DPI — реализован качественно
`OutboundConfigStep.kt`:
- TCP keepalive на outbound: `tcpKeepAliveIdle = OUTBOUND_TCP_KEEPALIVE_IDLE_SECONDS` (`~:127`) — защита от закрытия «мёртвых» соединений и их DPI-детекта;
- Fragmentation: `packets=tlshello`, для REALITY автоматически `"1-3"` (`:193-197`), `length 50-100`, `interval 10-20`;
- Noise: `type=rand`, `delay 10-16` (`:211-213`);
- MUX **выключен по умолчанию** и принудительно отключается для SS/Socks/Http/Trojan/WG/Hysteria2/xHTTP; для VLESS+flow — `concurrency = -1` (безопасный режим) (`:93-122`).

### 7.2 Happy Eyeballs
В `DomainResolveStep`: при наличии резолва выставляется `domainStrategy=UseIP` + `HappyEyeballs(prioritizeIPv6, interleave=2)` — корректная работа с dual-stack серверами.

### 7.3 Архитектура (исправленное описание)
Реальный API навигации (`core/src/main/java/com/thindie/engine/core/Router.kt`): `class Router(val onPopLast: () -> Unit)` поверх `MutableStateFlow<List<Route>>`, операции `push/pop/replaceTop`, публичное состояние — `route: StateFlow<Pair<Route, Route?>>`. Состояния загрузки — `WorkState.Error(message, cause)`.
⚠️ Версия 1 описывала API (`Command<STATE, COMMAND>`, `Build`/`Sink`, `Failed(error)`, `Deeplink.Type.OpenProfile/AddProfile`), **которого в коде нет** — см. раздел «Исправления».

## 8. Pre-release чек-лист (по приоритету)

1. [ ] C2: `allowBackup="false"` либо явные `<exclude>` для `mmkv/` и `hev-socks5-tunnel.yaml`.
2. [ ] C1: шифрование чувствительных полей MMKV (Keystore + AES-GCM).
3. [ ] S2: предрезолвинг — выключить по умолчанию или резолвить через прокси.
4. [ ] M4: права 600 на `hev-socks5-tunnel.yaml`, убрать `Log.d` с содержимым конфига.
5. [ ] S1: задокументировать доверие user-CA; сузить cleartext по доменам API.
6. [ ] M7: удалить неиспользуемую зависимость OKHttp (или использовать её осознанно).
7. [ ] Tесты для `runtimebuilder` (ConfigAssembler/Dns/DomainResolve steps) — это самый безопаскритичный код без покрытия.

## 9. Исправления относительно версии 1 документа

Версия 1 содержала ошибки, исправленные в этой ревизии:

| Было в v1 | Стало / факт |
|---|---|
| «6 критических проблем» | Критических — **2** (C1, C2); остальное переклассифицировано в серьёзные/средние |
| minSdk 28, versionCode 18 / "3.1.5" | minSdk **24**, versionCode **1** / **"1.0"** (`app/build.gradle.kts`) |
| `jvmToolchain = 21` | Toolchain не задан; `jvmTarget = JVM_17` |
| Флаги `enableUnitTest=false`, `enableAndroidTest=false` | Таких флагов в проекте нет (выдуман механизм); факт: один тест-файл `RouterScopeTest.kt` |
| Секция Router: `Command<STATE,COMMAND>`, `Build/Sink/StateChanges`, `Failed(error)`, `Deeplink.Type.OpenProfile/AddProfile` | В коде — `Router(onPopLast)` на `MutableStateFlow<List<Route>>`, `WorkState.Error(message, cause)`; описанного API не существует |
| Сеть: Ktor-плагины Logging/Retry/UserAgent/Auth, `OkHttpSseClient`, кастомный DnsResolver OKHttp, `ProxySelector` | В коде только `HttpClient(CIO)` + `HttpTimeout`; плагинов нет; **OKHttp в источниках не используется вообще** |
| «Класс `WebDavClient(url, login, password)`» | Отдельного класса нет — WebDAV-операции инлайном в `ConnectionProfileRepositoryImpl.kt` |
| #15 «нет .editorconfig», #16 «нет kotlin.code.style=official», #19 «нет buildFeatures» | Все три утверждения неверны: `.editorconfig` есть (корень + app), стиль задан, `buildFeatures { compose; buildConfig }` присутствует |
| MMKV MULTI_PROCESS_MODE → «блокирует main-поток при каждом decode/encode» | Механизм описан неверно: MMKV работает через mmap, мультипроцессный режим добавляет file-lock для межпроцессной безопасности (он нужен — VPN-сервис живёт в своём процессе). Как критическая проблема производительности не подтверждается |
| `removeInvalidServer` O(n) — «серьёзная» | Понижено до средней: MMKV хранит ключи в памяти, обход дёшев |
| Итог 7.0 при арифметике 6.7 | Явная взвешенная формула (раздел 1), сумма 6.85 ≈ 7/10 |
| VPN-трафик практически не рассмотрен | Добавлены: нативный слой, HEV/TUN-fd режимы, kill-switch, fakedns/DNS-split, anti-DPI параметры (разделы 6–7) |
