package org.cat.editor.utility

import java.util.Stack
import java.util.regex.Pattern

// Struktur Node UI
sealed class ComposeNode {
    data class Layout(
        val name: String,
        val children: MutableList<ComposeNode> = mutableListOf(),
        val isHorizontal: Boolean = false,
        val colorHex: String = "#757575"
    ) : ComposeNode()

    data class Leaf(
        var text: String,
        val colorHex: String = "#B0BEC5"
    ) : ComposeNode()
}

object IdComposeParser {
    fun parse(rawCode: String): ComposeNode.Layout {
        var code = rawCode.replace(Regex("/\\*([\\s\\S]*?)\\*/"), "").replace(Regex("//.*"), "")
        val root = ComposeNode.Layout("PreviewRoot", colorHex = "#FFFFFF")
        val stack = Stack<ComposeNode.Layout>()
        stack.push(root)

        var lastCreatedLayout: ComposeNode.Layout? = null
        var lastCreatedLeaf: ComposeNode.Leaf? = null

        val p = Pattern.compile("\"([^\"]*)\"|\\b([A-Z][a-zA-Z0-9_]+)\\b(?=\\s*[\\(\\{])|(\\{|\\})")
        val m = p.matcher(code)

        while (m.find()) {
            val strGrp = m.group(1)
            val compGrp = m.group(2)
            val braceGrp = m.group(3)
            val currentParent = stack.peek()

            if (strGrp != null) {
                lastCreatedLeaf?.text = strGrp
            } else if (braceGrp != null) {
                if (braceGrp == "{") {
                    if (lastCreatedLayout != null) stack.push(lastCreatedLayout)
                    else stack.push(currentParent)
                    lastCreatedLayout = null
                    lastCreatedLeaf = null
                } else if (braceGrp == "}") {
                    if (stack.size > 1) stack.pop()
                    lastCreatedLayout = null
                    lastCreatedLeaf = null
                }
            } else if (compGrp != null) {
                var isLayout = true
                var colorHex = "#757575"
                var isHorizontal = false

                when (compGrp) {
                    "Column", "LazyColumn" -> colorHex = "#FF9800"
                    "Row", "LazyRow" -> { colorHex = "#E040FB"; isHorizontal = true }
                    "Box", "Surface", "Card" -> colorHex = "#03A9F4"
                    "Button", "OutlinedButton" -> { colorHex = "#00E676"; isHorizontal = true }
                    "Text", "Icon", "Image", "Spacer" -> { isLayout = false; colorHex = "#B0BEC5" }
                    else -> colorHex = "#F48FB1"
                }

                if (isLayout) {
                    val newLayout = ComposeNode.Layout(compGrp, colorHex = colorHex, isHorizontal = isHorizontal)
                    currentParent.children.add(newLayout)
                    lastCreatedLayout = newLayout
                } else {
                    val newLeaf = ComposeNode.Leaf(compGrp, colorHex = colorHex)
                    currentParent.children.add(newLeaf)
                    lastCreatedLeaf = newLeaf
                }
            }
        }
        return root
    }
}