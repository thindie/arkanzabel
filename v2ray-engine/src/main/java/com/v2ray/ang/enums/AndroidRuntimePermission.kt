package com.v2ray.ang.enums

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi

enum class AndroidRuntimePermission {
  Camera {
    override fun getPermission(): String = Manifest.permission.CAMERA
  },

  ReadStorage {
    override fun getPermission(): String {
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
      } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
      }
    }
  },

  PostNotifications {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun getPermission(): String = Manifest.permission.POST_NOTIFICATIONS
  };

  abstract fun getPermission(): String


  fun getLabel(): String {
    return when (this) {
      Camera -> "Camera"
      ReadStorage -> "Storage"
      PostNotifications -> "Notification"
    }
  }
}