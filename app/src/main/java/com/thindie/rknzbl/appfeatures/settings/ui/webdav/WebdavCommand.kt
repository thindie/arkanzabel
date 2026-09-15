package com.thindie.rknzbl.appfeatures.settings.ui.webdav

import com.thindie.engine.core.Command

internal sealed interface WebdavCommand : Command {
  data object Back : WebdavCommand

  data object Save : WebdavCommand

  data object Clear : WebdavCommand

  data class SetUrl(val value: String) : WebdavCommand

  data class SetUsername(val value: String) : WebdavCommand

  data class SetPassword(val value: String) : WebdavCommand

  data object ToggleUseDefaults : WebdavCommand
}
