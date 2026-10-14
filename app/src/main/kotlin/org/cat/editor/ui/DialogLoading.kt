package org.cat.editor.ui

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.material.loadingindicator.LoadingIndicator

/**
 * Material 3 Expressive LoadingIndicator view wrapper.
 * Uses official com.google.android.material.loadingindicator.LoadingIndicator.
 */
@Composable
fun M3ELoadingIndicator(
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            LoadingIndicator(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        },
        modifier = modifier
    )
}

/**
 * Full-screen transparent overlay with dim and centered M3E LoadingIndicator.
 * Used for in-screen loading states without a separate window Dialog.
 */
@Composable
fun M3ELoadingOverlay(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        M3ELoadingIndicator()
    }
}

/**
 * Universal Material 3 Expressive Loading Dialog.
 *
 * Transparent with background dim, no actual dialog box/card and no text,
 * centering the official Material 3 Expressive LoadingIndicator.
 *
 * @param message Kept for backward compatibility (no text rendered).
 * @param title Kept for backward compatibility (no text rendered).
 * @param canDismiss Whether back press or clicking outside dismisses (default false).
 * @param onDismissRequest Callback invoked when dismissed.
 */
@Composable
fun M3ELoadingDialog(
    message: String? = null,
    title: String? = null,
    canDismiss: Boolean = false,
    onDismissRequest: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = {
            if (canDismiss) onDismissRequest()
        },
        properties = DialogProperties(
            dismissOnBackPress = canDismiss,
            dismissOnClickOutside = canDismiss,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            M3ELoadingIndicator()
        }
    }
}

/**
 * Backward compatible alias for M3ELoadingDialog.
 */
@Composable
fun DialogLoading(
    message: String? = null,
    title: String? = null,
    canDismiss: Boolean = false,
    onDismissRequest: () -> Unit = {}
) {
    M3ELoadingDialog(
        message = message,
        title = title,
        canDismiss = canDismiss,
        onDismissRequest = onDismissRequest
    )
}