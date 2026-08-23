# Audit: outbound-building, anti-detection and profile «expiry» in Arkanzabel

**Date:** 2026-08-23
**Scope:** `v2ray-engine/src/main/java/com/v2ray/ang` — runtime + runtimebuilder + parsers.
The referential external VPN-config README was not fetched (as before); conclusions are drawn from source code, which is stronger than an external baseline anyway.

## TL;DR

- **There is no client-side profile expiry.** Nothing in `V2RayServiceManager`, `DomainResolveStep` or `KeyValueStorage` implements a TTL / session-lifetime for profiles. So "profiles expire fast" is almost certainly **not** a client bug — it's one of two things:
  1. the remote provider's session token/heartbeat times out (server-side, not fixable from here), **or**
  2. `DomainResolveStep` resolves the domain to an IP **once at config-build time** (`domainStrategy: UseIP`) and caches it in `dns.hosts`. If the server's DNS / CDN-IP drifts after that, connections die silently while the profile looks healthy on-screen.
- **The core already has strong anti-detection machinery, but none of it is exposed in the app UI.** The main lever missing is not code — it's a settings screen to choose transport (XHTTP/WSS/H2/GRPC) and Reality instead of the default plain-TCP HTTP header, plus toggles for fragment.
- **Default transport = plain TCP with an `http` fake-header** (`AppConfig.DEFAULT_NETWORK = "tcp"`, `HEADER_TYPE_HTTP`). That is the easiest traffic shape for a provider DPI to fingerprint as VPN.

## Where outbound-building happens

```
V2RayServiceManager.startCoreLoop() (runtime)
  └─ V2rayConfigManager.getV2rayConfig(service, guid) (runtime/V2rayConfigManager.kt:415+)
       ├─ DomainResolveStep.resolveOutboundDomainsToHosts()   ← domain→IP at build time
       └─ OutboundConfigStep.applyOutbounds()/applyMoreOutbounds()  (runtimebuilder/OutboundConfigStep.kt)
             ├─ ConnectionProfileToOutboundMapper.map()        (per-protocol parsers)
             ├─ applyGlobalOutboundSettings()                  ← mux, tcp/http fake-header, wireguard tun
             └─ applyOutboundFragment()                        ← fragment/noise (TLS or REALITY only!)
```

Key files:
- `runtimebuilder/OutboundConfigStep.kt` — global settings + fragment.
- `runtime/V2rayConfigManager.kt` — transport/TLS populators (`populateTransportSettings`, `populateTlsSettings`).
- `runtimebuilder/ConnectionProfileToOutboundMapper.kt` — maps a stored profile to an Xray outbound via per-protocol parsers.
- `protocolstringsparsers/*.kt` (Vless, Vmess, Trojan, Shadowsocks, WireGuard, Hysteria2, Http, Socks) + `ProtocolParser.kt`.

## Transport population (`populateTransportSettings`, L728–882)

Supports: TCP (default), KCP, WS, HTTP-Upgrade, **XHTTP**, H2/HTTP, GRPC, Hysteria2.
```
streamSettings.network = transport.ifEmpty { NetworkType.TCP.type }   // default = plain TCP
```
The `ConnectionProfile` DTO already carries the fields to drive XHTTP (`xhttpMode`, `xhttpExtra`) — they are parsed and wired in but have **no settings UI**.

## TLS / REALITY population (`populateTlsSettings`, L893+)

- Reads `fingerPrint`, `publicKey` (Reality), builds `realitySettings`.
- Vless parser validates Reality requires a public key (`Vless.kt:78`).
- So **REALITY + fp spoof** is fully supported in core; it's not exposed to users.

## Fragment (`applyOutboundFragment`, OutboundConfigStep.kt L162–227) — IMPORTANT LIMITATION

```kotlin
if (v2rayConfig.outbounds[0].streamSettings?.security != TLS &&
    v2rayConfig.outbounds[0].streamSettings?.security != REALITY) {
  return v2rayConfig   // fragment is NOT applied to plain TCP/HTTP!
}
packets = ... "tlshello" (TLS) or "1-3" (REALITY)
```

Fragment works **only** behind TLS or Reality, and it defaults to **disabled** (`PREF_FRAGMENT_ENABLED == false`). It also only adds anti-DPI; it does not itself prevent a provider from seeing that you use a VPN.

## Domain resolution — the likely culprit for "expiry"

`DomainResolveStep.resolveOutboundDomainsToHosts()` (called in `V2rayConfigManager`):
- For each proxy outbound whose domain is not already in `dns.hosts`, it calls `HttpUtil.resolveHostToIP(domain, preferIpv6)` and injects the resolved IPs into `dns.hosts`, then sets `sockopt.domainStrategy = "UseIP"` + `happyEyeballs`.
- Effect: **the connection binds to whatever IP was valid at config-build time.** If that IP changes later (CDN rotation, paid-domain move), you get a dead connection without the profile ever looking "expired."

This is a design trade-off (privacy / no per-flow DNS leak) but it's the most concrete client-side reason for sudden dropouts.

## Session/runtime state in `KeyValueStorage` (nothing like an expiry)

Only:
- `setVpnSessionActive/StartEpochMs/Guid/clearVpnSessionRuntime` — flags + start timestamp, used by the speed UI and auto-save notifications.
- Auto-save notification timestamps / saved-profiles JSON.
There is **no** per-profile TTL, no heartbeat-based refresh, no "session expired → reconnect" logic in core.

## Speedtest / connection test (where a provider may notice)

`SpeedtestManager.testConnection()` (`runtime/SpeedtestManager.kt:100`) opens a real proxy connection to `SettingsManager.getDelayTestUrl()`. The core callback also calls `measureDelay(...)`. These are periodic and use the local proxy port — harmless on their own, but they do generate outbound traffic patterns.

## Recommendations (prioritised)

1. **Expose transport choice in UI** — let the user pick XHTTP / WSS / H2 / GRPC instead of the default plain TCP/http-header. Plain TCP is the easiest DPI target. This is the single biggest anti-detection win and requires almost no new core code (the parsers + `populateTransportSettings` already handle all transports).
2. **Expose REALITY + fingerprint toggle** for VLESS/Trojan/Shadowsocks — Reality spoofs SNI/fp and hides the TLS layer; the strongest current anti-DPI method, fully supported in core, unexposed to users.
3. **Make fragment user-toggleable**, but document that it only helps when transport is TLS or REALITY (it's skipped otherwise).
4. **Fix stale-IP risk** — either resolve per-hop at connect time, add a "refresh DNS" / re-import flow, or rely on the subscription to push fresh nodes. This directly addresses the "profiles expire" symptom if cause #2 above is what you see.
5. **Server-side session:** if profiles drop because the provider's own token/heartbeat expires, that can't be fixed client-side — confirm with the provider first.

## What this audit does NOT cover

- The external referential VPN-config README (never fetched; no content to compare against).
- Any redacted secrets / connection strings (not extracted, not stored here).
