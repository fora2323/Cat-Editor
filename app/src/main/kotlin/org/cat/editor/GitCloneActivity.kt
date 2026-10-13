package org.cat.editor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cat.editor.ui.theme.AppTheme
import org.cat.editor.utils.ConfigPath
import org.cat.editor.utils.LocaleHelper
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ProgressMonitor
import java.io.File
import java.util.concurrent.CancellationException

class GitCloneActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        setContent {
            AppTheme {
                GitCloneScreen(
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitCloneScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var linkText by remember { mutableStateOf("") }
    var branchText by remember { mutableStateOf("") }
    
    var isCloning by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var cloneProgressText by remember { mutableStateOf("") }
    var cloneJob by remember { mutableStateOf<Job?>(null) }

    // Dialog konfirmasi jika user klik back saat proses clone berjalan
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text(stringResource(id = R.string.cancel_clone_title)) },
            text = { Text(stringResource(id = R.string.cancel_clone_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        isCloning = false
                        cloneJob?.cancel() // Batalkan coroutine
                        onNavigateBack() // Tutup activity
                    }
                ) {
                    Text(stringResource(id = R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text(stringResource(id = R.string.no))
                }
            }
        )
    }

    // Tangani aksi back dari sistem (Bottom Navigation Bar)
    BackHandler(enabled = true) {
        if (isCloning) {
            showCancelDialog = true
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(id = R.string.git_clone_title))
                        Text(
                            text = stringResource(id = R.string.git_clone_subtitle),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isCloning) {
                                showCancelDialog = true
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = linkText,
                onValueChange = { linkText = it },
                label = { Text(stringResource(id = R.string.git_clone_link_label)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCloning,
                singleLine = true
            )

            // Jarak input didekatkan
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = branchText,
                onValueChange = { branchText = it },
                label = { Text(stringResource(id = R.string.git_clone_branch_label)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCloning,
                singleLine = true
            )

            // Jarak button/progress bar didekatkan
            Spacer(modifier = Modifier.height(16.dp))

            if (isCloning) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Download: $cloneProgressText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Button(
                    onClick = {
                        if (linkText.isNotEmpty()) {
                            isCloning = true
                            cloneProgressText = linkText
                            
                            cloneJob = scope.launch(Dispatchers.IO) {
                                // Ekstrak nama repo dari link, contoh: "https://github.com/user/repo.git" -> "repo"
                                val repoName = linkText.substringAfterLast("/").removeSuffix(".git")
                                val destFolder = File(ConfigPath.VAR_PROJECTS_PATH, repoName)
                                
                                try {
                                    if (!destFolder.exists()) {
                                        destFolder.mkdirs()
                                    }

                                    // Monitor progress download dari JGit
                                    val monitor = object : ProgressMonitor {
                                        override fun start(totalTasks: Int) {}
                                        override fun beginTask(title: String?, totalWork: Int) {
                                            if (title != null) cloneProgressText = title
                                        }
                                        override fun update(completed: Int) {}
                                        override fun endTask() {}
                                        override fun isCancelled(): Boolean = !isCloning
                                        
                                        // Fix tambahan: Override showDuration wajib pada JGit versi 6.8.0
                                        override fun showDuration(enabled: Boolean) {}
                                    }

                                    val cloneCommand = Git.cloneRepository()
                                        .setURI(linkText)
                                        .setDirectory(destFolder)
                                        .setProgressMonitor(monitor)
                                    
                                    if (branchText.isNotEmpty()) {
                                        cloneCommand.setBranch(branchText)
                                    }

                                    // Eksekusi clone
                                    cloneCommand.call().close()

                                    // Berhasil, pindah ke MainActivity
                                    withContext(Dispatchers.Main) {
                                        if (isCloning) { // Pastikan tidak dibatalkan
                                            isCloning = false
                                            val intent = Intent(context, MainActivity::class.java).apply {
                                                putExtra("project_path", destFolder.absolutePath)
                                            }
                                            context.startActivity(intent)
                                            (context as ComponentActivity).finish()
                                        }
                                    }

                                } catch (e: Exception) {
                                    // Bersihkan folder jika gagal atau batal
                                    destFolder.deleteRecursively()
                                    
                                    withContext(Dispatchers.Main) {
                                        isCloning = false
                                        if (e !is CancellationException && e !is org.eclipse.jgit.api.errors.JGitInternalException) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.git_clone_button))
                }
            }
        }
    }
}