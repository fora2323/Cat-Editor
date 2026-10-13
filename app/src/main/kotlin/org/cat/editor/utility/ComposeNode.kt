package org.cat.editor.utility

sealed class ComposeNode {
    class Layout(
        @JvmField var name: String,
        @JvmField var children: MutableList<ComposeNode> = mutableListOf(),
        @JvmField var isHorizontal: Boolean = false,
        @JvmField var colorHex: String = "#757575"
    ) : ComposeNode() {
        constructor(name: String) : this(name, mutableListOf(), false, "#757575")
        constructor(name: String, colorHex: String) : this(name, mutableListOf(), false, colorHex)
    }

    class Leaf(
        @JvmField var text: String,
        @JvmField var colorHex: String = "#B0BEC5"
    ) : ComposeNode() {
        constructor(text: String) : this(text, "#B0BEC5")
    }
}
