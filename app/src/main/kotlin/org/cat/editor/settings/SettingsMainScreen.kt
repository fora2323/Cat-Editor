package org.cat.editor.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.cat.editor.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMainScreen(
    onBackClick: () -> Unit,
    onNavigate: (SettingsScreenRoute) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings),
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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingsCategoryRow(
                        icon = Icons.Default.Android,
                        title = stringResource(id = R.string.settings_app_title),
                        subtitle = stringResource(id = R.string.settings_app_desc),
                        onClick = { onNavigate(SettingsScreenRoute.APP) }
                    )

                    SettingsCategoryRow(
                        icon = Icons.Default.EditNote,
                        title = stringResource(id = R.string.settings_editor_title),
                        subtitle = stringResource(id = R.string.settings_editor_desc),
                        onClick = { onNavigate(SettingsScreenRoute.EDITOR) }
                    )

                    SettingsCategoryRow(
                        icon = Icons.AutoMirrored.Filled.CallSplit,
                        title = stringResource(id = R.string.settings_git_title),
                        subtitle = stringResource(id = R.string.settings_git_desc),
                        onClick = { onNavigate(SettingsScreenRoute.GIT) }
                    )

                    SettingsCategoryRow(
                        icon = Icons.Default.Info,
                        title = stringResource(id = R.string.about),
                        subtitle = stringResource(id = R.string.settings_about_desc, stringResource(id = R.string.app_name)),
                        onClick = { onNavigate(SettingsScreenRoute.ABOUT) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
