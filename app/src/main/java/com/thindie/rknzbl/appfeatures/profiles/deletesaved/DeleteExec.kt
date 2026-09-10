package com.thindie.rknzbl.appfeatures.profiles.deletesaved

import com.thindie.rknzbl.application.work.GlobalJobManager
import com.thindie.rknzbl.application.work.GlobalJobManager.Companion.DISCONNECT_KEY
import com.thindie.rknzbl.domain.ConnectionProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun exec(
  c: DeleteSavedProfilesCommand,
  s: DeleteSavedProfilesState,
  connectionProfileRepository: ConnectionProfileRepository,
  globalJobManager: GlobalJobManager,
  onFinish: () -> Unit,
): DeleteSavedProfilesState? {
  return when (c) {
    DeleteSavedProfilesCommand.LoadSaved ->
      withContext(Dispatchers.IO) {
        connectionProfileRepository.invalidateStoredCache()
        val saved = connectionProfileRepository.read()
        s.copy(savedProfiles = saved)
      }

    is DeleteSavedProfilesCommand.ToggleSelect ->
      s.copy(
        selectedProfiles =
          if (c.profile in s.selectedProfiles) {
            s.selectedProfiles - c.profile
          } else {
            s.selectedProfiles + c.profile
          },
      )

    DeleteSavedProfilesCommand.ConfirmDelete ->
      withContext(Dispatchers.IO) {
        var disconnectNeeded = false
        for (profile in s.selectedProfiles) {
          connectionProfileRepository.delete(profile)
          if (profile == s.connectedProfile) disconnectNeeded = true
        }
        // Deleting the active/connected profile must stop the VPN service.
        if (disconnectNeeded) {
          globalJobManager.launchGlobal(DISCONNECT_KEY) {
            connectionProfileRepository.disconnect()
          }
        }
        onFinish()
        null
      }

    DeleteSavedProfilesCommand.Exit -> {
      onFinish()
      null
    }
  }
}
