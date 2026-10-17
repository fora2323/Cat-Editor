package org.cat.editor.ui.preview

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.cat.editor.R
import java.io.File

enum class XmlPreviewTabMode(val label: String) {
    CANVAS("Preview"),
    TREE("Component Tree")
}

data class FlatXmlTreeNode(
    val id: String,
    val node: XmlNode,
    val depth: Int,
    val branchPrefix: String,
    val icon: ImageVector,
    val summaryText: String
)

/**
 * Dedicated Android XML Layout Preview Screen.
 * Fully decoupled from Compose preview, displaying authentic XML layout elements,
 * exact XML attributes, and real layout hierarchy.
 */
@Composable
fun PreviewLayoutXml(
    activeFilePath: String,
    sourceCode: String = "",
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (activeFilePath.isBlank()) {
        EmptyXmlPreviewPlaceholder(
            title = stringResource(id = R.string.preview_hint),
            subtitle = "Open an Android XML layout file to view live preview."
        )
        return
    }

    val file = remember(activeFilePath) { File(activeFilePath) }
    if (!file.exists() || !file.isFile) {
        EmptyXmlPreviewPlaceholder(
            title = "File not found",
            subtitle = "The selected XML file does not exist on disk."
        )
        return
    }

    val fileContent = remember(activeFilePath, file.lastModified()) {
        try {
            file.readText()
        } catch (_: Throwable) {
            ""
        }
    }
    val effectiveContent = if (sourceCode.isNotBlank()) sourceCode else fileContent

    if (VectorDrawableParser.isVectorXml(effectiveContent)) {
        VectorPreviewScreen(
            filePath = activeFilePath,
            content = effectiveContent,
            onBackClick = onBackClick
        )
        return
    }

    if (!XmlLayoutParser.isLayoutXml(effectiveContent)) {
        EmptyXmlPreviewPlaceholder(
            title = "XML Resource / Manifest File",
            subtitle = "Layout preview is only applicable for Android UI layouts (e.g. LinearLayout, MaterialCardView, etc.), not value resources."
        )
        return
    }

    // Preview state
    var isDarkMode by remember { mutableStateOf(false) }
    var showDeviceFrame by remember { mutableStateOf(false) }
    var isInspectMode by remember { mutableStateOf(true) }
    var previewViewRef by remember { mutableStateOf<XmlPreviewView?>(null) }
    var activeTabMode by remember { mutableStateOf(XmlPreviewTabMode.CANVAS) }

    // Parse AST
    val astRoot = remember(effectiveContent, activeFilePath) {
        XmlLayoutParser.parse(effectiveContent)
    }

    val flattenedNodes = remember(astRoot) {
        val list = mutableListOf<XmlNode>()
        collectAllXmlNodes(astRoot, list)
        list
    }

    val flatTreeList = remember(astRoot) {
        val list = mutableListOf<FlatXmlTreeNode>()
        buildFlatXmlTree(astRoot, 0, true, list)
        list
    }

    var inspectedNode by remember { mutableStateOf<XmlNode?>(null) }
    var currentInspectedIndex by remember { mutableStateOf(-1) }

    fun selectAndInspectNode(node: XmlNode) {
        inspectedNode = node
        val idx = flattenedNodes.indexOf(node)
        currentInspectedIndex = if (idx != -1) idx else 0
        if (!isInspectMode) {
            isInspectMode = true
            previewViewRef?.setInspectMode(true)
        }
        val targetView = XmlLayoutRenderer.findViewForNode(previewViewRef?.viewportContent, node)
        if (targetView != null) {
            previewViewRef?.setInspectedView(targetView)
        } else {
            previewViewRef?.post {
                val tv = XmlLayoutRenderer.findViewForNode(previewViewRef?.viewportContent, node)
                previewViewRef?.setInspectedView(tv)
            }
        }
    }

    fun navigateInspector(delta: Int) {
        if (flattenedNodes.isEmpty()) return
        var nextIndex = currentInspectedIndex + delta
        if (nextIndex < 0) {
            nextIndex = flattenedNodes.size - 1
        } else if (nextIndex >= flattenedNodes.size) {
            nextIndex = 0
        }
        currentInspectedIndex = nextIndex
        val node = flattenedNodes[currentInspectedIndex]
        inspectedNode = node
        val targetView = XmlLayoutRenderer.findViewForNode(previewViewRef?.viewportContent, node)
        previewViewRef?.setInspectedView(targetView)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab Row: Preview | Component Tree
        TabRow(
            selectedTabIndex = activeTabMode.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            XmlPreviewTabMode.values().forEach { mode ->
                Tab(
                    selected = activeTabMode == mode,
                    onClick = { activeTabMode = mode },
                    text = {
                        Text(
                            text = mode.label,
                            fontSize = 13.sp,
                            fontWeight = if (activeTabMode == mode) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }

        // Main Content Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Viewport Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E))
                    .graphicsLayer {
                        val isVisible = activeTabMode == XmlPreviewTabMode.CANVAS
                        alpha = if (isVisible) 1f else 0f
                        translationX = if (isVisible) 0f else 20000f
                    }
            ) {
                AndroidView(
                    factory = { ctx ->
                        XmlPreviewView(ctx).apply {
                            previewViewRef = this
                            setShowDeviceFrame(showDeviceFrame)
                            setInspectMode(isInspectMode)

                            val config = XmlLayoutRenderer.XmlRenderConfig.create(isDarkMode, activeFilePath).apply {
                                this.isInspectMode = isInspectMode
                                this.inspectListener = XmlLayoutRenderer.OnXmlNodeSelectedListener { node, _ ->
                                    selectAndInspectNode(node)
                                }
                            }
                            applyThemeConfig(config)
                            val rendered = XmlLayoutRenderer.render(ctx, astRoot, config)
                            setContent(rendered)
                        }
                    },
                    update = { pv ->
                        previewViewRef = pv
                        pv.setShowDeviceFrame(showDeviceFrame)
                        pv.setInspectMode(isInspectMode)

                        val config = XmlLayoutRenderer.XmlRenderConfig.create(isDarkMode, activeFilePath).apply {
                            this.isInspectMode = isInspectMode
                            this.inspectListener = XmlLayoutRenderer.OnXmlNodeSelectedListener { node, _ ->
                                selectAndInspectNode(node)
                            }
                        }
                        pv.applyThemeConfig(config)
                        val rendered = XmlLayoutRenderer.render(pv.context, astRoot, config)
                        pv.setContent(rendered)

                        if (inspectedNode != null) {
                            val targetView = XmlLayoutRenderer.findViewForNode(pv.viewportContent, inspectedNode)
                            pv.setInspectedView(targetView)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Bottom Container: Floating Actions stacked above Inspector Bottom Card (NO Overlap!)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    // Floating Action Buttons Column
                    Column(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(end = 16.dp, bottom = 12.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Theme Toggle
                        SmallFloatingActionButton(
                            onClick = {
                                isDarkMode = !isDarkMode
                                val config = XmlLayoutRenderer.XmlRenderConfig.create(isDarkMode, activeFilePath).apply {
                                    this.isInspectMode = isInspectMode
                                    this.inspectListener = XmlLayoutRenderer.OnXmlNodeSelectedListener { node, _ ->
                                        selectAndInspectNode(node)
                                    }
                                }
                                previewViewRef?.applyThemeConfig(config)
                            },
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                                contentDescription = "Toggle Theme",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Inspect Toggle
                        SmallFloatingActionButton(
                            onClick = {
                                isInspectMode = !isInspectMode
                                previewViewRef?.setInspectMode(isInspectMode)
                                if (!isInspectMode) {
                                    inspectedNode = null
                                    previewViewRef?.setInspectedView(null)
                                }
                            },
                            containerColor = if (isInspectMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (isInspectMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isInspectMode) Icons.Outlined.TrackChanges else Icons.Outlined.Adjust,
                                contentDescription = "Toggle Inspect Mode",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Phone Frame Toggle
                        SmallFloatingActionButton(
                            onClick = {
                                showDeviceFrame = !showDeviceFrame
                                previewViewRef?.setShowDeviceFrame(showDeviceFrame)
                            },
                            containerColor = if (showDeviceFrame) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (showDeviceFrame) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhoneAndroid,
                                contentDescription = "Toggle Device Frame",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Single Edit Floating Action Button
                        ExtendedFloatingActionButton(
                            onClick = { onBackClick?.invoke() },
                            icon = {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null
                                )
                            },
                            text = {
                                Text(
                                    text = "Edit",
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Docked XML Inspector Bottom Card
                    val currentNode = inspectedNode
                    if (isInspectMode && currentNode != null) {
                        XmlInspectorBottomCard(
                            node = currentNode,
                            currentIndex = currentInspectedIndex,
                            totalCount = flattenedNodes.size,
                            onPrev = { navigateInspector(-1) },
                            onNext = { navigateInspector(1) },
                            onDismiss = {
                                inspectedNode = null
                                previewViewRef?.setInspectedView(null)
                            }
                        )
                    }
                }
            }

            // Component Tree Tab
            if (activeTabMode == XmlPreviewTabMode.TREE) {
                XmlComponentHierarchyTreeView(
                    flatTree = flatTreeList,
                    selectedNode = inspectedNode,
                    onNodeClick = { node ->
                        selectAndInspectNode(node)
                        activeTabMode = XmlPreviewTabMode.CANVAS
                    }
                )
            }
        }
    }
}

/**
 * Docked Inspector Card specifically tailored for XML elements and attributes.
 */
@Composable
private fun XmlInspectorBottomCard(
    node: XmlNode,
    currentIndex: Int,
    totalCount: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Header Row: Tag icon, tag name, pagination, dismiss
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getXmlTagIcon(node.simpleName),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "<${node.simpleName}>",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onPrev,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Previous",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${currentIndex + 1} / $totalCount",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Next",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ID row if present
            if (node.id != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "android:id = \"${node.id}\"",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Real XML Attributes (No Compose modifiers!)
            if (node.attributes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    node.attributes.entries
                        .filter { it.key != "android:id" && it.key != "id" }
                        .take(8)
                        .forEach { entry ->
                            Text(
                                text = "${entry.key} = \"${entry.value}\"",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
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

/**
 * Component Tree View for XML elements.
 */
@Composable
private fun XmlComponentHierarchyTreeView(
    flatTree: List<FlatXmlTreeNode>,
    selectedNode: XmlNode?,
    onNodeClick: (XmlNode) -> Unit
) {
    if (flatTree.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No XML layout hierarchy detected", color = MaterialTheme.colorScheme.outline)
        }
        return
    }

    val listState = rememberLazyListState()

    LaunchedEffect(selectedNode, flatTree) {
        if (selectedNode != null) {
            val idx = flatTree.indexOfFirst { it.node === selectedNode }
            if (idx >= 0) {
                listState.animateScrollToItem(maxOf(0, idx - 2))
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 4.dp)
    ) {
        items(
            items = flatTree,
            key = { it.id },
            contentType = { "xml_tree_item" }
        ) { item ->
            val isSelected = item.node === selectedNode
            val node = item.node

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNodeClick(node) }
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        else Color.Transparent
                    )
                    .padding(vertical = 6.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.depth > 0) {
                    Spacer(modifier = Modifier.width((item.depth * 16).dp))
                    Text(
                        text = item.branchPrefix,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "<${node.simpleName}>",
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )

                if (node.shortId != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "#${node.shortId}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (item.summaryText.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.summaryText,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun collectAllXmlNodes(node: XmlNode, list: MutableList<XmlNode>) {
    list.add(node)
    for (child in node.children) {
        collectAllXmlNodes(child, list)
    }
}

private fun buildFlatXmlTree(
    node: XmlNode,
    depth: Int,
    isLast: Boolean,
    list: MutableList<FlatXmlTreeNode>
) {
    val prefix = if (depth == 0) "" else if (isLast) "└── " else "├── "
    val summary = when {
        node.hasAttribute("text") -> "\"${node.getAttribute("text")}\""
        node.hasAttribute("orientation") -> "${node.getAttribute("orientation")}"
        node.hasAttribute("layout_width") -> "${node.getAttribute("layout_width")} × ${node.getAttribute("layout_height", "wrap_content")}"
        else -> ""
    }

    list.add(
        FlatXmlTreeNode(
            id = "${node.tagName}_${list.size}_$depth",
            node = node,
            depth = depth,
            branchPrefix = prefix,
            icon = getXmlTagIcon(node.simpleName),
            summaryText = summary
        )
    )

    for (i in node.children.indices) {
        buildFlatXmlTree(
            node = node.children[i],
            depth = depth + 1,
            isLast = i == node.children.size - 1,
            list = list
        )
    }
}

fun getXmlTagIcon(simpleTag: String): ImageVector {
    val lower = simpleTag.lowercase()
    return when {
        lower.contains("card") -> Icons.Default.CreditCard
        lower.contains("text") -> Icons.Default.TextFields
        lower.contains("image") -> Icons.Default.Image
        lower.contains("button") -> Icons.Default.SmartButton
        lower.contains("scroll") -> Icons.Default.SwapVert
        lower.contains("check") -> Icons.Default.CheckBox
        lower.contains("radio") -> Icons.Default.RadioButtonChecked
        lower.contains("switch") -> Icons.Default.ToggleOn
        lower.contains("progress") -> Icons.Default.HourglassTop
        lower.contains("layout") -> Icons.Default.Layers
        lower.contains("toolbar") -> Icons.Default.WebAsset
        else -> Icons.Default.Code
    }
}

@Composable
private fun EmptyXmlPreviewPlaceholder(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Card(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}