package com.thindie.rknzbl.feature.home

import android.content.Context
import com.thindie.rknzbl.engine.Route
import com.thindie.rknzbl.engine.Router
import com.thindie.rknzbl.engine.ScreenFlow
import com.thindie.rknzbl.feature.home.domain.ConnectionProfileRepository
import com.thindie.rknzbl.feature.home.ui.select.select
import com.thindie.rknzbl.feature.managegate.gatelist.SelectSourceFlow
import com.thindie.rknzbl.feature.managegate.storedgates.FavoriteProfilesFlow
import com.thindie.rknzbl.feature.perapp.PerAppProxyFlow
import com.thindie.rknzbl.feature.settings.domain.SettingsRepository
import com.v2ray.ang.dto.ConnectionProfile
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

class HomeFlow(
  private val router: Router,
  val appContext: Context,
  val repository: ConnectionProfileRepository,
  val settingsRepository: SettingsRepository,
) : ScreenFlow<Route, Unit>(router) {
  internal val sourceChanges =
    MutableSharedFlow<SelectSourceFlow.Result>(
      extraBufferCapacity = 1,
      onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

  internal val selected =
    MutableSharedFlow<ConnectionProfile>(
      replay = 0,
      extraBufferCapacity = 3,
      BufferOverflow.DROP_OLDEST,
    )

  override fun start() {
    if (settingsRepository.isStartWithFavoriteProfilesEnabled()) {
      startStoredProfilesFlow {
        go(select(settingsRepository, repository))
      }
    } else {
      go(select(settingsRepository, repository))
    }
  }

  fun startSelectSourceFlow() {
    SelectSourceFlow(router, appContext)
      .onFinishBuilder { result -> sourceChanges.tryEmit(result) }
      .start()
  }

  fun startStoredProfilesFlow(onFinish: () -> Unit) {
    FavoriteProfilesFlow(
      router = router,
      repository = repository,
      appContext = appContext,
    )
      .onFinishBuilder { onFinish.invoke() }
      .start()
  }

  fun startPerAppProxyFlow() {
    PerAppProxyFlow(router = router, appContext = appContext).start()
  }
}
