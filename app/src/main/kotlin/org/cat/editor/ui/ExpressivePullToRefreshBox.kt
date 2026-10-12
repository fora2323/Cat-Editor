package org.cat.editor.ui

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import com.expressive.refresh.ExpressivePullToRefresh

@Composable
fun ExpressivePullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    indicatorStyle: Int = ExpressivePullToRefresh.STYLE_CONTAINED,
    content: @Composable () -> Unit
) {
    val currentContent by rememberUpdatedState(content)
    val currentOnRefresh by rememberUpdatedState(onRefresh)
    val parentComposition = rememberCompositionContext()

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
            }
        },
        update = { view ->
            if (view.isRefreshing != isRefreshing) {
                view.isRefreshing = isRefreshing
            }
            view.setIndicatorStyle(indicatorStyle)
            val composeView = view.getChildAt(0) as? ComposeView
            composeView?.setContent {
                currentContent()
            }
        }
    )
}