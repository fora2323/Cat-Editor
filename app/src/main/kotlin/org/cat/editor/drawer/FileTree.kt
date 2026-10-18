package org.cat.editor.drawer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cat.editor.R
import java.io.File

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
                when {
                    file.name == ".git" -> false
                    file.name.startsWith(".") -> if (file.isDirectory) true else showHiddenFiles
                    else -> true
                }
            }
            ?.sortedWith(comparator)
            ?: emptyList()
    } catch (_: Throwable) {
        emptyList()
    }
}

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