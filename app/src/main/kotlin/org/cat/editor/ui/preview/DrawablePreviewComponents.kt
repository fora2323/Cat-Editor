package org.cat.editor.ui.preview

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cat.editor.R
import java.io.File
import kotlin.math.roundToInt

enum class PreviewBgMode(val displayName: String) {
    CHECKERBOARD("Checkerboard"),
    DARK("Dark"),
    LIGHT("Light")
}

/**
 * Draws a professional transparency checkerboard, dark solid, or light solid background
 * that dynamically matches the active theme.
 */
@Composable
fun Modifier.drawPreviewBackground(
    mode: PreviewBgMode,
    isDarkTheme: Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f
): Modifier {
    val squareSize = with(LocalDensity.current) { 10.dp.toPx() }

    val colorLight = if (isDarkTheme) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        Color(0xFFFFFFFF)
    }

    val colorDark = if (isDarkTheme) {
        MaterialTheme.colorScheme.surfaceContainerLowest
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }

    val solidDark = if (isDarkTheme) MaterialTheme.colorScheme.surfaceContainerLowest else Color(0xFF181818)
    val solidLight = Color(0xFFFFFFFF)

    return this.drawBehind {
        when (mode) {
            PreviewBgMode.CHECKERBOARD -> {
                val cols = (size.width / squareSize).toInt() + 1
                val rows = (size.height / squareSize).toInt() + 1

                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val color = if ((r + c) % 2 == 0) colorLight else colorDark
                        drawRect(
                            color = color,
                            topLeft = Offset(c * squareSize, r * squareSize),
                            size = Size(squareSize, squareSize)
                        )
                    }
                }
            }
            PreviewBgMode.DARK -> {
                drawRect(color = solidDark)
            }
            PreviewBgMode.LIGHT -> {
                drawRect(color = solidLight)
            }
        }
    }
}

/**
 * Movable / draggable live preview card for XML Vector Drawables and Raster Images.
 * Floating above the code editor (Matches Screenshot 1).
 */
@Composable
fun FloatingDrawablePreviewCard(
    fileName: String,
    infoText: String,
    bitmap: Bitmap?,
    isLoading: Boolean,
    bgMode: PreviewBgMode,
    onToggleBg: () -> Unit,
    onExpandClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .width(175.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Title + Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Background mode toggle button (cycles Checkerboard -> Dark -> Light)
                IconButton(
                    onClick = onToggleBg,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = when (bgMode) {
                            PreviewBgMode.CHECKERBOARD -> Icons.Outlined.Contrast
                            PreviewBgMode.DARK -> Icons.Outlined.DarkMode
                            PreviewBgMode.LIGHT -> Icons.Outlined.LightMode
                        },
                        contentDescription = "Toggle Background",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Fullscreen / Expand Dialog
                IconButton(
                    onClick = onExpandClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = "Expand Preview",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Close / Dismiss
                IconButton(
                    onClick = onCloseClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Preview Canvas Frame with checkerboard / dark / light background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .drawPreviewBackground(bgMode)
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = fileName,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        contentScale = ContentScale.Fit
                    )
                } else if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Parse error",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Info text (dimensions and viewport)
            Text(
                text = infoText,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Expanded Material 3 Modal Bottom Sheet for Vector Drawables and Raster Images.
 * (Matches Screenshot 2).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedDrawablePreviewBottomSheet(
    fileName: String,
    infoText: String,
    bitmap: Bitmap?,
    bgMode: PreviewBgMode,
    onToggleBg: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val baseName = remember(fileName) { VectorDrawableParser.getResourceBaseName(fileName) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row: Icon + Title + Info + Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = infoText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onToggleBg,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = when (bgMode) {
                            PreviewBgMode.CHECKERBOARD -> Icons.Outlined.Contrast
                            PreviewBgMode.DARK -> Icons.Outlined.DarkMode
                            PreviewBgMode.LIGHT -> Icons.Outlined.LightMode
                        },
                        contentDescription = "Toggle Background",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large Preview Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .drawPreviewBackground(bgMode)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = fileName,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Reference Copy Chips (R.drawable and @drawable)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        val text = "R.drawable.$baseName"
                        copyToClipboard(context, "Resource ID", text)
                        Toast.makeText(context, context.getString(R.string.drawable_preview_copied, text), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "R.drawable.$baseName",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = {
                        val text = "@drawable/$baseName"
                        copyToClipboard(context, "XML Reference", text)
                        Toast.makeText(context, context.getString(R.string.drawable_preview_copied, text), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "@drawable/$baseName",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Close Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp)
            ) {
                Text(
                    text = "Close",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard?.setPrimaryClip(clip)
}

/**
 * Dedicated full screen for Vector Drawables when switched to Preview mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VectorPreviewScreen(
    filePath: String,
    content: String,
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val fileName = remember(filePath) { File(filePath).name }
    val baseName = remember(fileName) { VectorDrawableParser.getResourceBaseName(fileName) }
    var bgMode by remember { mutableStateOf(PreviewBgMode.CHECKERBOARD) }

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var meta by remember { mutableStateOf(VectorDrawableMeta()) }
    var isLoading by remember { mutableStateOf(true) }

    val isThemeDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    LaunchedEffect(content, bgMode, isThemeDark) {
        isLoading = true
        withContext(Dispatchers.Default) {
            try {
                val parser = VectorDrawableParser(content, filePath, context)
                val m = parser.getMeta()
                val isDark = when (bgMode) {
                    PreviewBgMode.CHECKERBOARD -> isThemeDark
                    PreviewBgMode.DARK -> true
                    PreviewBgMode.LIGHT -> false
                }
                val bmp = parser.renderBitmap(1024, 1024, isDark)
                withContext(Dispatchers.Main) {
                    meta = m
                    bitmap = bmp
                    isLoading = false
                }
            } catch (_: Throwable) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (meta.infoText.isNotBlank()) meta.infoText else "Vector Drawable",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Preview"
                        )
                    }
                }
            },
            actions = {
                // Background toggle
                IconButton(onClick = {
                    bgMode = when (bgMode) {
                        PreviewBgMode.CHECKERBOARD -> PreviewBgMode.DARK
                        PreviewBgMode.DARK -> PreviewBgMode.LIGHT
                        PreviewBgMode.LIGHT -> PreviewBgMode.CHECKERBOARD
                    }
                    val msg = when (bgMode) {
                        PreviewBgMode.CHECKERBOARD -> context.getString(R.string.drawable_preview_bg_checkerboard)
                        PreviewBgMode.DARK -> context.getString(R.string.drawable_preview_bg_dark)
                        PreviewBgMode.LIGHT -> context.getString(R.string.drawable_preview_bg_light)
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        imageVector = when (bgMode) {
                            PreviewBgMode.CHECKERBOARD -> Icons.Outlined.Contrast
                            PreviewBgMode.DARK -> Icons.Outlined.DarkMode
                            PreviewBgMode.LIGHT -> Icons.Outlined.LightMode
                        },
                        contentDescription = stringResource(id = R.string.drawable_preview_toggle_bg),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Edit Button
                if (onBackClick != null) {
                    FilledTonalButton(
                        onClick = onBackClick,
                        modifier = Modifier.padding(end = 8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Edit", style = MaterialTheme.typography.labelMedium)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

        // Center Preview Canvas Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .drawPreviewBackground(bgMode),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = fileName,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentScale = ContentScale.Fit
                )
            } else if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "Unable to render vector drawable",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

        // Bottom Reference Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        val text = "R.drawable.$baseName"
                        copyToClipboard(context, "Resource ID", text)
                        Toast.makeText(context, context.getString(R.string.drawable_preview_copied, text), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "R.drawable.$baseName",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = {
                        val text = "@drawable/$baseName"
                        copyToClipboard(context, "XML Reference", text)
                        Toast.makeText(context, context.getString(R.string.drawable_preview_copied, text), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "@drawable/$baseName",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

