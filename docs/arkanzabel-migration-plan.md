# Arkanzabel ← v2rayNG Technical Migration Plan

## Проверка и статус верификации (pipeline)

**Дата прохода:** 2026-03-22 — сверка деревьев `d:\Android\v2rayNg` и `d:\Android\Arkanzabel` без выдуманных путей.

### Подтверждено (v2rayNg)

| Утверждение в плане | Проверка |
|---------------------|----------|
| Модуль `V2rayNG/app`, `namespace` / `applicationId` `com.v2ray.ang` | `V2rayNG/app/build.gradle.kts` |
| `AppConfig.ANG_PACKAGE` = `BuildConfig.APPLICATION_ID` | `AppConfig.kt:7-8` |
| Сервисы VPN/прокси/тест/тайл в `android:process=":RunSoLibV2RayDaemon"` | `AndroidManifest.xml` строки 155–227 |
| `BootReceiver` без отдельного процесса | манифест `receiver` ~205 |
| `WorkManager` + `:bg` в `AngApplication` | `AngApplication.kt:25-41` |
| `handler` — ровно **12** `.kt` файлов | glob `handler/*.kt` |
| `fmt` — **10** файлов (`FmtBase` … `WireguardFmt`) | glob `fmt/*.kt` |
| `util` — **8** файлов (`MessageUtil`, `Utils`, `JsonUtil`, `HttpUtil`, `ZipUtil`, `AppManagerUtil`, `MyContextWrapper`, `QRCodeDecoder`) | glob `util/*.kt` |
| `dto` — **17** файлов | `AssetUrlItem`, `AssetUrlCache`, `AppInfo`, `CheckUpdateResult`, `ConfigResult`, `GitHubRelease`, `GroupMapItem`, `IPAPIInfo`, `ProfileItem`, `RulesetItem`, `ServerAffiliationInfo`, `ServersCache`, `SubscriptionCache`, `SubscriptionItem`, `VmessQRCode`, `V2rayConfig`, `WebDavConfig` |
| `contracts` — 4 файла (`ServiceControl`, `Tun2SocksControl`, `MainAdapterListener`, `BaseAdapterListener`) | glob `contracts/*.kt` |
| `extension` — **`_Ext.kt`** | `extension/_Ext.kt` |
| Assets в `V2rayNG/app/src/main/assets/` | 9 файлов: `v2ray_config.json`, `v2ray_config_with_tun.json`, `custom_routing_*` (5), `proxy_package_name`, `open_source_licenses.html` |
| hev `PKGNAME` | `compile-hevtun.sh` строка с `PKGNAME=com/v2ray/ang/service` |

### Подтверждено / исправлено (Arkanzabel)

| Утверждение | Проверка |
|-------------|----------|
| `include(":v2ray-engine")`, app → `implementation(project(":v2ray-engine"))` | `settings.gradle.kts`, `app/build.gradle.kts` |
| `applicationId` / UI `namespace` = `com.thindie.rknzbl` | `app/build.gradle.kts` |
| `Application` = `com.thindie.rknzbl.application.Application` | манифест + `Application.kt` |
| Library `namespace` = `com.thindie.rknzbl.v2rayengine` | `v2ray-engine/build.gradle.kts` |
| `AppConfig.initHostApplicationId(packageName, …)` в `onCreate` | `Application.kt:16` |
| MMKV: **`KeyValueStorage.initialize(this)`** вызывает `MMKV.initialize` | `KeyValueStorage.kt:20-22` — это **отличие** от оригинального `MmkvManager`-only пути; план допускает эволюцию слоя хранилища |
| **WorkManager.initialize** с `:bg` в host `Application` | **отсутствует** в текущем `Application.kt` — нужно добавить до использования `SubscriptionUpdater` / `RemoteWorkManager` (как в `AngApplication`) |
| Ресурсы engine: `res/xml/network_security_config.xml`, `cache_paths.xml`, `raw/licenses.xml`, строки, `drawable/ic_stat_name`, набор **assets** (как в upstream) | glob `v2ray-engine/src/main` |
| Манифест library: permissions + `V2RayVpnService`, `V2RayProxyOnlyService`, `V2RayTestService`, `QSTileService`, `BootReceiver`, `InitializationProvider` (remove WM init), `FileProvider` | `v2ray-engine/src/main/AndroidManifest.xml` |

### Критическое расхождение (блокер сборки)

- В **`v2ray-engine`** объявлены **`<service>` / `<receiver>`** на классы `com.v2ray.ang.service.*` и `com.v2ray.ang.receiver.BootReceiver`, но каталога **`v2ray-engine/.../com/v2ray/ang/service/`** и **`receiver/`** в исходниках **нет** (поиск `class V2RayVpnService` — пусто).  
- **Вывод:** план корректен по целевой архитектуре, но текущее состояние репозитория — **манифест опережает Phase 3**; до переноса Kotlin-классов сборка модуля будет недействительна. В плане Phase 3 следует трактовать как **«закрыть этот разрыв»**.

### Замечания по переносимости

- В Arkanzabel расширения: **`KotlinExtensions.kt`** вместо upstream **`_Ext.kt`** — при мерже новых коммитов v2rayNg сверять вручную.
- **`LocaleContextWrapper.kt`** в engine соответствует роли **`MyContextWrapper.kt`** upstream (имена различаются).

### Pipeline

1. **feature-plan-reviewer** — сверка плана с файловой системой и манифестами; расхождения внесены в этот раздел.  
2. **feature-executor** — обновлён только документ `arkanzabel-migration-plan.md`, код проектов не менялся.

---

## Overview

- **Цель**: вынести «технику» v2rayNG (сервисы, обработчики, нативный мост Xray, MMKV, фон, конфиг) в модуль **`v2ray-engine`**; приложение Arkanzabel остаётся на **Compose** (`com.thindie.rknzbl`).
- **Лицензия**: исходный проект v2rayNg — **GPLv3** (`LICENSE` в корне); публичный репозиторий и уведомления — см. `open_source_licenses.html`, `res/raw/licenses.xml` в оригинальном приложении.
- **Архитектура v2rayNG** (если нужен разбор слоёв): при необходимости восстановите отдельно `docs/tmp/v2rayng-architecture-plan.md` по этому же репо.

## Source and target

| Роль | Путь / модуль |
|------|----------------|
| Исходное приложение | `V2rayNG/app` (`com.v2ray.ang`) |
| Go / gomobile | `AndroidLibXrayLite/` → AAR `libv2ray` |
| hev | `hev-socks5-tunnel/`, сборка `compile-hevtun.sh` |
| Нативные артефакты в app | `V2rayNG/app/libs` (`jniLibs.srcDirs("libs")`, `fileTree` `*.aar`, `*.jar`) |
| Целевое приложение | `d:\Android\Arkanzabel` — модуль `:app` |
| Целевая библиотека | `:v2ray-engine` (уже подключена к `:app`) |

## New modules (Arkanzabel)

- **`v2ray-engine`**: `com.android.library`. Сверить `minSdk` / `compileSdk` / JVM с `:app` (сейчас app: Java 21; v2rayNg app: Java 17 — либо унифицировать, либо оставить 21 в engine).
- Добавить в `v2ray-engine/build.gradle.kts` по мере переноса: **desugaring** (если нужен как в v2rayNg), **multidex** при росте методов, `jniLibs`, `fileTree` для AAR, зависимости: MMKV, Gson, OkHttp, coroutines, WorkManager + **multiprocess**, lifecycle, при необходимости Toasty / Material — только если код движка тянет.
- **Манифест library**: перенос mergeable `<service>`, `<receiver>`, `<provider>` из `V2rayNG/app/src/main/AndroidManifest.xml` (без старых `activity` UI).

## Package strategy

- **Рекомендация**: оставить **`com.v2ray.ang.*`** для переносимого кода движка (минимум правок, hev без пересборки).
- **Gradle `namespace`** библиотеки (`com.thindie.rknzbl.v2rayengine`) **не обязан** совпадать с Kotlin-пакетом исходников.
- **`AppConfig.ANG_PACKAGE`**: в library **нельзя** использовать `BuildConfig.APPLICATION_ID` библиотеки как id приложения. Уже принятый паттерн в Arkanzabel: **`AppConfig.initHostApplicationId(applicationId, versionName)`** в `Application.onCreate` до любого кода, который шлёт broadcast или строит имя процесса `:bg`. От этого зависят `BROADCAST_ACTION_*` и `WorkManager.setDefaultProcessName("${ANG_PACKAGE}:bg")`.
- **Переименование пакетов** под `com.thindie…`: возможно, но для hev нужен новый **`PKGNAME`** в ndk-build **или** тонкий shim **`com.v2ray.ang.service.TProxyService`**.

## Migrate list

Источник: `V2rayNG/app/src/main/java/…`

### `com.v2ray.ang.handler` (12 файлов в upstream)

- `AngConfigManager`, `MmkvManager`, `NotificationManager`, `SettingsChangeManager`, `SettingsManager`, `SpeedtestManager`, `SubscriptionUpdater` (+ `UpdateTask`), `UpdateCheckerManager`, `V2RayNativeManager`, `V2RayServiceManager`, `V2rayConfigManager`, `WebDavManager`
- **Arkanzabel (осознанное решение):** upstream-`MmkvManager` **не** возвращаем в код. Единый слой персистентности — **`KeyValueStorage`** (те же MMKV ID / multi-process). При порте из v2rayNG все обращения к `MmkvManager` заменяются на **`KeyValueStorage`**; дублирующий класс `MmkvManager` в репозитории не вводить.

### `com.v2ray.ang.service`

- `V2RayVpnService`, `V2RayProxyOnlyService`, `V2RayTestService`, `QSTileService`, `TProxyService`, `ProcessService`
- `RealPingWorkerService` — **не** `<service>` в манифесте; вспомогательный класс для `V2RayTestService`

### `com.v2ray.ang.contracts`

- `ServiceControl`, `Tun2SocksControl`  
- **Не переносить** в движок без необходимости UI: `MainAdapterListener`, `BaseAdapterListener`

### `com.v2ray.ang.dto`

- Все типы, от которых зависят handlers/config (в upstream есть в т.ч. `AssetUrlItem`, `AssetUrlCache`, `AppInfo`, `CheckUpdateResult`, `ConfigResult`, `GitHubRelease`, `GroupMapItem`, `IPAPIInfo`, `ProfileItem`, `RulesetItem`, `ServerAffiliationInfo`, `ServersCache`, `SubscriptionCache`, `SubscriptionItem`, `VmessQRCode`, `V2rayConfig`, `WebDavConfig` — сверять по факту импортов после копирования).

### `com.v2ray.ang.fmt`

- `FmtBase`, `CustomFmt`, `HttpFmt`, `Hysteria2Fmt`, `ShadowsocksFmt`, `SocksFmt`, `TrojanFmt`, `VlessFmt`, `VmessFmt`, `WireguardFmt`

### `com.v2ray.ang.enums`

- `EConfigType`, `Language`, `NetworkType`, `PermissionType`, `RoutingType`, `VpnInterfaceAddressConfig`

### `com.v2ray.ang.util`

- Минимум: `MessageUtil`, `Utils`, `JsonUtil`; по ссылкам из handlers: `HttpUtil`, `ZipUtil`, `AppManagerUtil`, `MyContextWrapper` / `LocaleContextWrapper` (как назовёте при мерже), `QRCodeDecoder` — в engine только если логика импорта QR остаётся в библиотеке

### `com.v2ray.ang.extension`

- Upstream: `_Ext.kt` — в Arkanzabel уже лежит **`KotlinExtensions.kt`** (тот же слой; при обновлениях с upstream сверять дифф).

### `com.v2ray.ang` (корень)

- `AppConfig` — адаптировать под host `applicationId` (как в текущем Arkanzabel `AppConfig.kt`)
- Логику из **`AngApplication`**: вынести в **`V2rayEngineInitializer`** / `object` с методом `init(application: Application)`, вызываемым из **`com.thindie.rknzbl.application.Application`**: `MMKV.initialize` (сейчас — через **`KeyValueStorage.initialize`**), **`WorkManager.initialize`** с `:bg` (**ещё не подключено** в `Application.kt` — добавить перед подписками), `SettingsManager.ensureDefaultSettings`, `SettingsManager.setNightMode` (или вынести тему в app), `SettingsManager.initRoutingRulesets`, `SettingsManager.migrateHysteria2PinSHA256`. Toasty — по желанию только в app.

### Receivers (опционально, паритет с v2rayNg)

- `com.v2ray.ang.receiver.BootReceiver`, `WidgetProvider`, `TaskerReceiver`

### Ресурсы

- **Assets**: `v2ray_config.json`, `v2ray_config_with_tun.json`, `custom_routing_*`, `proxy_package_name`, при необходимости `open_source_licenses.html`
- **res**: строки/иконки для уведомлений и FGS, `xml/network_security_config.xml`, `xml/cache_paths.xml` для FileProvider, `raw/licenses.xml` — переносить в engine или дублировать в app с `tools:replace` по соглашению команды

### Явно EXCLUDE

- `com.v2ray.ang.ui.*`
- `com.v2ray.ang.viewmodel.*` (переписать под Compose в `:app`)
- `com.v2ray.ang.helper.*` — по необходимости точечно (часть завязана на RecyclerView/Preference)

## Gradle and native

### libv2ray AAR

- Собрать из `AndroidLibXrayLite` (CI: `AndroidLibXrayLite/.github/workflows/main.yml`) или положить готовый AAR в **`v2ray-engine/libs/`**, зеркало паттерна `V2rayNG/app/build.gradle.kts`.

### hev

- Собрать `compile-hevtun.sh`, артефакты положить в `libs/` модуля engine (ABI как у приложения).

### Сборочные скрипты в репозитории (воспроизводимость)

- В дереве Arkanzabel **по умолчанию нет** `compile-hevtun.sh`, каталога **`hev-socks5-tunnel/`** и **`AndroidLibXrayLite/`** — ранее в плане фигурировали только **выходы** сборки (AAR и `.so` в **`v2ray-engine/libs`** / **`jniLibs`**), а сами скрипты живут во **v2rayNG** или рядом с ним.
- **Зафиксировать один подход** (и описать в `README` / `docs`):
  - **Git submodule** (или subtree) на upstream-репозитории `hev-socks5-tunnel` и при необходимости **`AndroidLibXrayLite`**; в Arkanzabel — краткая обёртка или документ с командами, идентичными upstream **`compile-hevtun.sh`** и workflow **`AndroidLibXrayLite/.github/workflows/main.yml`**;
  - либо **CI в Arkanzabel** (например GitHub Actions): те же шаги, что upstream, артефакты складывать в **`v2ray-engine`** или в артефакт-хранилище; в git при необходимости коммитить только бинарники — по политике команды.
- Вести учёт **NDK revision**, **ABI**, **commit SHA** upstream на момент каждой зафиксированной сборки (см. также `docs/COMPLIANCE.md`).

### ABI splits

- Опционально как во v2rayNg (`splits.abi`); для внутренней сборки часто один universal APK.

## Manifest merge checklist (в `:v2ray-engine` и/или `:app`)

Сверка с `V2rayNG/app/src/main/AndroidManifest.xml`:

- Permissions: `INTERNET`, `ACCESS_NETWORK_STATE`, `CHANGE_NETWORK_STATE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` (min 34), `POST_NOTIFICATIONS`, `QUERY_ALL_PACKAGES` (если per-app), storage/camera при QR и т.д., `RECEIVE_BOOT_COMPLETED`
- `V2RayVpnService`: `BIND_VPN_SERVICE`, `foregroundServiceType="specialUse"`, `android:process=":RunSoLibV2RayDaemon"`, meta/property для VPN / special use subtype
- `V2RayProxyOnlyService`, `V2RayTestService`, `QSTileService` — процесс и типы FGS
- `BootReceiver` (exported), `WidgetProvider` / `TaskerReceiver` при паритете
- `InitializationProvider` merge + **удаление** `WorkManagerInitializer` при ручном `WorkManager.initialize`
- `FileProvider` authority **`${applicationId}.cache`**
- `networkSecurityConfig`, `usesCleartextTraffic` — по политике Arkanzabel (сейчас в app уже есть `network_security_config`)

## Public API для Compose UI (`:app`)

Определить узкий фасад (интерфейс или object), например:

- Старт/стоп VPN или proxy-only через существующие **`V2RayServiceManager.startVService` / `stop`**
- Текущий профиль / список — **`KeyValueStorage`**
- Импорт URI — **`AngConfigManager`** (или упрощённый use-case)
- Состояние: подписка на broadcast через обёртку или перевод на **Flow** в одном месте

Внутри фасада остаются `MessageUtil`, `AppConfig`, сервисы без изменения контракта, пока не решите заменить IPC.

## Phased steps

### Phase 0 — Репозиторий и лицензии

- Добавить `LICENSE` / уведомления в корень Arkanzabel при публичной публикации; перенести/скопировать тексты лицензий зависимостей.

### Phase 1 — Завершить shell `v2ray-engine`

- Дописать `build.gradle.kts`: AAR, jniLibs, недостающие зависимости, при необходимости desugar/multidex.

### Phase 2 — Kotlin: handlers + fmt + недостающие util

- Скопировать из v2rayNg, починить импорты и **R** (ресурсы должны жить в engine или передаваться из app).
- Вызвать **`AppConfig.initHostApplicationId`** первым в `Application.onCreate`.

### Phase 3 — Сервисы + манифест

- Перенести Kotlin из `service/` и `receiver/` (и недостающие ресурсы), чтобы объявления в `v2ray-engine/src/main/AndroidManifest.xml` соответствовали классам. **Сейчас манифест уже частично скопирован — классов под него нет (блокер).** Проверить **процесс** `:RunSoLibV2RayDaemon`.

### Phase 4 — Нативка

- Выбрать и внедрить стратегию из раздела **«Сборочные скрипты в репозитории»** (submodule/CI/ручная сборка соседнего клона — но тогда задокументировать), чтобы нативка не была «магией с ноутбука одного разработчика».
- Подключить `libv2ray` и `libhev-socks5-tunnel.so` в Gradle (`fileTree` / `jniLibs`), прогнать старт на устройстве.

### Phase 5 — Compose MVP

- Кнопки старт/стоп, выбор одного профиля, обработка `VpnService.prepare`.

### Phase 6 — Паритет (по желанию)

- Тайл, виджет, Tasker, авто-подписки (`RemoteWorkManager` в настройках — как в `SettingsActivity` v2rayNg), hev-режим.

## Risks and mitigations

| Риск | Митигация |
|------|-----------|
| Неверный **`ANG_PACKAGE`** → broadcast не доходят между процессами | Ранний **`initHostApplicationId`**; не использовать library `BuildConfig.APPLICATION_ID` как id приложения |
| MMKV в нескольких процессах | `MMKV.initialize` в каждом процессе; хранилища с **`MULTI_PROCESS_MODE`** (как в `MmkvManager`) |
| FGS / `targetSdk 36` | Строго по манифесту типы и property special use |
| OEM / VPN | Ручной регресс на 2–3 устройствах |
| Разрыв **R** между app и engine | Вынести строки/иконки уведомлений в engine или использовать контекст app и `applicationId` |

## Dependencies and APIs (ориентир)

- **Xray**: `libv2ray.Libv2ray`, `libv2ray.CoreController`, `libv2ray.CoreCallbackHandler`, `go.Seq` — через `com.v2ray.ang.handler.V2RayNativeManager`
- **MMKV**: `com.tencent.mmkv.MMKV`
- **WorkManager**: `androidx.work`, `androidx.work.multiprocess.RemoteWorkManager` для постановки work из UI-процесса в `:bg`

---

*Документ: миграция техчасти v2rayNG → Arkanzabel (`:v2ray-engine` + Compose). Актуальная сводка проверки — в секции «Проверка и статус верификации (pipeline)».*
