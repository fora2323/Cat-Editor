package org.cat.editor

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import org.cat.editor.ui.settings.AboutSettingsScreen
import org.cat.editor.ui.settings.AppSettingsScreen
import org.cat.editor.ui.settings.GitSettingsScreen
import org.cat.editor.ui.settings.LanguageSettingsScreen
import org.cat.editor.ui.settings.PlaceholderSettingsScreen
import org.cat.editor.ui.settings.SettingsMainScreen
import org.cat.editor.ui.settings.SettingsScreenRoute
import org.cat.editor.ui.settings.SupportSettingsScreen
import org.cat.editor.ui.theme.AppTheme
import org.cat.editor.utils.LocaleHelper

class SettingsActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        org.cat.editor.utils.SettingsManager.applyFullscreen(this)

        setContent {
            AppTheme {
                SettingsNavigationHost(
                    onFinishActivity = { finish() },
                    onLanguageChanged = {
                        recreate()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        org.cat.editor.utils.SettingsManager.applyFullscreen(this)
    }
}

@Composable
fun SettingsNavigationHost(
    onFinishActivity: () -> Unit,
    onLanguageChanged: (String) -> Unit
) {
    var currentRoute by rememberSaveable { mutableStateOf(SettingsScreenRoute.MAIN) }

    BackHandler(enabled = currentRoute != SettingsScreenRoute.MAIN) {
        currentRoute = when (currentRoute) {
            SettingsScreenRoute.LANGUAGE -> SettingsScreenRoute.APP
            SettingsScreenRoute.SUPPORT -> SettingsScreenRoute.ABOUT
            else -> SettingsScreenRoute.MAIN
        }
    }

    AnimatedContent(
        targetState = currentRoute,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "SettingsScreenTransition"
    ) { route ->
        when (route) {
            SettingsScreenRoute.MAIN -> {
                SettingsMainScreen(
                    onBackClick = onFinishActivity,
                    onNavigate = { currentRoute = it }
                )
            }
            SettingsScreenRoute.APP -> {
                AppSettingsScreen(
                    onBackClick = { currentRoute = SettingsScreenRoute.MAIN },
                    onNavigateLanguage = { currentRoute = SettingsScreenRoute.LANGUAGE }
                )
            }
            SettingsScreenRoute.LANGUAGE -> {
                LanguageSettingsScreen(
                    onBackClick = { currentRoute = SettingsScreenRoute.APP },
                    onLanguageChanged = onLanguageChanged
                )
            }
            SettingsScreenRoute.ABOUT -> {
                AboutSettingsScreen(
                    onBackClick = { currentRoute = SettingsScreenRoute.MAIN },
                    onNavigateSupport = { currentRoute = SettingsScreenRoute.SUPPORT }
                )
            }
            SettingsScreenRoute.SUPPORT -> {
                SupportSettingsScreen(
                    onBackClick = { currentRoute = SettingsScreenRoute.ABOUT }
                )
            }
            SettingsScreenRoute.EDITOR -> {
                PlaceholderSettingsScreen(
                    title = stringResource(id = R.string.settings_editor_title),
                    icon = Icons.Default.EditNote,
                    onBackClick = { currentRoute = SettingsScreenRoute.MAIN }
                )
            }
            SettingsScreenRoute.GIT -> {
                GitSettingsScreen(
                    onBackClick = { currentRoute = SettingsScreenRoute.MAIN }
                )
            }
        }
    }
}