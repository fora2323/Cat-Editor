package org.cat.editor.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.cat.editor.R
import org.cat.editor.utils.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderSettingsScreen(
    title: String,
    icon: ImageVector,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { SettingsManager.getPrefs(context) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    // State Fitur Cerdas
    var autoCompile by remember {
        mutableStateOf(prefs.getBoolean("editor_auto_compile", true))
    }
    var autoCloseTag by remember {
        mutableStateOf(prefs.getBoolean("editor_auto_close_tag", true))
    }
    var colorPreview by remember {
        mutableStateOf(prefs.getBoolean("editor_color_preview", true))
    }

    // State Konten
    var wordWrap by remember {
        mutableStateOf(prefs.getBoolean("editor_word_wrap", false))
    }
    var wrapTxt by remember {
        mutableStateOf(prefs.getBoolean("editor_wrap_txt", true))
    }
    var readOnlyMode by remember {
        mutableStateOf(prefs.getBoolean("editor_read_only", false))
    }

    // State Editor
    var showLineNumbers by remember {
        mutableStateOf(prefs.getBoolean("editor_line_numbers", true))
    }
    var disableVirtualKeyboard by remember {
        mutableStateOf(prefs.getBoolean("editor_disable_virtual_kbd", false))
    }
    var cursorAnimation by remember {
        mutableStateOf(prefs.getBoolean("editor_cursor_animation", true))
    }
    var stickyScroll by remember {
        mutableStateOf(prefs.getBoolean("editor_sticky_scroll", true))
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Kategori 1: Tema & Tampilan Kode
            EditorSettingsCategory(title = stringResource(id = R.string.editor_cat_appearance)) {
                EditorSwitchItem(
                    title = stringResource(id = R.string.editor_sticky_scroll_title),
                    description = stringResource(id = R.string.editor_sticky_scroll_desc),
                    checked = stickyScroll,
                    onCheckedChange = {
                        stickyScroll = it
                        prefs.edit().putBoolean("editor_sticky_scroll", it).apply()
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Kategori 2: Fitur Cerdas
            EditorSettingsCategory(title = stringResource(id = R.string.editor_cat_smart_features)) {
                EditorSwitchItem(
                    title = stringResource(id = R.string.editor_auto_compile_title),
                    description = stringResource(id = R.string.editor_auto_compile_desc),
                    checked = autoCompile,
                    onCheckedChange = {
                        autoCompile = it
                        prefs.edit().putBoolean("editor_auto_compile", it).apply()
                    }
                )
                EditorItemDivider()
                EditorSwitchItem(
                    title = stringResource(id = R.string.editor_auto_close_tag_title),
                    description = stringResource(id = R.string.editor_auto_close_tag_desc),
                    checked = autoCloseTag,
                    onCheckedChange = {
                        autoCloseTag = it
                        prefs.edit().putBoolean("editor_auto_close_tag", it).apply()
                    }
                )
                EditorItemDivider()
                EditorSwitchItem(
                    title = stringResource(id = R.string.editor_color_preview_title),
                    description = stringResource(id = R.string.editor_color_preview_desc),
                    checked = colorPreview,
                    onCheckedChange = {
                        colorPreview = it
                        prefs.edit().putBoolean("editor_color_preview", it).apply()
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Kategori 3: Konten
            EditorSettingsCategory(title = stringResource(id = R.string.editor_cat_content)) {
                EditorSwitchItem(
                    title = stringResource(id = R.string.editor_word_wrap_title),
                    description = stringResource(id = R.string.editor_word_wrap_desc),
                    checked = wordWrap,
                    onCheckedChange = {
                        wordWrap = it
                        prefs.edit().putBoolean("editor_word_wrap", it).apply()
                    }
                )
                EditorItemDivider()
                EditorSwitchItem(
                    title = stringResource(id = R.string.editor_wrap_txt_title),
                    description = stringResource(id = R.string.editor_wrap_txt_desc),
                    checked = wrapTxt,
                    onCheckedChange = {
                        wrapTxt = it
                        prefs.edit().putBoolean("editor_wrap_txt", it).apply()
                    }
                )
                EditorItemDivider()
                EditorSwitchItem(
                    title = stringResource(id = R.string.editor_read_only_title),
                    description = stringResource(id = R.string.editor_read_only_desc),
                    checked = readOnlyMode,
                    onCheckedChange = {
                        readOnlyMode = it
                        prefs.edit().putBoolean("editor_read_only", it).apply()
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Kategori 4: Editor
            EditorSettingsCategory(title = stringResource(id = R.string.editor_cat_editor)) {
                EditorSwitchItem(
                    title = stringResource(id = R.string.editor_line_numbers_title),
                    description = stringResource(id = R.string.editor_line_numbers_desc),
                    checked = showLineNumbers,
                    onCheckedChange = {
                        showLineNumbers = it
                        prefs.edit().putBoolean("editor_line_numbers", it).apply()
                    }
                )
                EditorItemDivider()
                EditorSwitchItem(
                    title = stringResource(id = R.string.editor_disable_virtual_kbd_title),
                    description = stringResource(id = R.string.editor_disable_virtual_kbd_desc),
                    checked = disableVirtualKeyboard,
                    onCheckedChange = {
                        disableVirtualKeyboard = it
                        prefs.edit().putBoolean("editor_disable_virtual_kbd", it).apply()
                    }
                )
                EditorItemDivider()
                EditorSwitchItem(
                    title = stringResource(id = R.string.editor_cursor_anim_title),
                    description = stringResource(id = R.string.editor_cursor_anim_desc),
                    checked = cursorAnimation,
                    onCheckedChange = {
                        cursorAnimation = it
                        prefs.edit().putBoolean("editor_cursor_animation", it).apply()
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun EditorSettingsCategory(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp, bottom = 10.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun EditorSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun EditorItemDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}
