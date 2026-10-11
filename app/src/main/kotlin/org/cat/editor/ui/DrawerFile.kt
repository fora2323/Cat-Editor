package org.cat.editor.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
import org.cat.editor.preview.ImagePreviewIcon
import org.cat.editor.preview.isImageFile
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
                onTogglePicker(true)  // ✅ Trigger native file picker dari MainActivity
            }
        )
        return
    }

    // ✅ Tidak perlu FolderPickerDialog lagi - pakai ActivityResultContracts di MainActivity

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
    onFileSelected(file)
    scope.launch { drawerState.close() }
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
                                            FileIcon(file = file, modifier = Modifier.size(16.dp))
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
fun FileIcon(file: File, modifier: Modifier = Modifier) {
    val fileName = file.name
    when {
        isImageFile(fileName) -> ImagePreviewIcon(file = file, modifier = modifier)
        fileName == "AndroidManifest.xml" -> Icon(painterResource(R.drawable.ic_android), "Manifest", modifier = modifier, tint = Color.Unspecified)
        fileName.endsWith(".kt") -> Icon(painterResource(R.drawable.ic_kotlin), "Kotlin", modifier = modifier, tint = Color.Unspecified)
        fileName.endsWith(".kts") || fileName == "gradlew" || fileName == "gradlew.bat" || fileName.endsWith(".gradle") || fileName == "gradle.properties" || fileName == "local.properties" -> Icon(painterResource(R.drawable.ic_gradle), "Gradle", modifier = modifier, tint = Color.Unspecified)
        fileName.endsWith(".java") -> Icon(painterResource(R.drawable.ic_java), "Java", modifier = modifier, tint = Color.Unspecified)
        fileName.endsWith(".xml") -> Icon(painterResource(R.drawable.ic_xml), "XML", modifier = modifier, tint = Color.Unspecified)
        fileName.endsWith(".cpp") || fileName.endsWith(".cc") || fileName.endsWith(".cxx") || fileName.endsWith(".hpp") -> Icon(painterResource(R.drawable.ic_cpp), "C++", modifier = modifier, tint = Color.Unspecified)
        fileName.endsWith(".c") || fileName.endsWith(".h") -> Icon(painterResource(R.drawable.ic_c), "C", modifier = modifier, tint = Color.Unspecified)
        fileName == "CMakeLists.txt" || fileName.endsWith(".cmake") -> Icon(painterResource(R.drawable.ic_cmake), "CMake", modifier = modifier, tint = Color.Unspecified)
        fileName.endsWith(".py") -> Icon(Icons.Default.Code, "Python", modifier = modifier, tint = Color(0xFF3776AB))
        fileName.endsWith(".md") -> Icon(Icons.Default.Description, "Markdown", modifier = modifier, tint = Color(0xFF42A5F5))
        fileName.endsWith(".json") -> Icon(Icons.Default.DataObject, "JSON", modifier = modifier, tint = Color(0xFFFFCA28))
        fileName.endsWith(".jks") || fileName.endsWith(".keystore") -> Icon(Icons.Default.Key, "Keystore", modifier = modifier, tint = Color(0xFFFFCA28))
        fileName.startsWith(".git") -> Icon(Icons.Default.Source, "Git", modifier = modifier, tint = Color(0xFFF05032))
        fileName.endsWith(".pro") -> Icon(Icons.Default.Shield, "Proguard", modifier = modifier, tint = Color(0xFF7E57C2))
        else -> Icon(Icons.Default.Description, "File", modifier = modifier, tint = Color.Gray)
    }
}

@Composable
fun FileTreeView(root: File, onFileClick: (File) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var refreshTrigger by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari file...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                modifier = Modifier.weight(1f).height(48.dp),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = { refreshTrigger++ }) {
                Icon(Icons.Default.Refresh, "Refresh", modifier = Modifier.size(20.dp))
            }
        }

        // File tree dengan horizontal scroll
        key(refreshTrigger) {
            val scrollStateH = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollStateH)
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (searchQuery.isEmpty()) {
                        FileTreeNode(root = root, level = 0, onFileClick = onFileClick)
                    } else {
                        SearchResults(root = root, query = searchQuery, onFileClick = onFileClick)
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResults(root: File, query: String, onFileClick: (File) -> Unit) {
    val results = remember(query) {
        root.walkTopDown()
            .filter { it.isFile && it.name.contains(query, ignoreCase = true) }
            .toList()
    }
    if (results.isEmpty()) {
        Text("Tidak ditemukan", modifier = Modifier.padding(16.dp), color = Color.Gray, fontSize = 12.sp)
    } else {
        results.forEach { file ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFileClick(file) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FileIcon(file = file, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(file.name, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(file.parent ?: "", fontSize = 10.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun FileTreeNode(root: File, level: Int = 0, onFileClick: (File) -> Unit) {
    val files = root.listFiles()
        ?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
        ?: emptyList()

    files.forEach { file ->
        var isExpanded by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
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
                FolderIcon(folderName = file.name, isExpanded = isExpanded, modifier = Modifier.size(20.dp))
            } else {
                Spacer(Modifier.width(22.dp))
                FileIcon(file = file, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(8.dp))
            Text(
                text = file.name,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                softWrap = false
            )
        }

        if (file.isDirectory && isExpanded) {
            FileTreeNode(root = file, level = level + 1, onFileClick = onFileClick)
        }
    }
}

@Composable
fun FolderIcon(
    folderName: String,
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    Icon(
        painter = painterResource(
            if (isExpanded) R.drawable.ic_folder_open
            else R.drawable.ic_folder
        ),
        contentDescription = folderName,
        modifier = modifier,
        tint = Color.Unspecified
    )
}