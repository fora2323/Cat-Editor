package org.cat.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import org.cat.editor.ui.BottomSheetDialog
import org.cat.editor.utils.ConfigPath
import org.cat.editor.utils.PresetIconType
import org.cat.editor.utils.ProjectLanguage
import org.cat.editor.utils.ProjectTemplateManager
import org.cat.editor.utils.ProjectTemplatePreset
import org.cat.editor.utils.StorageUtils

enum class PresetFilterTab(val id: String, val displayName: String) {
    ALL("all", "All"),
    KOTLIN("kotlin", "Kotlin"),
    JAVA("java", "Java"),
    PYTHON("python", "Python"),
    BLANK("blank", "Blank")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectSheet(
    isCreating: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onCreate: (projectName: String, packageName: String, preset: ProjectTemplatePreset, targetDirectory: File) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var projectName by rememberSaveable { mutableStateOf("") }
    var packageName by rememberSaveable { mutableStateOf("com.example.catapp") }
    var isPackageManual by rememberSaveable { mutableStateOf(false) }

    var selectedDirectoryPath by rememberSaveable {
        mutableStateOf(ConfigPath.VAR_PROJECTS_PATH)
    }

    var selectedFilterTab by rememberSaveable {
        mutableStateOf(PresetFilterTab.KOTLIN)
    }

    val filteredPresets = remember(selectedFilterTab) {
        when (selectedFilterTab) {
            PresetFilterTab.ALL -> ProjectTemplateManager.presets
            PresetFilterTab.KOTLIN -> ProjectTemplateManager.presets.filter { it.language == ProjectLanguage.KOTLIN }
            PresetFilterTab.JAVA -> ProjectTemplateManager.presets.filter { it.language == ProjectLanguage.JAVA }
            PresetFilterTab.PYTHON -> ProjectTemplateManager.presets.filter { it.language == ProjectLanguage.PYTHON }
            PresetFilterTab.BLANK -> ProjectTemplateManager.presets.filter { it.isBlank }
        }
    }

    var selectedPresetId by rememberSaveable(selectedFilterTab) {
        mutableStateOf(filteredPresets.firstOrNull()?.id)
    }

    val selectedPreset = filteredPresets.firstOrNull { it.id == selectedPresetId }
        ?: filteredPresets.firstOrNull()

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val resolved = StorageUtils.getRealPathFromTreeUri(context, it)
            if (!resolved.isNullOrBlank() && File(resolved).isDirectory) {
                selectedDirectoryPath = resolved
            }
        }
    }

    BottomSheetDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .imePadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(org.cat.editor.R.string.create_project_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(org.cat.editor.R.string.create_project_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onDismiss, enabled = !isCreating) {
                    Text(stringResource(org.cat.editor.R.string.cancel))
                }
            }

            // Project Basics Section (M3 Expressive Card)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = stringResource(org.cat.editor.R.string.project_basics),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    // Project Name Field
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { newName ->
                            projectName = newName
                            if (!isPackageManual) {
                                packageName = derivePackageName(newName)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(org.cat.editor.R.string.project_name_label)) },
                        placeholder = { Text(stringResource(org.cat.editor.R.string.project_name_placeholder)) },
                        singleLine = true,
                        enabled = !isCreating,
                        shape = RoundedCornerShape(18.dp)
                    )

                    // Package Name Field (Auto-derived with manual edit support)
                    OutlinedTextField(
                        value = packageName,
                        onValueChange = {
                            isPackageManual = true
                            packageName = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(org.cat.editor.R.string.package_name_label)) },
                        placeholder = { Text(stringResource(org.cat.editor.R.string.package_name_placeholder)) },
                        supportingText = {
                            Text(
                                if (!isPackageManual) "Auto-generated from project name"
                                else stringResource(org.cat.editor.R.string.package_name_supporting)
                            )
                        },
                        trailingIcon = {
                            if (isPackageManual) {
                                IconButton(
                                    onClick = {
                                        isPackageManual = false
                                        packageName = derivePackageName(projectName)
                                    },
                                    enabled = !isCreating
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reset auto package name",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        enabled = !isCreating,
                        shape = RoundedCornerShape(18.dp)
                    )

                    // Save Location Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(org.cat.editor.R.string.project_location_label),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedDirectoryPath,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val targetFolder = if (projectName.isNotBlank()) projectName.trim() else "..."
                                    Text(
                                        text = "Project folder: $targetFolder",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                if (selectedDirectoryPath != ConfigPath.VAR_PROJECTS_PATH) {
                                    TextButton(
                                        onClick = { selectedDirectoryPath = ConfigPath.VAR_PROJECTS_PATH },
                                        enabled = !isCreating,
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text(
                                            text = stringResource(org.cat.editor.R.string.project_location_default),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = { folderPickerLauncher.launch(null) },
                                    enabled = !isCreating,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        text = stringResource(org.cat.editor.R.string.project_location_change),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Language & Template Filter Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(org.cat.editor.R.string.project_language_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetFilterTab.entries.forEach { tab ->
                        val isSelected = tab == selectedFilterTab
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (!isCreating) {
                                    selectedFilterTab = tab
                                }
                            },
                            label = { Text(tab.displayName) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null,
                            enabled = !isCreating,
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }
            }

            // Starter Presets Section (Android Studio Inspired Cards)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(org.cat.editor.R.string.project_preset_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${filteredPresets.size} available",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                filteredPresets.forEach { preset ->
                    val isSelected = preset.id == selectedPreset?.id
                    val animatedBorderColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        animationSpec = tween(durationMillis = 200),
                        label = "borderColor"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isCreating) {
                                selectedPresetId = preset.id
                            },
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = animatedBorderColor
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Android Studio Style Phone / Component Illustration
                            TemplatePreviewIllustration(
                                iconType = preset.iconType,
                                isSelected = isSelected
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            // Details Column
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = preset.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Badges Row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BadgePill(
                                        text = preset.language.displayName,
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        textColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    preset.badge?.let { badge ->
                                        BadgePill(
                                            text = badge,
                                            color = MaterialTheme.colorScheme.tertiaryContainer,
                                            textColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = preset.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Selection Indicator
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Error Message
            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            // Create & Open Action Button
            Button(
                onClick = {
                    selectedPreset?.let { preset ->
                        val targetDir = File(selectedDirectoryPath)
                        onCreate(projectName.trim(), packageName.trim(), preset, targetDir)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isCreating && projectName.isNotBlank() && packageName.isNotBlank() && selectedPreset != null,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = stringResource(org.cat.editor.R.string.project_create_action),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Android Studio style phone/component mockup preview illustration.
 */
@Composable
private fun TemplatePreviewIllustration(
    iconType: PresetIconType,
    isSelected: Boolean
) {
    val phoneBg = if (isSelected) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }

    Box(
        modifier = Modifier
            .size(width = 54.dp, height = 76.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(phoneBg)
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        when (iconType) {
            PresetIconType.NO_ACTIVITY -> {
                // Dashed outline box matching Android Studio "No Activity"
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(6.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
                }
            }

            PresetIconType.COMPOSE_EMPTY -> {
                // Compose Preview: Top bar + Compose geometric logo
                Column(modifier = Modifier.fillMaxSize()) {
                    // Mini Status + TopBar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                            )
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Compose Cube / Star Geometry
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            PresetIconType.VIEWS_EMPTY -> {
                // Classic Empty Views: TopBar + Centered Card
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                            )
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Centered content representation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(6.dp)
                            .align(Alignment.CenterHorizontally)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(5.dp)
                            .align(Alignment.CenterHorizontally)
                            .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            PresetIconType.VIEWS_BASIC_FAB -> {
                // Basic Views: TopBar + Centered Content + Floating Action Button (FAB)
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(5.dp)
                                .padding(start = 4.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                        )
                    }
                    // Mini FAB button in bottom-right corner!
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 2.dp, end = 2.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3DDC84)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }

            PresetIconType.VIEWS_BOTTOM_NAV -> {
                // Bottom Navigation: TopBar + Content + Bottom Bar with 3 tabs
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                            )
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(5.dp)
                            .align(Alignment.CenterHorizontally)
                            .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Bottom Navigation Bar with 3 green/primary tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(9.dp)
                            .background(
                                color = Color(0xFF3DDC84),
                                shape = RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp)
                            )
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                            )
                        }
                    }
                }
            }

            PresetIconType.PYTHON, PresetIconType.CONSOLE -> {
                // Terminal / Script Window
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E1E1E))
                        .padding(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(modifier = Modifier.size(3.dp).background(Color(0xFFFF5F56), CircleShape))
                        Box(modifier = Modifier.size(3.dp).background(Color(0xFFFFBD2E), CircleShape))
                        Box(modifier = Modifier.size(3.dp).background(Color(0xFF27C93F), CircleShape))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ">_",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3DDC84)
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgePill(text: String, color: Color, textColor: Color) {
    Surface(
        color = color,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Derives a sanitized, valid Android/Java package name from the project name.
 */
private fun derivePackageName(projectName: String): String {
    val raw = projectName.trim().lowercase()
        .replace(Regex("[\\s-]+"), "_")
        .replace(Regex("[^a-z0-9_]"), "")
        .trim('_')
    val segment = if (raw.isEmpty()) {
        "catapp"
    } else if (raw.first().isDigit()) {
        "app$raw"
    } else {
        raw
    }
    return "com.example.$segment"
}
