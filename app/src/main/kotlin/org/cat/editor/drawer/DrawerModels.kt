package org.cat.editor.drawer

import java.io.File

enum class DrawerSortMode(val value: Int) {
    NAME(0),
    SIZE(1),
    DATE(2);

    companion object {
        fun fromInt(value: Int): DrawerSortMode = entries.firstOrNull { it.value == value } ?: NAME
    }
}

data class FileTreeItem(
    val file: File,
    val displayName: String,
    val level: Int,
    val isDirectory: Boolean,
    val isExpanded: Boolean
)