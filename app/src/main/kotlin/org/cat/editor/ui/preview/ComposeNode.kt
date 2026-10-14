package org.cat.editor.ui.preview

import java.io.Serializable

/**
 * Represents a parsed Jetpack Compose or XML layout AST element/node.
 * Supports layouts, containers, Material 3 components, controls, and spacers.
 */
open class ComposeNode(
    var type: String = "Box"
) : Serializable {

    var attributes: MutableMap<String, String> = mutableMapOf()
    var modifiers: MutableList<String> = mutableListOf()
    var children: MutableList<ComposeNode> = mutableListOf()
    var rawSource: String = ""
    var lineNumber: Int = 0

    fun addAttribute(key: String?, value: String?) {
        if (!key.isNullOrBlank() && value != null) {
            attributes[key.trim()] = value.trim()
        }
    }

    fun getAttribute(key: String, fallback: String = ""): String {
        return attributes[key] ?: fallback
    }

    fun addModifier(modifier: String?) {
        if (!modifier.isNullOrBlank()) {
            modifiers.add(modifier.trim())
        }
    }

    fun hasModifier(name: String): Boolean {
        return modifiers.any { it.startsWith(name) }
    }

    fun getModifierValue(name: String): String? {
        for (m in modifiers) {
            if (m.startsWith("$name(")) {
                val start = m.indexOf('(')
                val end = m.lastIndexOf(')')
                if (start != -1 && end > start) {
                    return m.substring(start + 1, end).trim()
                }
            }
        }
        return null
    }

    fun addChild(child: ComposeNode?) {
        if (child != null) {
            children.add(child)
        }
    }

    fun isContainer(): Boolean {
        val lower = type.lowercase()
        return lower in listOf(
            "column", "row", "box", "surface", "card", "elevatedcard", "outlinedcard",
            "scaffold", "lazycolumn", "lazyrow", "linearlayout", "framelayout",
            "relativelayout", "constraintlayout", "scrollview", "horizontalscrollview"
        )
    }
}
