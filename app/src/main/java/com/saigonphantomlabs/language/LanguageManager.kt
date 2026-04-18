package com.saigonphantomlabs.language

import android.content.Context
import android.content.res.Configuration
import android.preference.PreferenceManager
import java.util.Locale

object LanguageManager {
    private const val PREF_LANG = "pref_language"

    fun setLanguage(context: Context, langCode: String) {
        saveLanguage(context, langCode)
        applyLocale(context, langCode)
    }

    fun getLanguage(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        // Default language is English
        return prefs.getString(PREF_LANG, "en") ?: "en"
    }

    private fun saveLanguage(context: Context, lang: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putString(PREF_LANG, lang).apply()
    }

    fun applyLocale(context: Context, langCode: String): Context {
        val locale = Locale(langCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}
