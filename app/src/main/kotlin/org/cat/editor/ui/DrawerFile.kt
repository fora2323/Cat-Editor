package org.cat.editor.ui

import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
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
import org.cat.editor.R
import org.cat.editor.code.KotlinCodeEditor
import org.cat.editor.preview.PreviewScreen
import org.cat.editor.utility.ComposeNode
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeMainScreen(
    projectPath: String,
    openedFiles: List<String>,
    activeFilePath: String,
    currentRootNode: ComposeNode.Layout,
    onProjectPicked: (String) -> Unit,
    onFileSelected: (File) -> Unit,
    onTabSelected: (String) -> Unit,
    showProjectPicker: Boolean,
    onTogglePicker: (Boolean) -> Unit,
    onCloseFile: ((String) -> Unit)? = null,
    onAddFile: (() -> Unit)? = null,
    onSaveFile: (() -> Unit)? = null
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showProjectMenu by remember { mutableStateOf(false) }
    var currentCode by remember { mutableStateOf("") }

    // Tampilkan welcome screen jika belum ada project
    if (projectPath.isEmpty()) {
        WelcomeScreen(onBrowseClick = { onTogglePicker(true) })
        return
    }

    // Tampilkan project menu jika user klik add button
    if (showProjectMenu) {
        ProjectMenuScreen(
            onNewProject = { 
                showProjectMenu = false
                onAddFile?.invoke()
            },
            onCloneRepository = { 
                showProjectMenu = false
                Toast.makeText(context, "Clone repository akan diimplementasikan", Toast.LENGTH_SHORT).show()
            },
            onBrowseFolder = {
                showProjectMenu = false
                onTogglePicker(true)
            }
        )
        return
    }

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
                Column {
                    TopAppBar(
                        title = { /* Kosong */ },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            // Undo
                            IconButton(onClick = { 
                                Toast.makeText(context, "Undo", Toast.LENGTH_SHORT).show() 
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                            }
                            
                            // Redo
                            IconButton(onClick = { 
                                Toast.makeText(context, "Redo", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                            }
                            
                            // Save
                            IconButton(onClick = { 
                                if (activeFilePath.isNotEmpty()) {
                                    try {
                                        File(activeFilePath).writeText(currentCode)
                                        Toast.makeText(context, "File Disimpan", Toast.LENGTH_SHORT).show()
                                        onSaveFile?.invoke()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Save, contentDescription = "Save")
                            }
                            
                            // Add (buka project menu)
                            IconButton(onClick = { 
                                showProjectMenu = true
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                            }
                            
                            // More Options
                            IconButton(onClick = { 
                                Toast.makeText(context, "Menu lainnya", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    
                    // TAB ROW - Multi-File dengan close button
                    if (openedFiles.isNotEmpty()) {
                        val activeIndex = openedFiles.indexOf(activeFilePath).coerceAtLeast(0)
                        
                        ScrollableTabRow(
                            selectedTabIndex = activeIndex,
                            edgePadding = 0.dp,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            divider = { HorizontalDivider() }
                        ) {
                            openedFiles.forEachIndexed { index, path ->
                                val file = File(path)
                                Tab(
                                    selected = activeFilePath == path,
                                    onClick = { onTabSelected(path) },
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.wrapContentWidth()
                                        ) {
                                            FileIcon(fileName = file.name, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = file.name, fontSize = 12.sp, maxLines = 1)
                                            
                                            // Close button
                                            IconButton(
                                                onClick = { 
                                                    onCloseFile?.invoke(path)
                                                },
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .padding(0.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Close",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                if (activeFilePath.isNotEmpty()) {
                    // SPLIT LAYOUT - Code Editor + Preview
                    Row(modifier = Modifier.fillMaxSize()) {
                        // LEFT SIDE - Code Editor (50%)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            KotlinCodeEditor(
                                filePath = activeFilePath,
                                onCodeChange = { newCode ->
                                    currentCode = newCode
                                }
                            )
                        }
                        
                        // DIVIDER
                        VerticalDivider(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            thickness = 1.dp
                        )
                        
                        // RIGHT SIDE - Preview (50%)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            PreviewScreen(activeFilePath, currentRootNode)
                        }
                    }
                } else {
                    // Jika tidak ada file yang dibuka
                    PreviewScreen("", currentRootNode)
                }
            }
        }
    }
}

// Komponen Helper untuk Ikon File
@Composable
fun FileIcon(fileName: String, modifier: Modifier = Modifier) {
    when {
        fileName == "AndroidManifest.xml" -> Icon(painterResource(R.drawable.ic_android), "Manifest", modifier = modifier, tint = Color.Unspecified)
        fileName.endsWith(".kt") || fileName.endsWith(".kts") -> Icon(painterResource(R.drawable.ic_kotlin), "Kotlin", modifier = modifier, tint = Color.Unspecified)
        fileName.endsWith(".java") -> Icon(painterResource(R.drawable.ic_java), "Java", modifier = modifier, tint = Color.Unspecified)
        fileName.endsWith(".xml") -> Icon(painterResource(R.drawable.ic_xml), "XML", modifier = modifier, tint = Color.Unspecified)
        fileName.endsWith(".gradle") -> Icon(painterResource(R.drawable.ic_gradle), "Gradle", modifier = modifier, tint = Color.Unspecified)
        else -> Icon(Icons.Default.Description, "File", modifier = modifier, tint = Color.Gray)
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
                FileIcon(fileName = file.name, modifier = Modifier.size(20.dp))
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