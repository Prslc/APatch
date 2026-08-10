package me.bmax.apatch.util.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Resolve the app's persisted language override. On API 33+ this is managed by
 * the framework LocaleManager; on older platforms we fall back to the
 * "app_lang" preference written by SettingsViewModel.updateLanguage.
 */
fun resolveAppLocale(context: Context): Locale? {
    return try {
        val tag = context.getSharedPreferences("config", Context.MODE_PRIVATE)
            .getString("app_lang", "") ?: ""
        if (tag.isEmpty()) null else Locale.forLanguageTag(tag)
    } catch (_: Throwable) {
        null
    }
}

/** Apply the persisted language override to the given base context. */
fun applyAppLocale(context: Context): Context {
    val locale = resolveAppLocale(context) ?: return context
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        context.createConfigurationContext(config)
    } else {
        val config = Configuration(context.resources.configuration)
        config.locale = locale
        context.createConfigurationContext(config)
    }
}
