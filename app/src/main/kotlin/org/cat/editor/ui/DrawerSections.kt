package org.cat.editor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.cat.editor.R

enum class DrawerSection {
    PROJECT, COMMIT
}

@Composable
fun DrawerWithSections(
    projectContent: @Composable () -> Unit,
    commitContent: @Composable () -> Unit
) {
    var currentSection by remember { mutableStateOf(DrawerSection.PROJECT) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (currentSection) {
                DrawerSection.PROJECT -> projectContent()
                DrawerSection.COMMIT -> commitContent()
            }
        }

        HorizontalDivider()

        // Bottom Tab Layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            TextButton(
                onClick = { currentSection = DrawerSection.PROJECT },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RectangleShape,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (currentSection == DrawerSection.PROJECT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(stringResource(id = R.string.tab_project), fontWeight = FontWeight.Bold)
            }

            TextButton(
                onClick = { currentSection = DrawerSection.COMMIT },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RectangleShape,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (currentSection == DrawerSection.COMMIT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(stringResource(id = R.string.tab_commit), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CommitPanel(
    changes: List<Any>, // Tipe sementara agar fleksibel
    branchName: String = "main",
    onCommit: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // TOOLBAR COMMIT PANEL
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Kiri: Branch Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = branchName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Kanan: Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { /* TODO: fetch logic */ }) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = stringResource(id = R.string.git_action_fetch),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { /* TODO: sync logic */ }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(id = R.string.git_action_sync),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { /* TODO: push logic */ }) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = stringResource(id = R.string.git_action_push),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { /* TODO: settings logic */ }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(id = R.string.git_action_settings),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalDivider()

        // Placeholder untuk Changes/History
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = "Changes",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // TODO: LOGIKA DAFTAR PERUBAHAN FILE & TOMBOL COMMIT DISINI
        }
    }
}