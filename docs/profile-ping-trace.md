# Profile Ping: как измеряется задержка профиля (трассировка)

Трассировка пути измерения outbound-задержки для профилей подключения,
от `ProfilePingManager` до сетевого стека ядра Android.

## Цепочка вызовов

```
ProfilePingManager.pingProfiles / pingSaved
  └─ V2rayConfigManager.getV2rayConfig4Speedtest(context, profile|guid)
       └─ getV2rayNormalConfig4Speedtest          (V2rayConfigManager.kt:285)
            ├─ getOutbounds / getMoreOutbounds    — только proxy outbound(ы)
            └─ invalidateForSpeedtest             (V2rayConfigManager.kt:630)
  └─ V2RayNativeManager.measureOutboundDelay(json, url)   (V2RayNativeManager.kt:71)
       └─ Libv2ray.measureOutboundDelay(config, testUrl)  — JNI → Go (libgojni.so)
            └─ HTTP GET https://www.gstatic.com/generate_204 через proxy outbound
```

## Шаг 1. Минимальный speedtest-конфиг

`getV2rayNormalConfig4Speedtest` строит урезанный Xray-конфиг: только proxy
outbound (плюс доп. outbounds подписки), затем `invalidateForSpeedtest()`
вычищает всё лишнее:

```kotlin
inbounds.clear()      // нет TUN / локальных inbounds
routing.rules.clear()
dns = null            // резолв делает сам Go через системный резолвер
fakedns = null
stats = null
policy = null
outbound.mux = null   // без мультиплексинга — чистый замер RTT
```

## Шаг 2. Нативный замер (JNI)

`V2RayNativeManager.measureOutboundDelay` вызывает нативный
`Libv2ray.measureOutboundDelay(config, testUrl)` — gomobile-биндинг
(`libgojni.so`, пакет `github.com/2dust/AndroidLibXrayLite`). Внутри Go:
поднимается outbound из переданного JSON и через него делается настоящий
HTTP GET на URL. Возвращается elapsed в мс или `-1` при ошибке.

> Оговорка: Go-исходники биндинга в репозитории отсутствуют
> (`v2ray-engine/libs/` содержит только AAR + Java-стабы), реализация описана
> по апстриму AndroidLibXrayLite.

## Шаг 3. Почему «через ядро»

Go-рантайм внутри `libgojni.so` создаёт реальные сокеты через syscall — и на
Android любой IP-трафик обязан пройти сетевой стек ядра (socket layer → TCP/IP
→ Wi-Fi драйвер). Обойти его из userspace нельзя. Поэтому замеряется не ICMP,
а полная цепочка:

```
DNS(UDP 53) → TCP SYN/SYN-ACK/ACK → TLS handshake → GET /generate_204 → 204 No Content
   \________________________ всё через ядро Android _________________________/
```

Итоговое значение = RTT устройство→прокси + RTT прокси→gstatic + стоимость
handshakes. `dns = null` в speedtest-конфиге означает, что резолв
`www.gstatic.com` делает сам Go через системный резолвер — тоже UDP через ядро.

## Почему именно `generate_204`

Сервер отвечает `204 No Content` без тела — замеряется time-to-first-byte
минимального ответа, без скачивания данных; плюс gstatic доступен почти
отовсюду (хороший «маяк» за прокси). URL настраивается:
`SettingsManager.getDelayTestUrl()` → `AppConfig.DELAY_TEST_URL`
(`https://www.gstatic.com/generate_204`, fallback — `DELAY_TEST_URL2`).

## Примечания по ProfilePingManager

- Два режима: in-memory профили (без GUID) и сохранённые (результат пишется в
  `KeyValueStorage.encodeServerTestDelayMillis`).
- `force = true` → параллельный замер всех профилей на пуле `cpu * 4`;
  `force = false` → последовательно, с промежуточным обновлением `lastMeasured`.
- `Protocol.Custom` и `Protocol.PolicyGroup` не пингуются (`isPingable()`).

### Семантика замеров (после рефакторинга)

- **Per-profile таймаут** — `PER_PROFILE_TIMEOUT_MS = 15s`. Блокирующий JNI-вызов нельзя
  прервать, поэтому замер выполняется в `CompletableFuture` на пуле, а корутина ожидает его
  через `withTimeout`: по таймауту профиль получает `-1`, батч продолжается; «зависший» вызов
  доживает на своём потоке, результат отбрасывается.
- **Отмена** — новый `pingProfiles` отменяет предыдущий батч (`batchJob?.cancel()` +
  `ensureActive()` в последовательной ветке); незавершённые нативные вызовы доживают и
  игнорируются.
- **Результаты**:
  - `batch: StateFlow<BatchResult?>` — последний завершённый батч с монотонным `id`; потребитель
    (`ConnectionProfileRepositoryImpl.fetch`) ждёт именно свой id через
    `pingManager.batch.first { it?.id == batchId }`, защищаясь от stale-данных.
  - `measureResults: SharedFlow<Map>` (без replay) — разовая эмиссия для UI; поздние подписчики
    ничего не получают, что корректно для «свежего» списка.
  - `lastMeasured: StateFlow<Pair?>` — прогресс последовательного батча, сбрасывается при старте
    нового.
- **`pingSaved`** — fire-and-forget (`scope.launch(dispatcher)`), не блокирует UI; результат
  персистится в `KeyValueStorage.encodeServerTestDelayMillis`.
- **Конвенция задержек**: `0` = не замеряно, `-1` (`FAILED_DELAY_MS`) = недостижим/ошибка;
  `< 0` трактуется как «невалидный» в `removeInvalidServer`.
