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
    var customPresetsEnabled by remember {
        mutableStateOf(prefs.getBoolean("editor_custom_presets_enabled", false))
    }
    var selectedPresetId by remember {
        mutableStateOf(prefs.getString("editor_color_preset", "material3") ?: "material3")
    }
    var previewIsDark by remember {
        mutableStateOf(true)
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
                EditorItemDivider()
                EditorSwitchItem(
                    title = stringResource(id = R.string.editor_custom_presets_title),
                    description = stringResource(id = R.string.editor_custom_presets_desc),
                    checked = customPresetsEnabled,
                    onCheckedChange = {
                        customPresetsEnabled = it
                        prefs.edit().putBoolean("editor_custom_presets_enabled", it).apply()
                    }
                )

                if (customPresetsEnabled) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Preset Palettes",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val presets = remember { getCodePresets() }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            presets.forEach { preset ->
                                val isSelected = preset.id.equals(selectedPresetId, ignoreCase = true)
                                PresetChipItem(
                                    preset = preset,
                                    isSelected = isSelected,
                                    onClick = {
                                        selectedPresetId = preset.id
                                        prefs.edit().putString("editor_color_preset", preset.id).apply()
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val currentPreset = presets.firstOrNull { it.id.equals(selectedPresetId, ignoreCase = true) }
                            ?: presets.first()

                        CodePresetPreviewCard(
                            preset = currentPreset,
                            isDark = previewIsDark,
                            onToggleDark = { previewIsDark = !previewIsDark }
                        )
                    }
                }
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

data class CodePreset(
    val id: String,
    val name: String,
    val description: String,
    val darkColors: CodePresetColors,
    val lightColors: CodePresetColors
)

data class CodePresetColors(
    val background: Color,
    val gutterBackground: Color,
    val text: Color,
    val keyword: Color,
    val string: Color,
    val type: Color,
    val function: Color,
    val annotation: Color
)

fun getCodePresets(): List<CodePreset> = listOf(
    CodePreset(
        id = "material3",
        name = "Material 3",
        description = "Adaptive Expressive M3",
        darkColors = CodePresetColors(
            background = Color(0xFF141218),
            gutterBackground = Color(0xFF1D1B20),
            text = Color(0xFFE6E1E5),
            keyword = Color(0xFFA8C7FA),
            string = Color(0xFFA8DAB5),
            type = Color(0xFFFDE28A),
            function = Color(0xFF7CD5FF),
            annotation = Color(0xFFF5D67B)
        ),
        lightColors = CodePresetColors(
            background = Color(0xFFFEF7FF),
            gutterBackground = Color(0xFFF3EDF7),
            text = Color(0xFF1D1B20),
            keyword = Color(0xFF005FB0),
            string = Color(0xFF006E1C),
            type = Color(0xFF006874),
            function = Color(0xFF5E35B1),
            annotation = Color(0xFF785900)
        )
    ),
    CodePreset(
        id = "github",
        name = "GitHub",
        description = "Industry standard GitHub",
        darkColors = CodePresetColors(
            background = Color(0xFF0D1117),
            gutterBackground = Color(0xFF161B22),
            text = Color(0xFFE6EDF3),
            keyword = Color(0xFFFF7B72),
            string = Color(0xFFA5D6FF),
            type = Color(0xFFFFA657),
            function = Color(0xFFD2A8FF),
            annotation = Color(0xFFD2A8FF)
        ),
        lightColors = CodePresetColors(
            background = Color(0xFFFFFFFF),
            gutterBackground = Color(0xFFF6F8FA),
            text = Color(0xFF24292F),
            keyword = Color(0xFFCF222E),
            string = Color(0xFF0A3069),
            type = Color(0xFF953800),
            function = Color(0xFF8250DF),
            annotation = Color(0xFF8250DF)
        )
    ),
    CodePreset(
        id = "one",
        name = "One (Atom)",
        description = "Modern One Dark / Light",
        darkColors = CodePresetColors(
            background = Color(0xFF282C34),
            gutterBackground = Color(0xFF21252B),
            text = Color(0xFFABB2BF),
            keyword = Color(0xFFC678DD),
            string = Color(0xFF98C379),
            type = Color(0xFFE5C07B),
            function = Color(0xFF61AFEF),
            annotation = Color(0xFFE5C07B)
        ),
        lightColors = CodePresetColors(
            background = Color(0xFFFAFAFA),
            gutterBackground = Color(0xFFF0F0F0),
            text = Color(0xFF383A42),
            keyword = Color(0xFFA626A4),
            string = Color(0xFF50A14F),
            type = Color(0xFFC18401),
            function = Color(0xFF4078F2),
            annotation = Color(0xFFC18401)
        )
    ),
    CodePreset(
        id = "dracula",
        name = "Dracula",
        description = "Dracula & Solarized",
        darkColors = CodePresetColors(
            background = Color(0xFF282A36),
            gutterBackground = Color(0xFF21222C),
            text = Color(0xFFF8F8F2),
            keyword = Color(0xFFFF79C6),
            string = Color(0xFFF1FA8C),
            type = Color(0xFF8BE9FD),
            function = Color(0xFF50FA7B),
            annotation = Color(0xFF50FA7B)
        ),
        lightColors = CodePresetColors(
            background = Color(0xFFFDF6E3),
            gutterBackground = Color(0xFFEEE8D5),
            text = Color(0xFF657B83),
            keyword = Color(0xFF859900),
            string = Color(0xFF2AA198),
            type = Color(0xFFB58900),
            function = Color(0xFF268BD2),
            annotation = Color(0xFFCB4B16)
        )
    )
)

@Composable
private fun PresetChipItem(
    preset: CodePreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    Box(
        modifier = Modifier
            .width(135.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = preset.description,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Color circles palette dots
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(preset.darkColors.keyword)
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(preset.darkColors.string)
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(preset.darkColors.type)
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(preset.darkColors.function)
                )
            }
        }
    }
}

@Composable
private fun CodePresetPreviewCard(
    preset: CodePreset,
    isDark: Boolean,
    onToggleDark: () -> Unit
) {
    val colors = if (isDark) preset.darkColors else preset.lightColors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Card Header: Title + Light/Dark Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "• " + if (isDark) stringResource(id = R.string.editor_preview_dark) else stringResource(id = R.string.editor_preview_light),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Interactive Toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(onClick = onToggleDark)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isDark) stringResource(id = R.string.editor_preview_dark) else stringResource(id = R.string.editor_preview_light),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Mock Editor Screen
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background)
                    .padding(bottom = 8.dp)
            ) {
                // Pinned Parent Sticky Header Preview (Image 2 style)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.gutterBackground)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = " 1 ",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = colors.text.copy(alpha = 0.5f),
                        modifier = Modifier.width(28.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .width(1.dp)
                            .height(14.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    val stickyLine = buildAnnotatedString {
                        withStyle(SpanStyle(color = colors.keyword, fontWeight = FontWeight.Bold)) {
                            append("public class ")
                        }
                        withStyle(SpanStyle(color = colors.type, fontWeight = FontWeight.Bold)) {
                            append("MainActivity ")
                        }
                        withStyle(SpanStyle(color = colors.text)) {
                            append("{")
                        }
                    }
                    Text(
                        text = stickyLine,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned parent header",
                        tint = colors.keyword.copy(alpha = 0.8f),
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(12.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                // Code Lines inside class
                val lines = listOf(
                    2 to buildAnnotatedString {
                        append("    ")
                        withStyle(SpanStyle(color = colors.annotation, fontWeight = FontWeight.Bold)) {
                            append("@Override")
                        }
                    },
                    3 to buildAnnotatedString {
                        append("    ")
                        withStyle(SpanStyle(color = colors.keyword, fontWeight = FontWeight.Bold)) {
                            append("protected void ")
                        }
                        withStyle(SpanStyle(color = colors.function)) {
                            append("onCreate")
                        }
                        withStyle(SpanStyle(color = colors.text)) {
                            append("() {")
                        }
                    },
                    4 to buildAnnotatedString {
                        append("        ")
                        withStyle(SpanStyle(color = colors.keyword, fontWeight = FontWeight.Bold)) {
                            append("String ")
                        }
                        withStyle(SpanStyle(color = colors.text)) {
                            append("app = ")
                        }
                        withStyle(SpanStyle(color = colors.string)) {
                            append("\"Cat Editor\"")
                        }
                        withStyle(SpanStyle(color = colors.text)) {
                            append(";")
                        }
                    },
                    5 to buildAnnotatedString {
                        append("        ")
                        withStyle(SpanStyle(color = colors.function)) {
                            append("initTheme")
                        }
                        withStyle(SpanStyle(color = colors.text)) {
                            append("(app);")
                        }
                    },
                    6 to buildAnnotatedString {
                        append("    ")
                        withStyle(SpanStyle(color = colors.text)) {
                            append("}")
                        }
                    },
                    7 to buildAnnotatedString {
                        withStyle(SpanStyle(color = colors.text)) {
                            append("}")
                        }
                    }
                )

                lines.forEach { (lineNum, content) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = " $lineNum ",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = colors.text.copy(alpha = 0.45f),
                            modifier = Modifier
                                .width(28.dp)
                                .background(colors.gutterBackground),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .width(1.dp)
                                .height(14.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                        Text(
                            text = content,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}