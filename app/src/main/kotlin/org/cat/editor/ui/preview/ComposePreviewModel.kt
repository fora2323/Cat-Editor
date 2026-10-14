package org.cat.editor.ui.preview

import android.content.res.Configuration
import java.io.Serializable

/**
 * Represents the configuration and metadata of a Kotlin Jetpack Compose @Preview annotation.
 */
class ComposePreviewModel(
    var functionName: String = "",
    var previewName: String = ""
) : Serializable {

    var group: String = ""
    var showBackground: Boolean = false
    var backgroundColor: Int = 0
    var widthDp: Int = 0
    var heightDp: Int = 0
    var device: String = ""
    var uiMode: Int = Configuration.UI_MODE_NIGHT_NO
    var fontScale: Float = 1.0f
    var showSystemUi: Boolean = false
    var apiLevel: String = ""
    var locale: String = ""
    var functionBody: String = ""
    var isAutoDetected: Boolean = false

    fun isDarkMode(): Boolean {
        return (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    val displayName: String
        get() {
            if (previewName.isNotBlank()) {
                return previewName.trim()
            }
            if (functionName.isBlank()) {
                return "Compose Preview"
            }
            val sb = StringBuilder()
            for (i in functionName.indices) {
                val c = functionName[i]
                if (i > 0 && c.isUpperCase() && !functionName[i - 1].isUpperCase()) {
                    sb.append(' ')
                }
                sb.append(c)
            }
            return sb.toString().replace(" Preview", "")
        }
}
