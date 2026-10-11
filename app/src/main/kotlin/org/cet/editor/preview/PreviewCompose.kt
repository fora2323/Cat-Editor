package org.cet.editor.preview

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.cet.editor.utility.ComposeNode

@Composable
fun PreviewScreen(currentPreviewPath: String, currentRootNode: ComposeNode.Layout) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .drawBehind {
                val dotRadius = 1.5f.dp.toPx()
                val spacing = 24.dp.toPx()
                for (x in 0 until size.width.toInt() step spacing.toInt()) {
                    for (y in 0 until size.height.toInt() step spacing.toInt()) {
                        drawCircle(
                            color = Color(0xFF3A3A3A),
                            radius = dotRadius,
                            center = Offset(x.toFloat(), y.toFloat())
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (currentPreviewPath.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .width(360.dp)
                    .height(720.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .border(4.dp, Color.Black, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    currentRootNode.children.forEach { childNode ->
                        WireframeNode(childNode)
                    }
                }
            }
        } else {
            Text(
                text = "Buka Drawer (Kiri) dan pilih File .kt dengan @Preview",
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun WireframeNode(node: ComposeNode) {
    when (node) {
        is ComposeNode.Layout -> {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .border(1.dp, Color(AndroidColor.parseColor(node.colorHex)), RoundedCornerShape(8.dp))
                    .background(Color(0xFFF0F0F0))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = node.name,
                        color = Color(AndroidColor.parseColor(node.colorHex)),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    if (node.children.isNotEmpty()) {
                        if (node.isHorizontal) {
                            Row { node.children.forEach { WireframeNode(it) } }
                        } else {
                            Column { node.children.forEach { WireframeNode(it) } }
                        }
                    }
                }
            }
        }
        is ComposeNode.Leaf -> {
            Text(
                text = node.text,
                color = Color(AndroidColor.parseColor(node.colorHex)),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}