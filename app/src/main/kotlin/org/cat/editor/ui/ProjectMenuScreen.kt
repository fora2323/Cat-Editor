package org.cat.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.cat.editor.R

sealed class MenuIcon {
    data class Vector(val icon: ImageVector) : MenuIcon()
    data class Drawable(val resId: Int) : MenuIcon()
}

data class ProjectMenuItem(
    val title: String,
    val description: String,
    val icon: MenuIcon,
    val action: () -> Unit
)

@Composable
fun ProjectMenuScreen(
    onNewProject: () -> Unit,
    onCloneRepository: () -> Unit,
    onBrowseFolder: () -> Unit
) {
    val menuItems = listOf(
        ProjectMenuItem(
            title = "Proyek baru",
            description = "Pilih template",
            icon = MenuIcon.Vector(Icons.Default.Add),
            action = onNewProject
        ),
        ProjectMenuItem(
            title = "Clone repositori",
            description = "Clone repositori jarak jauh",
            icon = MenuIcon.Drawable(R.drawable.ic_branch),
            action = onCloneRepository
        ),
        ProjectMenuItem(
            title = "Buka direktori",
            description = "Buka direktori melalui pemilih file sistem",
            icon = MenuIcon.Vector(Icons.Default.FolderOpen),
            action = onBrowseFolder
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Penyimpanan",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            menuItems.forEach { item ->
                ProjectMenuItemCard(
                    item = item,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }
        }
    }
}

@Composable
fun ProjectMenuItemCard(
    item: ProjectMenuItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { item.action() }
            .height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (val icon = item.icon) {
                    is MenuIcon.Vector -> Icon(
                        imageVector = icon.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    is MenuIcon.Drawable -> Icon(
                        painter = painterResource(icon.resId),
                        contentDescription = item.title,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}