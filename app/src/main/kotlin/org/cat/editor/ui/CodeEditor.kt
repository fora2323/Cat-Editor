package org.cat.editor.ui

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.PopupWindow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
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
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import org.cat.editor.ui.preview.isBinaryFile
import org.cat.editor.utils.SettingsManager
import org.eclipse.tm4e.core.registry.IThemeSource
import java.io.File

private const val TAG = "CodeEditor"

data class EditorThemeColors(
    val surface: Int,
    val lineAreaBackground: Int,
    val onSurface: Int,
    val primary: Int,
    val surfaceContainerHighest: Int,
    val surfaceContainer: Int,
    val outline: Int,
    val outlineVariant: Int
) {
    companion object {
        fun defaultDark(): EditorThemeColors = EditorThemeColors(
            surface = 0xFF141218.toInt(),
            lineAreaBackground = 0xFF1D1B20.toInt(),
            onSurface = 0xFFE6E1E5.toInt(),
            primary = 0xFFD0BCFF.toInt(),
            surfaceContainerHighest = 0xFF2B2930.toInt(),
            surfaceContainer = 0xFF211F26.toInt(),
            outline = 0xFF938F99.toInt(),
            outlineVariant = 0xFF49454F.toInt()
        )
    }
}

private fun getScopeForFile(filePath: String): String? {
    if (filePath.isBlank()) return null
    val file = File(filePath)
    if (!file.exists() || file.isDirectory) return null
    val name = file.name.lowercase()
    if (name == "cmakelists.txt") return "source.cmake"
    return when (file.extension.lowercase()) {
        "kt", "kts" -> "source.kotlin"
        "java" -> "source.java"
        "py", "pyw" -> "source.python"
        "cpp", "cc", "cxx", "hpp", "c", "h" -> "source.cpp"
        "gradle", "groovy" -> "source.groovy"
        "xml", "html", "htm", "xhtml", "svg" -> "text.xml"
        "cmake" -> "source.cmake"
        else -> null
    }
}

private var textMateInitialized = false

private val THEME_FILES = listOf(
    "material3_dark.json" to "Material3 Dark",
    "material3_light.json" to "Material3 Light",
    "github_dark.json" to "GitHub Dark",
    "github_light.json" to "GitHub Light",
    "one_dark.json" to "One Dark",
    "one_light.json" to "One Light",
    "dracula.json" to "Dracula",
    "solarized_light.json" to "Solarized Light",
    "material3.json" to "Material3"
)

fun resolveThemeName(context: Context, isDark: Boolean): String {
    val prefs = SettingsManager.getPrefs(context)
    val customPresetsEnabled = prefs.getBoolean("editor_custom_presets_enabled", false)
    val preset = if (customPresetsEnabled) {
        prefs.getString("editor_color_preset", "material3") ?: "material3"
    } else {
        "material3"
    }

    return when (preset.lowercase()) {
        "github" -> if (isDark) "GitHub Dark" else "GitHub Light"
        "one" -> if (isDark) "One Dark" else "One Light"
        "dracula" -> if (isDark) "Dracula" else "Solarized Light"
        else -> if (isDark) "Material3 Dark" else "Material3 Light"
    }
}

@Synchronized
fun ensureTextMateInitialized(context: Context) {
    if (textMateInitialized) return
    try {
        FileProviderRegistry.getInstance().addFileProvider(
            AssetsFileResolver(context.assets)
        )
        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")

        for ((file, name) in THEME_FILES) {
            try {
                val stream = context.assets.open("textmate/$file")
                val themeSource = IThemeSource.fromInputStream(stream, file, null)
                val model = ThemeModel(themeSource, name)
                ThemeRegistry.getInstance().loadTheme(model)
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to load theme $file: ${e.message}")
            }
        }
        textMateInitialized = true
        Log.d(TAG, "TextMate grammars and theme presets successfully initialized")
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

fun applyCompletionWindowTheme(
    scheme: EditorColorScheme,
    colors: EditorThemeColors = EditorThemeColors.defaultDark()
) {
    scheme.setColor(EditorColorScheme.COMPLETION_WND_BACKGROUND, colors.surfaceContainer)
    scheme.setColor(EditorColorScheme.COMPLETION_WND_CORNER, colors.outlineVariant)
    scheme.setColor(EditorColorScheme.COMPLETION_WND_TEXT_PRIMARY, colors.onSurface)
    scheme.setColor(EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY, colors.outline)
    val itemCurrent = (colors.primary and 0x00FFFFFF) or 0x33000000
    scheme.setColor(EditorColorScheme.COMPLETION_WND_ITEM_CURRENT, itemCurrent)
}

fun applyThemeColors(
    scheme: EditorColorScheme,
    colors: EditorThemeColors
) {
    scheme.setColor(EditorColorScheme.WHOLE_BACKGROUND, colors.surface)
    scheme.setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, colors.lineAreaBackground)
    scheme.setColor(EditorColorScheme.LINE_NUMBER, colors.outline)
    try { scheme.setColor(EditorColorScheme.LINE_NUMBER_CURRENT, colors.primary) } catch (_: Throwable) {}
    try { scheme.setColor(EditorColorScheme.LINE_NUMBER_PANEL, colors.lineAreaBackground) } catch (_: Throwable) {}
    try { scheme.setColor(EditorColorScheme.LINE_NUMBER_PANEL_TEXT, colors.outline) } catch (_: Throwable) {}
    scheme.setColor(EditorColorScheme.CURRENT_LINE, colors.surfaceContainerHighest)
    val selectionAlpha = (colors.primary and 0x00FFFFFF) or 0x4D000000
    scheme.setColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND, selectionAlpha)
    scheme.setColor(EditorColorScheme.SELECTION_HANDLE, colors.primary)
    try { scheme.setColor(EditorColorScheme.SELECTION_INSERT, colors.primary) } catch (_: Throwable) {}
    try { scheme.setColor(EditorColorScheme.LINE_DIVIDER, colors.outlineVariant) } catch (_: Throwable) {}
    try { scheme.setColor(EditorColorScheme.BLOCK_LINE, colors.outlineVariant) } catch (_: Throwable) {}
    try { scheme.setColor(EditorColorScheme.STICKY_SCROLL_DIVIDER, colors.outlineVariant) } catch (_: Throwable) {}
    try { scheme.setColor(EditorColorScheme.TEXT_NORMAL, colors.onSurface) } catch (_: Throwable) {}

    // Delimiter / bracket highlighting (when cursor is beside a bracket, like '(')
    val delimiterBg = (colors.primary and 0x00FFFFFF) or 0x33000000
    try { scheme.setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_BACKGROUND, delimiterBg) } catch (_: Throwable) {}
    try { scheme.setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND, colors.primary) } catch (_: Throwable) {}
    try { scheme.setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_UNDERLINE, colors.primary) } catch (_: Throwable) {}
    try { scheme.setColor(EditorColorScheme.MATCHED_TEXT_BACKGROUND, delimiterBg) } catch (_: Throwable) {}

    applyCompletionWindowTheme(scheme, colors)
}

fun applyThemeColors(scheme: EditorColorScheme, surfaceColorInt: Int, dividerColorInt: Int) {
    applyThemeColors(
        scheme,
        EditorThemeColors.defaultDark().copy(
            surface = surfaceColorInt,
            lineAreaBackground = surfaceColorInt,
            outlineVariant = dividerColorInt
        )
    )
}

private fun createEditorColorScheme(context: Context, colors: EditorThemeColors): EditorColorScheme {
    return try {
        val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(colors.surface) < 0.5
        val targetTheme = resolveThemeName(context, isDark)
        try {
            ThemeRegistry.getInstance().setTheme(targetTheme)
        } catch (_: Throwable) {}

        val themeModel = ThemeRegistry.getInstance().findThemeByThemeName(targetTheme)
            ?: ThemeRegistry.getInstance().currentThemeModel
        val scheme = if (themeModel != null) {
            TextMateColorScheme.create(ThemeRegistry.getInstance(), themeModel)
        } else {
            TextMateColorScheme.create(ThemeRegistry.getInstance())
        }
        applyCompletionWindowTheme(scheme, colors)
        scheme
    } catch (e: Throwable) {
        Log.w(TAG, "Failed to create TextMateColorScheme, falling back to KotlinDarkColorScheme", e)
        KotlinDarkColorScheme(colors)
    }
}

private fun applyLanguageAndTheme(
    editor: CodeEditor,
    filePath: String,
    autoCloseTag: Boolean,
    colors: EditorThemeColors
) {
    val file = File(filePath)
    if (!file.exists() || file.isDirectory) {
        val scheme = KotlinDarkColorScheme(colors)
        editor.colorScheme = scheme
        applyThemeColors(scheme, colors)
        try { editor.setEditorLanguage(EmptyLanguage()) } catch (_: Throwable) {}
        return
    }

    val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(colors.surface) < 0.5
    val targetTheme = resolveThemeName(editor.context, isDark)
    try {
        ThemeRegistry.getInstance().setTheme(targetTheme)
    } catch (_: Throwable) {}

    val lang = createEditorLanguage(filePath, autoCloseTag)
    val scheme = if (lang is TextMateLanguage) {
        try {
            createEditorColorScheme(editor.context, colors)
        } catch (_: Throwable) {
            KotlinDarkColorScheme(colors)
        }
    } else {
        KotlinDarkColorScheme(colors)
    }

    // Assign colorScheme to editor FIRST, so its attachEditor/setTheme finishes,
    // and THEN apply our custom Material 3 colors so they are never wiped by default settings!
    editor.colorScheme = scheme
    applyThemeColors(scheme, colors)

    try {
        editor.setEditorLanguage(lang)
    } catch (_: Throwable) {
        try { editor.setEditorLanguage(EmptyLanguage()) } catch (_: Throwable) {}
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

private fun applyEditorSettings(
    editor: CodeEditor,
    filePath: String,
    colors: EditorThemeColors? = null
) {
    val context = editor.context
    val prefs = SettingsManager.getPrefs(context)
    val isTxtFile = filePath.lowercase().endsWith(".txt")

    val wordWrap = prefs.getBoolean("editor_word_wrap", false)
    val wrapTxt = prefs.getBoolean("editor_wrap_txt", true)
    val showLineNumbers = prefs.getBoolean("editor_line_numbers", true)
    val readOnly = prefs.getBoolean("editor_read_only", false)
    val cursorAnimation = prefs.getBoolean("editor_cursor_animation", true)
    val stickyScroll = prefs.getBoolean("editor_sticky_scroll", true)

    editor.typefaceText = SettingsManager.getEditorTypeface(context)
    editor.isWordwrap = wordWrap || (isTxtFile && wrapTxt)
    editor.isLineNumberEnabled = showLineNumbers
    editor.isEditable = !readOnly
    editor.isCursorAnimationEnabled = cursorAnimation

    try {
        editor.props.stickyScroll = stickyScroll
        editor.props.stickyScrollMaxLines = 3
        editor.props.stickyScrollAutoCollapse = true
    } catch (_: Throwable) {}

    // Check if syntax color theme needs update (e.g. preset changed in Settings or mode changed)
    if (colors != null) {
        val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(colors.surface) < 0.5
        val targetTheme = resolveThemeName(context, isDark)
        val currentTheme = ThemeRegistry.getInstance().currentThemeModel?.name
        if (currentTheme != targetTheme) {
            try {
                ThemeRegistry.getInstance().setTheme(targetTheme)
                (editor.colorScheme as? TextMateColorScheme)?.let { tmScheme ->
                    ThemeRegistry.getInstance().findThemeByThemeName(targetTheme)?.let { model ->
                        tmScheme.setTheme(model)
                    }
                }
                applyThemeColors(editor.colorScheme, colors)
            } catch (_: Throwable) {}
        }
    }

    editor.colorScheme?.let { scheme ->
        if (colors != null) {
            applyCompletionWindowTheme(scheme, colors)
        } else {
            applyCompletionWindowTheme(scheme)
        }
    }
    try {
        val autoCompletion = editor.getComponent(EditorAutoCompletion::class.java)
        autoCompletion?.apply {
            applyColorScheme()
            popup?.apply {
                inputMethodMode = PopupWindow.INPUT_METHOD_NEEDED
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                isClippingEnabled = true
            }
        }
    } catch (_: Throwable) {}

    hideSoftKeyboardIfNeeded(editor)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KotlinCodeEditor(
    filePath: String,
    onCodeChange: (String) -> Unit,
    onEditorInteraction: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val m3Scheme = MaterialTheme.colorScheme
    val editorColors = remember(m3Scheme) {
        val surfaceContainer = m3Scheme.surfaceContainer.toArgb()
        val surfaceContainerLow = m3Scheme.surfaceContainerLow.toArgb()
        val lineAreaBg = if (surfaceContainerLow != 0) surfaceContainerLow else surfaceContainer
        EditorThemeColors(
            surface = m3Scheme.surface.toArgb(),
            lineAreaBackground = lineAreaBg,
            onSurface = m3Scheme.onSurface.toArgb(),
            primary = m3Scheme.primary.toArgb(),
            surfaceContainerHighest = m3Scheme.surfaceContainerHighest.toArgb(),
            surfaceContainer = surfaceContainer,
            outline = m3Scheme.outline.toArgb(),
            outlineVariant = if (m3Scheme.outlineVariant.toArgb() != 0) m3Scheme.outlineVariant.toArgb() else 0xFF49454F.toInt()
        )
    }

    LaunchedEffect(Unit) {
        ensureTextMateInitialized(context)
    }

    // Read initial text synchronously so editor renders immediately with code
    val initialContent = remember(filePath) {
        if (filePath.isNotEmpty() && !isBinaryFile(filePath)) {
            try {
                val file = File(filePath)
                if (file.exists() && file.isFile) file.readText() else ""
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to read file $filePath", e)
                ""
            }
        } else ""
    }

    var currentLoadedPath by remember { mutableStateOf(filePath) }
    var editorInstance by remember { mutableStateOf<CodeEditor?>(null) }
    var isProgrammaticTextUpdate by remember { mutableStateOf(false) }

    // Perbarui pengaturan editor secara otomatis saat kembali dari halaman Settings
    DisposableEffect(lifecycleOwner, filePath, editorColors) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                editorInstance?.let { editor ->
                    applyEditorSettings(editor, filePath, editorColors)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(m3Scheme.surface)
            .imePadding()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                factory = { ctx ->
                    ensureTextMateInitialized(ctx)
                    val autoCloseTag = SettingsManager.getPrefs(ctx).getBoolean("editor_auto_close_tag", true)

                    CodeEditor(ctx).apply {
                        editorInstance = this
                        currentLoadedPath = filePath
                        isHorizontalScrollBarEnabled = true
                        setDividerWidth(2f)

                        applyLanguageAndTheme(this, filePath, autoCloseTag, editorColors)
                        isProgrammaticTextUpdate = true
                        try {
                            setText(initialContent)
                        } catch (_: Throwable) {
                        } finally {
                            isProgrammaticTextUpdate = false
                        }
                        setTextSize(14f)
                        applyEditorSettings(this, filePath, editorColors)

                        subscribeAlways(ContentChangeEvent::class.java) { _ ->
                            if (!isProgrammaticTextUpdate) {
                                onCodeChange(text.toString())
                            }
                        }

                        subscribeAlways(SelectionChangeEvent::class.java) { _ ->
                            onEditorInteraction?.invoke()
                        }

                        subscribeAlways(ClickEvent::class.java) { _ ->
                            onEditorInteraction?.invoke()
                            hideSoftKeyboardIfNeeded(this)
                        }

                        setOnTouchListener { v, event ->
                            when (event.actionMasked) {
                                android.view.MotionEvent.ACTION_DOWN -> {
                                    v.parent?.requestDisallowInterceptTouchEvent(true)
                                    onEditorInteraction?.invoke()
                                }
                                android.view.MotionEvent.ACTION_MOVE -> {
                                    v.parent?.requestDisallowInterceptTouchEvent(true)
                                }
                                android.view.MotionEvent.ACTION_UP,
                                android.view.MotionEvent.ACTION_CANCEL -> {
                                    v.parent?.requestDisallowInterceptTouchEvent(false)
                                }
                            }
                            false
                        }
                    }
                },
                update = { editor ->
                    editorInstance = editor
                    applyEditorSettings(editor, filePath, editorColors)
                    editor.colorScheme?.let { applyThemeColors(it, editorColors) }

                    // Only reload text when switching files, preserving cursor & active edits
                    if (currentLoadedPath != filePath) {
                        currentLoadedPath = filePath
                        val autoCloseTag = SettingsManager.getPrefs(editor.context).getBoolean("editor_auto_close_tag", true)

                        val file = File(filePath)
                        val textToSet = if (file.exists() && file.isFile && !isBinaryFile(filePath)) {
                            try {
                                file.readText()
                            } catch (_: Throwable) {
                                try {
                                    file.readBytes().toString(Charsets.ISO_8859_1)
                                } catch (_: Throwable) {
                                    ""
                                }
                            }
                        } else ""

                        isProgrammaticTextUpdate = true
                        try {
                            editor.setText(textToSet)
                        } catch (e: Throwable) {
                            Log.e(TAG, "Error setting text in editor", e)
                        } finally {
                            isProgrammaticTextUpdate = false
                        }

                        applyLanguageAndTheme(editor, filePath, autoCloseTag, editorColors)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        QuickSymbolBar(
            filePath = filePath,
            onSymbolClick = { symbol ->
                editorInstance?.let { editor ->
                    try {
                        val cursor = editor.cursor
                        val leftLine = cursor.leftLine
                        val leftColumn = cursor.leftColumn
                        editor.text.insert(leftLine, leftColumn, symbol.insertText)
                        if (symbol.cursorOffset != symbol.insertText.length) {
                            val newCol = leftColumn + symbol.cursorOffset
                            editor.setSelection(leftLine, newCol)
                        }
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error inserting quick symbol", e)
                    }
                }
            }
        )
    }
}

class KotlinDarkColorScheme(
    colors: EditorThemeColors = EditorThemeColors.defaultDark()
) : EditorColorScheme() {

    constructor(surfaceColorInt: Int, dividerColorInt: Int) : this(
        EditorThemeColors.defaultDark().copy(
            surface = surfaceColorInt,
            lineAreaBackground = surfaceColorInt,
            outlineVariant = dividerColorInt
        )
    )

    init {
        val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(colors.surface) < 0.5
        setColor(WHOLE_BACKGROUND, colors.surface)
        setColor(TEXT_NORMAL, colors.onSurface)
        val selectionAlpha = (colors.primary and 0x00FFFFFF) or 0x4D000000
        setColor(SELECTED_TEXT_BACKGROUND, selectionAlpha)
        setColor(SELECTION_HANDLE, colors.primary)
        setColor(LINE_NUMBER_BACKGROUND, colors.lineAreaBackground)
        setColor(LINE_NUMBER, colors.outline)
        try { setColor(LINE_NUMBER_CURRENT, colors.primary) } catch (_: Throwable) {}
        try { setColor(LINE_NUMBER_PANEL, colors.lineAreaBackground) } catch (_: Throwable) {}
        try { setColor(LINE_NUMBER_PANEL_TEXT, colors.outline) } catch (_: Throwable) {}
        setColor(CURRENT_LINE, colors.surfaceContainerHighest)

        val keywordColor = if (isDark) 0xFFA8C7FA.toInt() else 0xFF005FB0.toInt()
        val literalColor = if (isDark) 0xFFA8DAB5.toInt() else 0xFF006E1C.toInt()
        val commentColor = if (isDark) 0xFF938F99.toInt() else 0xFF79747E.toInt()
        val operatorColor = if (isDark) 0xFFCAC4D0.toInt() else 0xFF49454F.toInt()
        val functionColor = if (isDark) 0xFF7CD5FF.toInt() else 0xFF5E35B1.toInt()
        val annotationColor = if (isDark) 0xFFF5D67B.toInt() else 0xFF785900.toInt()

        setColor(KEYWORD, keywordColor)
        setColor(LITERAL, literalColor)
        setColor(COMMENT, commentColor)
        setColor(OPERATOR, operatorColor)
        setColor(FUNCTION_NAME, functionColor)
        setColor(IDENTIFIER_NAME, colors.onSurface)
        setColor(ANNOTATION, annotationColor)
        try { setColor(LINE_DIVIDER, colors.outlineVariant) } catch (_: Throwable) {}
        try { setColor(BLOCK_LINE, colors.outlineVariant) } catch (_: Throwable) {}
        try { setColor(STICKY_SCROLL_DIVIDER, colors.outlineVariant) } catch (_: Throwable) {}

        val delimiterBg = (colors.primary and 0x00FFFFFF) or 0x33000000
        try { setColor(HIGHLIGHTED_DELIMITERS_BACKGROUND, delimiterBg) } catch (_: Throwable) {}
        try { setColor(HIGHLIGHTED_DELIMITERS_FOREGROUND, colors.primary) } catch (_: Throwable) {}
        try { setColor(HIGHLIGHTED_DELIMITERS_UNDERLINE, colors.primary) } catch (_: Throwable) {}
        try { setColor(MATCHED_TEXT_BACKGROUND, delimiterBg) } catch (_: Throwable) {}

        applyCompletionWindowTheme(this, colors)
    }
}
