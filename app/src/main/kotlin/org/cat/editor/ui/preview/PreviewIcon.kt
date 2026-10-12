package org.cat.editor.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import androidx.compose.ui.layout.ContentScale
import java.io.File

@Composable
fun ImagePreviewIcon(file: File, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(file.path) {
        try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) { null }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = file.name,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

fun isImageFile(fileName: String): Boolean {
    return fileName.endsWith(".png") || fileName.endsWith(".jpg") ||
           fileName.endsWith(".jpeg") || fileName.endsWith(".webp") ||
           fileName.endsWith(".gif")
}

fun isVectorFile(fileName: String): Boolean {
    return fileName.endsWith(".svg")
}