package org.cat.editor.ui.preview

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

/**
 * Node structure for the indented Hierarchy Tree.
 */
data class FlatTreeNode(
    val node: ComposeNode,
    val depth: Int,
    val isLastSibling: Boolean
)

private fun buildFlatTree(
    node: ComposeNode?,
    depth: Int,
    isLast: Boolean,
    list: MutableList<FlatTreeNode>
) {
    if (node == null) return
    list.add(FlatTreeNode(node, depth, isLast))
    val children = node.children
    for (i in children.indices) {
        buildFlatTree(children[i], depth + 1, i == children.size - 1, list)
    }
}

private fun collectAllNodes(node: ComposeNode?, list: MutableList<ComposeNode>) {
    if (node == null) return
    list.add(node)
    for (child in node.children) {
        collectAllNodes(child, list)
    }
}

enum class PreviewTabMode(val label: String, val icon: ImageVector) {
    CANVAS("Preview", Icons.Default.PhoneAndroid),
    TREE("Hierarchy Tree", Icons.Default.AccountTree),
    TEMPLATES("Templates", Icons.Default.AutoAwesome)
}

/**
 * Android Studio-grade Jetpack Compose & XML Preview Tool Window.
 * Features:
 * - Interactive Device Frame Canvas with realistic frame, system UI, and pan/zoom
 * - Component Inspection mode with highlight rectangles and Inspector Card
 * - Interactive Component Hierarchy Tree with branch prefixes, icons, parameters, and selection sync
 * - Prev / Next component stepping through the AST
 * - Multi-preview chip variants
 * - Material 3 Compose Code Templates / Assistant
 * - Bulletproof safety against non-UI files (HTML, Java, binary, images)
 */
@Composable
fun PreviewScreen(
    activeFilePath: String,
    currentRootNode: ComposeNode? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (activeFilePath.isBlank()) {
        EmptyPreviewPlaceholder(
            title = stringResource(id = R.string.preview_hint),
            subtitle = "Open a Kotlin Compose (@Composable) or Android XML layout file to view live preview."
        )
        return
    }

    val file = remember(activeFilePath) { File(activeFilePath) }
    if (!file.exists()) {
        EmptyPreviewPlaceholder(
            title = "File not found",
            subtitle = "The selected file does not exist on disk."
        )
        return
    }

    val fileName = file.name.lowercase()

    // 1. Image preview check
    if (isImageFile(fileName) || isVectorFile(fileName)) {
        ImageDetailViewer(file = file, modifier = modifier)
        return
    }

    // 2. Read file content safely
    val fileContent = remember(activeFilePath, file.lastModified()) {
        try {
            file.readText()
        } catch (_: Throwable) {
            ""
        }
    }

    // Determine preview type
    val isKotlin = fileName.endsWith(".kt") || fileName.endsWith(".kts")
    val isXml = fileName.endsWith(".xml")

    if (!isKotlin && !isXml) {
        EmptyPreviewPlaceholder(
            title = "Preview Not Supported",
            subtitle = "Preview is available for Kotlin Compose (.kt, .kts), XML Layouts (.xml), and image files."
        )
        return
    }

    // If XML, check whether it is a layout XML or a resource XML (strings/colors/etc.)
    if (isXml && !XmlLayoutParser.isLayoutXml(fileContent)) {
        EmptyPreviewPlaceholder(
            title = "XML Resource / Manifest File",
            subtitle = "Layout preview is only applicable for Android UI layouts (e.g. LinearLayout, ConstraintLayout, etc.), not value resources."
        )
        return
    }

    // If Kotlin, check if it contains Compose code
    val isCompose = isKotlin && ComposePreviewParser.isComposeSource(fileContent)
    if (isKotlin && !isCompose) {
        EmptyPreviewPlaceholder(
            title = "Kotlin File (No UI Detected)",
            subtitle = "No @Composable functions or Compose UI components were found in this file."
        )
        return
    }

    // State for preview canvas
    var isDarkMode by remember { mutableStateOf(false) }
    var showDeviceFrame by remember { mutableStateOf(true) }
    var showSystemUi by remember { mutableStateOf(true) }
    var isInspectMode by remember { mutableStateOf(true) }
    var selectedPreset by remember { mutableStateOf(ComposePreviewView.DevicePreset.PHONE) }
    var previewViewRef by remember { mutableStateOf<ComposePreviewView?>(null) }
    var showPresetMenu by remember { mutableStateOf(false) }
    var activeTabMode by remember { mutableStateOf(PreviewTabMode.CANVAS) }

    // Multi-preview variants for Compose
    val previews = remember(fileContent, activeFilePath) {
        if (isCompose) {
            try {
                ComposePreviewParser.parse(fileContent)
            } catch (_: Throwable) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    var activePreviewIndex by remember(previews) { mutableStateOf(0) }

    // Parse AST
    val astRoot = remember(fileContent, activeFilePath, activePreviewIndex) {
        try {
            if (isXml) {
                XmlLayoutParser.parse(fileContent)
            } else if (previews.isNotEmpty()) {
                val model = previews.getOrElse(activePreviewIndex) { previews[0] }
                ComposeAstParser.parse(model.functionBody, fileContent)
            } else {
                ComposeAstParser.parse(fileContent, fileContent)
            }
        } catch (e: Throwable) {
            ComposeNode("Column").apply {
                addChild(ComposeNode("Text").apply {
                    addAttribute("text", "Parsing Error: " + (e.message ?: "Invalid syntax"))
                })
            }
        }
    }

    // Flatten AST nodes for tree view & inspection navigation
    val flattenedNodes = remember(astRoot) {
        val list = mutableListOf<ComposeNode>()
        collectAllNodes(astRoot, list)
        list
    }

    val flatTreeList = remember(astRoot) {
        val list = mutableListOf<FlatTreeNode>()
        buildFlatTree(astRoot, 0, true, list)
        list
    }

    var inspectedNode by remember { mutableStateOf<ComposeNode?>(null) }
    var currentInspectedIndex by remember { mutableStateOf(-1) }

    fun selectAndInspectNode(node: ComposeNode) {
        inspectedNode = node
        val idx = flattenedNodes.indexOf(node)
        currentInspectedIndex = if (idx != -1) idx else 0
        if (!isInspectMode) {
            isInspectMode = true
            previewViewRef?.setInspectMode(true)
        }
        val targetView = ComposeRenderer.findViewForNode(previewViewRef?.viewportContent, node)
        previewViewRef?.setInspectedView(targetView)
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
        val targetView = ComposeRenderer.findViewForNode(previewViewRef?.viewportContent, node)
        previewViewRef?.setInspectedView(targetView)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Toolbar with preview controls
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Preset selector dropdown button
                Box {
                    FilledTonalButton(
                        onClick = { showPresetMenu = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedPreset.label.substringBefore(" ("),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showPresetMenu,
                        onDismissRequest = { showPresetMenu = false }
                    ) {
                        ComposePreviewView.DevicePreset.values().forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.label, fontSize = 13.sp) },
                                onClick = {
                                    selectedPreset = preset
                                    previewViewRef?.setDevicePreset(preset)
                                    showPresetMenu = false
                                }
                            )
                        }
                    }
                }

                // Action buttons: Theme, Frame, Inspect, Zoom
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            isDarkMode = !isDarkMode
                            val config = ComposeRenderer.RenderConfig.create(isDarkMode)
                            previewViewRef?.applyThemeConfig(config)
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            showDeviceFrame = !showDeviceFrame
                            previewViewRef?.setShowDeviceFrame(showDeviceFrame)
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            if (showDeviceFrame) Icons.Default.CropPortrait else Icons.Default.CropFree,
                            contentDescription = "Toggle Frame",
                            modifier = Modifier.size(18.dp),
                            tint = if (showDeviceFrame) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            isInspectMode = !isInspectMode
                            previewViewRef?.setInspectMode(isInspectMode)
                            if (!isInspectMode) {
                                inspectedNode = null
                                previewViewRef?.setInspectedView(null)
                            }
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.Highlight,
                            contentDescription = "Inspect Element",
                            modifier = Modifier.size(18.dp),
                            tint = if (isInspectMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { previewViewRef?.zoomOut() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = { previewViewRef?.zoomIn() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = { previewViewRef?.zoomFit() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.FitScreen, contentDescription = "Fit Canvas", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Multi-preview variants bar
        if (previews.size > 1) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    previews.forEachIndexed { index, previewModel ->
                        FilterChip(
                            selected = activePreviewIndex == index,
                            onClick = { activePreviewIndex = index },
                            label = { Text(previewModel.displayName, fontSize = 11.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }
        }

        // Mode Navigation Tabs: Preview Canvas | Hierarchy Tree | Templates
        TabRow(
            selectedTabIndex = activeTabMode.ordinal,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            PreviewTabMode.values().forEach { mode ->
                Tab(
                    selected = activeTabMode == mode,
                    onClick = { activeTabMode = mode },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(mode.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(mode.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }
        }

        // Main Tab Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (activeTabMode) {
                PreviewTabMode.CANVAS -> {
                    // Preview Canvas Viewport
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E1E1E))
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                ComposePreviewView(ctx).apply {
                                    previewViewRef = this
                                    setShowDeviceFrame(showDeviceFrame)
                                    setShowSystemUi(showSystemUi)
                                    setDevicePreset(selectedPreset)
                                    setInspectMode(isInspectMode)
                                    onNodeTapListener = ComposeRenderer.OnNodeSelectedListener { node, view ->
                                        selectAndInspectNode(node)
                                    }

                                    val config = ComposeRenderer.RenderConfig.create(isDarkMode).apply {
                                        this.isInspectMode = isInspectMode
                                        this.inspectListener = ComposeRenderer.OnNodeSelectedListener { node, view ->
                                            selectAndInspectNode(node)
                                        }
                                    }
                                    applyThemeConfig(config)
                                    val rendered = ComposeRenderer.render(ctx, astRoot, config)
                                    setContent(rendered)
                                    post { zoomFit() }
                                }
                            },
                            update = { pv ->
                                previewViewRef = pv
                                pv.setShowDeviceFrame(showDeviceFrame)
                                pv.setShowSystemUi(showSystemUi)
                                pv.setDevicePreset(selectedPreset)
                                pv.setInspectMode(isInspectMode)
                                val config = ComposeRenderer.RenderConfig.create(isDarkMode).apply {
                                    this.isInspectMode = isInspectMode
                                    this.inspectListener = ComposeRenderer.OnNodeSelectedListener { node, view ->
                                        selectAndInspectNode(node)
                                    }
                                }
                                pv.applyThemeConfig(config)
                                val rendered = ComposeRenderer.render(pv.context, astRoot, config)
                                pv.setContent(rendered)

                                // Keep inspected view highlighted if node exists
                                if (inspectedNode != null) {
                                    val targetView = ComposeRenderer.findViewForNode(pv.viewportContent, inspectedNode)
                                    pv.setInspectedView(targetView)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Inspector Bottom Card
                        val currentNode = inspectedNode
                        if (isInspectMode && currentNode != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                            ) {
                                InspectorBottomCard(
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
                }

                PreviewTabMode.TREE -> {
                    // Hierarchy Tree View
                    ComponentHierarchyTreeView(
                        flatTree = flatTreeList,
                        selectedNode = inspectedNode,
                        onNodeClick = { node ->
                            selectAndInspectNode(node)
                            activeTabMode = PreviewTabMode.CANVAS
                        }
                    )
                }

                PreviewTabMode.TEMPLATES -> {
                    // Code Templates / Assistant
                    ComposeTemplatesView(context = context)
                }
            }
        }
    }
}

/**
 * Inspector Card shown when inspecting a component.
 */
@Composable
private fun InspectorBottomCard(
    node: ComposeNode,
    currentIndex: Int,
    totalCount: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Header: Icon, Name, Prev/Next, Dismiss
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getNodeIcon(node.type),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = node.type,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = onPrev,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Previous Component",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${currentIndex + 1} / $totalCount",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Next Component",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss Inspector",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            // Details: Attributes & Modifiers
            val detailLines = remember(node) {
                val list = mutableListOf<String>()
                if (node.attributes.isNotEmpty()) {
                    list.add("Parameters: " + node.attributes.entries.joinToString(", ") { "${it.key} = \"${it.value}\"" })
                }
                if (node.modifiers.isNotEmpty()) {
                    list.add("Modifiers: " + node.modifiers.joinToString(" • "))
                }
                if (list.isEmpty() && node.rawSource.isNotBlank()) {
                    list.add(node.rawSource.take(120))
                }
                if (list.isEmpty()) {
                    list.add("Standard ${node.type} component without custom parameters.")
                }
                list
            }

            detailLines.forEach { line ->
                Text(
                    text = line,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}

/**
 * Component Hierarchy Tree list view with authentic branch indentation.
 */
@Composable
private fun ComponentHierarchyTreeView(
    flatTree: List<FlatTreeNode>,
    selectedNode: ComposeNode?,
    onNodeClick: (ComposeNode) -> Unit
) {
    if (flatTree.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No UI hierarchy detected", color = MaterialTheme.colorScheme.outline)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp)
    ) {
        items(flatTree) { item ->
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
                // Indentation
                if (item.depth > 0) {
                    Spacer(modifier = Modifier.width((item.depth * 18).dp))
                    Text(
                        text = if (item.isLastSibling) "└── " else "├── ",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Component Icon
                Icon(
                    imageVector = getNodeIcon(node.type),
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Component Name
                Text(
                    text = node.type,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )

                // Parameters summary
                val paramsPreview = remember(node) {
                    when {
                        node.attributes.containsKey("text") -> "\"${node.attributes["text"]}\""
                        node.attributes.containsKey("title") -> "\"${node.attributes["title"]}\""
                        node.modifiers.isNotEmpty() -> node.modifiers.first()
                        node.attributes.isNotEmpty() -> node.attributes.entries.first().let { "${it.key}: ${it.value}" }
                        else -> ""
                    }
                }

                if (paramsPreview.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = paramsPreview,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (node.children.isNotEmpty()) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Text("${node.children.size}", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

/**
 * Compose Templates Assistant (from Cipher IDE).
 */
@Composable
private fun ComposeTemplatesView(context: Context) {
    val templates = listOf(
        TemplateItem(
            title = "@Preview Composable Function",
            desc = "Inserts standard @Preview annotation with Light & Dark preview parameters",
            snippet = """
                @androidx.compose.ui.tooling.preview.Preview(showBackground = true)
                @androidx.compose.ui.tooling.preview.Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
                @androidx.compose.runtime.Composable
                fun ScreenPreview() {
                    MaterialTheme {
                        // YourScreenContent()
                    }
                }
            """.trimIndent()
        ),
        TemplateItem(
            title = "Scaffold Screen with TopBar",
            desc = "Full-screen Scaffold with Material 3 TopAppBar and inner padding content",
            snippet = """
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Screen Title") }
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp)
                    ) {
                        Text("Hello Compose!")
                    }
                }
            """.trimIndent()
        ),
        TemplateItem(
            title = "Material 3 Elevated Card",
            desc = "Elevated card with title, subtitle, and action buttons",
            snippet = """
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Card Title", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Card description text goes here.", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(12.dp))
                        FilledTonalButton(onClick = { /* action */ }) {
                            Text("Action")
                        }
                    }
                }
            """.trimIndent()
        ),
        TemplateItem(
            title = "LazyColumn Scrollable List",
            desc = "Vertically scrollable list container with spaced items",
            snippet = """
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(10) { index ->
                        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Item #" + (index + 1),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            """.trimIndent()
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(templates) { item ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.desc,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Compose Template", item.snippet))
                                Toast.makeText(context, "Snippet copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Snippet", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

private data class TemplateItem(val title: String, val desc: String, val snippet: String)

private fun getNodeIcon(type: String): ImageVector {
    return when (type) {
        "Column", "LazyColumn", "Row", "LazyRow", "Box", "Scaffold", "Surface" -> Icons.Default.Layers
        "Card", "ElevatedCard", "OutlinedCard" -> Icons.Default.Layers
        "Text", "BasicText" -> Icons.Default.TextFields
        "Icon", "Image" -> Icons.Default.Image
        "Button", "ElevatedButton", "FilledTonalButton", "OutlinedButton", "TextButton", "IconButton", "FloatingActionButton" -> Icons.Default.SmartButton
        "TextField", "OutlinedTextField", "BasicTextField", "Checkbox", "Switch", "RadioButton", "Slider" -> Icons.Default.Tune
        else -> Icons.Default.Code
    }
}

@Composable
private fun EmptyPreviewPlaceholder(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
    }
}