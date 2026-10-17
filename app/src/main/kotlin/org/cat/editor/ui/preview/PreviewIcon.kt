package org.cat.editor.ui.preview

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun ImagePreviewIcon(file: File, modifier: Modifier = Modifier) {
    val bitmap = remember(file.absolutePath, file.lastModified()) {
        try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (_: Throwable) { null }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = file.name,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else {
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = file.name,
            modifier = modifier,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Full-screen dedicated Image Viewer for drawable icons, PNG, JPG, WEBP, and SVG files.
 * Replaces the code editor view when viewing binary image assets.
 */
@Composable
fun ImageDetailViewer(file: File, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bitmap = remember(file.absolutePath, file.lastModified()) {
        try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (_: Throwable) { null }
    }

    var bgMode by remember { mutableStateOf(PreviewBgMode.CHECKERBOARD) }
    var showExpandedSheet by remember { mutableStateOf(false) }

    val baseName = remember(file.name) { VectorDrawableParser.getResourceBaseName(file.name) }
    val sizeKb = file.length() / 1024f
    val sizeStr = if (sizeKb > 1024) String.format("%.2f MB", sizeKb / 1024f) else String.format("%.1f KB", sizeKb)
    val infoText = if (bitmap != null) "${bitmap.width} × ${bitmap.height} px • $sizeStr" else sizeStr

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Container with background mode toggle (Checkerboard / Dark / Light)
            Surface(
                modifier = Modifier
                    .sizeIn(minWidth = 140.dp, minHeight = 140.dp, maxWidth = 340.dp, maxHeight = 340.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .drawPreviewBackground(bgMode),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = file.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.BrokenImage,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Unable to decode image",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Floating quick toggles on the image container
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
                        ) {
                            IconButton(
                                onClick = {
                                    bgMode = when (bgMode) {
                                        PreviewBgMode.CHECKERBOARD -> PreviewBgMode.DARK
                                        PreviewBgMode.DARK -> PreviewBgMode.LIGHT
                                        PreviewBgMode.LIGHT -> PreviewBgMode.CHECKERBOARD
                                    }
                                    val msg = when (bgMode) {
                                        PreviewBgMode.CHECKERBOARD -> "Checkerboard background"
                                        PreviewBgMode.DARK -> "Dark background"
                                        PreviewBgMode.LIGHT -> "Light background"
                                    }
                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = when (bgMode) {
                                        PreviewBgMode.CHECKERBOARD -> Icons.Outlined.Contrast
                                        PreviewBgMode.DARK -> Icons.Outlined.DarkMode
                                        PreviewBgMode.LIGHT -> Icons.Outlined.LightMode
                                    },
                                    contentDescription = "Toggle Background",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
                        ) {
                            IconButton(
                                onClick = { showExpandedSheet = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.OpenInFull,
                                    contentDescription = "Expand",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Copy Buttons for Android Developers
            Row(
                modifier = Modifier.fillMaxWidth(0.92f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        val text = "R.drawable.$baseName"
                        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        cm?.setPrimaryClip(android.content.ClipData.newPlainText("Resource ID", text))
                        android.widget.Toast.makeText(context, "Copied: $text", android.widget.Toast.LENGTH_SHORT).show()
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
                        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        cm?.setPrimaryClip(android.content.ClipData.newPlainText("XML Reference", text))
                        android.widget.Toast.makeText(context, "Copied: $text", android.widget.Toast.LENGTH_SHORT).show()
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

            Spacer(modifier = Modifier.height(16.dp))

            // Metadata Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(0.92f),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = file.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Dimensions:", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = if (bitmap != null) "${bitmap.width} × ${bitmap.height} px" else "Unknown",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("File Size:", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = sizeStr,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Type:", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = file.extension.uppercase() + " Image",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (showExpandedSheet) {
            ExpandedDrawablePreviewBottomSheet(
                fileName = file.name,
                infoText = infoText,
                bitmap = bitmap,
                bgMode = bgMode,
                onToggleBg = {
                    bgMode = when (bgMode) {
                        PreviewBgMode.CHECKERBOARD -> PreviewBgMode.DARK
                        PreviewBgMode.DARK -> PreviewBgMode.LIGHT
                        PreviewBgMode.LIGHT -> PreviewBgMode.CHECKERBOARD
                    }
                },
                onDismiss = { showExpandedSheet = false }
            )
        }
    }
}

@Composable
fun BinaryFilePlaceholder(file: File, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF181818)),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            modifier = Modifier.padding(24.dp).fillMaxWidth(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = file.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                val sizeKb = file.length() / 1024f
                val sizeStr = if (sizeKb > 1024) String.format("%.2f MB", sizeKb / 1024f) else String.format("%.1f KB", sizeKb)
                Text(
                    text = "Binary File ($sizeStr)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "This file contains binary data and cannot be opened in the text editor.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun isImageFile(fileName: String): Boolean {
    val lower = fileName.lowercase()
    return lower.endsWith(".png") || lower.endsWith(".jpg") ||
           lower.endsWith(".jpeg") || lower.endsWith(".webp") ||
           lower.endsWith(".gif") || lower.endsWith(".bmp") ||
           lower.endsWith(".ico")
}

fun isVectorFile(fileName: String): Boolean {
    return fileName.lowercase().endsWith(".svg")
}

fun isBinaryFile(fileName: String): Boolean {
    val lower = fileName.lowercase()
    return isImageFile(lower) || isVectorFile(lower) ||
           lower.endsWith(".jks") || lower.endsWith(".keystore") ||
           lower.endsWith(".jar") || lower.endsWith(".aar") ||
           lower.endsWith(".zip") || lower.endsWith(".tar") ||
           lower.endsWith(".gz") || lower.endsWith(".7z") ||
           lower.endsWith(".so") || lower.endsWith(".dylib") ||
           lower.endsWith(".dll") || lower.endsWith(".class") ||
           lower.endsWith(".dex") || lower.endsWith(".apk") ||
           lower.endsWith(".aab") || lower.endsWith(".mp3") ||
           lower.endsWith(".wav") || lower.endsWith(".ogg") ||
           lower.endsWith(".mp4") || lower.endsWith(".avi") ||
           lower.endsWith(".mov") || lower.endsWith(".pdf") ||
           lower.endsWith(".ttf") || lower.endsWith(".otf") ||
           lower.endsWith(".woff") || lower.endsWith(".woff2")
}