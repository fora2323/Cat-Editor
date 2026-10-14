package org.cat.editor.ui

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.rosemoe.sora.event.ClickEvent
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import org.cat.editor.ui.preview.isBinaryFile
import org.cat.editor.utils.SettingsManager
import org.eclipse.tm4e.core.registry.IThemeSource
import java.io.File

private const val TAG = "CodeEditor"

private fun getScopeForFile(filePath: String): String? {
    return when (File(filePath).extension.lowercase()) {
        "kt", "kts" -> "source.kotlin"
        "java" -> "source.java"
        "cpp", "cc", "cxx", "hpp", "c", "h" -> "source.cpp"
        "gradle", "groovy" -> "source.groovy"
        "xml", "html", "htm", "xhtml", "svg" -> "text.xml"
        "cmake" -> "source.cmake"
        else -> null
    }
}

private var textMateInitialized = false

@Synchronized
fun ensureTextMateInitialized(context: Context) {
    if (textMateInitialized) return
    try {
        FileProviderRegistry.getInstance().addFileProvider(
            AssetsFileResolver(context.assets)
        )
        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")

        val inputStream = context.assets.open("textmate/darcula.json")
        val themeSource = IThemeSource.fromInputStream(inputStream, "darcula.json", null)
        ThemeRegistry.getInstance().loadTheme(themeSource)
        ThemeRegistry.getInstance().setTheme("Darcula")
        textMateInitialized = true
        Log.d(TAG, "TextMate grammars and Darcula theme successfully initialized")
    } catch (e: Throwable) {
        Log.e(TAG, "Failed to initialize TextMate environment", e)
    }
}

private fun createEditorLanguage(filePath: String, autoComplete: Boolean = true): Language {
    val scope = getScopeForFile(filePath) ?: return EmptyLanguage()
    return try {
        TextMateLanguage.create(scope, autoComplete)
    } catch (e: Throwable) {
        Log.w(TAG, "Failed to create TextMateLanguage for $scope, falling back to basic/empty", e)
        try {
            TextMateLanguage.create(scope, false)
        } catch (_: Throwable) {
            EmptyLanguage()
        }
    }
}

private fun createEditorColorScheme(): EditorColorScheme {
    return try {
        TextMateColorScheme.create(ThemeRegistry.getInstance())
    } catch (e: Throwable) {
        Log.w(TAG, "Failed to create TextMateColorScheme, falling back to KotlinDarkColorScheme", e)
        KotlinDarkColorScheme()
    }
}

private fun hideSoftKeyboardIfNeeded(editor: CodeEditor) {
    val context = editor.context
    val disableVirtualKbd = SettingsManager.getPrefs(context).getBoolean("editor_disable_virtual_kbd", false)
    val hasPhysicalKeyboard = context.resources.configuration.keyboard != Configuration.KEYBOARD_NOKEYS

    if (disableVirtualKbd && hasPhysicalKeyboard) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(editor.windowToken, 0)
    }
}

private fun applyEditorSettings(editor: CodeEditor, filePath: String) {
    val context = editor.context
    val prefs = SettingsManager.getPrefs(context)
    val isTxtFile = filePath.lowercase().endsWith(".txt")

    val wordWrap = prefs.getBoolean("editor_word_wrap", false)
    val wrapTxt = prefs.getBoolean("editor_wrap_txt", true)
    val showLineNumbers = prefs.getBoolean("editor_line_numbers", true)
    val readOnly = prefs.getBoolean("editor_read_only", false)
    val cursorAnimation = prefs.getBoolean("editor_cursor_animation", true)

    editor.typefaceText = SettingsManager.getEditorTypeface(context)
    editor.isWordwrap = wordWrap || (isTxtFile && wrapTxt)
    editor.isLineNumberEnabled = showLineNumbers
    editor.isEditable = !readOnly
    editor.isCursorAnimationEnabled = cursorAnimation

    hideSoftKeyboardIfNeeded(editor)
}

@Composable
fun KotlinCodeEditor(
    filePath: String,
    onCodeChange: (String) -> Unit,
    onEditorInteraction: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        ensureTextMateInitialized(context)
    }

    // Read initial text synchronously so editor renders immediately with code
    val initialContent = remember(filePath) {
        if (filePath.isNotEmpty() && !isBinaryFile(filePath)) {
            try {
                val file = File(filePath)
                if (file.exists()) file.readText() else ""
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to read file $filePath", e)
                ""
            }
        } else ""
    }

    var currentLoadedPath by remember { mutableStateOf(filePath) }
    var editorInstance by remember { mutableStateOf<CodeEditor?>(null) }

    // Perbarui pengaturan editor secara otomatis saat kembali dari halaman Settings
    DisposableEffect(lifecycleOwner, filePath) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                editorInstance?.let { editor ->
                    applyEditorSettings(editor, filePath)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    onEditorInteraction?.invoke()
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                ensureTextMateInitialized(ctx)
                val autoCloseTag = SettingsManager.getPrefs(ctx).getBoolean("editor_auto_close_tag", true)

                CodeEditor(ctx).apply {
                    editorInstance = this
                    currentLoadedPath = filePath

                    colorScheme = createEditorColorScheme()
                    setEditorLanguage(createEditorLanguage(filePath, autoCloseTag))
                    setText(initialContent)
                    setTextSize(14f)
                    applyEditorSettings(this, filePath)

                    subscribeAlways(ContentChangeEvent::class.java) { _ ->
                        onCodeChange(text.toString())
                    }

                    subscribeAlways(SelectionChangeEvent::class.java) { _ ->
                        onEditorInteraction?.invoke()
                    }

                    subscribeAlways(ClickEvent::class.java) { _ ->
                        onEditorInteraction?.invoke()
                        hideSoftKeyboardIfNeeded(this)
                    }

                    setOnTouchListener { _, event ->
                        if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                            onEditorInteraction?.invoke()
                        }
                        false
                    }
                }
            },
            update = { editor ->
                editorInstance = editor
                applyEditorSettings(editor, filePath)

                // Only reload text when switching files, preserving cursor & active edits
                if (currentLoadedPath != filePath) {
                    currentLoadedPath = filePath
                    val autoCloseTag = SettingsManager.getPrefs(editor.context).getBoolean("editor_auto_close_tag", true)
                    editor.colorScheme = createEditorColorScheme()
                    editor.setEditorLanguage(createEditorLanguage(filePath, autoCloseTag))
                    val file = File(filePath)
                    val textToSet = if (file.exists() && !isBinaryFile(filePath)) {
                        try { file.readText() } catch (_: Throwable) { "" }
                    } else ""
                    editor.setText(textToSet)
                }
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