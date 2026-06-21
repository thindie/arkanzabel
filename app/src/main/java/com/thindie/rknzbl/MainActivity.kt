package com.thindie.rknzbl

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.unit.IntOffset
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.thindie.rknzbl.application.Application
import com.thindie.rknzbl.engine.Route
import com.thindie.rknzbl.feature.home.HomeFlow
import com.thindie.rknzbl.feature.intro.IntroFlow
import com.thindie.rknzbl.uikit.AppTheme
import com.thindie.rknzbl.uikit.LocalThemeSwitcher
import com.thindie.rknzbl.uikit.ThemeSwitcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val hasPermission =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      } else {
        null
      }

    enableEdgeToEdge()
    val app = application as Application
    val router = app.requireRouter()
    val repository = app.applicationScope.data.repository
    awaitFinish()
    setContent {
      SideEffect {
        IntroFlow(
          router,
          hasPushPermission = if (hasPermission != null) hasPermission else true,
          appContext = app,
        )
          .onFinishBuilder {
            val settingsRepository = app.applicationScope.settings.repository
            HomeFlow(router = router, appContext = app, repository = repository, settingsRepository)
              .onFinishBuilder { router.pop() }
              .start()
          }
          .start()
      }
      val themeSwitcher = remember { ThemeSwitcher(repository = app.applicationScope.settings.repository) }
      CompositionLocalProvider(
        LocalThemeSwitcher provides themeSwitcher,
      ) {
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
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
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
            ) { route -> route.content.invoke() }
          }
        }
      }
    }
  }

  override fun attachBaseContext(newBase: Context?) {
    val lang = (application as? Application)?.applicationScope?.settings?.repository?.language()
    if (lang != null) {
      val locale = Locale(lang)
      Locale.setDefault(locale)

      val config = newBase?.resources?.configuration
      config?.setLocale(locale)
      val createdContext = config?.let { createConfigurationContext(it) }
      if (createdContext != null) {
        super.attachBaseContext(createdContext)
      } else {
        super.attachBaseContext(newBase)
      }
    } else {
      super.attachBaseContext(newBase)
    }
  }

  private fun awaitFinish() {
    lifecycleScope.launch {
      (application as Application).finishCommand.first()
      finish()
    }
  }
}
