package org.cat.editor.code

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.langs.kotlin.KotlinLanguage
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import java.io.File

@Composable
fun KotlinCodeEditor(
    filePath: String,
    onCodeChange: (String) -> Unit
) {
    val context = LocalContext.current
    var codeContent by remember { mutableStateOf("") }

    // Load file content saat pertama kali
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
            factory = { context ->
                CodeEditor(context).apply {
                    // Set language to Kotlin
                    setEditorLanguage(KotlinLanguage())
                    
                    // Set dark color scheme
                    colorScheme = KotlinDarkColorScheme()
                    
                    // Set text dari file
                    setText(codeContent)
                    
                    // Listener untuk perubahan text
                    subscribeEvent(object : CodeEditor.EditorEventListener {
                        override fun onNewline(editor: CodeEditor) {}
                        
                        override fun onClose(editor: CodeEditor) {}
                        
                        override fun beforeReplace(
                            editor: CodeEditor,
                            text: CharSequence
                        ) {
                        }
                        
                        override fun afterInsert(
                            editor: CodeEditor,
                            text: CharSequence,
                            length: Int
                        ) {
                            onCodeChange(editor.text.toString())
                        }
                        
                        override fun afterDelete(
                            editor: CodeEditor,
                            text: CharSequence,
                            length: Int
                        ) {
                            onCodeChange(editor.text.toString())
                        }
                    })
                    
                    // Other settings
                    isWordwrap = true
                    props.useICON_INTELLISENSE = true
                    props.useICON_GREP = true
                    props.useICON_SEMANTIC = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

class KotlinDarkColorScheme : EditorColorScheme() {
    init {
        // Background color
        background = 0xFF1E1E1E
        
        // Text colors
        foreground = 0xFFE0E0E0
        
        // Selection
        selectionForeground = 0xFF000000
        selectionBackground = 0xFF0E639C
        
        // Cursor
        caret = 0xFFAEAFAD
        secondaryTextColor = 0xFF8B8B8B
        
        // Line number
        lineNumberBackground = 0xFF1E1E1E
        lineNumber = 0xFF6A6A6A
        
        // Current line background
        currentLineBackground = 0xFF2D2D2D
        
        // Syntax highlighting
        
        // Keywords - Blue
        keyword = 0xFF569CD6
        
        // Strings - Red/Orange
        literal = 0xFFC77E70
        
        // Comments - Green
        comment = 0xFF6A9955
        
        // Operators - Blue
        operator = 0xFF89B4D8
        
        // Function names - Light Blue
        function = 0xFF9CDCFE
        
        // Classes/Types - Light Blue
        identifier = 0xFF9CDCFE
        
        // Numbers - Light Green
        number = 0xFFB5CEA8
        
        // Attributes/Variables
        attribute = 0xFFC8E6C9
        
        // Annotations - Light Yellow
        annotation = 0xFFDCDCAA
    }
}