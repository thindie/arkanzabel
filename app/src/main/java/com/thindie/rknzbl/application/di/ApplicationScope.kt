package com.thindie.rknzbl.application.di

import com.thindie.rknzbl.feature.home.data.di.HomeFlowModule
import com.thindie.rknzbl.feature.settings.data.di.SettingsFlowModule
import com.v2ray.ang.runtime.KeyValueStorage

class ApplicationScope {
  private object Creds {
    const val NAME = ""
    const val PWD = ""
    const val URL = ""
  }

  val data =
    HomeFlowModule(
      userName = Creds.NAME,
      password = Creds.PWD,
      storage = KeyValueStorage,
      baseUrl = Creds.URL,
    )

  val settings = SettingsFlowModule(storage = KeyValueStorage)
}
