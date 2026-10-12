package org.cat.editor

import android.content.Context
import androidx.activity.ComponentActivity
import org.cat.editor.utils.LocaleHelper

class DebugActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }
}