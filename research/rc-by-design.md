# rc (race condition) по дизайну — где exec и stateSink пишут в один _state

После перевода всех `*exec` на `suspend (c: C, s: S) -> S?` расхождение `rc` vs `stateSink` **не устранено полностью** — в 4 экранах оно залено **по дизайну**: `exec` делает `state.copy(...)` (реальное изменение), а `stateSink` через `transition { state.copy(...) }` пишет тот же `_state`.

## Механика (Router.kt)

- `exec` → `Router.kt:216` — `_state.value = newState` **под `commandMutex.withLock`**
- `stateSink` → `Transition.kt` → `update(newState)` → `ScreenScope.kt:181` — `_state.update { s }` **без mutex** (flow-пайп из `LaunchedEffect`)

Два корутина пишут в один `MutableStateFlow<S>`. Синхронизации между ними нет → **lost update**: победит тот, кто написал последним, вторая потеряется.

## 4 экрана с rc по дизайну

### 1. SettingsRoute (`HomeFlow.settings`)
- `exec` (`SettingsRoute.kt`): `ToggleAutosave` → `s.copy(autosaveEnabled)`, `ToggleMux` → `s.copy(muxEnabled)`, `SelectLanguage` (TIRAMISU) → `null`, `ToggleCustomSource` → `s.copy(...)`, `SetCustomSourceUrl` → `s.copy(...)`, `ToggleSpeed` → `s.copy(speedEnabled)`, etc.
- `stateSink` (`SettingsScreenStateSink.kt`): 8 `transition { state.copy(...) }` — autosave, mux, language, isLocalSave, startWithFavoriteProfiles, speed, isCustomSourceEnabled, customSourceUrl
- **Два писателя в один state.**

### 2. NewProfilesRoute (`HomeFlow.newProfiles`)
- `exec` (`NewProfilesRoute.kt`): `Select` → `homeState.copy(...)`, `Start` → `homeState.copy(...)`, `Refresh` → `homeState.copy(...)`, `Dismissed` → `homeState.copy(...)`
- `stateSink` (`NewProfilesStateSink.kt`): `selected.mapLatest { ... }.transition { state.copy(selected = profile, selectedTestConnectionMessage = result) }`
- **`Select` в exec и `selected` в stateSink — обе пишут `selected`.**

### 3. ProfilesRoute (`FavoriteProfilesFlow.profiles`)
- `exec` (`ProfilesStateSink.kt:66`): `RequestStoredProfiles` → `s.copy(...)`, `Delete` → `s.copy(...)`, `Activate` → `s.copy(...)`, `StopService` → `s.copy(...)`, `EnterMultiDeletionMode`/`TogglePendingDelete`/`ExitMultiDeletionMode`/`BatchDelete` → `s.copy(...)`
- `stateSink` (`ProfilesStateSink.kt:23`): `selected.mapLatest { ... }.transition { state.copy(selected, selectedTestConnectionMessage) }`, `settingsRepository.isLocalSave → transition { state.copy(isLocalMode) }`
- **`Activate`/`Delete` в exec и `selected`/`isLocalMode` в stateSink — гонка.**

### 4. HomeSelectRoute (`HomeFlow.select`)
- `exec` (`HomeSelectRoute.kt`): `FetchAutoSaved` → `s`, `DismissAutoSaved` → `s`
- `stateSink` (`HomeSelectStateSink.kt`): `repository.autoSaved().transition { state.copy(autoSaved) }`
- **`FetchAutoSaved`/`DismissAutoSaved` в exec и `autoSaved` в stateSink — гонка.**

## Где rc НЕ было (exec → null, stateSink не пишет state)
- `IntroRoute` — exec → null, stateSink отсутствует
- `SelectSourceRoute` — exec `Back` → null, stateSink `selectSourceStateSink` не пишет `_state`
- `PerAppProxyRoute` / `PerAppSearchRoute` — exec → null, stateSink отсутствует

## Вывод
`rc` — не баг правки, а **архитектурная особенность**: `exec` и `stateSink` — два независимых писателя в один `MutableStateFlow`. Устраняется только синхронизацией (mutex вокруг обоих) или переносом логики state в один источник.
