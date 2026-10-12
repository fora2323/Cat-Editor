package org.cat.editor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import org.cat.editor.ui.ExpressivePullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import org.cat.editor.ui.theme.AppTheme
import org.cat.editor.utils.ConfigPath
import org.cat.editor.utils.LocaleHelper
import org.cat.editor.utils.StorageUtils
import java.io.File

data class ProjectItem(
    val name: String,
    val path: String,
    val iconBitmap: Bitmap?
)

class ProjectMenuActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        setContent {
            AppTheme {
                ProjectMenuScreen(
                    onProjectSelected = { project ->
                        val intent = Intent(this, MainActivity::class.java).apply {
                            putExtra("project_path", project.path)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectMenuScreen(
    onProjectSelected: (ProjectItem) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember { mutableStateOf(checkStoragePermission(context)) }
    var showPermissionDialog by remember { mutableStateOf(!hasPermission) }
    var projects by remember { mutableStateOf<List<ProjectItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(hasPermission) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }

    suspend fun refresh() {
        isRefreshing = true
        try {
            delay(600)
            projects = withContext(Dispatchers.IO) {
                scanProjects(ConfigPath.VAR_PROJECTS_PATH)
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
                    scanProjects(ConfigPath.VAR_PROJECTS_PATH)
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
            if (path != null && File(path).exists()) {
                val intent = Intent(context, MainActivity::class.java).apply {
                    putExtra("project_path", path)
                }
                context.startActivity(intent)
            } else {
                Toast.makeText(context, context.getString(R.string.cannot_access_folder), Toast.LENGTH_SHORT).show()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val currentPermission = checkStoragePermission(context)
                hasPermission = currentPermission
                if (currentPermission) {
                    showPermissionDialog = false
                    projects = scanProjects(ConfigPath.VAR_PROJECTS_PATH)
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
                scanProjects(ConfigPath.VAR_PROJECTS_PATH)
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
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = stringResource(id = R.string.git_clone),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            SmallFloatingActionButton(
                                onClick = {
                                    isFabExpanded = false
                                    Toast.makeText(context, context.getString(R.string.feature_git_clone), Toast.LENGTH_SHORT).show()
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = stringResource(id = R.string.git_clone)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = stringResource(id = R.string.open_folder_saf),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            SmallFloatingActionButton(
                                onClick = {
                                    isFabExpanded = false
                                    openDocumentTreeLauncher.launch(null)
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = stringResource(id = R.string.select_folder)
                                )
                            }
                        }
                    }
                }

                ExtendedFloatingActionButton(
                    text = { Text(text = if (isFabExpanded) stringResource(id = R.string.close) else stringResource(id = R.string.new_project)) },
                    icon = {
                        Icon(
                            imageVector = if (isFabExpanded) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = null
                        )
                    },
                    onClick = { isFabExpanded = !isFabExpanded },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
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
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
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
                                onClick = { onProjectSelected(item) }
                            )
                        }
                    }
                }
            }
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

@Composable
fun ProjectItemRow(
    project: ProjectItem,
    onClick: () -> Unit
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

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = project.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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

fun scanProjects(basePath: String): List<ProjectItem> {
    val directory = File(basePath)
    if (!directory.exists() || !directory.isDirectory) {
        return emptyList()
    }

    val projectFolders = directory.listFiles { file ->
        file.isDirectory && !file.name.startsWith(".")
    } ?: return emptyList()

    return projectFolders.map { folder ->
        val iconBitmap = findProjectIcon(folder)
        ProjectItem(
            name = folder.name,
            path = folder.absolutePath,
            iconBitmap = iconBitmap
        )
    }.sortedBy { it.name.lowercase() }
}

fun findProjectIcon(projectDir: File): Bitmap? {
    val candidateResDirs = listOf(
        File(projectDir, "app/src/main/res"),
        File(projectDir, "src/main/res"),
        File(projectDir, "res")
    )
    val resDir = candidateResDirs.firstOrNull { it.exists() && it.isDirectory } ?: return null
    val bitmapExtensions = listOf("png", "webp", "jpg", "jpeg")

    val mipmapDirs = resDir.listFiles { file -> file.isDirectory && file.name.startsWith("mipmap") }
    if (mipmapDirs != null) {
        val sortedMipmaps = mipmapDirs.sortedByDescending { it.name }
        for (dir in sortedMipmaps) {
            val launcherFiles = dir.listFiles { file ->
                file.isFile && file.name.contains("launcher", ignoreCase = true) && bitmapExtensions.contains(file.extension.lowercase())
            }
            val bestFile = launcherFiles?.firstOrNull()
            if (bestFile != null) {
                val bitmap = BitmapFactory.decodeFile(bestFile.absolutePath)
                if (bitmap != null) return bitmap
            }
        }
    }

    val drawableDirs = resDir.listFiles { file -> file.isDirectory && file.name.startsWith("drawable") }
    if (drawableDirs != null) {
        val sortedDrawables = drawableDirs.sortedByDescending { it.name }
        for (dir in sortedDrawables) {
            val launcherFiles = dir.listFiles { file ->
                file.isFile && file.name.contains("launcher", ignoreCase = true) && bitmapExtensions.contains(file.extension.lowercase())
            }
            val bestFile = launcherFiles?.firstOrNull()
            if (bestFile != null) {
                val bitmap = BitmapFactory.decodeFile(bestFile.absolutePath)
                if (bitmap != null) return bitmap
            }
        }
    }
    return null
}