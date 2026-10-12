package org.cat.editor

import android.app.Application
import android.content.Context
import org.cat.editor.utils.LocaleHelper

class AppApplication : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        // Anda dapat menginisialisasi library atau dependensi global (seperti Dagger/Hilt, Firebase, dll) di sini.
    }
}