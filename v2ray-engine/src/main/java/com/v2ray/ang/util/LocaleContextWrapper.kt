package com.v2ray.ang.util

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import java.util.Locale

object LocaleContextWrapper {
    fun wrap(context: Context, newLocale: Locale?): ContextWrapper {
        val res: Resources = context.resources
        val configuration = Configuration(res.configuration)

        val locale = newLocale ?: Locale.getDefault()
        configuration.setLocale(locale)
        val localeList = LocaleList(locale)
        LocaleList.setDefault(localeList)
        configuration.setLocales(localeList)

        val updated = context.createConfigurationContext(configuration)
        return ContextWrapper(updated)
    }
}
