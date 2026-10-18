package org.cat.editor.drawer

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cat.editor.R
import org.cat.editor.ui.BottomSheetDialog
import org.cat.editor.ui.M3ELoadingDialog
import java.io.File

@Composable
fun CreateItemDialog(
    targetFolder: File,
    projectRoot: File,
    onDismiss: () -> Unit,
    onItemCreated: (File) -> Unit
) {
    var isFolder by remember { mutableStateOf(false) }
    var itemName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val relativePath = targetFolder.relativeToOrNull(projectRoot)?.path?.ifEmpty { "/" } ?: targetFolder.name

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.create_new),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${stringResource(id = R.string.project_files)}: $relativePath",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isFolder,
                        onClick = { isFolder = false },
                        label = { Text(stringResource(id = R.string.type_file)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    FilterChip(
                        selected = isFolder,
                        onClick = { isFolder = true },
                        label = { Text(stringResource(id = R.string.type_folder)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }

                OutlinedTextField(
                    value = itemName,
                    onValueChange = {
                        itemName = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(id = R.string.name_hint)) },
                    placeholder = {
                        Text(if (isFolder) "models" else "Main.kt")
                    },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = itemName.trim()
                    if (trimmed.isEmpty()) {
                        errorMessage = context.getString(R.string.file_name_empty)
                        return@Button
                    }
                    val target = File(targetFolder, trimmed)
                    if (target.exists()) {
                        errorMessage = context.getString(R.string.file_already_exists)
                        return@Button
                    }
                    try {
                        val created = if (isFolder) {
                            target.mkdirs()
                        } else {
                            target.parentFile?.mkdirs()
                            target.createNewFile()
                        }
                        if (created || target.exists()) {
                            Toast.makeText(context, context.getString(R.string.file_created), Toast.LENGTH_SHORT).show()
                            onItemCreated(target)
                            onDismiss()
                        } else {
                            errorMessage = context.getString(R.string.file_create_failed, "Permission denied")
                        }
                    } catch (e: Exception) {
                        errorMessage = context.getString(R.string.file_create_failed, e.localizedMessage ?: "")
                    }
                }
            ) {
                Text(stringResource(id = R.string.action_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.action_cancel))
            }
        }
    )
}

@Composable
fun FileActionRow(
    icon: ImageVector,
    title: String,
    description: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileActionsBottomSheet(
    file: File,
    projectRoot: File,
    onDismiss: () -> Unit,
    onNewInside: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onCopyPath: (String) -> Unit,
    onCopyRelativePath: (String) -> Unit
) {
    val relativePath = file.relativeToOrNull(projectRoot)?.path ?: file.name

    BottomSheetDialog(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (file.isDirectory) {
                FolderIcon(folderName = file.name, isExpanded = false, modifier = Modifier.size(28.dp))
            } else {
                FileIcon(file = file, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = relativePath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        if (file.isDirectory) {
            FileActionRow(
                icon = Icons.Default.CreateNewFolder,
                title = stringResource(id = R.string.new_item_here),
                description = stringResource(id = R.string.new_item_here_desc),
                onClick = onNewInside
            )
        }

        FileActionRow(
            icon = Icons.Default.DriveFileRenameOutline,
            title = stringResource(id = R.string.rename),
            description = stringResource(id = R.string.rename_desc),
            onClick = onRename
        )

        FileActionRow(
            icon = Icons.Default.Delete,
            title = stringResource(id = R.string.delete),
            description = stringResource(id = R.string.delete_desc),
            isDestructive = true,
            onClick = onDelete
        )

        FileActionRow(
            icon = Icons.Default.ContentCopy,
            title = stringResource(id = R.string.copy_relative_path),
            description = stringResource(id = R.string.copy_relative_path_desc),
            onClick = { onCopyRelativePath(relativePath) }
        )

        FileActionRow(
            icon = Icons.Default.ContentCopy,
            title = stringResource(id = R.string.copy_path),
            description = stringResource(id = R.string.copy_path_desc),
            onClick = { onCopyPath(file.absolutePath) }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun RenameFileDialog(
    file: File,
    onDismiss: () -> Unit,
    onRenamed: (File) -> Unit
) {
    var newName by remember { mutableStateOf(file.name) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.rename),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(id = R.string.enter_new_name)) },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = newName.trim()
                    if (trimmed.isEmpty()) {
                        errorMessage = context.getString(R.string.file_name_empty)
                        return@Button
                    }
                    if (trimmed == file.name) {
                        onDismiss()
                        return@Button
                    }
                    val target = File(file.parentFile, trimmed)
                    if (target.exists()) {
                        errorMessage = context.getString(R.string.file_already_exists)
                        return@Button
                    }
                    if (file.renameTo(target)) {
                        Toast.makeText(context, context.getString(R.string.file_renamed), Toast.LENGTH_SHORT).show()
                        onRenamed(target)
                        onDismiss()
                    } else {
                        errorMessage = context.getString(R.string.file_rename_failed)
                    }
                }
            ) {
                Text(stringResource(id = R.string.rename))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.action_cancel))
            }
        }
    )
}

@Composable
fun DeleteFileDialog(
    file: File,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit
) {
    val context = LocalContext.current
    var isDeleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (isDeleting) {
        M3ELoadingDialog()
    }

    AlertDialog(
        onDismissRequest = {
            if (!isDeleting) onDismiss()
        },
        title = {
            Text(
                text = stringResource(id = R.string.delete_confirm_title, file.name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            val itemTypeStr = if (file.isDirectory) {
                stringResource(id = R.string.type_folder).lowercase()
            } else {
                stringResource(id = R.string.type_file).lowercase()
            }
            Text(
                text = stringResource(id = R.string.delete_confirm_msg, itemTypeStr),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                onClick = {
                    isDeleting = true
                    scope.launch(Dispatchers.IO) {
                        val success = if (file.isDirectory) file.deleteRecursively() else file.delete()
                        withContext(Dispatchers.Main) {
                            isDeleting = false
                            if (success) {
                                Toast.makeText(context, context.getString(R.string.file_deleted), Toast.LENGTH_SHORT).show()
                                onDeleted()
                                onDismiss()
                            } else {
                                Toast.makeText(context, context.getString(R.string.file_delete_failed), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            ) {
                Text(stringResource(id = R.string.delete))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting
            ) {
                Text(stringResource(id = R.string.action_cancel))
            }
        }
    )
}