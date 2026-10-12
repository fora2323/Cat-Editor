package org.cat.editor.ui

import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import com.expressive.refresh.ExpressivePullToRefresh
import com.google.android.material.loadingindicator.LoadingIndicator

@Composable
fun ExpressivePullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    indicatorStyle: Int = ExpressivePullToRefresh.STYLE_CONTAINED,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: @Composable () -> Unit
) {
    val currentContent by rememberUpdatedState(content)
    val currentOnRefresh by rememberUpdatedState(onRefresh)
    val parentComposition = rememberCompositionContext()
    val indicatorColorArgb = indicatorColor.toArgb()
    val containerColorArgb = containerColor.toArgb()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            ExpressivePullToRefresh(context).apply {
                setIndicatorStyle(indicatorStyle)

                val composeView = ComposeView(context).apply {
                    setParentCompositionContext(parentComposition)
                    setContent {
                        currentContent()
                    }
                }
                addView(
                    composeView,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                setOnRefreshListener {
                    currentOnRefresh()
                }

                applyColors(this, indicatorColorArgb, containerColorArgb)
            }
        },
        update = { view ->
            if (view.isRefreshing != isRefreshing) {
                view.isRefreshing = isRefreshing
            }
            if (view.indicatorStyle != indicatorStyle) {
                view.indicatorStyle = indicatorStyle
            }
            applyColors(view, indicatorColorArgb, containerColorArgb)

            val composeView = view.getChildAt(0) as? ComposeView
            composeView?.setContent {
                currentContent()
            }
        }
    )
}

private fun applyColors(
    refreshView: ExpressivePullToRefresh,
    indicatorColorArgb: Int,
    containerColorArgb: Int
) {
    var found = false
    for (i in 0 until refreshView.childCount) {
        val child = refreshView.getChildAt(i)
        if (child is LoadingIndicator) {
            child.setIndicatorColor(indicatorColorArgb)
            child.setContainerColor(containerColorArgb)
            found = true
        }
    }
    if (!found) {
        try {
            val field = ExpressivePullToRefresh::class.java.getDeclaredField("mLoadingIndicator")
            field.isAccessible = true
            val indicator = field.get(refreshView) as? LoadingIndicator
            indicator?.setIndicatorColor(indicatorColorArgb)
            indicator?.setContainerColor(containerColorArgb)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}