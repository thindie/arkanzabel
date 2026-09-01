package com.thindie.rknzbl.appfeatures.home

import android.app.Activity
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.core.view.WindowCompat
import com.thindie.engine.core.Route
import com.thindie.engine.core.Router
import com.thindie.engine.uikit.AppTheme
import com.thindie.engine.uikit.LocalThemeSwitcher
import com.thindie.engine.uikit.ThemeSwitcher

/**
 * New-design content: same as [com.thindie.rknzbl.MainActivity.AppContent] but adds the
 * [BottomNavigationBar] for routes tagged with [HomeSection].
 *
 * Legacy design uses [com.thindie.rknzbl.MainActivity.AppContent] directly.
 */
@Composable
fun AppContent(
  router: Router,
  onHomeClick: () -> Unit,
  onSettingsClick: () -> Unit,
) {
  val themeSwitcher =
    remember {
      ThemeSwitcher()
    }
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
      BackHandler { }
      val routes by router.route.collectAsState(null)
      var prev by remember { mutableStateOf<Pair<Route, Route?>?>(null) }
      val isPop = routes != null && prev != null && routes!!.first == prev!!.second
      LaunchedEffect(routes) { prev = routes }
      if (routes != null) {
        val tween = tween<IntOffset>(durationMillis = 280)
        AnimatedContent(
          modifier = Modifier.background(AppTheme.colors.backgroundPrimary),
          targetState = routes!!.first,
          transitionSpec = {
            if (isPop) {
              slideInHorizontally(tween) { -it } + fadeIn(tween()) togetherWith
                slideOutHorizontally(tween) { it } + fadeOut(tween())
            } else {
              slideInHorizontally(tween) { it } + fadeIn(tween()) togetherWith
                slideOutHorizontally(tween) { -it } + fadeOut(tween())
            }
          },
          label = "route",
        ) { route ->
          when (route.section) {
            is HomeSection ->
              Box(modifier = Modifier.fillMaxWidth()) {
                route.content.invoke()
                BottomNavigationBar(
                  modifier = Modifier.align(Alignment.BottomCenter),
                  onHomeClick = onHomeClick,
                  onSettingsClick = onSettingsClick,
                  selected = route.section as HomeSection,
                )
              }

            else -> route.content.invoke()
          }
        }
      }
    }
  }
}
