package org.cat.editor.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Source
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.DismissibleDrawerSheet
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.cat.editor.R
import org.cat.editor.SettingsActivity
import org.cat.editor.ui.KotlinCodeEditor
import org.cat.editor.ui.preview.ImagePreviewIcon
import org.cat.editor.ui.preview.PreviewScreen
import org.cat.editor.ui.preview.isImageFile
import org.cat.editor.utility.ComposeNode
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
    val isSmartToolbar = remember { org.cat.editor.utils.SettingsManager.isSmartToolbarEnabled(context) }
    var isToolbarVisible by remember { mutableStateOf(true) }
    var currentCode by remember { mutableStateOf("") }
    var showMoreMenu by remember { mutableStateOf(false) }

    if (projectPath.isEmpty()) {
        WelcomeScreen(onBrowseClick = { onTogglePicker(true) })
        return
    }

    LaunchedEffect(projectPath) {
        if (projectPath.isNotEmpty()) {
            drawerState.open()
        }
    }

    DismissibleNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DismissibleDrawerSheet(modifier = Modifier.width(300.dp)) {
                Text(
                    text = stringResource(id = R.string.project_files),
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                HorizontalDivider()
                if (projectPath.isNotEmpty()) {
                    Column {
                        FileTreeView(
                            root = File(projectPath),
                            onFileClick = { file ->
                                onFileSelected(file)
                                scope.launch {
                                    drawerState.close()
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
                    AnimatedVisibility(visible = isToolbarVisible) {
                        TopAppBar(
                            title = { },
                            navigationIcon = {
                                IconButton(onClick = { 
                                    scope.launch { 
                                        if (drawerState.isClosed) drawerState.open() else drawerState.close() 
                                    } 
                                }) {
                                    Icon(Icons.Default.Menu, contentDescription = stringResource(id = R.string.menu))
                                }
                            },
                            actions = {
                                if (org.cat.editor.utils.SettingsManager.isDebugOptionsEnabled(context)) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        Text(
                                            text = "DEBUG",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (isSmartToolbar) {
                                    IconButton(onClick = { isToolbarVisible = false }) {
                                        Icon(Icons.Default.Fullscreen, contentDescription = "Hide Toolbar")
                                    }
                                }

                                IconButton(onClick = {
                                    Toast.makeText(context, context.getString(R.string.undo), Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(id = R.string.undo))
                            }

                            IconButton(onClick = {
                                Toast.makeText(context, context.getString(R.string.redo), Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = stringResource(id = R.string.redo))
                            }

                            IconButton(onClick = {
                                if (activeFilePath.isNotEmpty()) {
                                    try {
                                        File(activeFilePath).writeText(currentCode)
                                        Toast.makeText(context, context.getString(R.string.file_saved), Toast.LENGTH_SHORT).show()
                                        onSaveFile?.invoke()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, context.getString(R.string.failed_to_save, e.message ?: ""), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Save, contentDescription = stringResource(id = R.string.save))
                            }

                            IconButton(onClick = {
                                onAddFile?.invoke()
                            }) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add))
                            }

                            Box {
                                IconButton(onClick = {
                                    showMoreMenu = true
                                }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(id = R.string.more_options))
                                }

                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(id = R.string.settings)) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Settings, contentDescription = null)
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            context.startActivity(Intent(context, SettingsActivity::class.java))
                                        }
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

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
                                                    contentDescription = stringResource(id = R.string.close),
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
            },
            floatingActionButton = {
                if (isSmartToolbar && !isToolbarVisible) {
                    SmallFloatingActionButton(
                        onClick = { isToolbarVisible = true },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(Icons.Default.FullscreenExit, contentDescription = "Show Toolbar")
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                if (activeFilePath.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxSize()) {
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

                        VerticalDivider(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            thickness = 1.dp
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            PreviewScreen(activeFilePath, currentRootNode)
                        }
                    }
                } else {
                    PreviewScreen("", currentRootNode)
                }
            }
        }
    }
}

@Composable
fun FileIcon(file: File, modifier: Modifier = Modifier) {
    val fileName = file.name
    when {
        isImageFile(fileName) -> ImagePreviewIcon(file = file, modifier = modifier)
        fileName == "AndroidManifest.xml" -> Icon(painterResource(R.drawable.ic_android), stringResource(id = R.string.manifest), modifier = modifier, tint = Color.Unspecified)
        fileName.endsWith(".kt") -> Icon(painterResource(R.drawable.ic_kotlin), stringResource(id = R.string.file_kotlin), modifier = modifier, tint = Color.Unspecified)
        fileName.endsWith(".kts") || fileName == "gradlew" || fileName == "gradlew.bat" || fileName.endsWith(".gradle") || fileName == "gradle.properties" || fileName == "local.properties" -> Icon(painterResource(R.drawable.ic_gradle), stringResource(id = R.string.file_gradle), modifier = modifier, tint = Color.Unspecified)
        fileName.endsWith(".java") -> Icon(painterResource(R.drawable.ic_java), stringResource(id = R.string.file_java), modifier = modifier, tint = Color.Unspecified)
        fileName.endsWith(".xml") -> Icon(painterResource(R.drawable.ic_xml), stringResource(id = R.string.file_xml), modifier = modifier, tint = Color.Unspecified)
        fileName.endsWith(".cpp") || fileName.endsWith(".cc") || fileName.endsWith(".cxx") || fileName.endsWith(".hpp") -> Icon(painterResource(R.drawable.ic_cpp), stringResource(id = R.string.file_cpp), modifier = modifier, tint = Color.Unspecified)
        fileName.endsWith(".c") || fileName.endsWith(".h") -> Icon(painterResource(R.drawable.ic_c), stringResource(id = R.string.file_c), modifier = modifier, tint = Color.Unspecified)
        fileName == "CMakeLists.txt" || fileName.endsWith(".cmake") -> Icon(painterResource(R.drawable.ic_cmake), stringResource(id = R.string.file_cmake), modifier = modifier, tint = Color.Unspecified)
        fileName.endsWith(".py") -> Icon(Icons.Default.Code, stringResource(id = R.string.file_python), modifier = modifier, tint = Color(0xFF3776AB))
        fileName.endsWith(".md") -> Icon(Icons.Default.Description, stringResource(id = R.string.file_markdown), modifier = modifier, tint = Color(0xFF42A5F5))
        fileName.endsWith(".json") -> Icon(Icons.Default.DataObject, stringResource(id = R.string.file_json), modifier = modifier, tint = Color(0xFFFFCA28))
        fileName.endsWith(".jks") || fileName.endsWith(".keystore") -> Icon(Icons.Default.Key, stringResource(id = R.string.file_keystore), modifier = modifier, tint = Color(0xFFFFCA28))
        fileName.startsWith(".git") -> Icon(Icons.Default.Source, stringResource(id = R.string.file_git), modifier = modifier, tint = Color(0xFFF05032))
        fileName.endsWith(".pro") -> Icon(Icons.Default.Shield, stringResource(id = R.string.file_proguard), modifier = borderModifier(false), tint = Color(0xFF7E57C2))
        else -> Icon(Icons.Default.Description, stringResource(id = R.string.file_generic), modifier = modifier, tint = Color.Gray)
    }
}

private fun borderModifier(selected: Boolean): Modifier = Modifier

@Composable
fun FileTreeView(root: File, onFileClick: (File) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(id = R.string.search_files), fontSize = 12.sp) },
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
                textStyle = TextStyle(fontSize = 12.sp)
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = { refreshTrigger++ }) {
                Icon(Icons.Default.Refresh, stringResource(id = R.string.refresh), modifier = Modifier.size(20.dp))
            }
        }

        key(refreshTrigger) {
            val scrollStateH = rememberScrollState()
            val scrollStateV = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollStateH)
                    .verticalScroll(scrollStateV)
            ) {
                Column {
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
        Text(stringResource(id = R.string.not_found), modifier = Modifier.padding(16.dp), color = Color.Gray, fontSize = 12.sp)
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
        ?.filter { !it.name.startsWith(".") }
        ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        ?: emptyList()

    if (files.isEmpty() && level == 0) {
        Text(
            text = stringResource(id = R.string.not_found),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    files.forEach { file ->
        var isExpanded by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (file.isDirectory) isExpanded = !isExpanded
                    else onFileClick(file)
                }
                .padding(start = (level * 16 + 8).dp, top = 8.dp, bottom = 8.dp, end = 16.dp),
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
                maxLines = 1,
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