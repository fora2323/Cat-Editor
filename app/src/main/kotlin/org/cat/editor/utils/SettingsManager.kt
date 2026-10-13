package org.cat.editor.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cat.editor.BuildConfig
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object SettingsManager {
    const val PREFS_NAME = "app_settings"

    const val KEY_CHECK_UPDATES = "check_updates"
    const val KEY_FULLSCREEN = "fullscreen"
    const val KEY_SMART_TOOLBAR = "smart_toolbar"
    const val KEY_EXIT_DIALOG = "exit_dialog"
    const val KEY_DEBUG_OPTIONS = "debug_options"
    const val KEY_EDITOR_FONT = "editor_font"
    const val KEY_CACHED_STARS = "cached_github_stars"
    const val KEY_CACHED_COMMIT = "cached_github_commit"

    const val FONT_MONOSPACE = "Monospace"
    const val FONT_SANS = "Sans-Serif"
    const val FONT_SERIF = "Serif"

    fun getPrefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isCheckUpdatesEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_CHECK_UPDATES, true)

    fun setCheckUpdatesEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_CHECK_UPDATES, enabled).apply()
    }

    fun isFullscreen(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_FULLSCREEN, false)

    fun setFullscreen(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_FULLSCREEN, enabled).apply()
    }

    fun isSmartToolbarEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_SMART_TOOLBAR, false)

    fun setSmartToolbarEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SMART_TOOLBAR, enabled).apply()
    }

    fun isExitDialogEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_EXIT_DIALOG, true)

    fun setExitDialogEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_EXIT_DIALOG, enabled).apply()
    }

    fun isDebugOptionsEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_DEBUG_OPTIONS, false)

    fun setDebugOptionsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DEBUG_OPTIONS, enabled).apply()
    }

    fun getEditorFont(context: Context): String =
        getPrefs(context).getString(KEY_EDITOR_FONT, FONT_MONOSPACE) ?: FONT_MONOSPACE

    fun setEditorFont(context: Context, font: String) {
        getPrefs(context).edit().putString(KEY_EDITOR_FONT, font).apply()
    }

    fun getCachedStars(context: Context): String =
        getPrefs(context).getString(KEY_CACHED_STARS, "0") ?: "0"

    fun setCachedStars(context: Context, stars: String) {
        getPrefs(context).edit().putString(KEY_CACHED_STARS, stars).apply()
    }

    fun getCachedCommit(context: Context): String =
        getPrefs(context).getString(KEY_CACHED_COMMIT, BuildConfig.GIT_HASH) ?: BuildConfig.GIT_HASH

    fun setCachedCommit(context: Context, commit: String) {
        getPrefs(context).edit().putString(KEY_CACHED_COMMIT, commit).apply()
    }

    fun applyFullscreen(activity: Activity) {
        val fullscreen = isFullscreen(activity)
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (fullscreen) {
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun openStorageSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            } else {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
        } catch (_: Exception) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }

    fun getEditorTypeface(context: Context): Typeface {
        return when (getEditorFont(context)) {
            FONT_SANS -> Typeface.SANS_SERIF
            FONT_SERIF -> Typeface.SERIF
            else -> Typeface.MONOSPACE
        }
    }

    suspend fun syncGithubInfo(context: Context): Triple<String, String, Boolean> = withContext(Dispatchers.IO) {
        var stars = getCachedStars(context)
        var commit = getCachedCommit(context)
        var success = false

        try {
            // 1. Fetch repo details for stars
            val repoUrl = URL("https://api.github.com/repos/fora2323/Cat-Editor")
            val repoConn = repoUrl.openConnection() as HttpURLConnection
            repoConn.connectTimeout = 6000
            repoConn.readTimeout = 6000
            repoConn.setRequestProperty("User-Agent", "Cat-Editor-App")
            repoConn.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (repoConn.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(repoConn.inputStream))
                val response = reader.readText()
                reader.close()
                val json = JSONObject(response)
                val starCount = json.optInt("stargazers_count", -1)
                if (starCount >= 0) {
                    stars = starCount.toString()
                    setCachedStars(context, stars)
                    success = true
                }
            }
            repoConn.disconnect()

            // 2. Fetch latest commit SHA
            val commitUrl = URL("https://api.github.com/repos/fora2323/Cat-Editor/commits/main")
            val commitConn = commitUrl.openConnection() as HttpURLConnection
            commitConn.connectTimeout = 6000
            commitConn.readTimeout = 6000
            commitConn.setRequestProperty("User-Agent", "Cat-Editor-App")
            commitConn.setRequestProperty("Accept", "application/vnd.github.v3+json")

            if (commitConn.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(commitConn.inputStream))
                val response = reader.readText()
                reader.close()
                val json = JSONObject(response)
                val fullSha = json.optString("sha", "")
                if (fullSha.isNotEmpty()) {
                    commit = fullSha.take(7)
                    setCachedCommit(context, commit)
                    success = true
                }
            }
            commitConn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Triple(stars, commit, success)
    }
}
