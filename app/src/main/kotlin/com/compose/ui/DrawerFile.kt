package com.compose.ui

import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.compose.R
import com.compose.preview.PreviewScreen
import com.compose.utility.ComposeNode
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeMainScreen(
    projectPath: String,
    currentPreviewPath: String,
    currentRootNode: ComposeNode.Layout,
    onProjectPicked: (String) -> Unit,
    onFileSelected: (File) -> Unit,
    showProjectPicker: Boolean,
    onTogglePicker: (Boolean) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (showProjectPicker) {
        FolderPickerDialog(
            initialPath = Environment.getExternalStorageDirectory().absolutePath,
            onDismiss = { onTogglePicker(false) },
            onFolderPicked = { path ->
                onProjectPicked(path)
                onTogglePicker(false)
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Text(
                    text = "Project Files",
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                HorizontalDivider()
                if (projectPath.isNotEmpty()) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        FileTreeView(
                            root = File(projectPath),
                            onFileClick = { file ->
                                if (!file.name.endsWith(".kt")) {
                                    Toast.makeText(context, "Hanya mendukung file Kotlin (.kt)", Toast.LENGTH_SHORT).show()
                                    return@FileTreeView
                                }
                                val content = file.readText()
                                if (content.contains("@Preview")) {
                                    onFileSelected(file)
                                    scope.launch { drawerState.close() }
                                } else {
                                    Toast.makeText(context, "File ini tidak memiliki anotasi @Preview", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(if (currentPreviewPath.isNotEmpty()) File(currentPreviewPath).name else "CodeAssist IDE")
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        Button(onClick = { onTogglePicker(true) }) {
                            Text("Open Project")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                PreviewScreen(currentPreviewPath, currentRootNode)
            }
        }
    }
}

@Composable
fun FileTreeView(root: File, level: Int = 0, onFileClick: (File) -> Unit) {
    val files = root.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()

    files.forEach { file ->
        var isExpanded by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (file.isDirectory) isExpanded = !isExpanded
                    else onFileClick(file)
                }
                .padding(start = (level * 16 + 8).dp, top = 6.dp, bottom = 6.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (file.isDirectory) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.Folder,
                    contentDescription = "Folder",
                    tint = Color(0xFFFFCA28),
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Spacer(Modifier.width(22.dp))
                val iconModifier = Modifier.size(20.dp)
                when {
                    file.name == "AndroidManifest.xml" -> Icon(painterResource(R.drawable.ic_android), "Manifest", modifier = iconModifier, tint = Color.Unspecified)
                    file.name.endsWith(".kt") || file.name.endsWith(".kts") -> Icon(painterResource(R.drawable.ic_kotlin), "Kotlin", modifier = iconModifier, tint = Color.Unspecified)
                    file.name.endsWith(".java") -> Icon(painterResource(R.drawable.ic_java), "Java", modifier = iconModifier, tint = Color.Unspecified)
                    file.name.endsWith(".xml") -> Icon(painterResource(R.drawable.ic_xml), "XML", modifier = iconModifier, tint = Color.Unspecified)
                    file.name.endsWith(".gradle") -> Icon(painterResource(R.drawable.ic_gradle), "Gradle", modifier = iconModifier, tint = Color.Unspecified)
                    else -> Icon(Icons.Default.Description, "File", modifier = iconModifier, tint = Color.Gray)
                }
            }

            Spacer(Modifier.width(8.dp))
            Text(
                text = file.name,
                fontSize = 14.sp,
                color = if (!file.isDirectory && !file.name.endsWith(".kt")) Color.Gray else MaterialTheme.colorScheme.onSurface
            )
        }

        if (file.isDirectory && isExpanded) {
            FileTreeView(root = file, level = level + 1, onFileClick = onFileClick)
        }
    }
}

@Composable
fun FolderPickerDialog(initialPath: String, onDismiss: () -> Unit, onFolderPicked: (String) -> Unit) {
    var currentPath by remember { mutableStateOf(initialPath) }
    val currentDir = File(currentPath)
    val dirs = currentDir.listFiles { it.isDirectory }?.sortedBy { it.name } ?: emptyList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Folder Project") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                if (currentDir.parent != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { currentPath = currentDir.parent!! }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(".. (Kembali ke Induk)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                HorizontalDivider()
                Text(text = currentPath, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(4.dp))
                HorizontalDivider()
                
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(dirs) { dir ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { currentPath = dir.absolutePath }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFFFFCA28))
                            Spacer(Modifier.width(8.dp))
                            Text(dir.name, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onFolderPicked(currentPath) }) { Text("Pilih Folder Ini") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}