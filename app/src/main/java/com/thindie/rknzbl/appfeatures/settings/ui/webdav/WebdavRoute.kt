package com.thindie.rknzbl.appfeatures.settings.ui.webdav

import com.thindie.engine.core.RouteFactory
import com.thindie.rknzbl.appfeatures.settings.SettingsFlow
import com.v2ray.ang.dto.WebDavConfig

fun SettingsFlow.webdav() =
  RouteFactory.create(
    initialState = WebdavState(),
    execute = { c: WebdavCommand, s: WebdavState ->
      when (c) {
        is WebdavCommand.Back -> {
          back()
          null
        }

        is WebdavCommand.Save -> {
          val url = s.urlInput.trim()
          if (url.isEmpty()) {
            s
          } else {
            flowModule.settingsRepository.setWebDavConfig(
              WebDavConfig(
                baseUrl = url,
                username = s.usernameInput.ifBlank { null },
                password = s.passwordInput.ifBlank { null },
              ),
            )
            back()
            null
          }
        }

        WebdavCommand.Clear -> {
          flowModule.settingsRepository.setWebDavConfig(null)
          back()
          null
        }

        is WebdavCommand.SetUrl -> s.copy(urlInput = c.value)

        is WebdavCommand.SetUsername -> s.copy(usernameInput = c.value)

        is WebdavCommand.SetPassword -> s.copy(passwordInput = c.value)
      }
    },
    stateSink = { screenScope -> webdavStateSink(screenScope, flowModule.settingsRepository) },
    id = "SettingsFlow-webdav",
    routeContent = ::WebdavScreenContent,
  )
