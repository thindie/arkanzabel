# Audit: outbound-building, anti-detection and profile «expiry» in Arkanzabel

**Date:** 2026-08-26
**Scope:** `v2ray-engine/src/main/java/com/v2ray/ang` — runtime + runtimebuilder + parsers.
Supersedes the 2026-08-23 audit. That earlier version is now **stale**: its top recommendations (#1 transport-in-UI, #2 Reality-in-UI, #3 fragment-toggle) were all closed by commits `f8f4480`/`f63a6c5`/`ee32c34`, and a new headline mechanism — **utls fingerprint emulation** (`c163067`) — was added. See the "What changed since the 08-23 audit" table at the end.

## TL;DR

- **Fingerprint spoofing is now the headline anti-DPI mechanism, and it is broader than Reality.** `populateTlsSettings` (V2rayConfigManager.kt:933-934) sets both `tlsSettings.fingerprint` **and** `tlsSettings.utls` from the profile's `fp` field — for **any** TLS outbound (not just REALITY). This is utls-bro style browser-ClientHello emulation, the strongest current stealth lever, and it is fully wired from the profile.
- **Transport choice is now in the UI.** `NewProfilesScreen.kt` groups profiles by protocol and labels non-TCP transports (XHTTP/WebSocket/HTTP-Upgrade/KCP/gRPC/HTTP/2) on the cards; plain TCP is hidden as the detectable baseline. The parsers + `populateTransportSettings` already handle every transport, so no core work was needed.
- **Reality `show` and fragment are now user toggles** (SettingsRepository + Settings UI), but both default off for a reason: `REALITY_SHOW_ENABLED = false` (changing the TLS fingerprint can break already-working Reality connections) and fragment is skipped on non-TLS/REALITY outbounds.
- **There is still no client-side profile expiry.** No TTL / heartbeat / session-lifetime anywhere. "Profiles expire fast" is server-side (provider token) or stale-IP (`DomainResolveStep` resolves domain→IP once at build time; `dns.refreshInterval` now mitigates this but doesn't eliminate it).

## Where outbound-building happens

```
V2RayServiceManager.startCoreLoop()  (runtime)
  └─ V2rayConfigManager.getV2rayConfig(service, guid)  (runtime/V2rayConfigManager.kt:415+)
       ├─ DomainResolveStep.resolveOutboundDomainsToHosts()   ← domain→IP at build time (stale-IP risk)
       └─ OutboundConfigStep.applyOutbounds()/applyMoreOutbounds()  (runtimebuilder/OutboundConfigStep.kt)
             ├─ ConnectionProfileToOutboundMapper.map()        (per-protocol parsers)
             ├─ applyGlobalOutboundSettings()                  ← mux, tcp/http fake-header, utls/fp, wireguard tun
             └─ applyOutboundFragment()                        ← fragment/noise (TLS or REALITY only!)
```

Key files:
- `runtimebuilder/OutboundConfigStep.kt` — global settings + fragment.
- `runtime/V2rayConfigManager.kt` — transport/TLS/REALITY populators (`populateTransportSettings`, `populateTlsSettings`). **utls fingerprint wiring lives here.**
- `runtimebuilder/ConnectionProfileToOutboundMapper.kt` — maps a stored profile to an Xray outbound via per-protocol parsers.
- `protocolstringsparsers/*.kt` (Vless, Vmess, Trojan, Shadowsocks, WireGuard, Hysteria2, Http, Socks) + `ProtocolParser.kt`.

## Fingerprint spoofing (NEW since 08-23 audit)

`populateTlsSettings` (V2rayConfigManager.kt:893-955) builds `TlsSettings` for both TLS and REALITY:

```kotlin
val tlsSetting = StreamSettings.TlsSettings(
  ...
  fingerprint = connectionProfile.fingerPrint.nullIfBlank(),
  utls = connectionProfile.fingerPrint.nullIfBlank(),   // ← utls-bro browser emulation from the profile's fp
  ...
  show = KeyValueStorage.decodeSettingsBool(AppConfig.PREF_REALITY_SHOW_ENABLED, AppConfig.REALITY_SHOW_ENABLED),
  fallback = AppConfig.REALITY_FALLBACK,
)
```

- `connectionProfile.fingerPrint` is parsed from the `fp` query param (`ProtocolParser.kt:121`, echoed into the config at `Vmess.kt:153`).
- **Effect:** for any TLS/REALITY outbound carrying an `fp`, the core sends a browser-like ClientHello (utls) with the spoofed fingerprint. This hides the TLS layer from DPI **without** needing the Reality `show` toggle — Reality `show` is only needed to also masquerade the SNI/cert chain.
- `REALITY_SHOW_ENABLED` stays `false` by default (commit `f8f4480`) because enabling `show` changes the fingerprint and may break existing Reality handshakes.

## Transport selection (Recommendation #1 — CLOSED)

`NewProfilesScreen.kt` (home/newprofiles):
- `transportSections(profiles)` groups fetched profiles by protocol.
- `transportLabel(item.network)` renders a human-readable stealth tag on each card (WebSocket / HTTP Upgrade / XHTTP / HTTP/2 / gRPC / KCP / HTTP); **TCP is omitted** because it is the detectable baseline.
- The user therefore chooses transport at profile creation/edit time; the core already supports XHTTP/WSS/H2/GRPC via `populateTransportSettings`. No new core code required.

## Reality `show` + fragment (Recommendations #2/#3 — CLOSED, with caveats)

- **Reality `show`:** `SettingsRepositoryImpl.kt` exposes `realityShowEnabled` Setting (`PREF_REALITY_SHOW_ENABLED`) + Settings UI toggle (commit `f63a6c5`). Maps to `tlsSettings.show`. Defaults off (see above).
- **Fragment:** `SettingsRepositoryImpl.kt` exposes `fragmentEnabled` Setting (`PREF_FRAGMENT_ENABLED`) + Settings UI toggle (commit `ee32c34`).

**Fragment limitation still holds:** `applyOutboundFragment()` (OutboundConfigStep.kt:175-241) returns early unless `security == TLS || security == REALITY` (L180-184). It is also disabled by default. Fragment adds anti-DPI (packet randomization + noise) but does not hide the VPN itself.

## TCP keepalive / happyEyeballs (idle resilience)

`OutboundConfigStep.applyGlobalOutboundSettings()` (L125-127):
```kotlin
outbound.ensureSockopt().tcpKeepAliveIdle = AppConfig.OUTBOUND_TCP_KEEPALIVE_IDLE_SECONDS
```
Keeps idle connections alive so DPI doesn't classify them as dead. `DomainResolveStep` also sets `happyEyeballs` + `domainStrategy = "UseIP"`.

## Sniffing (destination masking)

`InboundConfigStep.applyInbounds()` enables sniffing (`fakedns` destOverride + `routeOnly`) and exposes two independent selectors (both follow the `toStorageString()`/`labelRes` theme-mapper convention):
- `SniffingTarget` — which protocols: All / Http / Tls / Quic (`PREF_SNIFFING_TARGET`).
- `SniffingPortRange` — which ports: All / Common(80,443) / Http(80,8080,8880) / Https(443,8443) (`PREF_SNIFFING_PORT_RANGE`).
A Sniffing FAQ dialog is shown in Settings (`86130c2`). Narrowing to port-range reduces how much traffic DPI has to classify.

## Domain resolution — the stale-IP risk (Recommendation #4 — PARTIALLY CLOSED)

`DomainResolveStep.resolveOutboundDomainsToHosts()` still resolves each proxy domain to an IP **once at config-build time**, injects it into `dns.hosts`, and sets `domainStrategy = "UseIP"` + `happyEyeballs`. If the server's IP/CDN drifts after that, connections die silently while the profile looks healthy on-screen.

**Mitigation added (commit `2faa697`):** `DnsConfigStep.kt:160-165` sets `dns.refreshInterval` from `PREF_DNS_REFRESH_INTERVAL` — the core re-resolves cached IPs every N seconds, which directly reduces the stale-IP death window. This is a mitigation, not a full re-resolve-per-hop; the resolve-once-at-build behaviour is still the default.

## Session/runtime state in `KeyValueStorage` (no expiry)

Only:
- `setVpnSessionActive/StartEpochMs/Guid/clearVpnSessionRuntime` — flags + start timestamp for the speed UI and auto-save notifications.
- Auto-save notification timestamps / saved-profiles JSON.

There is **no** per-profile TTL, no heartbeat-based refresh, no "session expired → reconnect" logic in core. So "profiles expire" is either server-side (provider token/heartbeat) or stale-IP.

## What changed since the 2026-08-23 audit

| 08-23 finding / recommendation | Status now | Evidence |
|---|---|---|
| #1 Transport in core but no UI | **CLOSED** | `NewProfilesScreen.transportSections()` + `transportLabel()` |
| #2 Reality + fp spoof in core but unexposed | **CLOSED** | `realityShowEnabled` Setting + UI toggle (`f63a6c5`) |
| #3 Fragment user-toggleable | **CLOSED** | `fragmentEnabled` Setting + UI toggle (`ee32c34`) |
| — utls/fingerprint browser emulation | **NEW** | `c163067`; `populateTlsSettings` sets `utls = fp` (V2rayConfigManager:934) |
| #4 Fix stale-IP | **PARTIALLY CLOSED** | `dns.refreshInterval` mitigation added (`2faa697`); resolve-once-at-build still default |
| — Sniffing target/port-range selectors | **CLOSED** | `SniffingTarget`/`SniffingPortRange` enums + UI (`e722e5e`) |
| No client-side profile expiry | **STILL VALID** | No TTL/heartbeat/session-lifetime anywhere |
| Fragment only works behind TLS/Reality | **STILL VALID** | `applyOutboundFragment` early-returns unless TLS/REALITY |
| #5 Server-side session | **STILL OPEN** | Provider token/heartbeat expiry is not fixable client-side |

## Remaining recommendations (prioritised)

1. **Document the utls/fp wiring** — fingerprint spoofing is now the primary stealth lever and applies to any TLS/REALITY outbound with an `fp`, independent of the Reality `show` toggle. Worth a short note in release notes / runbook (what `fp` values work per provider, and the "enable `show` may break existing Reality" caveat).
2. **stale-IP:** the `dns.refreshInterval` mitigation helps, but resolve-once-at-build remains. If dropouts persist, confirm whether the provider rotates IPs (server-side) vs. the client binding to a stale IP.
3. **Server-side session (#5):** if profiles drop because the provider's own token/heartbeat expires, that can't be fixed client-side — confirm with the provider first.

## What this audit does NOT cover

- The external referential VPN-config README (never fetched; no content to compare against).
- Any redacted secrets / connection strings (not extracted, not stored here).
