package com.thindie.rknzbl.appfeatures.profiles.deletesaved

import com.thindie.engine.core.Command
import com.v2ray.ang.dto.ConnectionProfile

sealed interface DeleteSavedProfilesCommand : Command {
  data object LoadSaved : DeleteSavedProfilesCommand

  data class ToggleSelect(val profile: ConnectionProfile) : DeleteSavedProfilesCommand

  data object ConfirmDelete : DeleteSavedProfilesCommand

  data object Exit : DeleteSavedProfilesCommand
}
