package com.thindie.rknzbl.appfeatures.settings.ui.webdav

import com.thindie.engine.core.ScreenScope
import com.thindie.engine.core.sub
import com.thindie.engine.core.transition
import com.thindie.rknzbl.domain.SettingsRepository

internal fun webdavStateSink(
  screenScope: ScreenScope<WebdavState, WebdavCommand>,
  repository: SettingsRepository,
) {
  // [repository.webDavConfig] is nullable; null means the endpoint is not configured and the
  // inputs reset to empty. [sub] accepts the nullable flow directly.
  screenScope.sub(repository.webDavConfig)
    .transition { state, config ->
      state.copy(
        urlInput = config?.baseUrl.orEmpty(),
        usernameInput = config?.username.orEmpty(),
        passwordInput = config?.password.orEmpty(),
      )
    }
}
