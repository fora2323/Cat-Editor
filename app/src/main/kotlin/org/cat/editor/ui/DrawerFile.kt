package org.cat.editor.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cat.editor.R
import org.cat.editor.SettingsActivity
import org.cat.editor.ui.preview.BinaryFilePlaceholder
import org.cat.editor.ui.preview.ComposeNode
import org.cat.editor.ui.preview.ImageDetailViewer
import org.cat.editor.ui.preview.ImagePreviewIcon
import org.cat.editor.ui.preview.PreviewLayoutXml
import org.cat.editor.ui.preview.PreviewScreen
import org.cat.editor.ui.preview.XmlLayoutParser
import org.cat.editor.ui.preview.VectorDrawableParser
import org.cat.editor.ui.preview.isBinaryFile
import org.cat.editor.ui.preview.isImageFile
import org.cat.editor.utils.SettingsManager
import java.io.File
import kotlin.coroutines.coroutineContext

enum class EditorViewMode { EDIT, PREVIEW }

enum class DrawerSortMode(val value: Int) {
    NAME(0),
    SIZE(1),
    DATE(2);

    companion object {
        fun fromInt(value: Int): DrawerSortMode = entries.firstOrNull { it.value == value } ?: NAME
    }
}

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
    var showCreateDialog by remember { mutableStateOf(false) }
    var targetCreateFolder by remember { mutableStateOf<File?>(null) }
    var selectedFileForActions by remember { mutableStateOf<File?>(null) }
    var fileToRename by remember { mutableStateOf<File?>(null) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }
    val expandedPaths = remember { mutableStateMapOf<String, Boolean>() }

    // State untuk menu titik tiga Drawer (Show hidden files, Compact folders, Sort mode)
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

    // State untuk menu Search di Drawer
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

    // Jika Indeks Proyek aktif, simpan daftar file di memori sesuai filter hidden files
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
                        // Menu opsi tampilan dan urutan file sesuai gambar referensi
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchOptionsBottomSheet(
    isProjectIndexed: Boolean,
    onToggleIndexProject: (Boolean) -> Unit,
    onSearchFilesClick: () -> Unit,
    onSearchCodeClick: () -> Unit,
    onDismiss: () -> Unit
) {
    BottomSheetDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp)
                .clickable { onToggleIndexProject(!isProjectIndexed) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.search_index_project_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(id = R.string.search_index_project_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = isProjectIndexed,
                    onCheckedChange = onToggleIndexProject
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSearchFilesClick() }
                .padding(vertical = 14.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(id = R.string.search_files_in_project),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSearchCodeClick() }
                .padding(vertical = 14.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(id = R.string.search_code_in_project),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun SearchFileDialog(
    projectRoot: File,
    showHiddenFiles: Boolean,
    cachedFiles: List<File>?,
    onDismiss: () -> Unit,
    onItemSelected: (File) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var allProjectItems by remember { mutableStateOf<List<File>>(emptyList()) }
    var isScanning by remember { mutableStateOf(true) }

    LaunchedEffect(projectRoot, showHiddenFiles, cachedFiles) {
        isScanning = true
        val items = withContext(Dispatchers.IO) {
            if (cachedFiles != null) {
                cachedFiles
            } else {
                val list = mutableListOf<File>()
                try {
                    projectRoot.walkTopDown()
                        .onEnter { dir ->
                            dir.absolutePath == projectRoot.absolutePath ||
                                (!dir.name.startsWith(".") && dir.name != "build")
                        }
                        .forEach { f ->
                            if (f.absolutePath != projectRoot.absolutePath) {
                                if (!f.name.startsWith(".") || (showHiddenFiles && f.isFile)) {
                                    list.add(f)
                                }
                            }
                        }
                } catch (_: Throwable) {}
                list
            }
        }
        allProjectItems = items
        isScanning = false
    }

    val filteredItems = remember(query, allProjectItems) {
        val q = query.trim()
        if (q.isEmpty()) {
            allProjectItems
        } else {
            allProjectItems.filter { it.name.contains(q, ignoreCase = true) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.82f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.search_enter_name_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider(
                    thickness = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.searching_in_project, projectRoot.name),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (filteredItems.isEmpty() && !isScanning) {
                            stringResource(id = R.string.search_no_results)
                        } else {
                            stringResource(id = R.string.search_results_count, filteredItems.size)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (isScanning) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(
                                count = filteredItems.size,
                                key = { index -> filteredItems[index].absolutePath }
                            ) { index ->
                                val item = filteredItems[index]
                                val relPath = "./" + (item.relativeToOrNull(projectRoot)?.invariantSeparatorsPath ?: item.name)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onItemSelected(item) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (item.isDirectory) {
                                        FolderIcon(
                                            folderName = item.name,
                                            isExpanded = false,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        FileIcon(
                                            file = item,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = relPath,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class CodeSearchMatch(
    val file: File,
    val relativePath: String,
    val lineNumber: Int,
    val lineSnippet: String
)

@Composable
fun SearchCodeDialog(
    projectRoot: File,
    showHiddenFiles: Boolean,
    cachedFiles: List<File>?,
    onDismiss: () -> Unit,
    onResultClick: (File) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var ignoreCase by remember { mutableStateOf(true) }
    var fileFilter by remember { mutableStateOf("") }
    var showFilterInput by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    var isSearching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<CodeSearchMatch>>(emptyList()) }

    LaunchedEffect(query, ignoreCase, fileFilter, projectRoot, showHiddenFiles, cachedFiles) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            results = emptyList()
            isSearching = false
            return@LaunchedEffect
        }

        isSearching = true
        delay(300)

        val matchedList = withContext(Dispatchers.IO) {
            val found = mutableListOf<CodeSearchMatch>()
            val filterExtensions = fileFilter.split(",")
                .map { it.trim().removePrefix("*.").removePrefix(".").lowercase() }
                .filter { it.isNotEmpty() }

            val candidateFiles = cachedFiles ?: run {
                val list = mutableListOf<File>()
                try {
                    projectRoot.walkTopDown()
                        .onEnter { dir ->
                            dir.absolutePath == projectRoot.absolutePath ||
                                (!dir.name.startsWith(".") && dir.name != "build")
                        }
                        .forEach { f ->
                            if (f.isFile && (!f.name.startsWith(".") || showHiddenFiles)) {
                                list.add(f)
                            }
                        }
                } catch (_: Throwable) {}
                list
            }

            for (file in candidateFiles) {
                coroutineContext.ensureActive()
                if (!file.isFile) continue
                if (isBinaryFile(file.name) || isImageFile(file.name)) continue
                if (file.length() > 2 * 1024 * 1024) continue

                if (filterExtensions.isNotEmpty()) {
                    val ext = file.extension.lowercase()
                    if (ext !in filterExtensions) continue
                }

                val relPath = "./" + (file.relativeToOrNull(projectRoot)?.invariantSeparatorsPath ?: file.name)
                try {
                    file.bufferedReader().useLines { lines ->
                        var lineIdx = 0
                        for (line in lines) {
                            lineIdx++
                            if (line.contains(trimmedQuery, ignoreCase = ignoreCase)) {
                                found.add(
                                    CodeSearchMatch(
                                        file = file,
                                        relativePath = relPath,
                                        lineNumber = lineIdx,
                                        lineSnippet = line.trim()
                                    )
                                )
                                if (found.size >= 300) break
                            }
                        }
                    }
                } catch (_: Throwable) {}
                if (found.size >= 300) break
            }
            found
        }

        results = matchedList
        isSearching = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.82f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showFilterInput = !showFilterInput }) {
                        Icon(
                            imageVector = if (showFilterInput) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(id = R.string.search_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Box {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.search_ignore_case)) },
                                leadingIcon = {
                                    Checkbox(
                                        checked = ignoreCase,
                                        onCheckedChange = null
                                    )
                                },
                                onClick = {
                                    ignoreCase = !ignoreCase
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.search_filter_files)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    showFilterInput = true
                                }
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = showFilterInput) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        OutlinedTextField(
                            value = fileFilter,
                            onValueChange = { fileFilter = it },
                            label = { Text(stringResource(id = R.string.search_filter_ext_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                HorizontalDivider(
                    thickness = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.searching_in_project, projectRoot.name),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (query.isBlank() || (results.isEmpty() && !isSearching)) {
                            stringResource(id = R.string.search_no_results)
                        } else if (isSearching) {
                            stringResource(id = R.string.search_code_searching)
                        } else {
                            stringResource(id = R.string.search_results_count, results.size)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (query.isBlank()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(id = R.string.search_code_empty_prompt),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else if (isSearching) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    strokeWidth = 3.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(id = R.string.search_code_scanning),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(
                                count = results.size,
                                key = { index -> "${results[index].file.absolutePath}:${results[index].lineNumber}:$index" }
                            ) { index ->
                                val match = results[index]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onResultClick(match.file) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    FileIcon(
                                        file = match.file,
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${match.file.name} : ${match.lineNumber}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = match.lineSnippet,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = match.relativePath,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileIcon(file: File, modifier: Modifier = Modifier) {
    val fileName = file.name
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
    when {
        isImageFile(fileName) -> ImagePreviewIcon(file = file, modifier = modifier)
        fileName == "AndroidManifest.xml" -> Icon(painterResource(R.drawable.ic_android), stringResource(id = R.string.manifest), modifier = modifier, tint = iconTint)
        fileName.endsWith(".kt") -> Icon(painterResource(R.drawable.ic_kotlin), stringResource(id = R.string.file_kotlin), modifier = modifier, tint = iconTint)
        fileName.endsWith(".kts") || fileName == "gradlew" || fileName == "gradlew.bat" || fileName.endsWith(".gradle") || fileName == "gradle.properties" || fileName == "local.properties" -> Icon(painterResource(R.drawable.ic_gradle), stringResource(id = R.string.file_gradle), modifier = modifier, tint = iconTint)
        fileName.endsWith(".java") -> Icon(painterResource(R.drawable.ic_java), stringResource(id = R.string.file_java), modifier = modifier, tint = iconTint)
        fileName.endsWith(".xml") -> Icon(painterResource(R.drawable.ic_xml), stringResource(id = R.string.file_xml), modifier = modifier, tint = iconTint)
        fileName.endsWith(".cpp") || fileName.endsWith(".cc") || fileName.endsWith(".cxx") || fileName.endsWith(".hpp") -> Icon(painterResource(R.drawable.ic_cpp), stringResource(id = R.string.file_cpp), modifier = modifier, tint = iconTint)
        fileName.endsWith(".c") || fileName.endsWith(".h") -> Icon(painterResource(R.drawable.ic_c), stringResource(id = R.string.file_c), modifier = modifier, tint = iconTint)
        fileName == "CMakeLists.txt" || fileName.endsWith(".cmake") -> Icon(painterResource(R.drawable.ic_cmake), stringResource(id = R.string.file_cmake), modifier = modifier, tint = iconTint)
        fileName.endsWith(".py") -> Icon(Icons.Default.Code, stringResource(id = R.string.file_python), modifier = modifier, tint = iconTint)
        fileName.endsWith(".md") -> Icon(Icons.Default.Description, stringResource(id = R.string.file_markdown), modifier = modifier, tint = iconTint)
        fileName.endsWith(".json") -> Icon(Icons.Default.DataObject, stringResource(id = R.string.file_json), modifier = modifier, tint = iconTint)
        fileName.startsWith(".git") -> Icon(Icons.Default.Source, stringResource(id = R.string.file_git), modifier = modifier, tint = iconTint)
        else -> Icon(Icons.Default.Description, stringResource(id = R.string.file_generic), modifier = modifier, tint = iconTint)
    }
}

data class FileTreeItem(
    val file: File,
    val displayName: String,
    val level: Int,
    val isDirectory: Boolean,
    val isExpanded: Boolean
)

// Filter hanya menampilkan file berawalan '.' jika showHiddenFiles = true, lalu urutkan sesuai sortMode
private fun loadDirectoryChildren(
    dir: File,
    showHiddenFiles: Boolean,
    sortMode: DrawerSortMode
): List<File> {
    return try {
        val comparator = when (sortMode) {
            DrawerSortMode.SIZE -> compareBy<File> { !it.isDirectory }
                .thenByDescending { if (it.isFile) it.length() else 0L }
                .thenBy { it.name.lowercase() }
            DrawerSortMode.DATE -> compareBy<File> { !it.isDirectory }
                .thenByDescending { it.lastModified() }
                .thenBy { it.name.lowercase() }
            DrawerSortMode.NAME -> compareBy<File> { !it.isDirectory }
                .thenBy { it.name.lowercase() }
        }
        dir.listFiles()
            ?.filter { file ->
                if (file.name.startsWith(".")) {
                    showHiddenFiles && file.isFile
                } else {
                    true
                }
            }
            ?.sortedWith(comparator)
            ?: emptyList()
    } catch (_: Throwable) {
        emptyList()
    }
}

// Muat isi direktori sekaligus menelusuri rantai subfolder tunggal untuk fitur Compact Folders
private fun loadDirectoryWithCompactChains(
    dir: File,
    showHiddenFiles: Boolean,
    sortMode: DrawerSortMode
): Map<String, List<File>> {
    val result = mutableMapOf<String, List<File>>()
    val children = loadDirectoryChildren(dir, showHiddenFiles, sortMode)
    result[dir.absolutePath] = children

    for (child in children) {
        if (child.isDirectory) {
            var curr = child
            var depth = 0
            while (depth < 15) {
                val sub = loadDirectoryChildren(curr, showHiddenFiles, sortMode)
                result[curr.absolutePath] = sub
                if (sub.size == 1 && sub[0].isDirectory) {
                    curr = sub[0]
                    depth++
                } else {
                    break
                }
            }
        }
    }
    return result
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileTreeView(
    root: File,
    refreshKey: Int = 0,
    showHiddenFiles: Boolean,
    compactFolders: Boolean,
    sortMode: DrawerSortMode,
    expandedPaths: SnapshotStateMap<String, Boolean>,
    onFileClick: (File) -> Unit,
    onFileLongClick: (File) -> Unit
) {
    val scope = rememberCoroutineScope()

    val dirChildrenCache = remember(root) { mutableStateMapOf<String, List<File>>() }
    val loadingPaths = remember(root) { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(root, refreshKey, showHiddenFiles, compactFolders, sortMode) {
        if (!expandedPaths.containsKey(root.absolutePath)) {
            expandedPaths[root.absolutePath] = true
        }
        val pathsToLoad = (listOf(root.absolutePath) + expandedPaths.filterValues { it }.keys).distinct()
        pathsToLoad.forEach { path -> loadingPaths[path] = true }

        val loadedMap = withContext(Dispatchers.IO) {
            val combined = mutableMapOf<String, List<File>>()
            for (path in pathsToLoad) {
                combined.putAll(loadDirectoryWithCompactChains(File(path), showHiddenFiles, sortMode))
            }
            combined
        }

        dirChildrenCache.clear()
        dirChildrenCache.putAll(loadedMap)
        pathsToLoad.forEach { path -> loadingPaths.remove(path) }
    }

    // Susun daftar item tree: gabungkan folder yang hanya punya 1 subfolder menjadi "com/simple" jika compactFolders aktif
    val visibleItems = remember(root, expandedPaths.toMap(), dirChildrenCache.toMap(), compactFolders) {
        val items = mutableListOf<FileTreeItem>()
        val isRootExpanded = expandedPaths[root.absolutePath] ?: true
        items.add(FileTreeItem(root, root.name, 0, true, isRootExpanded))

        fun traverse(dir: File, level: Int) {
            val children = dirChildrenCache[dir.absolutePath] ?: emptyList()
            for (file in children) {
                if (file.isDirectory) {
                    var targetDir = file
                    var displayName = file.name
                    if (compactFolders) {
                        while (true) {
                            val subChildren = dirChildrenCache[targetDir.absolutePath]
                            if (subChildren != null && subChildren.size == 1 && subChildren[0].isDirectory) {
                                val singleChild = subChildren[0]
                                displayName = "$displayName/${singleChild.name}"
                                targetDir = singleChild
                            } else {
                                break
                            }
                        }
                    }
                    val isExpanded = expandedPaths[targetDir.absolutePath] == true
                    items.add(FileTreeItem(targetDir, displayName, level, true, isExpanded))
                    if (isExpanded) {
                        traverse(targetDir, level + 1)
                    }
                } else {
                    items.add(FileTreeItem(file, file.name, level, false, false))
                }
            }
        }
        if (isRootExpanded) {
            traverse(root, 1)
        }
        items
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (visibleItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (loadingPaths[root.absolutePath] == true) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.not_found),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 6.dp, bottom = 80.dp)
            ) {
                items(
                    count = visibleItems.size,
                    key = { index -> visibleItems[index].file.absolutePath }
                ) { index ->
                    val item = visibleItems[index]
                    val itemPath = item.file.absolutePath
                    val isFolderLoading = loadingPaths[itemPath] == true

                    FileTreeItemRow(
                        item = item,
                        isLoading = isFolderLoading,
                        onClick = {
                            if (item.isDirectory) {
                                if (isFolderLoading) return@FileTreeItemRow

                                if (item.isExpanded) {
                                    expandedPaths[itemPath] = false
                                } else {
                                    loadingPaths[itemPath] = true
                                    scope.launch(Dispatchers.IO) {
                                        val loaded = loadDirectoryWithCompactChains(item.file, showHiddenFiles, sortMode)
                                        withContext(Dispatchers.Main) {
                                            dirChildrenCache.putAll(loaded)
                                            loadingPaths.remove(itemPath)
                                            expandedPaths[itemPath] = true
                                        }
                                    }
                                }
                            } else {
                                onFileClick(item.file)
                            }
                        },
                        onLongClick = { onFileLongClick(item.file) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileTreeItemRow(
    item: FileTreeItem,
    isLoading: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val file = item.file
    val isExpanded = item.isExpanded
    val level = item.level

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(start = (level * 16 + 8).dp, top = 8.dp, bottom = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.isDirectory) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(4.dp))
            FolderIcon(folderName = item.displayName, isExpanded = isExpanded, modifier = Modifier.size(20.dp))
        } else {
            Spacer(Modifier.width(22.dp))
            FileIcon(file = file, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(8.dp))
        Text(
            text = item.displayName,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false
        )
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
        tint = MaterialTheme.colorScheme.primary
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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