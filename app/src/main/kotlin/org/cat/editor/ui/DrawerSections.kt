package org.cat.editor.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.ripple
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.cat.editor.R
import org.cat.editor.drawer.FileIcon
import org.cat.editor.utils.GitManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DrawerSection {
    PROJECT, COMMIT
}

enum class CommitPanelTab {
    CHANGES, HISTORY
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DrawerWithSections(
    projectContent: @Composable () -> Unit,
    commitContent: @Composable () -> Unit,
    currentSection: DrawerSection = DrawerSection.PROJECT,
    onSectionChange: (DrawerSection) -> Unit = {}
) {
    var section by remember { mutableStateOf(currentSection) }
    val isImeVisible = WindowInsets.isImeVisible

    LaunchedEffect(currentSection) {
        section = currentSection
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Box(modifier = Modifier.weight(1f)) {
            when (section) {
                DrawerSection.PROJECT -> projectContent()
                DrawerSection.COMMIT -> commitContent()
            }
        }

        // Hide bottom tab bar when keyboard is open so the commit message box has full room above the keyboard
        if (!isImeVisible) {
            HorizontalDivider()

            // Bottom Tab Navigation (Project / Commit)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                TextButton(
                    onClick = {
                        section = DrawerSection.PROJECT
                        onSectionChange(DrawerSection.PROJECT)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RectangleShape,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (section == DrawerSection.PROJECT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(stringResource(id = R.string.tab_project), fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = {
                        section = DrawerSection.COMMIT
                        onSectionChange(DrawerSection.COMMIT)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RectangleShape,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (section == DrawerSection.COMMIT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CallSplit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(id = R.string.tab_commit), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CommitPanel(
    projectPath: String,
    refreshTrigger: Int = 0,
    onOpenFile: ((File) -> Unit)? = null,
    onNavigateSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(CommitPanelTab.CHANGES) }
    var statusData by remember { mutableStateOf<GitManager.GitStatusData?>(null) }
    var branchInfo by remember { mutableStateOf<GitManager.BranchInfo?>(null) }
    var historyList by remember { mutableStateOf<List<GitManager.GitCommitItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isActionInProgress by remember { mutableStateOf(false) }

    // Commit state
    var commitMessage by remember { mutableStateOf("") }
    var isAmend by remember { mutableStateOf(false) }
    val selectedFiles = remember { mutableStateMapOf<String, Boolean>() }

    // UI Dialog & Menu states
    var showBranchDropdown by remember { mutableStateOf(false) }
    var selectedBranchForAction by remember { mutableStateOf<String?>(null) }
    var showNewBranchDialog by remember { mutableStateOf(false) }
    var showRenameBranchDialog by remember { mutableStateOf<String?>(null) }
    var showDeleteBranchDialog by remember { mutableStateOf<String?>(null) }
    var selectedCommitForDetails by remember { mutableStateOf<GitManager.GitCommitItem?>(null) }
    var commitChangedFiles by remember { mutableStateOf<List<String>>(emptyList()) }

    var isChangesExpanded by remember { mutableStateOf(true) }
    var isUnversionedExpanded by remember { mutableStateOf(true) }

    val projectDir = remember(projectPath) { File(projectPath) }
    val isRepo = remember(projectPath, refreshTrigger) { GitManager.isGitRepo(projectDir) }

    fun refreshAll(showToast: Boolean = false) {
        if (!isRepo) return
        scope.launch {
            isLoading = true
            // Load branches
            GitManager.getBranchInfo(projectDir).onSuccess {
                branchInfo = it
            }
            // Load status
            GitManager.getStatus(projectDir).onSuccess { data ->
                statusData = data
                val prevSelections = selectedFiles.toMap()
                selectedFiles.clear()
                // Tracked changes: checked by default
                for (item in data.changes) {
                    selectedFiles[item.path] = prevSelections[item.path] ?: true
                }
                // Unversioned: unchecked by default (like Android Studio!)
                for (item in data.unversioned) {
                    selectedFiles[item.path] = prevSelections[item.path] ?: false
                }
            }
            // Load history
            GitManager.getCommitHistory(projectDir).onSuccess {
                historyList = it
            }
            isLoading = false
            if (showToast) {
                Toast.makeText(context, context.getString(R.string.git_action_sync), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Auto-detect changes on mount, path change, or refresh trigger
    LaunchedEffect(projectPath, refreshTrigger) {
        refreshAll()
    }

    // When Amend is checked, auto-populate with last commit message
    LaunchedEffect(isAmend) {
        if (isAmend && isRepo) {
            val lastMsg = GitManager.getLastCommitMessage(projectDir)
            if (!lastMsg.isNullOrBlank()) {
                commitMessage = lastMsg
            }
        }
    }

    fun handlePull() {
        if (!isRepo || isActionInProgress) return
        scope.launch {
            isActionInProgress = true
            Toast.makeText(context, "Pulling updates...", Toast.LENGTH_SHORT).show()
            GitManager.pull(projectDir, context)
                .onSuccess { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    refreshAll()
                }
                .onFailure { err ->
                    Toast.makeText(context, "Pull failed: ${err.message}", Toast.LENGTH_LONG).show()
                }
            isActionInProgress = false
        }
    }

    fun handlePush() {
        if (!isRepo || isActionInProgress) return
        scope.launch {
            isActionInProgress = true
            Toast.makeText(context, "Pushing to remote...", Toast.LENGTH_SHORT).show()
            GitManager.push(projectDir, context)
                .onSuccess { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    refreshAll()
                }
                .onFailure { err ->
                    Toast.makeText(context, "Push failed: ${err.message}", Toast.LENGTH_LONG).show()
                }
            isActionInProgress = false
        }
    }

    fun handleFetch() {
        if (!isRepo || isActionInProgress) return
        scope.launch {
            isActionInProgress = true
            Toast.makeText(context, "Fetching...", Toast.LENGTH_SHORT).show()
            GitManager.fetch(projectDir, context)
                .onSuccess { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    refreshAll()
                }
                .onFailure { err ->
                    Toast.makeText(context, "Fetch failed: ${err.message}", Toast.LENGTH_LONG).show()
                    refreshAll()
                }
            isActionInProgress = false
        }
    }

    fun handleCommit(pushAfter: Boolean = false) {
        if (!isRepo || isActionInProgress) return
        val filesToCommit = selectedFiles.filter { it.value }.keys.toList()
        if (filesToCommit.isEmpty() && !isAmend) {
            Toast.makeText(context, "Please select files to commit", Toast.LENGTH_SHORT).show()
            return
        }
        if (commitMessage.isBlank()) {
            Toast.makeText(context, "Please enter a commit message", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            isActionInProgress = true
            GitManager.commit(projectDir, commitMessage.trim(), filesToCommit, isAmend, context)
                .onSuccess { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    commitMessage = ""
                    isAmend = false
                    refreshAll()
                    if (pushAfter) {
                        handlePush()
                    }
                }
                .onFailure { err ->
                    Toast.makeText(context, "Commit failed: ${err.message}", Toast.LENGTH_LONG).show()
                }
            isActionInProgress = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // TOP TOOLBAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Branch Selector Pill
            Box {
                Surface(
                    onClick = { showBranchDropdown = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CallSplit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = branchInfo?.current ?: "main",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Branches Dropdown Menu
                DropdownMenu(
                    expanded = showBranchDropdown,
                    onDismissRequest = { showBranchDropdown = false },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    val currentBranch = branchInfo?.current ?: "main"
                    val localBranches = branchInfo?.localBranches ?: listOf(currentBranch)
                    val remoteBranches = branchInfo?.remoteBranches ?: emptyList()

                    Text(
                        text = stringResource(id = R.string.git_local_branches),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    localBranches.forEach { branch ->
                        val isCurrent = branch == currentBranch
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isCurrent) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = branch,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            onClick = {
                                showBranchDropdown = false
                                selectedBranchForAction = branch
                            }
                        )
                    }

                    if (remoteBranches.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = stringResource(id = R.string.git_remote_branches),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                        remoteBranches.forEach { branch ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = branch, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outlineVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    showBranchDropdown = false
                                    selectedBranchForAction = branch
                                }
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(id = R.string.git_new_branch),
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        onClick = {
                            showBranchDropdown = false
                            showNewBranchDialog = true
                        }
                    )
                }
            }

            // Right: Toolbar Action Icons (Update/Pull, Sync/Fetch, Push, Branches replacing settings icon)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Update / Pull
                IconButton(
                    onClick = { handlePull() },
                    enabled = !isActionInProgress
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Update / Pull",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Sync / Fetch / Refresh
                IconButton(
                    onClick = { handleFetch() },
                    enabled = !isActionInProgress
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(id = R.string.git_action_sync),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Push
                IconButton(
                    onClick = { handlePush() },
                    enabled = !isActionInProgress
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = stringResource(id = R.string.git_action_push),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Branches button (Replaced settings gear icon as requested!)
                IconButton(
                    onClick = { showBranchDropdown = true }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CallSplit,
                        contentDescription = stringResource(id = R.string.git_branches),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalDivider()

        if (!isRepo) {
            // Not a Git Repository Placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CallSplit,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(id = R.string.git_not_a_repo),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    org.eclipse.jgit.api.Git.init().setDirectory(projectDir).call().close()
                                    Toast.makeText(context, "Git repository initialized", Toast.LENGTH_SHORT).show()
                                    refreshAll()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(id = R.string.git_init_repo))
                    }
                }
            }
            return
        }

        // TABS: Changes | History
        PrimaryTabRow(
            selectedTabIndex = activeTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(activeTab.ordinal),
                    width = 64.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Tab(
                selected = activeTab == CommitPanelTab.CHANGES,
                onClick = { activeTab = CommitPanelTab.CHANGES },
                text = {
                    val count = statusData?.totalCount ?: 0
                    Text(
                        text = if (count > 0) "${stringResource(id = R.string.git_changes)} ($count)" else stringResource(id = R.string.git_changes),
                        fontWeight = if (activeTab == CommitPanelTab.CHANGES) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
            Tab(
                selected = activeTab == CommitPanelTab.HISTORY,
                onClick = { activeTab = CommitPanelTab.HISTORY },
                text = {
                    Text(
                        text = stringResource(id = R.string.git_history),
                        fontWeight = if (activeTab == CommitPanelTab.HISTORY) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        when (activeTab) {
            CommitPanelTab.CHANGES -> {
                ChangesTabContent(
                    projectDir = projectDir,
                    statusData = statusData,
                    isLoading = isLoading,
                    selectedFiles = selectedFiles,
                    commitMessage = commitMessage,
                    onCommitMessageChange = { commitMessage = it },
                    isAmend = isAmend,
                    onAmendChange = { isAmend = it },
                    isChangesExpanded = isChangesExpanded,
                    onToggleChangesExpanded = { isChangesExpanded = !isChangesExpanded },
                    isUnversionedExpanded = isUnversionedExpanded,
                    onToggleUnversionedExpanded = { isUnversionedExpanded = !isUnversionedExpanded },
                    isActionInProgress = isActionInProgress,
                    onCommit = { handleCommit(pushAfter = false) },
                    onCommitAndPush = { handleCommit(pushAfter = true) },
                    onOpenFile = onOpenFile
                )
            }
            CommitPanelTab.HISTORY -> {
                HistoryTabContent(
                    history = historyList,
                    isLoading = isLoading,
                    onCommitClick = { commit ->
                        selectedCommitForDetails = commit
                        scope.launch {
                            GitManager.getCommitChangedFiles(projectDir, commit.hash).onSuccess {
                                commitChangedFiles = it
                            }
                        }
                    }
                )
            }
        }
    }

    // Branch Actions Dialog (Checkout, Merge, Rebase, Rename, Delete)
    selectedBranchForAction?.let { branch ->
        val currentBranch = branchInfo?.current ?: "main"
        val isCurrent = branch == currentBranch

        AlertDialog(
            onDismissRequest = { selectedBranchForAction = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CallSplit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = branch,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (!isCurrent) {
                        BranchActionRow(
                            icon = Icons.Default.Check,
                            title = stringResource(id = R.string.git_checkout),
                            onClick = {
                                selectedBranchForAction = null
                                scope.launch {
                                    GitManager.checkoutBranch(projectDir, branch)
                                        .onSuccess { msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            refreshAll()
                                        }
                                        .onFailure { err ->
                                            Toast.makeText(context, "Checkout failed: ${err.message}", Toast.LENGTH_LONG).show()
                                        }
                                }
                            }
                        )
                    }

                    BranchActionRow(
                        icon = Icons.Default.ArrowUpward,
                        title = "${stringResource(id = R.string.git_merge)} into '$currentBranch'",
                        onClick = {
                            selectedBranchForAction = null
                            scope.launch {
                                GitManager.mergeBranch(projectDir, branch)
                                    .onSuccess { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        refreshAll()
                                    }
                                    .onFailure { err ->
                                        Toast.makeText(context, "Merge failed: ${err.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                        }
                    )

                    BranchActionRow(
                        icon = Icons.AutoMirrored.Filled.CallSplit,
                        title = "${stringResource(id = R.string.git_rebase)} onto '$branch'",
                        onClick = {
                            selectedBranchForAction = null
                            scope.launch {
                                GitManager.rebaseBranch(projectDir, branch)
                                    .onSuccess { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        refreshAll()
                                    }
                                    .onFailure { err ->
                                        Toast.makeText(context, "Rebase failed: ${err.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                        }
                    )

                    if (!branch.startsWith("origin/")) {
                        BranchActionRow(
                            icon = Icons.Default.Edit,
                            title = stringResource(id = R.string.git_rename),
                            onClick = {
                                selectedBranchForAction = null
                                showRenameBranchDialog = branch
                            }
                        )

                        if (!isCurrent) {
                            BranchActionRow(
                                icon = Icons.Default.Delete,
                                title = stringResource(id = R.string.git_delete),
                                color = MaterialTheme.colorScheme.error,
                                onClick = {
                                    selectedBranchForAction = null
                                    showDeleteBranchDialog = branch
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedBranchForAction = null }) {
                    Text(stringResource(id = R.string.close))
                }
            }
        )
    }

    // New Branch Dialog
    if (showNewBranchDialog) {
        var newBranchName by remember { mutableStateOf("") }
        val currentBranch = branchInfo?.current ?: "main"

        AlertDialog(
            onDismissRequest = { showNewBranchDialog = false },
            title = {
                Text(
                    text = stringResource(id = R.string.git_new_branch),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newBranchName,
                        onValueChange = { newBranchName = it },
                        label = {
                            Text(stringResource(id = R.string.git_new_branch_label, currentBranch))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newBranchName.trim()
                        if (name.isNotEmpty()) {
                            showNewBranchDialog = false
                            scope.launch {
                                GitManager.createAndCheckoutBranch(projectDir, name, currentBranch)
                                    .onSuccess { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        refreshAll()
                                    }
                                    .onFailure { err ->
                                        Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                        }
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewBranchDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    // Rename Branch Dialog
    showRenameBranchDialog?.let { oldName ->
        var renameInput by remember { mutableStateOf(oldName) }

        AlertDialog(
            onDismissRequest = { showRenameBranchDialog = null },
            title = {
                Text(
                    text = stringResource(id = R.string.git_rename_branch),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Branch name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newName = renameInput.trim()
                        if (newName.isNotEmpty() && newName != oldName) {
                            showRenameBranchDialog = null
                            scope.launch {
                                GitManager.renameBranch(projectDir, oldName, newName)
                                    .onSuccess { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        refreshAll()
                                    }
                                    .onFailure { err ->
                                        Toast.makeText(context, "Rename failed: ${err.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                        }
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameBranchDialog = null }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    // Delete Branch Dialog
    showDeleteBranchDialog?.let { branchToDelete ->
        AlertDialog(
            onDismissRequest = { showDeleteBranchDialog = null },
            title = {
                Text(
                    text = stringResource(id = R.string.git_delete_branch),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(id = R.string.git_delete_branch_confirm, branchToDelete))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteBranchDialog = null
                        scope.launch {
                            GitManager.deleteBranch(projectDir, branchToDelete, force = true)
                                .onSuccess { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    refreshAll()
                                }
                                .onFailure { err ->
                                    Toast.makeText(context, "Delete failed: ${err.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(id = R.string.git_delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteBranchDialog = null }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    // Commit Details Dialog
    selectedCommitForDetails?.let { commit ->
        AlertDialog(
            onDismissRequest = {
                selectedCommitForDetails = null
                commitChangedFiles = emptyList()
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = commit.shortHash,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Commit Hash", commit.hash))
                            Toast.makeText(context, "Copied hash", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy hash",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = commit.fullMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Author: ${commit.authorName} <${commit.authorEmail}>",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Date: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(commit.date)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (commitChangedFiles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Changed files (${commitChangedFiles.size}):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyColumn(modifier = Modifier.height(120.dp)) {
                            items(commitChangedFiles) { path ->
                                Text(
                                    text = "• $path",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedCommitForDetails = null
                    commitChangedFiles = emptyList()
                }) {
                    Text(stringResource(id = R.string.close))
                }
            }
        )
    }
}

@Composable
private fun BranchActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = color
        )
    }
}

@Composable
private fun ChangesTabContent(
    projectDir: File,
    statusData: GitManager.GitStatusData?,
    isLoading: Boolean,
    selectedFiles: MutableMap<String, Boolean>,
    commitMessage: String,
    onCommitMessageChange: (String) -> Unit,
    isAmend: Boolean,
    onAmendChange: (Boolean) -> Unit,
    isChangesExpanded: Boolean,
    onToggleChangesExpanded: () -> Unit,
    isUnversionedExpanded: Boolean,
    onToggleUnversionedExpanded: () -> Unit,
    isActionInProgress: Boolean,
    onCommit: () -> Unit,
    onCommitAndPush: () -> Unit,
    onOpenFile: ((File) -> Unit)?
) {
    val changes = statusData?.changes ?: emptyList()
    val unversioned = statusData?.unversioned ?: emptyList()

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading && statusData == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
            return
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Scrollable Changes List Area
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                if (changes.isEmpty() && unversioned.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.git_no_changes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // SECTION 1: Tracked Changes (checked by default)
                    if (changes.isNotEmpty()) {
                        item {
                            val allChecked = changes.all { selectedFiles[it.path] == true }
                            val someChecked = changes.any { selectedFiles[it.path] == true }
                            val triState = when {
                                allChecked -> ToggleableState.On
                                someChecked -> ToggleableState.Indeterminate
                                else -> ToggleableState.Off
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onToggleChangesExpanded)
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isChangesExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                TriStateCheckbox(
                                    state = triState,
                                    onClick = {
                                        val newState = triState != ToggleableState.On
                                        for (item in changes) {
                                            selectedFiles[item.path] = newState
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${stringResource(id = R.string.git_changes)} (${changes.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (isChangesExpanded) {
                            items(changes, key = { it.path }) { item ->
                                GitFileItemRow(
                                    projectDir = projectDir,
                                    item = item,
                                    isChecked = selectedFiles[item.path] ?: true,
                                    onCheckedChange = { checked ->
                                        selectedFiles[item.path] = checked
                                    },
                                    onClick = {
                                        val f = File(projectDir, item.path)
                                        if (f.exists() && f.isFile) {
                                            onOpenFile?.invoke(f)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // SECTION 2: Unversioned Files (unchecked by default - like Android Studio!)
                    if (unversioned.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            val allChecked = unversioned.all { selectedFiles[it.path] == true }
                            val someChecked = unversioned.any { selectedFiles[it.path] == true }
                            val triState = when {
                                allChecked -> ToggleableState.On
                                someChecked -> ToggleableState.Indeterminate
                                else -> ToggleableState.Off
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onToggleUnversionedExpanded)
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isUnversionedExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                TriStateCheckbox(
                                    state = triState,
                                    onClick = {
                                        val newState = triState != ToggleableState.On
                                        for (item in unversioned) {
                                            selectedFiles[item.path] = newState
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${stringResource(id = R.string.git_unversioned)} (${unversioned.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isUnversionedExpanded) {
                            items(unversioned, key = { it.path }) { item ->
                                GitFileItemRow(
                                    projectDir = projectDir,
                                    item = item,
                                    isChecked = selectedFiles[item.path] ?: false,
                                    onCheckedChange = { checked ->
                                        selectedFiles[item.path] = checked
                                    },
                                    onClick = {
                                        val f = File(projectDir, item.path)
                                        if (f.exists() && f.isFile) {
                                            onOpenFile?.invoke(f)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // BOTTOM COMMIT CONTROLS (Screenshots 4 & 5)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Amend Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAmendChange(!isAmend) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isAmend,
                        onCheckedChange = onAmendChange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.git_amend),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Commit message input
                OutlinedTextField(
                    value = commitMessage,
                    onValueChange = onCommitMessageChange,
                    placeholder = { Text(stringResource(id = R.string.git_commit_message_hint)) },
                    maxLines = 4,
                    minLines = 2,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons
                val checkedCount = selectedFiles.count { it.value }
                val canCommit = (checkedCount > 0 || isAmend) && commitMessage.isNotBlank() && !isActionInProgress

                Button(
                    onClick = onCommit,
                    enabled = canCommit,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isActionInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CallSplit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (checkedCount > 0) "${stringResource(id = R.string.git_commit)} ($checkedCount)" else stringResource(id = R.string.git_commit),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = onCommitAndPush,
                    enabled = canCommit,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.git_commit_and_push),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun GitFileItemRow(
    projectDir: File,
    item: GitManager.GitFileItem,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val file = remember(item.path) { File(projectDir, item.path) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))

        // File icon based on type
        FileIcon(
            file = file,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.fileName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.dirPath.isNotEmpty()) {
                Text(
                    text = item.dirPath,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Status badge
        val badgeColor = when (item.type) {
            GitManager.ChangeType.MODIFIED -> Color(0xFFE5A00D)
            GitManager.ChangeType.ADDED -> Color(0xFF4CAF50)
            GitManager.ChangeType.DELETED -> Color(0xFFF44336)
            GitManager.ChangeType.UNTRACKED -> MaterialTheme.colorScheme.outline
            GitManager.ChangeType.MISSING -> Color(0xFFF44336)
            GitManager.ChangeType.CONFLICTING -> Color(0xFFFF5722)
        }
        val badgeLetter = when (item.type) {
            GitManager.ChangeType.MODIFIED -> "M"
            GitManager.ChangeType.ADDED -> "A"
            GitManager.ChangeType.DELETED -> "D"
            GitManager.ChangeType.UNTRACKED -> "?"
            GitManager.ChangeType.MISSING -> "!"
            GitManager.ChangeType.CONFLICTING -> "C"
        }

        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(badgeColor.copy(alpha = 0.18f))
                .border(0.7.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badgeLetter,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
        }
    }
}

@Composable
private fun HistoryTabContent(
    history: List<GitManager.GitCommitItem>,
    isLoading: Boolean,
    onCommitClick: (GitManager.GitCommitItem) -> Unit
) {
    if (isLoading && history.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
        }
        return
    }

    if (history.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.git_no_commits),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        items(history, key = { it.hash }) { commit ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCommitClick(commit) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Git Graph Timeline (Vertical line + circle dot)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE91E63))
                    )
                    Box(
                        modifier = Modifier
                            .width(1.5.dp)
                            .height(48.dp)
                            .background(Color(0xFFE91E63).copy(alpha = 0.4f))
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Commit Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = commit.message,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${commit.authorName} • ${dateFormat.format(commit.date)} • ${commit.shortHash}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}