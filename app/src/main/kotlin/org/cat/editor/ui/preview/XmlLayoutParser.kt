package org.cat.editor.ui.preview

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.util.Stack

/**
 * Intelligent parser that converts Android XML layout files into ComposeNode AST.
 * Allows Android XML layouts to be rendered natively on the exact same high-fidelity
 * Android Studio-grade canvas as Compose previews.
 */
object XmlLayoutParser {

    fun isLayoutXml(content: String?): Boolean {
        if (content.isNullOrBlank()) return false
        val trimmed = content.trim()
        if (!trimmed.startsWith("<")) return false
        if (trimmed.contains("<resources") || trimmed.contains("<manifest") || trimmed.contains("<vector")) {
            return false
        }
        return trimmed.contains("schemas.android.com/apk/res/android") ||
                trimmed.contains("LinearLayout") ||
                trimmed.contains("RelativeLayout") ||
                trimmed.contains("FrameLayout") ||
                trimmed.contains("ConstraintLayout") ||
                trimmed.contains("ScrollView") ||
                trimmed.contains("TextView") ||
                trimmed.contains("Button")
    }

    fun parse(xmlContent: String?): ComposeNode {
        if (xmlContent.isNullOrBlank()) {
            val empty = ComposeNode("Box")
            empty.addChild(ComposeNode("Text").apply { addAttribute("text", "Empty XML Layout") })
            return empty
        }

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))

            var rootNode: ComposeNode? = null
            val stack = Stack<ComposeNode>()

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val rawTag = parser.name
                    val node = createNodeFromTag(rawTag)

                    // Extract XML attributes
                    val count = parser.attributeCount
                    var width = ""
                    var height = ""
                    var weight = ""
                    var padding = ""
                    var margin = ""
                    var bg = ""
                    var orientation = "vertical"

                    for (i in 0 until count) {
                        val attrName = parser.getAttributeName(i)
                        val attrVal = parser.getAttributeValue(i)

                        when (attrName) {
                            "android:text", "text" -> node.addAttribute("text", cleanResourceRef(attrVal))
                            "android:hint", "hint" -> node.addAttribute("placeholder", cleanResourceRef(attrVal))
                            "android:textSize", "textSize" -> node.addAttribute("fontSize", attrVal)
                            "android:textColor", "textColor" -> node.addAttribute("color", attrVal)
                            "android:orientation", "orientation" -> orientation = attrVal
                            "android:layout_width", "layout_width" -> width = attrVal
                            "android:layout_height", "layout_height" -> height = attrVal
                            "android:layout_weight", "layout_weight" -> weight = attrVal
                            "android:padding", "padding" -> padding = attrVal
                            "android:layout_margin", "layout_margin" -> margin = attrVal
                            "android:background", "background" -> bg = attrVal
                            "android:checked", "checked" -> node.addAttribute("checked", attrVal)
                            "app:title", "android:title", "title" -> node.addAttribute("title", cleanResourceRef(attrVal))
                            "app:cardCornerRadius", "cardCornerRadius" -> node.addAttribute("shape", "RoundedCornerShape($attrVal)")
                            "app:cardBackgroundColor", "cardBackgroundColor" -> node.addAttribute("containerColor", attrVal)
                            "android:src", "src", "app:srcCompat", "srcCompat" -> node.addAttribute("src", attrVal)
                        }
                    }

                    // Adjust layout type based on orientation
                    if (rawTag.endsWith("LinearLayout") || rawTag == "LinearLayout") {
                        node.type = if (orientation == "horizontal") "Row" else "Column"
                    }

                    // Apply modifiers
                    if (width == "match_parent" || width == "fill_parent") {
                        node.addModifier("fillMaxWidth()")
                    } else if (width.endsWith("dp")) {
                        node.addModifier("width($width)")
                    }

                    if (height == "match_parent" || height == "fill_parent") {
                        node.addModifier("fillMaxHeight()")
                    } else if (height.endsWith("dp")) {
                        node.addModifier("height($height)")
                    }

                    if (weight.isNotEmpty()) {
                        node.addModifier("weight($weight)")
                    }

                    if (padding.isNotEmpty()) {
                        node.addModifier("padding($padding)")
                    } else if (margin.isNotEmpty()) {
                        node.addModifier("padding($margin)")
                    }

                    if (bg.isNotEmpty()) {
                        node.addModifier("background($bg)")
                    }

                    if (rootNode == null) {
                        rootNode = node
                    } else if (!stack.isEmpty()) {
                        stack.peek().addChild(node)
                    }

                    stack.push(node)
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (!stack.isEmpty()) {
                        stack.pop()
                    }
                }
                eventType = parser.next()
            }

            return rootNode ?: ComposeNode("Column").apply {
                addChild(ComposeNode("Text").apply { addAttribute("text", "XML Layout (Ready)") })
            }
        } catch (e: Exception) {
            val errNode = ComposeNode("Column").apply {
                addModifier("fillMaxWidth()")
                addModifier("padding(16.dp)")
            }
            errNode.addChild(ComposeNode("Text").apply {
                addAttribute("text", "XML Syntax Error: " + (e.message ?: "Invalid XML"))
            })
            return errNode
        }
    }

    private fun createNodeFromTag(rawTag: String): ComposeNode {
        val simpleTag = rawTag.substringAfterLast('.')
        return when (simpleTag) {
            "LinearLayout" -> ComposeNode("Column")
            "RelativeLayout", "FrameLayout", "ConstraintLayout", "CoordinatorLayout" -> ComposeNode("Box")
            "ScrollView" -> ComposeNode("Column")
            "HorizontalScrollView" -> ComposeNode("Row")
            "TextView", "AppCompatTextView" -> ComposeNode("Text")
            "Button", "AppCompatButton", "MaterialButton" -> ComposeNode("Button")
            "ImageView", "AppCompatImageView" -> ComposeNode("Image")
            "EditText", "AppCompatEditText", "TextInputEditText", "TextInputLayout" -> ComposeNode("TextField")
            "CheckBox", "AppCompatCheckBox" -> ComposeNode("Checkbox")
            "Switch", "SwitchCompat", "MaterialSwitch" -> ComposeNode("Switch")
            "RadioButton", "AppCompatRadioButton" -> ComposeNode("RadioButton")
            "ProgressBar" -> ComposeNode("CircularProgressIndicator")
            "CardView", "MaterialCardView" -> ComposeNode("Card")
            "FloatingActionButton" -> ComposeNode("FloatingActionButton")
            "MaterialDivider" -> ComposeNode("Divider")
            "Chip" -> ComposeNode("Chip")
            "Slider" -> ComposeNode("Slider")
            "Toolbar", "MaterialToolbar" -> ComposeNode("TopAppBar")
            "BottomNavigationView", "NavigationBarView" -> ComposeNode("NavigationBar")
            "View" -> ComposeNode("Divider")
            else -> ComposeNode(simpleTag)
        }
    }

    private fun cleanResourceRef(res: String): String {
        if (res.startsWith("@string/")) {
            val name = res.substringAfter("@string/")
            return name.replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        return res
    }
}
