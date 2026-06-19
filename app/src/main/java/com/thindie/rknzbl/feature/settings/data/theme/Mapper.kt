package com.thindie.rknzbl.feature.settings.data.theme

import com.thindie.rknzbl.uikit.ThemeSwitcher

/**
 * Extension functions for theme conversion.
 */
fun ThemeSwitcher.Choice?.toStorageString(): String {
  return when (this) {
    ThemeSwitcher.Choice.Auto -> "auto"
    ThemeSwitcher.Choice.Light -> "light"
    ThemeSwitcher.Choice.Dark -> "dark"
    null -> error("Unexpected param of Choice")
  }
}

/**
 * Convert string from storage to [ThemeSwitcher.Choice].
 */
fun String.toChoice(): ThemeSwitcher.Choice {
  return when (this.lowercase()) {
    "auto" -> ThemeSwitcher.Choice.Auto
    "light" -> ThemeSwitcher.Choice.Light
    "dark" -> ThemeSwitcher.Choice.Dark
    else -> error("Unexpected param of Choice")
  }
}