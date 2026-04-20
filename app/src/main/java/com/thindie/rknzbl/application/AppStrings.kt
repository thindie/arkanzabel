package com.thindie.rknzbl.application

import android.content.Context
import com.thindie.rknzbl.R

object AppStrings {
  lateinit var errorUnexpected: String
    private set

  fun init(context: Context) {
    errorUnexpected = context.getString(R.string.error_unexpected)
  }
}
