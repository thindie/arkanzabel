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

        is WebdavCommand.ToggleUseDefaults -> {
          val current = s.useDefaults
          flowModule.settingsRepository.toggleWebDavUseDefaults(!current)
          null
        }

        is WebdavCommand.Save -> {
          val url = s.urlInput.trim()
          val config =
            if (url.isEmpty()) {
              null
            } else {
              WebDavConfig(
                baseUrl = url,
                username = s.usernameInput.ifBlank { null },
                password = s.passwordInput.ifBlank { null },
              )
            }

          if (config == null) {
            s
          } else {
            flowModule.settingsRepository.setWebDavConfig(config)
            back()
            null
          }
        }

        WebdavCommand.Clear -> {
          s.copy(
            urlInput = "",
            usernameInput = "",
            passwordInput = "",
          )
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
