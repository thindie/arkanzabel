package com.thindie.rknzbl.appfeatures.home

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.core.view.WindowCompat
import com.thindie.engine.core.Route
import com.thindie.engine.core.Router
import com.thindie.engine.uikit.AppTheme
import com.thindie.engine.uikit.BottomNavItem
import com.thindie.engine.uikit.BottomNavigationBar
import com.thindie.engine.uikit.LocalThemeSwitcher
import com.thindie.engine.uikit.ThemeSwitcher
import com.thindie.rknzbl.R

/**
 * New-design content: same as [com.thindie.rknzbl.MainActivity.LegacyAppContent] but adds the
 * [BottomNavigationBar] for routes tagged with a [HomeSection].
 */
@Composable
fun AppContent(
  router: Router,
  onHomeClick: () -> Unit,
  onProfilesClick: () -> Unit,
  onSettingsClick: () -> Unit,
  onLogsClick: () -> Unit,
) {
  val themeSwitcher = remember { ThemeSwitcher() }
  CompositionLocalProvider(LocalThemeSwitcher provides themeSwitcher) {
    val themeColors = LocalThemeSwitcher.current.themeFlow.collectAsState(null)
    val isDark =
      when (themeColors.value) {
        null -> isSystemInDarkTheme()
        ThemeSwitcher.Choice.Dark -> true
        ThemeSwitcher.Choice.Light -> false
        ThemeSwitcher.Choice.Auto -> isSystemInDarkTheme()
      }
    val view = LocalView.current
    if (!view.isInEditMode) {
      SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
      }
    }
    AppTheme(isDark) {
      val routes by router.route.collectAsState(null)
      var prev by remember { mutableStateOf<Pair<Route, Route?>?>(null) }
      val isPop = routes != null && prev != null && routes!!.first == prev!!.second
      LaunchedEffect(routes) { prev = routes }
      if (routes != null) {
        val currentRoute = routes!!.first

        AnimatedContent(
          modifier =
            Modifier
              .background(AppTheme.colors.backgroundPrimary)
              .fillMaxSize(),
          targetState = currentRoute,
          transitionSpec = {
            // Fade-only for bottom nav sections, slide+fade otherwise
            if (targetState.section is HomeSection) {
              fadeIn(tween(200)) togetherWith fadeOut(tween(200))
            } else if (isPop) {
              val tween = tween<IntOffset>(durationMillis = 280)
              slideInHorizontally(tween) { -it } + fadeIn(tween()) togetherWith
                slideOutHorizontally(tween) { it } + fadeOut(tween())
            } else {
              val tween = tween<IntOffset>(durationMillis = 280)
              slideInHorizontally(tween) { it } + fadeIn(tween()) togetherWith
                slideOutHorizontally(tween) { -it } + fadeOut(tween())
            }
          },
          label = "route",
        ) { route ->
          when (val section = route.section) {
            is HomeSection ->
              Column(
                modifier =
                  Modifier
                    .fillMaxSize()
                    .background(color = AppTheme.colors.backgroundPrimary)
                    .navigationBarsPadding(),
              ) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                  route.content.invoke()
                }
                BottomNavigationBar(
                  items = rememberNavItems(),
                  selected = navIndexFor(section),
                  onItemClicked = { index ->
                    when (index) {
                      0 -> onHomeClick()
                      1 -> onProfilesClick()
                      2 -> onSettingsClick()
                      3 -> onLogsClick()
                    }
                  },
                )
              }

            else -> route.content.invoke()
          }
        }
      }
    }
  }
}

/**
 * Bottom navigation tabs. Icons and titles are resolved once per composition.
 */
@Composable
private fun rememberNavItems(): List<BottomNavItem> {
  val homeIcon = painterResource(R.drawable.ic_home_24)
  val profilesIcon = painterResource(R.drawable.ic_folder_24)
  val settingsIcon = painterResource(R.drawable.ic_settings_24)
  val logsIcon = painterResource(R.drawable.ic_attention_24)

  val homeTitle = stringResource(R.string.bottom_nav_home)
  val profilesTitle = stringResource(R.string.bottom_nav_profiles)
  val settingsTitle = stringResource(R.string.bottom_nav_settings)
  val logsTitle = stringResource(R.string.bottom_nav_logs)

  return remember(homeIcon, profilesIcon, settingsIcon, logsIcon) {
    listOf(
      BottomNavItem(icon = homeIcon, title = homeTitle),
      BottomNavItem(icon = profilesIcon, title = profilesTitle),
      BottomNavItem(icon = settingsIcon, title = settingsTitle),
      BottomNavItem(icon = logsIcon, title = logsTitle),
    )
  }
}

/** Maps a [HomeSection] to its index in the bottom navigation bar. */
@Suppress("MagicNumber")
private fun navIndexFor(section: HomeSection): Int =
  when (section) {
    HomeSection.Home -> 0
    HomeSection.Profiles -> 1
    HomeSection.Settings -> 2
    HomeSection.Logs -> 3
  }
