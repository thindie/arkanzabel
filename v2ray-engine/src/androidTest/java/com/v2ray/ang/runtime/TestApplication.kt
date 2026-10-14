package com.v2ray.ang.runtime

import android.app.Application
import com.tencent.mmkv.MMKV

class TestApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    MMKV.initialize(this)
  }
}
