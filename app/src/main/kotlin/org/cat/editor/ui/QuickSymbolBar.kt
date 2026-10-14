package org.cat.editor.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Data class representing an individual quick symbol button.
 *
 * @param label The text displayed on the button (e.g. "Tab", "()", ";")
 * @param insertText The actual text inserted into the code editor
 * @param cursorOffset The relative cursor position after insertion
 */
data class QuickSymbol(
    val label: String,
    val insertText: String,
    val cursorOffset: Int = insertText.length
)

/**
 * Manager responsible for loading and resolving quick symbols from JSON.
 * Matches Cipher IDE symbol bar with JSON configurability and language support.
 */
object QuickSymbolManager {

    private const val ASSET_FILE = "symbols.json"
    private const val CUSTOM_FILE = "custom_symbols.json"

    // Default Cipher IDE fallback symbols
    val DEFAULT_CIPHER_SYMBOLS = listOf(
        "(", ")", "{", "}", "[", "]", ";", "=", ".", "\"", "'", ",",
        "<", ">", "+", "-", "*", "/", "!", "&", "|", "\\", "?", ":",
        "Tab", "//", "<!--", "-->", "@", "#", "$", "_"
    )

    fun loadSymbols(context: Context, filePath: String): List<QuickSymbol> {
        val langKey = resolveLanguageKey(filePath)
        val jsonStr = readSymbolsJson(context)
        if (jsonStr.isNullOrBlank()) {
            return DEFAULT_CIPHER_SYMBOLS.map { createSymbolFromString(it) }
        }

        try {
            val trimmed = jsonStr.trim()
            if (trimmed.startsWith("{")) {
                val rootObj = JSONObject(trimmed)
                val array = when {
                    rootObj.has(langKey) -> rootObj.getJSONArray(langKey)
                    rootObj.has("default") -> rootObj.getJSONArray("default")
                    else -> null
                }
                if (array != null) {
                    return parseJsonArray(array)
                }
            } else if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                return parseJsonArray(array)
            }
        } catch (_: Throwable) {
            // Fall back safely if JSON is malformed
        }

        return DEFAULT_CIPHER_SYMBOLS.map { createSymbolFromString(it) }
    }

    private fun readSymbolsJson(context: Context): String? {
        try {
            val customFile = File(context.filesDir, CUSTOM_FILE)
            if (customFile.exists() && customFile.length() > 0) {
                return customFile.readText()
            }
        } catch (_: Throwable) {}

        return try {
            context.assets.open(ASSET_FILE).bufferedReader().use { it.readText() }
        } catch (_: Throwable) {
            null
        }
    }

    fun saveCustomSymbols(context: Context, jsonContent: String): Boolean {
        return try {
            val file = File(context.filesDir, CUSTOM_FILE)
            file.writeText(jsonContent)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun resetToDefaults(context: Context): Boolean {
        return try {
            val file = File(context.filesDir, CUSTOM_FILE)
            if (file.exists()) file.delete() else true
        } catch (_: Throwable) {
            false
        }
    }

    private fun resolveLanguageKey(filePath: String): String {
        val lower = filePath.lowercase()
        return when {
            lower.endsWith(".kt") || lower.endsWith(".kts") -> "kotlin"
            lower.endsWith(".java") -> "java"
            lower.endsWith(".xml") -> "xml"
            lower.endsWith(".json") -> "json"
            lower.endsWith(".cpp") || lower.endsWith(".c") || lower.endsWith(".h") || lower.endsWith(".hpp") -> "cpp"
            else -> "default"
        }
    }

    private fun parseJsonArray(array: JSONArray): List<QuickSymbol> {
        val list = mutableListOf<QuickSymbol>()
        for (i in 0 until array.length()) {
            val item = array.get(i)
            when (item) {
                is String -> list.add(createSymbolFromString(item))
                is JSONObject -> {
                    val label = item.optString("label", item.optString("text", ""))
                    val insert = item.optString("insert", item.optString("insertText", label))
                    val offset = item.optInt("offset", item.optInt("cursorOffset", insert.length))
                    if (label.isNotEmpty()) {
                        list.add(QuickSymbol(label, insert, offset))
                    }
                }
            }
        }
        return list
    }

    private fun createSymbolFromString(sym: String): QuickSymbol {
        return when (sym) {
            "Tab", "\t" -> QuickSymbol("Tab", "    ", 4)
            "()" -> QuickSymbol("()", "()", 1)
            "{}" -> QuickSymbol("{}", "{}", 1)
            "[]" -> QuickSymbol("[]", "[]", 1)
            "\"\"" -> QuickSymbol("\"\"", "\"\"", 1)
            "''" -> QuickSymbol("''", "''", 1)
            "<!--" -> QuickSymbol("<!--", "<!-- ", 5)
            "-->" -> QuickSymbol("-->", " -->", 4)
            "<!-- -->" -> QuickSymbol("<!-- -->", "<!--  -->", 5)
            "//" -> QuickSymbol("//", "// ", 3)
            "/*" -> QuickSymbol("/*", "/* ", 3)
            "*/" -> QuickSymbol("*/", " */", 3)
            "->" -> QuickSymbol("->", " -> ", 4)
            "?:" -> QuickSymbol("?:", " ?: ", 4)
            "?." -> QuickSymbol("?.", "?.", 2)
            "::" -> QuickSymbol("::", "::", 2)
            "android:" -> QuickSymbol("android:", "android:", 8)
            "app:" -> QuickSymbol("app:", "app:", 4)
            "tools:" -> QuickSymbol("tools:", "tools:", 6)
            else -> QuickSymbol(label = sym, insertText = sym, cursorOffset = sym.length)
        }
    }
}

/**
 * Quick Symbol Input Bar matching Cipher IDE.
 * Formatted with Material 3 Expressive styling: tonal button aesthetics,
 * clean monospace typography, tactile haptics, and responsive horizontal scrolling.
 */
@Composable
fun QuickSymbolBar(
    filePath: String,
    onSymbolClick: (QuickSymbol) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val symbols = remember(filePath) {
        QuickSymbolManager.loadSymbols(context, filePath)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                symbols.forEach { symbol ->
                    SymbolButton(
                        symbol = symbol,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSymbolClick(symbol)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SymbolButton(
    symbol: QuickSymbol,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
            .height(32.dp)
            .defaultMinSize(minWidth = 34.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol.label,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
