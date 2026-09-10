package com.thindie.rknzbl

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.thindie.rknzbl.appfeatures.home.AppContent
import com.thindie.rknzbl.appfeatures.home.HomeFlow
import com.thindie.rknzbl.appfeatures.intro.IntroFlow
import com.thindie.rknzbl.appfeatures.logs.LogsFlow
import com.thindie.rknzbl.appfeatures.profiles.ProfilesFlow
import com.thindie.rknzbl.appfeatures.settings.SettingsFlow
import com.thindie.rknzbl.application.Application
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val hasPermission =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      } else {
        true
      }
    enableEdgeToEdge()
    val app = application as Application
    val router = app.requireRouter()
    awaitFinish()

    setContent {
      val useNewDesign by app.applicationScope.useNewDesignFeature().collectAsState(false)
      if (useNewDesign) {
        val homeFlow = HomeFlow(router).apply { app.applicationScope.inject(this) }
        val profilesFlow = ProfilesFlow(router).apply { app.applicationScope.inject(this) }
        val settingsFlow =
          SettingsFlow(router).apply {
            app.applicationScope.inject(this)
            onFinishBuilder { router.pop() }
          }
        val logsFlow = LogsFlow(router, app)

        IntroFlow(
          router,
          hasPushPermission = hasPermission,
          appContext = app,
        )
          .onFinishBuilder { homeFlow.start() }
          .start()

        AppContent(
          router,
          onHomeClick = { homeFlow.switch() },
          onProfilesClick = { profilesFlow.switch() },
          onSettingsClick = { settingsFlow.switch() },
          onLogsClick = { logsFlow.switch() },
        )
      }
    }
  }

  private val languageBlocking =
    runBlocking {
      (application as? Application)
        ?.applicationScope
        ?.settingsRepository
        ?.language
        ?.filterNotNull()
        ?.firstOrNull()
    }

  override fun attachBaseContext(newBase: Context?) {
    val lang = languageBlocking
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

  override fun onConfigurationChanged(newConfig: Configuration) {
    val lang = languageBlocking
    if (lang != null) {
      val locale = Locale(lang)
      Locale.setDefault(locale)
      val config = resources.configuration
      config.setLocale(locale)
      resources.updateConfiguration(config, resources.displayMetrics)
    } else {
      super.onConfigurationChanged(newConfig)
    }
  }

  private fun awaitFinish() {
    lifecycleScope.launch {
      (application as Application).finishCommand.first()
      finish()
    }
  }
}
