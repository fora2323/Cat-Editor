package org.cat.editor.drawer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.cat.editor.R
import org.cat.editor.ui.BottomSheetDialog
import org.cat.editor.ui.preview.isBinaryFile
import org.cat.editor.ui.preview.isImageFile
import java.io.File
import kotlin.coroutines.coroutineContext

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