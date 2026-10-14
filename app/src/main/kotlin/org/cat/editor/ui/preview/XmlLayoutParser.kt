package org.cat.editor.ui.preview

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.util.Stack

/**
 * Intelligent parser that converts Android XML layout files into an authentic XmlNode AST.
 * Preserves exact Android layout tags, namespaces, attributes, and element hierarchy.
 */
object XmlLayoutParser {

    fun isLayoutXml(content: String?): Boolean {
        if (content.isNullOrBlank()) return false
        val trimmed = content.trim()
        if (!trimmed.startsWith("<")) return false
        if (trimmed.contains("<resources") || trimmed.contains("<manifest") || trimmed.contains("<vector") || trimmed.contains("<shape")) {
            return false
        }
        return trimmed.contains("schemas.android.com/apk/res/android") ||
                trimmed.contains("LinearLayout") ||
                trimmed.contains("RelativeLayout") ||
                trimmed.contains("FrameLayout") ||
                trimmed.contains("ConstraintLayout") ||
                trimmed.contains("ScrollView") ||
                trimmed.contains("TextView") ||
                trimmed.contains("Button") ||
                trimmed.contains("CardView") ||
                trimmed.contains("MaterialCardView")
    }

    fun parse(xmlContent: String?): XmlNode {
        if (xmlContent.isNullOrBlank()) {
            val empty = XmlNode("LinearLayout").apply {
                addAttribute("android:orientation", "vertical")
                val textNode = XmlNode("TextView").apply {
                    addAttribute("android:text", "Empty XML Layout")
                }
                children.add(textNode)
                textNode.parent = this
            }
            return empty
        }

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))

            var rootNode: XmlNode? = null
            val stack = Stack<XmlNode>()

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val rawTag = parser.name
                    val node = XmlNode(rawTag)
                    node.lineNumber = parser.lineNumber

                    // Extract all attributes preserving names and values
                    val count = parser.attributeCount
                    for (i in 0 until count) {
                        val attrName = parser.getAttributeName(i)
                        val attrVal = parser.getAttributeValue(i)
                        node.addAttribute(attrName, attrVal)
                    }

                    if (rootNode == null) {
                        rootNode = node
                    } else if (!stack.isEmpty()) {
                        val parent = stack.peek()
                        parent.children.add(node)
                        node.parent = parent
                    }

                    stack.push(node)
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (!stack.isEmpty()) {
                        stack.pop()
                    }
                }
                eventType = parser.next()
            }

            return rootNode ?: XmlNode("LinearLayout").apply {
                addAttribute("android:orientation", "vertical")
                val textNode = XmlNode("TextView").apply {
                    addAttribute("android:text", "XML Layout (Ready)")
                }
                children.add(textNode)
                textNode.parent = this
            }
        } catch (e: Exception) {
            val errNode = XmlNode("LinearLayout").apply {
                addAttribute("android:orientation", "vertical")
                addAttribute("android:padding", "16dp")
                val textNode = XmlNode("TextView").apply {
                    addAttribute("android:text", "XML Syntax Error: " + (e.message ?: "Invalid XML"))
                    addAttribute("android:textColor", "#D32F2F")
                }
                children.add(textNode)
                textNode.parent = this
            }
            return errNode
        }
    }
}
