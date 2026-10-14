package org.cat.editor.ui.preview

import java.io.Serializable

/**
 * Dedicated AST node representing an Android XML layout element.
 * Preserves exact XML tag names (e.g. com.google.android.material.card.MaterialCardView, LinearLayout, TextView),
 * android:id, and all XML attributes in their original format.
 */
class XmlNode(
    var tagName: String = "View"
) : Serializable {

    val simpleName: String
        get() = tagName.substringAfterLast('.')

    var id: String? = null
    var shortId: String? = null

    val attributes = LinkedHashMap<String, String>()
    val children = mutableListOf<XmlNode>()
    var parent: XmlNode? = null
    var lineNumber: Int = 0
    var rawSource: String = ""

    fun addAttribute(key: String, value: String) {
        val trimmedKey = key.trim()
        val trimmedVal = value.trim()
        attributes[trimmedKey] = trimmedVal
        if (trimmedKey == "android:id" || trimmedKey == "id") {
            id = trimmedVal
            shortId = trimmedVal.substringAfterLast('/')
        }
    }

    fun getAttribute(key: String, fallback: String = ""): String {
        return attributes[key]
            ?: attributes["android:$key"]
            ?: attributes["app:$key"]
            ?: fallback
    }

    fun hasAttribute(key: String): Boolean {
        return attributes.containsKey(key) ||
                attributes.containsKey("android:$key") ||
                attributes.containsKey("app:$key")
    }

    fun isViewGroup(): Boolean {
        val simple = simpleName.lowercase()
        return simple.endsWith("layout") ||
                simple.endsWith("viewgroup") ||
                simple.endsWith("group") ||
                (simple.endsWith("view") && (simple.contains("card") || simple.contains("scroll"))) ||
                simple in listOf(
                    "linearlayout", "relativelayout", "framelayout", "constraintlayout",
                    "coordinatorlayout", "scrollview", "horizontalscrollview", "cardview",
                    "materialcardview", "toolbar", "materialtoolbar", "bottomnavigationview",
                    "navigationbarview", "navigationview", "appbarlayout", "radiogroup", "chipgroup"
                )
    }
}
