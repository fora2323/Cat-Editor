package org.cat.editor.utils

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_LANGUAGE = "selected_language"

    const val LANG_SYSTEM = "system"
    const val LANG_EN = "en"
    const val LANG_ID = "in"
    const val LANG_AR = "ar"
    const val LANG_RU = "ru"
    const val LANG_ZH_CN = "zh-CN"
    const val LANG_ZH_TW = "zh-TW"
    const val LANG_JA = "ja"
    const val LANG_HI = "hi"
    const val LANG_TH = "th"
    const val LANG_UR = "ur"
    const val LANG_KO = "ko"

    data class LanguageOption(
        val code: String,
        val nativeName: String,
        val englishName: String,
        val flagOrTag: String
    )

    val supportedLanguages = listOf(
        LanguageOption(LANG_SYSTEM, "Default Sistem", "System Default", "🌐"),
        LanguageOption(LANG_AR, "العربية (ar)", "Arabic", "🇸🇦"),
        LanguageOption(LANG_EN, "English (en)", "English", "🇺🇸"),
        LanguageOption(LANG_ID, "Bahasa Indonesia (id)", "Indonesian", "🇮🇩"),
        LanguageOption(LANG_HI, "हिन्दी (hi)", "Hindi", "🇮🇳"),
        LanguageOption(LANG_JA, "日本語 (ja)", "Japanese", "🇯🇵"),
        LanguageOption(LANG_KO, "한국어 (ko)", "Korean", "🇰🇷"),
        LanguageOption(LANG_RU, "Русский (ru)", "Russian", "🇷🇺"),
        LanguageOption(LANG_TH, "ไทย (th)", "Thai", "🇹🇭"),
        LanguageOption(LANG_UR, "اردو (ur)", "Urdu", "🇵🇰"),
        LanguageOption(LANG_ZH_CN, "简体中文 (zh-CN)", "Chinese Simplified", "🇨🇳"),
        LanguageOption(LANG_ZH_TW, "繁體中文 (zh-TW)", "Chinese Traditional", "🇹🇼")
    )

    fun getPersistedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, LANG_SYSTEM) ?: LANG_SYSTEM
    }

    fun setLanguage(context: Context, languageCode: String): Context {
        persistLanguage(context, languageCode)
        syncWithSystemLocaleManager(context, languageCode)
        return applyLanguage(context, languageCode)
    }

    private fun persistLanguage(context: Context, languageCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageCode)
            .apply()
    }

    private fun syncWithSystemLocaleManager(context: Context, languageCode: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                if (languageCode == LANG_SYSTEM) {
                    localeManager?.applicationLocales = LocaleList.getEmptyLocaleList()
                } else {
                    val tag = when (languageCode) {
                        LANG_ID -> "id"
                        else -> languageCode
                    }
                    localeManager?.applicationLocales = LocaleList.forLanguageTags(tag)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun wrap(context: Context): Context {
        val languageCode = getPersistedLanguage(context)
        return if (languageCode == LANG_SYSTEM) {
            context
        } else {
            applyLanguage(context, languageCode)
        }
    }

    fun applyLanguage(context: Context, languageCode: String): Context {
        val locale = getLocaleForCode(languageCode)
        Locale.setDefault(locale)

        val res = context.resources
        val config = Configuration(res.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }

    fun getLocaleForCode(languageCode: String): Locale {
        return when (languageCode) {
            LANG_EN -> Locale.ENGLISH
            LANG_ID, "id" -> Locale("in", "ID")
            LANG_AR -> Locale("ar")
            LANG_RU -> Locale("ru")
            LANG_ZH_CN, "zh" -> Locale.SIMPLIFIED_CHINESE
            LANG_ZH_TW -> Locale.TRADITIONAL_CHINESE
            LANG_JA -> Locale.JAPANESE
            LANG_HI -> Locale("hi", "IN")
            LANG_TH -> Locale("th", "TH")
            LANG_UR -> Locale("ur")
            LANG_KO -> Locale.KOREAN
            else -> {
                if (languageCode != LANG_SYSTEM && languageCode.isNotBlank()) {
                    Locale.forLanguageTag(languageCode)
                } else {
                    val adjusted = LocaleListCompat.getAdjustedDefault()
                    if (!adjusted.isEmpty) adjusted[0] ?: Locale.getDefault()
                    else Locale.getDefault()
                }
            }
        }
    }
}
