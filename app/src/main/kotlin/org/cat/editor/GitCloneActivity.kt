package org.cat.editor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
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
    var patText by remember { mutableStateOf("") }
    var patVisible by remember { mutableStateOf(false) }
    var isFastClone by remember { mutableStateOf(true) }
    
    var isCloning by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var cloneProgressText by remember { mutableStateOf("") }
    var cloneJob by remember { mutableStateOf<Job?>(null) }

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
                        cloneJob?.cancel()
                        onNavigateBack()
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

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = branchText,
                onValueChange = { branchText = it },
                label = { Text(stringResource(id = R.string.git_clone_branch_label)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCloning,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = patText,
                onValueChange = { patText = it },
                label = { Text(stringResource(id = R.string.git_clone_pat_label)) },
                supportingText = { Text(stringResource(id = R.string.git_clone_pat_hint)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCloning,
                singleLine = true,
                visualTransformation = if (patVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { patVisible = !patVisible }) {
                        Icon(
                            imageVector = if (patVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isCloning) { isFastClone = !isFastClone }
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(id = R.string.git_clone_fast_label),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(id = R.string.git_clone_fast_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = isFastClone,
                    onCheckedChange = { isFastClone = it },
                    enabled = !isCloning
                )
            }

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
                        if (linkText.isNotBlank()) {
                            isCloning = true
                            cloneProgressText = linkText.trim()
                            
                            cloneJob = scope.launch(Dispatchers.IO) {
                                val cleanLink = linkText.trim()
                                val repoName = cleanLink.substringAfterLast("/").removeSuffix(".git")
                                val destFolder = File(ConfigPath.VAR_PROJECTS_PATH, repoName)
                                
                                try {
                                    if (!destFolder.exists()) {
                                        destFolder.mkdirs()
                                    }

                                    val monitor = object : ProgressMonitor {
                                        override fun start(totalTasks: Int) {}
                                        override fun beginTask(title: String?, totalWork: Int) {
                                            if (title != null) cloneProgressText = title
                                        }
                                        override fun update(completed: Int) {}
                                        override fun endTask() {}
                                        override fun isCancelled(): Boolean = !isCloning
                                        override fun showDuration(enabled: Boolean) {}
                                    }

                                    val cloneCommand = Git.cloneRepository()
                                        .setURI(cleanLink)
                                        .setDirectory(destFolder)
                                        .setProgressMonitor(monitor)
                                    
                                    if (branchText.isNotBlank()) {
                                        cloneCommand.setBranch(branchText.trim())
                                    }

                                    if (patText.isNotBlank()) {
                                        val token = patText.trim()
                                        val credentialsProvider = if (token.contains(":")) {
                                            val parts = token.split(":", limit = 2)
                                            UsernamePasswordCredentialsProvider(parts[0], parts[1])
                                        } else {
                                            UsernamePasswordCredentialsProvider(token, "")
                                        }
                                        cloneCommand.setCredentialsProvider(credentialsProvider)
                                    }

                                    if (isFastClone) {
                                        cloneCommand.setDepth(1)
                                        cloneCommand.setCloneAllBranches(false)
                                    }

                                    cloneCommand.call().close()

                                    withContext(Dispatchers.Main) {
                                        if (isCloning) {
                                            isCloning = false
                                            val intent = Intent(context, MainActivity::class.java).apply {
                                                putExtra("project_path", destFolder.absolutePath)
                                            }
                                            context.startActivity(intent)
                                            (context as ComponentActivity).finish()
                                        }
                                    }

                                } catch (e: Exception) {
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
