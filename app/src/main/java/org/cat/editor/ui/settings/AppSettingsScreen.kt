package org.cat.editor.ui.settings

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.cat.editor.BuildConfig
import org.cat.editor.R
import org.cat.editor.utils.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    onBackClick: () -> Unit,
    onNavigateLanguage: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var checkUpdates by remember { mutableStateOf(SettingsManager.isCheckUpdatesEnabled(context)) }
    var fullscreen by remember { mutableStateOf(SettingsManager.isFullscreen(context)) }
    var smartToolbar by remember { mutableStateOf(SettingsManager.isSmartToolbarEnabled(context)) }
    var exitDialog by remember { mutableStateOf(SettingsManager.isExitDialogEnabled(context)) }
    var debugOptions by remember { mutableStateOf(SettingsManager.isDebugOptionsEnabled(context)) }
    var currentFont by remember { mutableStateOf(SettingsManager.getEditorFont(context)) }

    var showFontDialog by remember { mutableStateOf(false) }

    fun runUpdateCheck() {
        scope.launch {
            Toast.makeText(context, context.getString(R.string.checking_updates), Toast.LENGTH_SHORT).show()
            val (_, latestCommit, success) = SettingsManager.syncGithubInfo(context)
            if (success) {
                if (latestCommit != BuildConfig.GIT_HASH && latestCommit.isNotEmpty()) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.update_available, latestCommit),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.up_to_date, BuildConfig.VERSION_NAME),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(context, context.getString(R.string.sync_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_app_title),
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Primary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Language Navigation
                        SettingNavigationItem(
                            title = stringResource(id = R.string.language),
                            subtitle = stringResource(id = R.string.language_desc),
                            onClick = onNavigateLanguage
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                            thickness = 0.7.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // Check updates
                        SettingSwitchItem(
                            title = stringResource(id = R.string.setting_check_updates_title),
                            subtitle = stringResource(id = R.string.setting_check_updates_desc),
                            checked = checkUpdates,
                            onCheckedChange = {
                                checkUpdates = it
                                SettingsManager.setCheckUpdatesEnabled(context, it)
                                if (it) {
                                    runUpdateCheck()
                                }
                            }
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                            thickness = 0.7.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // Fullscreen
                        SettingSwitchItem(
                            title = stringResource(id = R.string.setting_fullscreen_title),
                            subtitle = stringResource(id = R.string.setting_fullscreen_desc),
                            checked = fullscreen,
                            onCheckedChange = {
                                fullscreen = it
                                SettingsManager.setFullscreen(context, it)
                                (context as? Activity)?.let { act ->
                                    SettingsManager.applyFullscreen(act)
                                }
                            }
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                            thickness = 0.7.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // Smart toolbar
                        SettingSwitchItem(
                            title = stringResource(id = R.string.setting_smart_toolbar_title),
                            subtitle = stringResource(id = R.string.setting_smart_toolbar_desc),
                            checked = smartToolbar,
                            onCheckedChange = {
                                smartToolbar = it
                                SettingsManager.setSmartToolbarEnabled(context, it)
                            }
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                            thickness = 0.7.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // Exit dialog
                        SettingSwitchItem(
                            title = stringResource(id = R.string.setting_exit_dialog_title),
                            subtitle = stringResource(id = R.string.setting_exit_dialog_desc),
                            checked = exitDialog,
                            onCheckedChange = {
                                exitDialog = it
                                SettingsManager.setExitDialogEnabled(context, it)
                            }
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                            thickness = 0.7.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // Manage storage
                        SettingNavigationItem(
                            title = stringResource(id = R.string.setting_manage_storage_title),
                            subtitle = stringResource(id = R.string.setting_manage_storage_desc),
                            onClick = {
                                SettingsManager.openStorageSettings(context)
                            }
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                            thickness = 0.7.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // Manage fonts
                        SettingNavigationItem(
                            title = stringResource(id = R.string.setting_manage_fonts_title),
                            subtitle = "${stringResource(id = R.string.setting_manage_fonts_desc)} · $currentFont",
                            onClick = {
                                showFontDialog = true
                            }
                        )
                    }
                }
            }

            // Feature Toggles Subheader
            item {
                Text(
                    text = stringResource(id = R.string.section_feature_toggles),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Feature Toggles Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingSwitchItem(
                            title = stringResource(id = R.string.setting_debug_options_title),
                            subtitle = null,
                            leadingIcon = Icons.Default.BugReport,
                            checked = debugOptions,
                            onCheckedChange = {
                                debugOptions = it
                                SettingsManager.setDebugOptionsEnabled(context, it)
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Font selection dialog
    if (showFontDialog) {
        val fonts = listOf(
            SettingsManager.FONT_MONOSPACE to stringResource(id = R.string.font_monospace),
            SettingsManager.FONT_SANS to stringResource(id = R.string.font_sans),
            SettingsManager.FONT_SERIF to stringResource(id = R.string.font_serif)
        )

        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = {
                Text(
                    text = stringResource(id = R.string.select_font_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    fonts.forEach { (fontKey, fontLabel) ->
                        val isSelected = currentFont == fontKey
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true)
                                ) {
                                    currentFont = fontKey
                                    SettingsManager.setEditorFont(context, fontKey)
                                    showFontDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = fontLabel,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFontDialog = false }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        )
    }
}
