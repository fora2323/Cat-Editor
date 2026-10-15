package org.cat.editor

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import org.cat.editor.utils.LocaleHelper
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class AppApplication : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
        // Pre-warm TextMate grammars and Darcula theme in background to eliminate UI freeze when opening projects
        Thread {
            try {
                org.cat.editor.ui.ensureTextMateInitialized(applicationContext)
            } catch (_: Throwable) {}
        }.start()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val crashLog = buildCrashLog(throwable)
                val intent = Intent(applicationContext, DebugActivity::class.java).apply {
                    putExtra(DebugActivity.EXTRA_CRASH_LOG, crashLog)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
                Process.killProcess(Process.myPid())
                exitProcess(10)
            } catch (e: Exception) {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    @SuppressLint("HardwareIds")
    private fun buildCrashLog(throwable: Throwable): String {
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timestamp = timeFormat.format(Date())

        val androidId = try {
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }

        val deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} (${Build.DEVICE})"
        val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val deviceId = "${Build.ID} / $androidId"
        val armAbi = Build.SUPPORTED_ABIS.joinToString(", ")

        val stringWriter = StringWriter()
        throwable.printStackTrace(PrintWriter(stringWriter))
        val stackTrace = stringWriter.toString()

        return buildString {
            appendLine("========== DEVICE INFO ==========")
            appendLine("Time           : $timestamp")
            appendLine("Device Name    : $deviceName")
            appendLine("Android Ver    : $androidVersion")
            appendLine("Device ID      : $deviceId")
            appendLine("ARM / ABI      : $armAbi")
            appendLine("=================================")
            appendLine()
            appendLine("========== CRASH LOG ============")
            append(stackTrace)
        }
    }
}