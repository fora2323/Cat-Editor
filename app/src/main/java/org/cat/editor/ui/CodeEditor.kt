package org.cat.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import java.io.File

private fun getScopeForFile(filePath: String): String {
    return when (File(filePath).extension.lowercase()) {
        "kt", "kts" -> "source.kotlin"
        "java" -> "source.java"
        "cpp", "cc", "cxx", "hpp", "c", "h" -> "source.cpp"
        "gradle", "gradle.kts" -> "source.groovy"
        "xml" -> "text.xml"
        "cmake" -> "source.cmake"
        else -> "source.kotlin"
    }
}

private var grammarRegistered = false

@Composable
fun KotlinCodeEditor(
    filePath: String,
    onCodeChange: (String) -> Unit
) {
    val context = LocalContext.current
    var codeContent by remember { mutableStateOf("") }

    LaunchedEffect(filePath) {
        if (filePath.isNotEmpty()) {
            try {
                val file = File(filePath)
                if (file.exists()) {
                    codeContent = file.readText()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        AndroidView(
            factory = { ctx ->
                if (!grammarRegistered) {
                    FileProviderRegistry.getInstance().addFileProvider(
                        AssetsFileResolver(ctx.assets)
                    )
                    GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
                    grammarRegistered = true
                }

                CodeEditor(ctx).apply {
                    val scope = getScopeForFile(filePath)
                    setEditorLanguage(TextMateLanguage.create(scope, true))
                    colorScheme = KotlinDarkColorScheme()
                    setText(codeContent)
                    isWordwrap = true

                    subscribeAlways(ContentChangeEvent::class.java) { _ ->
                        onCodeChange(text.toString())
                    }
                }
            },
            update = { editor ->
                val scope = getScopeForFile(filePath)
                editor.setEditorLanguage(TextMateLanguage.create(scope, true))
                editor.setText(codeContent)
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

class KotlinDarkColorScheme : EditorColorScheme() {
    init {
        setColor(WHOLE_BACKGROUND, 0xFF1E1E1E.toInt())
        setColor(TEXT_NORMAL, 0xFFE0E0E0.toInt())
        setColor(SELECTED_TEXT_BACKGROUND, 0xFF0E639C.toInt())
        setColor(SELECTION_HANDLE, 0xFFAEAFAD.toInt())
        setColor(LINE_NUMBER_BACKGROUND, 0xFF1E1E1E.toInt())
        setColor(LINE_NUMBER, 0xFF6A6A6A.toInt())
        setColor(CURRENT_LINE, 0xFF2D2D2D.toInt())
        setColor(KEYWORD, 0xFF569CD6.toInt())
        setColor(LITERAL, 0xFFC77E70.toInt())
        setColor(COMMENT, 0xFF6A9955.toInt())
        setColor(OPERATOR, 0xFF89B4D8.toInt())
        setColor(FUNCTION_NAME, 0xFF9CDCFE.toInt())
        setColor(IDENTIFIER_NAME, 0xFF9CDCFE.toInt())
        setColor(ANNOTATION, 0xFFDCDCAA.toInt())
    }
}