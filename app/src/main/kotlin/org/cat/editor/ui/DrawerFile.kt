package org.cat.editor.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cat.editor.R
import org.cat.editor.SettingsActivity
import org.cat.editor.drawer.CreateItemDialog
import org.cat.editor.drawer.DeleteFileDialog
import org.cat.editor.drawer.DrawerSortMode
import org.cat.editor.drawer.FileActionsBottomSheet
import org.cat.editor.drawer.FileIcon
import org.cat.editor.drawer.FileTreeView
import org.cat.editor.drawer.RenameFileDialog
import org.cat.editor.drawer.SearchCodeDialog
import org.cat.editor.drawer.SearchFileDialog
import org.cat.editor.drawer.SearchOptionsBottomSheet
import org.cat.editor.ui.preview.BinaryFilePlaceholder
import org.cat.editor.ui.preview.ComposeNode
import org.cat.editor.ui.preview.ImageDetailViewer
import org.cat.editor.ui.preview.PreviewLayoutXml
import org.cat.editor.ui.preview.PreviewScreen
import org.cat.editor.ui.preview.VectorDrawableParser
import org.cat.editor.ui.preview.XmlLayoutParser
import org.cat.editor.ui.preview.isBinaryFile
import org.cat.editor.ui.preview.isImageFile
import org.cat.editor.utils.SettingsManager
import java.io.File

enum class EditorViewMode { EDIT, PREVIEW }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IdeMainScreen(
    projectPath: String,
    openedFiles: List<String>,
    activeFilePath: String,
    currentRootNode: ComposeNode = ComposeNode("Root"),
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
    val prefs = remember { SettingsManager.getPrefs(context) }
    val isSmartToolbar = remember { SettingsManager.isSmartToolbarEnabled(context) }
    var isToolbarVisible by remember { mutableStateOf(true) }
    var currentCode by remember { mutableStateOf("") }
    var showMoreMenu by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(EditorViewMode.EDIT) }
    var formatTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(activeFilePath) {
        if (activeFilePath.isNotEmpty()) {
            try {
                val f = File(activeFilePath)
                if (f.exists() && f.isFile) {
                    currentCode = f.readText()
                }
            } catch (_: Throwable) {}
        }
    }

    val clipboardManager = LocalClipboardManager.current
    var treeRefreshTrigger by remember { mutableIntStateOf(0) }
    var gitRefreshTrigger by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var targetCreateFolder by remember { mutableStateOf<File?>(null) }
    var selectedFileForActions by remember { mutableStateOf<File?>(null) }
    var fileToRename by remember { mutableStateOf<File?>(null) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }
    val expandedPaths = remember { mutableStateMapOf<String, Boolean>() }

    var showDrawerMoreMenu by remember { mutableStateOf(false) }
    var showHiddenFiles by remember {
        mutableStateOf(prefs.getBoolean("drawer_show_hidden_files", false))
    }
    var compactFolders by remember {
        mutableStateOf(prefs.getBoolean("drawer_compact_folders", false))
    }
    var sortMode by remember {
        mutableStateOf(DrawerSortMode.fromInt(prefs.getInt("drawer_sort_mode", 0)))
    }

    var showSearchBottomSheet by remember { mutableStateOf(false) }
    var showSearchFileDialog by remember { mutableStateOf(false) }
    var showSearchCodeDialog by remember { mutableStateOf(false) }
    var isProjectIndexed by remember {
        mutableStateOf(prefs.getBoolean("index_project", false))
    }
    var indexedFilesCache by remember(projectPath) { mutableStateOf<List<File>?>(null) }

    if (projectPath.isEmpty()) {
        WelcomeScreen(
            projectPath = projectPath,
            onBrowseClick = { onTogglePicker(true) }
        )
        return
    }

    LaunchedEffect(projectPath) {
        if (projectPath.isNotEmpty()) {
            expandedPaths[File(projectPath).absolutePath] = true
            drawerState.open()
        }
    }

    LaunchedEffect(projectPath, isProjectIndexed, showHiddenFiles, treeRefreshTrigger) {
        if (projectPath.isNotEmpty() && isProjectIndexed) {
            withContext(Dispatchers.IO) {
                val rootFile = File(projectPath)
                val list = mutableListOf<File>()
                try {
                    rootFile.walkTopDown()
                        .onEnter { dir ->
                            dir.absolutePath == rootFile.absolutePath ||
                                (!dir.name.startsWith(".") && dir.name != "build")
                        }
                        .forEach { f ->
                            if (f.absolutePath != rootFile.absolutePath) {
                                if (!f.name.startsWith(".") || (showHiddenFiles && f.isFile)) {
                                    list.add(f)
                                }
                            }
                        }
                } catch (_: Throwable) {}
                indexedFilesCache = list
            }
        } else {
            indexedFilesCache = null
        }
    }

    LaunchedEffect(activeFilePath) {
        if (activeFilePath.isNotEmpty()) {
            val lower = activeFilePath.lowercase()
            val isPreviewable = lower.endsWith(".kt") || lower.endsWith(".kts") || lower.endsWith(".xml")
            if (!isPreviewable) {
                viewMode = EditorViewMode.EDIT
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
                windowInsets = WindowInsets.systemBars
            ) {
                DrawerWithSections(
                    onSectionChange = { section ->
                        if (section == DrawerSection.COMMIT) {
                            gitRefreshTrigger++
                        }
                    },
                    projectContent = {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = { treeRefreshTrigger++ }) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = stringResource(id = R.string.refresh)
                                    )
                                }
                                IconButton(onClick = { showSearchBottomSheet = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = stringResource(id = R.string.search_files)
                                    )
                                }
                                Box {
                                    IconButton(onClick = { showDrawerMoreMenu = true }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = stringResource(id = R.string.more_options)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showDrawerMoreMenu,
                                        onDismissRequest = { showDrawerMoreMenu = false },
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.drawer_show_hidden_files)) },
                                            leadingIcon = {
                                                Checkbox(
                                                    checked = showHiddenFiles,
                                                    onCheckedChange = null
                                                )
                                            },
                                            onClick = {
                                                showHiddenFiles = !showHiddenFiles
                                                prefs.edit().putBoolean("drawer_show_hidden_files", showHiddenFiles).apply()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.drawer_compact_folders)) },
                                            leadingIcon = {
                                                Checkbox(
                                                    checked = compactFolders,
                                                    onCheckedChange = null
                                                )
                                            },
                                            onClick = {
                                                compactFolders = !compactFolders
                                                prefs.edit().putBoolean("drawer_compact_folders", compactFolders).apply()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.drawer_sort_by_name)) },
                                            leadingIcon = {
                                                RadioButton(
                                                    selected = sortMode == DrawerSortMode.NAME,
                                                    onClick = null
                                                )
                                            },
                                            onClick = {
                                                sortMode = DrawerSortMode.NAME
                                                prefs.edit().putInt("drawer_sort_mode", DrawerSortMode.NAME.value).apply()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.drawer_sort_by_size)) },
                                            leadingIcon = {
                                                RadioButton(
                                                    selected = sortMode == DrawerSortMode.SIZE,
                                                    onClick = null
                                                )
                                            },
                                            onClick = {
                                                sortMode = DrawerSortMode.SIZE
                                                prefs.edit().putInt("drawer_sort_mode", DrawerSortMode.SIZE.value).apply()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.drawer_sort_by_date)) },
                                            leadingIcon = {
                                                RadioButton(
                                                    selected = sortMode == DrawerSortMode.DATE,
                                                    onClick = null
                                                )
                                            },
                                            onClick = {
                                                sortMode = DrawerSortMode.DATE
                                                prefs.edit().putInt("drawer_sort_mode", DrawerSortMode.DATE.value).apply()
                                            }
                                        )
                                    }
                                }
                            }
                            HorizontalDivider()
                            if (projectPath.isNotEmpty()) {
                                Column {
                                    FileTreeView(
                                        root = File(projectPath),
                                        refreshKey = treeRefreshTrigger,
                                        showHiddenFiles = showHiddenFiles,
                                        compactFolders = compactFolders,
                                        sortMode = sortMode,
                                        expandedPaths = expandedPaths,
                                        onFileClick = { file ->
                                            if (!file.exists() || file.isDirectory) return@FileTreeView
                                            val lower = file.name.lowercase()
                                            val isPreviewable = lower.endsWith(".kt") || lower.endsWith(".kts") || lower.endsWith(".xml")
                                            if (!isPreviewable) {
                                                viewMode = EditorViewMode.EDIT
                                            }
                                            onFileSelected(file)
                                            scope.launch {
                                                drawerState.close()
                                            }
                                        },
                                        onFileLongClick = { file ->
                                            selectedFileForActions = file
                                        }
                                    )
                                }
                            }
                        }
                    },
                    commitContent = {
                        CommitPanel(
                            projectPath = projectPath,
                            refreshTrigger = gitRefreshTrigger,
                            onOpenFile = { file ->
                                if (!file.exists() || file.isDirectory) return@CommitPanel
                                val lower = file.name.lowercase()
                                val isPreviewable = lower.endsWith(".kt") || lower.endsWith(".kts") || lower.endsWith(".xml")
                                if (!isPreviewable) {
                                    viewMode = EditorViewMode.EDIT
                                }
                                onFileSelected(file)
                                scope.launch { drawerState.close() }
                            },
                            onNavigateSettings = {
                                context.startActivity(Intent(context, SettingsActivity::class.java))
                            }
                        )
                    }
                )
            }
        }
    ) {
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp
        val isSmallDpi = screenWidthDp < 380
        val isVerySmallDpi = screenWidthDp < 320

        val handleUndo = {
            Toast.makeText(context, context.getString(R.string.undo), Toast.LENGTH_SHORT).show()
        }
        val handleRedo = {
            Toast.makeText(context, context.getString(R.string.redo), Toast.LENGTH_SHORT).show()
        }
        val handleSave = {
            if (activeFilePath.isNotEmpty()) {
                try {
                    File(activeFilePath).writeText(currentCode)
                    Toast.makeText(context, context.getString(R.string.file_saved), Toast.LENGTH_SHORT).show()
                    gitRefreshTrigger++
                    onSaveFile?.invoke()
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.failed_to_save, e.message ?: ""), Toast.LENGTH_SHORT).show()
                }
            }
        }
        val handleAdd = {
            targetCreateFolder = if (activeFilePath.isNotEmpty()) {
                val active = File(activeFilePath)
                if (active.isDirectory) active else active.parentFile ?: File(projectPath)
            } else {
                File(projectPath)
            }
            showCreateDialog = true
            onAddFile?.invoke()
        }
        val canPreviewActiveFile = remember(activeFilePath, currentCode) {
            if (activeFilePath.isBlank()) false
            else {
                val lower = activeFilePath.lowercase()
                val effectiveCode = if (currentCode.isNotEmpty()) currentCode else {
                    try {
                        val f = File(activeFilePath)
                        if (f.exists() && f.isFile) f.readText() else ""
                    } catch (_: Throwable) { "" }
                }
                when {
                    lower.endsWith(".kt") || lower.endsWith(".kts") -> {
                        effectiveCode.contains("@Preview")
                    }
                    lower.endsWith(".xml") -> {
                        XmlLayoutParser.isLayoutXml(effectiveCode) || VectorDrawableParser.isVectorXml(effectiveCode)
                    }
                    else -> false
                }
            }
        }

        LaunchedEffect(canPreviewActiveFile) {
            if (!canPreviewActiveFile && viewMode == EditorViewMode.PREVIEW) {
                viewMode = EditorViewMode.EDIT
            }
        }

        val handlePlay = {
            Toast.makeText(context, "Run Code", Toast.LENGTH_SHORT).show()
        }
        val handlePreview = {
            if (activeFilePath.isNotEmpty() && canPreviewActiveFile) {
                viewMode = if (viewMode == EditorViewMode.EDIT) EditorViewMode.PREVIEW else EditorViewMode.EDIT
                if (drawerState.isOpen) {
                    scope.launch { drawerState.close() }
                }
            }
        }

        Scaffold(
            contentWindowInsets = WindowInsets.systemBars,
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
                                if (SettingsManager.isDebugOptionsEnabled(context)) {
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

                                if (!isVerySmallDpi) {
                                    IconButton(onClick = { handleUndo() }) {
                                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(id = R.string.undo))
                                    }

                                    IconButton(onClick = { handleRedo() }) {
                                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = stringResource(id = R.string.redo))
                                    }
                                }

                                if (!isSmallDpi) {
                                    IconButton(onClick = { handleSave() }) {
                                        Icon(Icons.Default.Save, contentDescription = stringResource(id = R.string.save))
                                    }

                                    IconButton(onClick = { handlePlay() }) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Run"
                                        )
                                    }

                                    if (activeFilePath.isNotEmpty() && canPreviewActiveFile) {
                                        IconButton(onClick = { handlePreview() }) {
                                            Icon(
                                                if (viewMode == EditorViewMode.EDIT) Icons.Default.Visibility else Icons.Default.Edit,
                                                contentDescription = if (viewMode == EditorViewMode.EDIT) "Preview" else "Edit",
                                                tint = if (viewMode == EditorViewMode.PREVIEW) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                            )
                                        }
                                    }

                                    IconButton(onClick = { handleAdd() }) {
                                        Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add))
                                    }
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
                                        if (isVerySmallDpi) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(id = R.string.undo)) },
                                                leadingIcon = {
                                                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                                                },
                                                onClick = {
                                                    showMoreMenu = false
                                                    handleUndo()
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(stringResource(id = R.string.redo)) },
                                                leadingIcon = {
                                                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = null)
                                                },
                                                onClick = {
                                                    showMoreMenu = false
                                                    handleRedo()
                                                }
                                            )
                                        }

                                        if (isSmallDpi) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(id = R.string.save)) },
                                                leadingIcon = {
                                                    Icon(Icons.Default.Save, contentDescription = null)
                                                },
                                                onClick = {
                                                    showMoreMenu = false
                                                    handleSave()
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Run Code") },
                                                leadingIcon = {
                                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                                },
                                                onClick = {
                                                    showMoreMenu = false
                                                    handlePlay()
                                                }
                                            )
                                            if (activeFilePath.isNotEmpty() && canPreviewActiveFile) {
                                                DropdownMenuItem(
                                                    text = { Text(if (viewMode == EditorViewMode.EDIT) "Preview" else "Edit Code") },
                                                    leadingIcon = {
                                                        Icon(
                                                            if (viewMode == EditorViewMode.EDIT) Icons.Default.Visibility else Icons.Default.Edit,
                                                            contentDescription = null
                                                        )
                                                    },
                                                    onClick = {
                                                        showMoreMenu = false
                                                        handlePreview()
                                                    }
                                                )
                                            }
                                            DropdownMenuItem(
                                                text = { Text(stringResource(id = R.string.add)) },
                                                leadingIcon = {
                                                    Icon(Icons.Default.Add, contentDescription = null)
                                                },
                                                onClick = {
                                                    showMoreMenu = false
                                                    handleAdd()
                                                }
                                            )
                                        }

                                        if (activeFilePath.isNotEmpty() && viewMode == EditorViewMode.EDIT) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(id = R.string.pretty_print)) },
                                                leadingIcon = {
                                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                                                },
                                                onClick = {
                                                    showMoreMenu = false
                                                    formatTrigger++
                                                }
                                            )
                                        }

                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.open_folder_saf)) },
                                            leadingIcon = {
                                                Icon(Icons.Default.FolderOpen, contentDescription = null)
                                            },
                                            onClick = {
                                                showMoreMenu = false
                                                onTogglePicker(true)
                                            }
                                        )
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
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }

                    val validOpenedFiles = remember(openedFiles, projectPath) {
                        if (projectPath.isBlank()) emptyList()
                        else {
                            try {
                                val projectDir = File(projectPath).canonicalFile
                                openedFiles.filter { path ->
                                    val f = File(path)
                                    f.exists() && f.isFile && f.canonicalFile.startsWith(projectDir)
                                }
                            } catch (_: Throwable) {
                                emptyList()
                            }
                        }
                    }

                    if (validOpenedFiles.isNotEmpty()) {
                        val activeIndex = validOpenedFiles.indexOf(activeFilePath).let { idx ->
                            if (idx in validOpenedFiles.indices) idx else 0
                        }

                        ScrollableTabRow(
                            selectedTabIndex = activeIndex,
                            edgePadding = 0.dp,
                            containerColor = MaterialTheme.colorScheme.surface,
                            divider = { },
                            indicator = { tabPositions ->
                                if (activeIndex in tabPositions.indices) {
                                    TabRowDefaults.SecondaryIndicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeIndex])
                                    )
                                }
                            }
                        ) {
                                validOpenedFiles.forEachIndexed { index, path ->
                                    val file = File(path)
                                    Tab(
                                        selected = activeFilePath == path,
                                        onClick = {
                                            val f = File(path)
                                            if (!f.exists() || !f.isFile) return@Tab
                                            val lower = path.lowercase()
                                            val isPreviewable = lower.endsWith(".kt") || lower.endsWith(".kts") || lower.endsWith(".xml")
                                            if (!isPreviewable) {
                                                viewMode = EditorViewMode.EDIT
                                            }
                                            onTabSelected(path)
                                        },
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

                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    }
                },
                floatingActionButton = {
                    val density = LocalDensity.current
                    val isImeVisible = WindowInsets.ime.getBottom(density) > 0 || WindowInsets.isImeVisible
                    if (!isImeVisible) {
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
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .consumeWindowInsets(paddingValues)
                        .fillMaxSize()
                ) {
                    if (activeFilePath.isNotEmpty()) {
                        val activeFile = remember(activeFilePath) { File(activeFilePath) }
                        val isImg = remember(activeFilePath) { isImageFile(activeFilePath) }
                        val isBin = remember(activeFilePath) { isBinaryFile(activeFilePath) }

                        if (!activeFile.exists() || activeFile.isDirectory) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(id = R.string.not_found),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (isImg) {
                            ImageDetailViewer(file = activeFile)
                        } else if (isBin) {
                            BinaryFilePlaceholder(file = activeFile)
                        } else {
                            when (viewMode) {
                                EditorViewMode.EDIT -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        KotlinCodeEditor(
                                            filePath = activeFilePath,
                                            onCodeChange = { newCode ->
                                                currentCode = newCode
                                            },
                                            onEditorInteraction = {
                                                if (drawerState.isOpen) {
                                                    scope.launch { drawerState.close() }
                                                }
                                            },
                                            formatTrigger = formatTrigger
                                        )
                                    }
                                }
                                EditorViewMode.PREVIEW -> {
                                    val isXml = activeFilePath.lowercase().endsWith(".xml")
                                    if (isXml) {
                                        PreviewLayoutXml(
                                            activeFilePath = activeFilePath,
                                            sourceCode = currentCode,
                                            onBackClick = {
                                                viewMode = EditorViewMode.EDIT
                                            }
                                        )
                                    } else {
                                        PreviewScreen(
                                            activeFilePath = activeFilePath,
                                            currentRootNode = currentRootNode,
                                            onBackClick = {
                                                viewMode = EditorViewMode.EDIT
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        WelcomeScreen(
                            projectPath = projectPath,
                            onBrowseClick = {
                                scope.launch {
                                    if (drawerState.isClosed) drawerState.open()
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showSearchBottomSheet) {
            SearchOptionsBottomSheet(
                isProjectIndexed = isProjectIndexed,
                onToggleIndexProject = { checked ->
                    isProjectIndexed = checked
                    prefs.edit().putBoolean("index_project", checked).apply()
                },
                onSearchFilesClick = {
                    showSearchBottomSheet = false
                    showSearchFileDialog = true
                },
                onSearchCodeClick = {
                    showSearchBottomSheet = false
                    showSearchCodeDialog = true
                },
                onDismiss = { showSearchBottomSheet = false }
            )
        }

        if (showSearchFileDialog) {
            SearchFileDialog(
                projectRoot = File(projectPath),
                showHiddenFiles = showHiddenFiles,
                cachedFiles = indexedFilesCache,
                onDismiss = { showSearchFileDialog = false },
                onItemSelected = { selectedFile ->
                    showSearchFileDialog = false
                    if (selectedFile.isDirectory) {
                        var current: File? = selectedFile
                        val rootAbs = File(projectPath).absolutePath
                        while (current != null && current.absolutePath.startsWith(rootAbs)) {
                            expandedPaths[current.absolutePath] = true
                            if (current.absolutePath == rootAbs) break
                            current = current.parentFile
                        }
                        treeRefreshTrigger++
                    } else {
                        val lower = selectedFile.name.lowercase()
                        val isPreviewable = lower.endsWith(".kt") || lower.endsWith(".kts") || lower.endsWith(".xml")
                        if (!isPreviewable) {
                            viewMode = EditorViewMode.EDIT
                        }
                        onFileSelected(selectedFile)
                        scope.launch { drawerState.close() }
                    }
                }
            )
        }

        if (showSearchCodeDialog) {
            SearchCodeDialog(
                projectRoot = File(projectPath),
                showHiddenFiles = showHiddenFiles,
                cachedFiles = indexedFilesCache,
                onDismiss = { showSearchCodeDialog = false },
                onResultClick = { matchedFile ->
                    showSearchCodeDialog = false
                    val lower = matchedFile.name.lowercase()
                    val isPreviewable = lower.endsWith(".kt") || lower.endsWith(".kts") || lower.endsWith(".xml")
                    if (!isPreviewable) {
                        viewMode = EditorViewMode.EDIT
                    }
                    onFileSelected(matchedFile)
                    scope.launch { drawerState.close() }
                }
            )
        }

        if (showCreateDialog && targetCreateFolder != null) {
            CreateItemDialog(
                targetFolder = targetCreateFolder!!,
                projectRoot = File(projectPath),
                onDismiss = {
                    showCreateDialog = false
                    targetCreateFolder = null
                },
                onItemCreated = { newFile ->
                    expandedPaths[targetCreateFolder!!.absolutePath] = true
                    treeRefreshTrigger++
                    gitRefreshTrigger++
                    if (newFile.isFile) {
                        onFileSelected(newFile)
                    }
                }
            )
        }

        if (selectedFileForActions != null) {
            val file = selectedFileForActions!!
            FileActionsBottomSheet(
                file = file,
                projectRoot = File(projectPath),
                onDismiss = { selectedFileForActions = null },
                onNewInside = {
                    selectedFileForActions = null
                    targetCreateFolder = file
                    showCreateDialog = true
                },
                onRename = {
                    selectedFileForActions = null
                    fileToRename = file
                },
                onDelete = {
                    selectedFileForActions = null
                    fileToDelete = file
                },
                onCopyPath = { path ->
                    clipboardManager.setText(AnnotatedString(path))
                    Toast.makeText(context, context.getString(R.string.path_copied), Toast.LENGTH_SHORT).show()
                    selectedFileForActions = null
                },
                onCopyRelativePath = { relPath ->
                    clipboardManager.setText(AnnotatedString(relPath))
                    Toast.makeText(context, context.getString(R.string.path_copied), Toast.LENGTH_SHORT).show()
                    selectedFileForActions = null
                }
            )
        }

        if (fileToRename != null) {
            val file = fileToRename!!
            RenameFileDialog(
                file = file,
                onDismiss = { fileToRename = null },
                onRenamed = { renamedFile ->
                    treeRefreshTrigger++
                    gitRefreshTrigger++
                    if (openedFiles.contains(file.absolutePath)) {
                        onCloseFile?.invoke(file.absolutePath)
                        if (renamedFile.isFile) {
                            onFileSelected(renamedFile)
                        }
                    }
                }
            )
        }

        if (fileToDelete != null) {
            val file = fileToDelete!!
            DeleteFileDialog(
                file = file,
                onDismiss = { fileToDelete = null },
                onDeleted = {
                    treeRefreshTrigger++
                    gitRefreshTrigger++
                    if (file.isDirectory) {
                        openedFiles.filter { it.startsWith(file.absolutePath) }.forEach {
                            onCloseFile?.invoke(it)
                        }
                    } else {
                        if (openedFiles.contains(file.absolutePath)) {
                            onCloseFile?.invoke(file.absolutePath)
                        }
                    }
                }
            )
        }
    }
