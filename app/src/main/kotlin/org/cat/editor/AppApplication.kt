package org.cat.editor

import android.app.Application

class AppApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Anda dapat menginisialisasi library atau dependensi global (seperti Dagger/Hilt, Firebase, dll) di sini.
    }
}