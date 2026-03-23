# Compliance and third-party attribution (Phase 0)

This document is **engineering and process guidance**, not legal advice. For distribution (especially public APK/AAB), involve your own legal review.

## Planned upstream components

Arkanzabel intends to embed or link code and native artifacts from the **v2rayNG** ecosystem, including:

| Component | Typical source layout (v2rayNG checkout) | License (verify at source) |
|-----------|----------------------------------------|----------------------------|
| Android app logic to be vendored into `:v2ray-engine` | `V2rayNG/app/src/main/java/com/v2ray/ang/...` | **GPL-3.0** (confirm in upstream `LICENSE` / notices) |
| Xray core binding (gomobile AAR) | `AndroidLibXrayLite/` → `libv2ray*.aar` | GPL-3.0 + Go module licenses (see upstream `go.mod` / notices) |
| hev-socks5-tunnel (optional TUN path) | `hev-socks5-tunnel/`, built via repo-root `compile-hevtun.sh` | Per upstream files (e.g. `LICENSE`, headers in `src/`) |

Upstream repositories commonly used as reference (verify clones match your audit):

- <https://github.com/2dust/v2rayNG>
- <https://github.com/2dust/AndroidLibXrayLite>

## License listing files to mirror from v2rayNG

When the engine is integrated, ship or link the same attribution surfaces v2rayNG uses (do not rewrite legal text casually):

- `V2rayNG/app/src/main/assets/open_source_licenses.html` — bundled HTML license roll-up.
- `V2rayNG/app/src/main/res/raw/licenses.xml` — raw resource used for in-app license UI patterns.

**Arkanzabel today:** these files are **not** copied into this repository in Phase 0. Copy them during engine/resource migration (per your local migration checklist, Phase 2 / assets) or load equivalent content in an in-app **Open source** screen (recommended before store release).

## Native artifacts — provenance

Document in release notes / internal runbooks **how each binary was produced**:

1. **`libv2ray` AAR**  
   - Built from `AndroidLibXrayLite` using gomobile (see `AndroidLibXrayLite/.github/workflows/main.yml` in upstream).  
   - Record: commit SHA, NDK version, `gomobile` / `-androidapi` flags.

2. **`libhev-socks5-tunnel.so`**  
   - Built with `ndk-build` via `compile-hevtun.sh`.  
   - **Critical:** `APP_CFLAGS` includes `-DPKGNAME=com/v2ray/ang/service` so JNI registers on `com.v2ray.ang.service.TProxyService`. Changing package without rebuilding hev breaks the tunnel (see your local v2ray migration notes).

Store copies under `v2ray-engine/libs/` (or your chosen layout) per your integration checklist; prefer **Git LFS** or CI-produced artifacts over unchecked large binaries in git.

## Copyleft reminder (high level)

Distributing a combined app that includes GPLv3-covered v2rayNG-derived code typically triggers **GPL obligations** for that combined work (source offer, license text, etc.). Plan for:

- Visible **license / third-party** UI or packaged `open_source_licenses.html`.
- Ability to provide **corresponding source** for the GPL-covered portions you ship, in line with your distribution model.

## Project planning references

Migration/architecture/atomic-commit plans are **not** stored in this git repository (local or private docs only). This file and `NOTICE` are the versioned compliance baseline for Phase 0; copy upstream license assets in later integration phases.
