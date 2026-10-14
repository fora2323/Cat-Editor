package org.cat.editor.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * High-level Composable for rendering XML Layout previews.
 * Delegates to the unified PreviewScreen.
 */
@Composable
fun PreviewLayoutXml(
    filePath: String,
    modifier: Modifier = Modifier
) {
    PreviewScreen(activeFilePath = filePath, modifier = modifier)
}