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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.cat.editor.ui.BottomSheetDialog
import org.cat.editor.ui.theme.AppTheme
import org.cat.editor.utils.ConfigPath
import java.io.File

data class ProjectItem(
    val name: String,
    val path: String,
    val iconBitmap: Bitmap?
)

class ProjectMenuActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                ProjectMenuScreen(
                    onProjectSelected = { project ->
                        Toast.makeText(this, "Buka proyek: ${project.name}", Toast.LENGTH_SHORT).show()
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

    var hasPermission by remember { mutableStateOf(checkStoragePermission(context)) }
    var showPermissionDialog by remember { mutableStateOf(!hasPermission) }
    var projects by remember { mutableStateOf<List<ProjectItem>>(emptyList()) }
    var isFabExpanded by remember { mutableStateOf(false) }

    // Launcher untuk Android 10 kebawah (runtime permissions)
    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        if (granted) {
            hasPermission = true
            showPermissionDialog = false
            projects = scanProjects(ConfigPath.VAR_PROJECTS_PATH)
        } else {
            Toast.makeText(context, "Izin penyimpanan ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher SAF (Buka folder direktori)
    val openDocumentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            Toast.makeText(context, "Folder terpilih: ${it.path}", Toast.LENGTH_SHORT).show()
        }
    }

    // Cek ulang izin ketika aplikasi kembali dari Settings (onResume)
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

    // Muat data jika izin sudah ada sejak awal
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            projects = scanProjects(ConfigPath.VAR_PROJECTS_PATH)
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
                    IconButton(onClick = { /* Belum diberi fungsi sesuai permintaan */ }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Pengaturan",
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
                // Sub-FABs saat expanded
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Opsi 1: Git Clone
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Git Clone",
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
                                    Toast.makeText(context, "Fitur Git Clone", Toast.LENGTH_SHORT).show()
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "Git Clone"
                                )
                            }
                        }

                        // Opsi 2: Buka Folder via SAF
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Buka Folder (SAF)",
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
                                    contentDescription = "Pilih Folder"
                                )
                            }
                        }
                    }
                }

                // Main FAB: New Project
                ExtendedFloatingActionButton(
                    text = { Text(text = if (isFabExpanded) "Tutup" else "New Project") },
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
            if (projects.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (!hasPermission) "Menunggu izin penyimpanan..." else "Tidak ada proyek ditemukan",
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

    // BottomSheet Dialog Permintaan Izin Penyimpanan
    if (showPermissionDialog) {
        BottomSheetDialog(
            onDismissRequest = { /* Tidak diizinkan tutup klik luar sebelum aksi */ }
        ) {
            Text(
                text = "Akses Penyimpanan Diperlukan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Aplikasi membutuhkan izin membaca file eksternal untuk memindai proyek dan ikon launcher pada folder ${ConfigPath.VAR_PROJECTS_PATH}.",
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
                Text(text = "Berikan Izin")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ProjectItemRow(
    project: ProjectItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Render icon dari bitmap jika ditemukan, jika tidak fallback ke R.drawable.ic_vanilla_android
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
                    painter = painterResource(id = R.drawable.ic_vanilla_android),
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

// Cek status izin penyimpanan
fun checkStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        val readGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val writeGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        readGranted && writeGranted
    }
}

// Memindai folder proyek
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

// Logika pemindaian icon: cari di mipmap, lalu cari di drawable dengan kata "launcher"
fun findProjectIcon(projectDir: File): Bitmap? {
    val candidateResDirs = listOf(
        File(projectDir, "app/src/main/res"),
        File(projectDir, "src/main/res"),
        File(projectDir, "res")
    )
    val resDir = candidateResDirs.firstOrNull { it.exists() && it.isDirectory } ?: return null
    val bitmapExtensions = listOf("png", "webp", "jpg", "jpeg")

    // 1. Scan folder mipmap
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

    // 2. Scan folder drawable jika di mipmap tidak ditemukan
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