# Bottom-Navigation Pattern (Avezer)

Референс рабочего bottom-nav для stack-based Router-архитектуры.
Источник: `E:\AndroidProject\avezer` (`com.thindie.avezer`).
Назначение: перенести в arkanzabel как замену хабу из 4 push-кнопок (HomeSelect).

## Ключевая идея

Все разделы приложения помечаются **секцией**. В `MainActivity` рендер по секции:
- `Section.Leaf` → обычный push/pop переход (как раньше).
- «Главная» секция (`HomeSection`) → экран + нижняя навигация поверх него.

Разделы живут в стеке **глубиной 1**. Переключение между ними = `router.replaceTop(...)`,
а не `push`. Поэтому bottom-nav остаётся видимым между разделами, а back-жест не уходит
внутрь табов.

## 1. Секции (`HomeSection`)

```kotlin
@Immutable
sealed interface HomeSection : Section {
  @Immutable data object Places : HomeSection
  @Immutable data object Search : HomeSection
  @Immutable data object Settings : HomeSection
}
```

Каждый route помечает себя в `RouteFactory.create(...)`:

```kotlin
section = HomeSection.Places,
```

## 2. MainActivity — рендер по секции

```kotlin
AnimatedContent(targetState = routes!!.first) { route ->
  when (route.section) {
    Section.Leaf -> route.content.invoke()          // обычный push-переход
    is HomeSection -> Box {                          // объединяем все табы
      route.content.invoke()
      BottomNavigationBar(
        modifier = Modifier.align(Alignment.BottomCenter),
        onPlacesClick  = { switchToHome() },
        onSearchClick  = { switchToSearch() },
        onSettingsClick= { switchToSettings() },
        selected = route.section,                    // подсветка активного таба
      )
    }
  }
}
```

Переходы (`AnimatedContent` + `transitionSpec`) остаются теми же: для Leaf/поиск — slide,
для первого раздела в стеке — без анимации.

## 3. Переключение = replaceTop (не push)

```kotlin
fun switch(screen: Start? = null) {
  when (screen) {
    is Start.Details -> router.replaceTop(placeDetail(screen.weather))
    null -> router.replaceTop(places)               // замена на месте, стек не растёт
  }
}
```

Каждый tab-переключатель в `MainActivity`:

```kotlin
private fun switchToHome(start: HomeFlow.Start? = null) {
  val flow = HomeFlow(router = router, flowModule = app.applicationScope.appFlowModule)
  flow.onFinishBuilder { result ->
    when (result) {
      HomeFlow.Result.Search -> switchToSearch()
      HomeFlow.Result.Settings -> switchToSettings()
    }
  }
  flow.switch(start)
}
```

## 4. BottomNavigationBar (готовый компонент)

```kotlin
@Composable
fun BottomNavigationBar(
  modifier: Modifier = Modifier,
  onPlacesClick: () -> Unit,
  onSearchClick: () -> Unit,
  onSettingsClick: () -> Unit,
  selected: Section,
) {
  val sections = remember { listOf(HomeSection.Places, HomeSection.Search, HomeSection.Settings) }
  Row(
    modifier = modifier.fillMaxWidth().background(AppTheme.colors.backgroundPrimary),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    sections.forEach {
      val (icon, title) = when (it) {
        HomeSection.Places   -> R.drawable.ic_home_24 to R.string.places
        HomeSection.Search   -> R.drawable.ic_search_24 to R.string.search_title
        HomeSection.Settings -> R.drawable.ic_settings_24 to R.string.settings_title
      }
      Section(
        title = stringResource(title),
        icon = painterResource(icon),
        onClick = { when (it) { ... invoke each on*Click } },
        isSelected = selected == it,
      )
    }
  }
}
```

### Section (один tab)

```kotlin
@Composable
fun Section(
  modifier: Modifier = Modifier,
  title: String, icon: Painter, onClick: () -> Unit, isSelected: Boolean,
) {
  val color by animateColorAsState(
    targetValue = if (isSelected) AppTheme.colors.accentPrimary else AppTheme.colors.contentSecondary,
    animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
  )
  Column(modifier.clickable(onClick = onClick, indication = null, interactionSource = null), ...) {
    VSpacer(16.dp)
    Icon(painter = icon, tint = color, modifier = Modifier.size(40.dp))
    Text(text = title, style = AppTheme.typography.labelMedium, color = color, maxLines = 1)
    VSpacer(8.dp)
  }
}
```

## Перенос в arkanzabel (план)

| Avezer | arkanzabel |
|--------|-----------|
| `HomeSection.Places`   | `HomeSection.NewProfiles` |
| `HomeSection.Search`   | `HomeSection.StoredProfiles` |
| `HomeSection.Settings` | `HomeSection.PerAppProxy` |
| (4-й tab)              | `HomeSection.Settings` |

Шаги:
1. Создать `HomeSection` sealed interface (аналог Avezer).
2. Пометить New / Stored / PerApp / Settings routes своими секциями в `RouteFactory.create`.
3. В `MainActivity` заменить `when(route.section)`: Leaf → content, HomeSection → content + NavigationBar.
4. В `HomeFlow` кнопки хаба перевести на `switch()` с `replaceTop` вместо `go()/startStoredProfilesFlow()`.

## Заметки

- Движок (`:core`) и домен не трогаются — паттерн работает поверх существующего Router/ScreenScope.
- Глубина стека = 1 между разделами → back-жест ведёт себя предсказуемо.
- `BottomNavigationBar` + `Section()` — copy-paste из Avezer, меняется только список секций и иконки/строки.
