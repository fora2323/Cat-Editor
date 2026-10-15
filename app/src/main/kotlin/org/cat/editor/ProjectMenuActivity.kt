package org.cat.editor

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cat.editor.ui.BottomSheetDialog
import org.cat.editor.ui.DialogLoading
import org.cat.editor.ui.ExpressivePullToRefreshBox
import org.cat.editor.ui.M3ELoadingDialog
import org.cat.editor.ui.M3ELoadingIndicator
import org.cat.editor.ui.theme.AppTheme
import org.cat.editor.utils.ConfigPath
import org.cat.editor.utils.LocaleHelper
import org.cat.editor.utils.SafProjectsManager
import org.cat.editor.utils.StorageUtils
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ProjectItem(
    val name: String,
    val path: String,
    val iconBitmap: Bitmap?,
    val isExternal: Boolean = false,
    val isPinned: Boolean = false
)

private const val PIN_PREFS_NAME = "project_pin_prefs"
private const val KEY_PINNED_PATHS = "pinned_paths_ordered"

class ProjectMenuActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        org.cat.editor.utils.SettingsManager.applyFullscreen(this)

        setContent {
            AppTheme {
                ProjectMenuScreen(
                    onProjectSelected = { project ->
                        val intent = Intent(this@ProjectMenuActivity, MainActivity::class.java).apply {
                            putExtra("project_path", project.path)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        org.cat.editor.utils.SettingsManager.applyFullscreen(this)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProjectMenuScreen(
    onProjectSelected: (ProjectItem) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val activity = context as? Activity
    var showExitDialog by remember { mutableStateOf(false) }
    var isFabExpanded by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (isFabExpanded) {
            isFabExpanded = false
        } else if (org.cat.editor.utils.SettingsManager.isExitDialogEnabled(context)) {
            showExitDialog = true
        } else {
            activity?.finish()
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(id = R.string.exit_dialog_title)) },
            text = { Text(stringResource(id = R.string.exit_dialog_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        activity?.finish()
                    }
                ) {
                    Text(stringResource(id = R.string.exit_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    var hasPermission by remember { mutableStateOf(checkStoragePermission(context)) }
    var showPermissionDialog by remember { mutableStateOf(!hasPermission) }
    var projects by remember { mutableStateOf<List<ProjectItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(hasPermission) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    var selectedProjectForMenu by remember { mutableStateOf<ProjectItem?>(null) }
    var showProjectMenu by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var isOpeningProject by remember { mutableStateOf(false) }
    var pendingBackupZipFile by remember { mutableStateOf<File?>(null) }

    suspend fun refresh() {
        isRefreshing = true
        try {
            projects = withContext(Dispatchers.IO) {
                loadAllProjects(context)
            }
        } finally {
            isRefreshing = false
        }
    }

    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        if (granted) {
            hasPermission = true
            showPermissionDialog = false
            scope.launch {
                projects = withContext(Dispatchers.IO) {
                    loadAllProjects(context)
                }
            }
        } else {
            Toast.makeText(context, context.getString(R.string.storage_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    val openDocumentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val path = StorageUtils.getRealPathFromTreeUri(context, it)
            if (path != null && File(path).exists() && File(path).isDirectory) {
                SafProjectsManager.addProject(context, path, it.toString())
                scope.launch {
                    projects = withContext(Dispatchers.IO) {
                        loadAllProjects(context)
                    }
                }
                val intent = Intent(context, MainActivity::class.java).apply {
                    putExtra("project_path", path)
                }
                isOpeningProject = true
                context.startActivity(intent)
            } else {
                Toast.makeText(context, context.getString(R.string.cannot_access_folder), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launcher SAF untuk menyimpan file hasil Backup (.zip)
    val saveBackupZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        val tempZip = pendingBackupZipFile
        if (uri != null && tempZip != null && tempZip.exists()) {
            isDeleting = true
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        tempZip.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    tempZip.delete()
                    withContext(Dispatchers.Main) {
                        isDeleting = false
                        pendingBackupZipFile = null
                        Toast.makeText(context, "Backup berhasil disimpan", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    tempZip.delete()
                    withContext(Dispatchers.Main) {
                        isDeleting = false
                        pendingBackupZipFile = null
                        Toast.makeText(context, "Gagal menyimpan backup: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            scope.launch(Dispatchers.IO) {
                tempZip?.delete()
                pendingBackupZipFile = null
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isOpeningProject = false
                val currentPermission = checkStoragePermission(context)
                hasPermission = currentPermission
                if (currentPermission) {
                    showPermissionDialog = false
                    if (projects.isEmpty()) {
                        scope.launch {
                            projects = withContext(Dispatchers.IO) {
                                loadAllProjects(context)
                            }
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            isLoading = true
            projects = withContext(Dispatchers.IO) {
                loadAllProjects(context)
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(onClick = {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(id = R.string.settings),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButtonMenu(
                expanded = isFabExpanded,
                button = {
                    ToggleFloatingActionButton(
                        modifier = Modifier.semantics {
                            traversalIndex = -1f
                            stateDescription = if (isFabExpanded) "Expanded" else "Collapsed"
                            contentDescription = "Toggle menu"
                        },
                        checked = isFabExpanded,
                        onCheckedChange = { isFabExpanded = !isFabExpanded }
                    ) {
                        val imageVector by remember {
                            derivedStateOf {
                                if (checkedProgress > 0.5f) Icons.Default.Close else Icons.Default.Add
                            }
                        }
                        Icon(
                            painter = rememberVectorPainter(imageVector),
                            contentDescription = null,
                            modifier = with(ToggleFloatingActionButtonDefaults) {
                                Modifier.animateIcon({ checkedProgress })
                            }
                        )
                    }
                }
            ) {
                FloatingActionButtonMenuItem(
                    modifier = Modifier.semantics {
                        traversalIndex = 0f
                    },
                    onClick = {
                        isFabExpanded = false
                        Toast.makeText(context, context.getString(R.string.feature_new_empty_project), Toast.LENGTH_SHORT).show()
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(text = stringResource(id = R.string.new_empty_project))
                    }
                )
                FloatingActionButtonMenuItem(
                    modifier = Modifier.semantics {
                        traversalIndex = 1f
                    },
                    onClick = {
                        isFabExpanded = false
                        openDocumentTreeLauncher.launch(null)
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(text = stringResource(id = R.string.open_folder_saf))
                    }
                )
                FloatingActionButtonMenuItem(
                    modifier = Modifier.semantics {
                        traversalIndex = 2f
                    },
                    onClick = {
                        isFabExpanded = false
                        val intent = Intent(context, GitCloneActivity::class.java)
                        context.startActivity(intent)
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(text = stringResource(id = R.string.git_clone))
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ExpressivePullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { scope.launch { refresh() } },
                modifier = Modifier.fillMaxSize(),
                indicatorColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        M3ELoadingIndicator()
                    }
                } else if (projects.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (!hasPermission) stringResource(id = R.string.waiting_storage_permission)
                            else stringResource(id = R.string.no_projects_found),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(projects, key = { it.path }) { item ->
                            ProjectItemRow(
                                project = item,
                                onClick = { 
                                    isOpeningProject = true
                                    onProjectSelected(item)
                                },
                                onMoreClick = { 
                                    selectedProjectForMenu = item
                                    showProjectMenu = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Tampilkan indikator saat proses Clone, Backup, Remove, atau buka proyek berlangsung
    if (isDeleting || isOpeningProject) {
        M3ELoadingDialog()
    }

    // Tampilkan Menu Project saat di-klik
    if (showProjectMenu && selectedProjectForMenu != null) {
        val project = selectedProjectForMenu!!
        val menuSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        
        BottomSheetDialog(
            onDismissRequest = { showProjectMenu = false },
            sheetState = menuSheetState
        ) {
            Text(
                text = project.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp, start = 8.dp)
            )

            // 1. FITUR PIN / UNPIN
            ProjectMenuItem(
                icon = Icons.Default.PushPin,
                title = stringResource(id = R.string.project_pin),
                description = stringResource(id = R.string.project_pin_desc),
                onClick = {
                    showProjectMenu = false
                    togglePinProject(context, project.path)
                    scope.launch(Dispatchers.IO) {
                        val updatedProjects = loadAllProjects(context)
                        withContext(Dispatchers.Main) {
                            projects = updatedProjects
                        }
                    }
                }
            )

            // 2. FITUR CLONE (Salin isi ke NamaProject-Clone)
            ProjectMenuItem(
                icon = Icons.Default.ContentCopy,
                title = stringResource(id = R.string.project_clone),
                description = stringResource(id = R.string.project_clone_desc),
                onClick = {
                    showProjectMenu = false
                    isDeleting = true
                    scope.launch(Dispatchers.IO) {
                        try {
                            val sourceDir = File(project.path)
                            val parentDir = if (project.isExternal) {
                                File(ConfigPath.VAR_PROJECTS_PATH)
                            } else {
                                sourceDir.parentFile ?: File(ConfigPath.VAR_PROJECTS_PATH)
                            }
                            if (!parentDir.exists()) parentDir.mkdirs()

                            var cloneFolderName = "${project.name}-Clone"
                            var targetDir = File(parentDir, cloneFolderName)
                            var counter = 2
                            while (targetDir.exists()) {
                                cloneFolderName = "${project.name}-Clone-$counter"
                                targetDir = File(parentDir, cloneFolderName)
                                counter++
                            }

                            sourceDir.copyRecursively(targetDir, overwrite = true)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        val updatedProjects = loadAllProjects(context)
                        withContext(Dispatchers.Main) {
                            projects = updatedProjects
                            isDeleting = false
                        }
                    }
                }
            )

            // 3. FITUR BACKUP (Zip lalu simpan via SAF)
            ProjectMenuItem(
                icon = Icons.Default.SdStorage,
                title = stringResource(id = R.string.project_backup),
                description = stringResource(id = R.string.project_backup_desc),
                onClick = {
                    showProjectMenu = false
                    isDeleting = true
                    scope.launch(Dispatchers.IO) {
                        try {
                            val sourceDir = File(project.path)
                            val tempZipFile = File(context.cacheDir, "${project.name}.zip")
                            if (tempZipFile.exists()) {
                                tempZipFile.delete()
                            }

                            zipDirectory(sourceDir, tempZipFile)

                            withContext(Dispatchers.Main) {
                                pendingBackupZipFile = tempZipFile
                                isDeleting = false
                                saveBackupZipLauncher.launch("${project.name}.zip")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            withContext(Dispatchers.Main) {
                                isDeleting = false
                                Toast.makeText(context, "Gagal membuat zip: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            )

            // 4. FITUR REMOVE
            ProjectMenuItem(
                icon = Icons.Default.Delete,
                title = stringResource(id = R.string.project_remove),
                description = stringResource(id = R.string.project_remove_desc),
                isDestructive = true,
                onClick = {
                    showProjectMenu = false
                    isDeleting = true
                    
                    // Eksekusi fungsi hapus di Background Thread (IO)
                    scope.launch(Dispatchers.IO) {
                        try {
                            File(project.path).deleteRecursively()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        SafProjectsManager.removeProject(context, project.path)
                        removePinnedPath(context, project.path)

                        // Setelah selesai, muat ulang list proyek dan tutup loading dialog
                        val updatedProjects = loadAllProjects(context)
                        withContext(Dispatchers.Main) {
                            projects = updatedProjects
                            isDeleting = false
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showPermissionDialog) {
        val permissionSheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { false }
        )
        BottomSheetDialog(
            onDismissRequest = { },
            sheetState = permissionSheetState
        ) {
            Text(
                text = stringResource(id = R.string.storage_access_required),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.storage_access_desc, ConfigPath.VAR_PROJECTS_PATH),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            context.startActivity(intent)
                        }
                    } else {
                        legacyPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = stringResource(id = R.string.grant_permission))
            }

            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// Composable baru untuk item menu di dalam BottomSheet
@Composable
fun ProjectMenuItem(
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
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ProjectItemRow(
    project: ProjectItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true)
                ) { onClick() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                if (project.iconBitmap != null) {
                    Image(
                        bitmap = project.iconBitmap.asImageBitmap(),
                        contentDescription = project.name,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_android),
                        contentDescription = project.name,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                if (project.isPinned) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (project.isExternal) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "SAF",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = project.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Icon Titik Tiga (More Options)
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opsi",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun getPinnedPaths(context: Context): List<String> {
    val prefs = context.getSharedPreferences(PIN_PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_PINNED_PATHS, "") ?: ""
    return raw.split("\n").filter { it.isNotBlank() }
}

fun togglePinProject(context: Context, projectPath: String) {
    val currentList = getPinnedPaths(context).toMutableList()
    if (currentList.contains(projectPath)) {
        currentList.remove(projectPath)
    } else {
        currentList.add(0, projectPath)
    }
    context.getSharedPreferences(PIN_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_PINNED_PATHS, currentList.joinToString("\n"))
        .apply()
}

fun removePinnedPath(context: Context, projectPath: String) {
    val currentList = getPinnedPaths(context).toMutableList()
    if (currentList.remove(projectPath)) {
        context.getSharedPreferences(PIN_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PINNED_PATHS, currentList.joinToString("\n"))
            .apply()
    }
}

fun zipDirectory(sourceDir: File, outputZipFile: File) {
    ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZipFile))).use { zos ->
        val baseDir = sourceDir.parentFile ?: sourceDir
        sourceDir.walkTopDown().forEach { file ->
            val relativePath = file.relativeTo(baseDir).invariantSeparatorsPath
            if (file.isDirectory) {
                val entryName = if (relativePath.endsWith("/")) relativePath else "$relativePath/"
                zos.putNextEntry(ZipEntry(entryName))
                zos.closeEntry()
            } else {
                zos.putNextEntry(ZipEntry(relativePath))
                file.inputStream().use { input ->
                    input.copyTo(zos)
                }
                zos.closeEntry()
            }
        }
    }
}

fun checkStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        val readGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val writeGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        readGranted && writeGranted
    }
}

fun loadAllProjects(context: Context): List<ProjectItem> {
    val pinnedPaths = getPinnedPaths(context)
    val localProjects = scanProjects(ConfigPath.VAR_PROJECTS_PATH, pinnedPaths)
    val knownPaths = localProjects.map { 
        try { File(it.path).canonicalPath } catch (_: Exception) { File(it.path).absolutePath } 
    }.toMutableSet()

    val safRecords = SafProjectsManager.getAllSafProjects(context)
    val safProjects = mutableListOf<ProjectItem>()

    for (record in safRecords) {
        val folder = File(record.path)
        val normalized = try { folder.canonicalPath } catch (_: Exception) { folder.absolutePath }
        if (knownPaths.add(normalized) && folder.exists() && folder.isDirectory) {
            val iconBitmap = findProjectIcon(folder)
            val absPath = folder.absolutePath
            safProjects.add(
                ProjectItem(
                    name = folder.name.ifBlank { record.name },
                    path = absPath,
                    iconBitmap = iconBitmap,
                    isExternal = true,
                    isPinned = pinnedPaths.contains(absPath)
                )
            )
        }
    }

    return (localProjects + safProjects).sortedWith(
        compareBy<ProjectItem> { item ->
            val pinIndex = pinnedPaths.indexOf(item.path)
            if (pinIndex != -1) pinIndex else Int.MAX_VALUE
        }.thenBy { it.name.lowercase() }
    )
}

fun scanProjects(basePath: String, pinnedPaths: List<String> = emptyList()): List<ProjectItem> {
    val directory = File(basePath)
    if (!directory.exists() || !directory.isDirectory) {
        return emptyList()
    }

    val projectFolders = directory.listFiles { file ->
        file.isDirectory && !file.name.startsWith(".")
    } ?: return emptyList()

    return projectFolders.map { folder ->
        val iconBitmap = findProjectIcon(folder)
        val absPath = folder.absolutePath
        ProjectItem(
            name = folder.name,
            path = absPath,
            iconBitmap = iconBitmap,
            isPinned = pinnedPaths.contains(absPath)
        )
    }.sortedBy { it.name.lowercase() }
}

private val projectIconCache = mutableMapOf<String, Bitmap>()

fun findProjectIcon(projectDir: File): Bitmap? {
    val absPath = projectDir.absolutePath
    projectIconCache[absPath]?.let { return it }

    val candidateManifests = listOf(
        File(projectDir, "app/src/main/AndroidManifest.xml"),
        File(projectDir, "src/main/AndroidManifest.xml"),
        File(projectDir, "AndroidManifest.xml")
    )
    val manifestFile = candidateManifests.firstOrNull { it.exists() && it.isFile }

    var iconNameFromManifest: String? = null
    var roundIconNameFromManifest: String? = null

    if (manifestFile != null) {
        try {
            val manifestText = manifestFile.readText()
            val iconMatch = Regex("""android:icon\s*=\s*"@[^/]+/([a-zA-Z0-9_]+)"""").find(manifestText)
            iconNameFromManifest = iconMatch?.groupValues?.get(1)

            val roundIconMatch = Regex("""android:roundIcon\s*=\s*"@[^/]+/([a-zA-Z0-9_]+)"""").find(manifestText)
            roundIconNameFromManifest = roundIconMatch?.groupValues?.get(1)
        } catch (_: Exception) {}
    }

    val targetNames = listOfNotNull(
        iconNameFromManifest,
        roundIconNameFromManifest,
        "ic_launcher",
        "ic_launcher_round",
        "ic_app",
        "app_icon"
    ).distinct()

    val candidateResDirs = listOf(
        File(projectDir, "app/src/main/res"),
        File(projectDir, "src/main/res"),
        File(projectDir, "res")
    )
    val resDir = candidateResDirs.firstOrNull { it.exists() && it.isDirectory } ?: return null

    for (targetName in targetNames) {
        val bitmap = loadIconBitmapForName(resDir, targetName)
        if (bitmap != null) {
            projectIconCache[absPath] = bitmap
            return bitmap
        }
    }

    return null
}

private fun loadIconBitmapForName(resDir: File, targetName: String): Bitmap? {
    val densityFolderOrder = listOf(
        "mipmap-xxxhdpi", "mipmap-xxhdpi", "mipmap-xhdpi", "mipmap-hdpi", "mipmap-mdpi", "mipmap",
        "drawable-xxxhdpi", "drawable-xxhdpi", "drawable-xhdpi", "drawable-hdpi", "drawable-mdpi", "drawable"
    )
    val bitmapExtensions = listOf("png", "webp", "jpg", "jpeg")
    val backgroundKeywords = listOf("back", "background", "adaptive_back", "adaptive_background")

    fun findExactRasterImage(name: String): File? {
        val allSubDirs = resDir.listFiles { f -> f.isDirectory } ?: return null
        for (subDirName in densityFolderOrder) {
            val dir = allSubDirs.firstOrNull { it.name.equals(subDirName, ignoreCase = true) } ?: continue
            for (ext in bitmapExtensions) {
                val candidate = File(dir, "$name.$ext")
                if (candidate.exists() && candidate.isFile) {
                    return candidate
                }
            }
        }
        for (dir in allSubDirs) {
            if (dir.name.contains("mipmap", ignoreCase = true) || dir.name.contains("drawable", ignoreCase = true)) {
                for (ext in bitmapExtensions) {
                    val candidate = File(dir, "$name.$ext")
                    if (candidate.exists() && candidate.isFile) {
                        return candidate
                    }
                }
            }
        }
        return null
    }

    // Step A: Check for exact raster image match for targetName (e.g. ic_launcher.png)
    val exactImageFile = findExactRasterImage(targetName)
    if (exactImageFile != null && backgroundKeywords.none { exactImageFile.name.contains(it, ignoreCase = true) }) {
        val bitmap = BitmapFactory.decodeFile(exactImageFile.absolutePath)
        if (bitmap != null) return bitmap
    }

    // Step B: Check for XML adaptive icon (e.g. ic_launcher.xml)
    val allSubDirs = resDir.listFiles { f -> f.isDirectory } ?: emptyArray()
    var adaptiveXmlFile: File? = null
    for (dir in allSubDirs) {
        val xmlCandidate = File(dir, "$targetName.xml")
        if (xmlCandidate.exists() && xmlCandidate.isFile) {
            adaptiveXmlFile = xmlCandidate
            break
        }
    }

    if (adaptiveXmlFile != null) {
        try {
            val xmlText = adaptiveXmlFile.readText()
            if (xmlText.contains("adaptive-icon")) {
                val foreMatch = Regex("""<foreground[^>]+android:drawable\s*=\s*"@[^/]+/([a-zA-Z0-9_]+)"""").find(xmlText)
                val backMatch = Regex("""<background[^>]+android:drawable\s*=\s*"@[^/]+/([a-zA-Z0-9_]+)"""").find(xmlText)

                val foreName = foreMatch?.groupValues?.get(1)
                val backName = backMatch?.groupValues?.get(1)

                val foreFile = foreName?.let { findExactRasterImage(it) }
                val backFile = backName?.let { findExactRasterImage(it) }

                val foreBitmap = foreFile?.let { BitmapFactory.decodeFile(it.absolutePath) }
                val backBitmap = backFile?.let { BitmapFactory.decodeFile(it.absolutePath) }

                if (foreBitmap != null && backBitmap != null) {
                    val width = backBitmap.width
                    val height = backBitmap.height
                    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(result)
                    canvas.drawBitmap(backBitmap, 0f, 0f, null)
                    val srcRect = Rect(0, 0, foreBitmap.width, foreBitmap.height)
                    val dstRect = Rect(0, 0, width, height)
                    canvas.drawBitmap(foreBitmap, srcRect, dstRect, null)
                    return result
                } else if (foreBitmap != null) {
                    return foreBitmap
                } else if (backBitmap != null) {
                    return backBitmap
                }
            }
        } catch (_: Exception) {}
    }

    // Step C: Fallback search for any image containing targetName excluding background-only files
    for (dir in allSubDirs) {
        if (dir.name.contains("mipmap", ignoreCase = true) || dir.name.contains("drawable", ignoreCase = true)) {
            val files = dir.listFiles { f ->
                f.isFile &&
                f.name.contains(targetName, ignoreCase = true) &&
                bitmapExtensions.contains(f.extension.lowercase()) &&
                backgroundKeywords.none { f.name.contains(it, ignoreCase = true) }
            }
            val bestFile = files?.firstOrNull()
            if (bestFile != null) {
                val bitmap = BitmapFactory.decodeFile(bestFile.absolutePath)
                if (bitmap != null) return bitmap
            }
        }
    }

    return null
}