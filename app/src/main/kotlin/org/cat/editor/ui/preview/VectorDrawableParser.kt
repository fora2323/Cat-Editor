package org.cat.editor.ui.preview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.util.Log
import com.caverock.androidsvg.SVG
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

private const val TAG = "VectorDrawableParser"

data class VectorDrawableMeta(
    val width: String = "",
    val height: String = "",
    val viewportWidth: String = "",
    val viewportHeight: String = "",
    val infoText: String = ""
)

/**
 * High-performance parser that converts Android VectorDrawable XML into SVG
 * and renders it into a Bitmap using AndroidSVG.
 * Handles nested groups, rotation/scale pivots, clip-paths, and theme/resource colors.
 */
class VectorDrawableParser(
    private val xmlContent: String,
    private val filePath: String = "",
    private val context: Context? = null
) {
    var width: String = ""
        private set
    var height: String = ""
        private set
    var viewportWidth: String = ""
        private set
    var viewportHeight: String = ""
        private set
    var tint: String = ""
        private set
    var alpha: String = ""
        private set

    init {
        parseMetadata()
    }

    private fun parseMetadata() {
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = false
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(ByteArrayInputStream(xmlContent.toByteArray(Charsets.UTF_8)))
            val root = doc.documentElement ?: return

            if (root.tagName.contains("vector", ignoreCase = true)) {
                width = getAttr(root, "android:width", "width")
                height = getAttr(root, "android:height", "height")
                viewportWidth = getAttr(root, "android:viewportWidth", "viewportWidth")
                viewportHeight = getAttr(root, "android:viewportHeight", "viewportHeight")
                tint = getAttr(root, "android:tint", "tint")
                alpha = getAttr(root, "android:alpha", "alpha")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error parsing vector root metadata: ${e.message}")
        }
    }

    fun getMeta(): VectorDrawableMeta {
        val w = if (width.isNotBlank()) width else "24dp"
        val h = if (height.isNotBlank()) height else "24dp"
        val vw = if (viewportWidth.isNotBlank()) viewportWidth else parseDimensionValue(w).toInt().toString()
        val vh = if (viewportHeight.isNotBlank()) viewportHeight else parseDimensionValue(h).toInt().toString()

        val sb = StringBuilder()
        if (width.isNotBlank() && height.isNotBlank()) {
            sb.append("$width × $height")
        }
        if (viewportWidth.isNotBlank() && viewportHeight.isNotBlank()) {
            if (sb.isNotEmpty()) sb.append(" • ")
            sb.append("vp ${viewportWidth}×${viewportHeight}")
        }
        val info = if (sb.isNotEmpty()) sb.toString() else "Vector Drawable"
        return VectorDrawableMeta(
            width = w,
            height = h,
            viewportWidth = vw,
            viewportHeight = vh,
            infoText = info
        )
    }

    private fun parseDimensionValue(dimStr: String): Float {
        val num = dimStr.replace(Regex("[^0-9.]"), "").toFloatOrNull() ?: 24f
        return if (num > 0f) num else 24f
    }

    /**
     * Converts Android VectorDrawable XML into valid SVG XML string.
     * Uses viewBox with 100% width/height so it scales sharply and responsively.
     */
    fun toSvg(isDark: Boolean = true): String {
        val meta = getMeta()
        val vw = meta.viewportWidth.toFloatOrNull() ?: parseDimensionValue(meta.width)
        val vh = meta.viewportHeight.toFloatOrNull() ?: parseDimensionValue(meta.height)

        val resolvedTint = resolveColorToHex(tint, isDark)

        val svg = StringBuilder()
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" ")
            .append("viewBox=\"0 0 $vw $vh\" ")
            .append("width=\"100%\" ")
            .append("height=\"100%\" ")
            .append("preserveAspectRatio=\"xMidYMid meet\">\n")

        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = false
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(ByteArrayInputStream(xmlContent.toByteArray(Charsets.UTF_8)))
            val root = doc.documentElement

            if (root != null) {
                renderNodeToSvg(root, svg, resolvedTint, isDark)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error translating vector XML to SVG: ${e.message}")
        }

        svg.append("</svg>")
        return svg.toString()
    }

    private fun renderNodeToSvg(
        element: Element,
        svg: StringBuilder,
        resolvedTint: String,
        isDark: Boolean
    ) {
        val children = element.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node.nodeType != Node.ELEMENT_NODE) continue
            val childEl = node as Element
            val tag = childEl.tagName.lowercase(Locale.ROOT)

            when {
                tag.endsWith("group") -> {
                    val rot = getAttr(childEl, "android:rotation", "rotation")
                    val px = getAttr(childEl, "android:pivotX", "pivotX")
                    val py = getAttr(childEl, "android:pivotY", "pivotY")
                    val sx = getAttr(childEl, "android:scaleX", "scaleX")
                    val sy = getAttr(childEl, "android:scaleY", "scaleY")
                    val tx = getAttr(childEl, "android:translateX", "translateX")
                    val ty = getAttr(childEl, "android:translateY", "translateY")

                    val transform = StringBuilder()
                    if (tx.isNotBlank() || ty.isNotBlank()) {
                        val xVal = tx.toFloatOrNull() ?: 0f
                        val yVal = ty.toFloatOrNull() ?: 0f
                        transform.append("translate($xVal, $yVal) ")
                    }

                    val pivotXVal = px.toFloatOrNull() ?: 0f
                    val pivotYVal = py.toFloatOrNull() ?: 0f
                    val hasPivot = pivotXVal != 0f || pivotYVal != 0f

                    if (hasPivot) {
                        transform.append("translate($pivotXVal, $pivotYVal) ")
                    }

                    if (rot.isNotBlank()) {
                        val rotVal = rot.toFloatOrNull() ?: 0f
                        transform.append("rotate($rotVal) ")
                    }

                    if (sx.isNotBlank() || sy.isNotBlank()) {
                        val sxVal = sx.toFloatOrNull() ?: 1f
                        val syVal = sy.toFloatOrNull() ?: 1f
                        transform.append("scale($sxVal, $syVal) ")
                    }

                    if (hasPivot) {
                        transform.append("translate(-$pivotXVal, -$pivotYVal) ")
                    }

                    val transformAttr = if (transform.isNotBlank()) {
                        " transform=\"${transform.toString().trim()}\""
                    } else ""

                    svg.append("<g$transformAttr>\n")
                    renderNodeToSvg(childEl, svg, resolvedTint, isDark)
                    svg.append("</g>\n")
                }
                tag.endsWith("clip-path") || tag.endsWith("clippath") -> {
                    val pathData = getAttr(childEl, "android:pathData", "pathData")
                    if (pathData.isNotBlank()) {
                        val clipId = "clip_${System.identityHashCode(childEl)}"
                        svg.append("<clipPath id=\"$clipId\"><path d=\"$pathData\" /></clipPath>\n")
                    }
                }
                tag.endsWith("path") -> {
                    val pathData = getAttr(childEl, "android:pathData", "pathData")
                    if (pathData.isBlank()) continue

                    val rawFill = getAttr(childEl, "android:fillColor", "fillColor")
                    val rawStroke = getAttr(childEl, "android:strokeColor", "strokeColor")
                    val fillAlpha = getAttr(childEl, "android:fillAlpha", "fillAlpha")
                    val strokeAlpha = getAttr(childEl, "android:strokeAlpha", "strokeAlpha")
                    val strokeWidth = getAttr(childEl, "android:strokeWidth", "strokeWidth")
                    val lineCap = getAttr(childEl, "android:strokeLineCap", "strokeLineCap")
                    val lineJoin = getAttr(childEl, "android:strokeLineJoin", "strokeLineJoin")
                    val miterLimit = getAttr(childEl, "android:strokeMiterLimit", "strokeMiterLimit")
                    val fillType = getAttr(childEl, "android:fillType", "fillType")

                    val resolvedFill = if (resolvedTint.isNotBlank()) {
                        if (rawFill.isNotBlank() && rawFill != "#00000000") resolvedTint else "none"
                    } else {
                        resolveColorToHex(rawFill, isDark)
                    }

                    val resolvedStroke = if (resolvedTint.isNotBlank()) {
                        if (rawStroke.isNotBlank() && rawStroke != "#00000000") resolvedTint else ""
                    } else {
                        resolveColorToHex(rawStroke, isDark)
                    }

                    svg.append("<path d=\"").append(pathData).append("\" ")

                    if (resolvedFill.isNotBlank() && resolvedFill != "none") {
                        svg.append("fill=\"").append(resolvedFill).append("\" ")
                        if (fillAlpha.isNotBlank()) {
                            svg.append("fill-opacity=\"").append(fillAlpha).append("\" ")
                        }
                    } else if (resolvedStroke.isBlank()) {
                        // Default fallback to black or primary if neither fill nor stroke is defined
                        svg.append("fill=\"#000000\" ")
                    } else {
                        svg.append("fill=\"none\" ")
                    }

                    if (resolvedStroke.isNotBlank() && resolvedStroke != "none") {
                        svg.append("stroke=\"").append(resolvedStroke).append("\" ")
                        val sw = strokeWidth.replace(Regex("[^0-9.]"), "")
                        if (sw.isNotBlank()) {
                            svg.append("stroke-width=\"").append(sw).append("\" ")
                        }
                        if (strokeAlpha.isNotBlank()) {
                            svg.append("stroke-opacity=\"").append(strokeAlpha).append("\" ")
                        }
                        if (lineCap.isNotBlank()) {
                            svg.append("stroke-linecap=\"").append(lineCap.lowercase(Locale.ROOT)).append("\" ")
                        }
                        if (lineJoin.isNotBlank()) {
                            svg.append("stroke-linejoin=\"").append(lineJoin.lowercase(Locale.ROOT)).append("\" ")
                        }
                        if (miterLimit.isNotBlank()) {
                            svg.append("stroke-miterlimit=\"").append(miterLimit).append("\" ")
                        }
                    }

                    if (fillType.equals("evenOdd", ignoreCase = true)) {
                        svg.append("fill-rule=\"evenodd\" ")
                    }

                    svg.append("/>\n")
                }
            }
        }
    }

    private fun resolveColorToHex(colorExpr: String, isDark: Boolean): String {
        if (colorExpr.isBlank()) return ""
        val trimmed = colorExpr.trim()
        if (trimmed == "@null" || trimmed == "@android:color/transparent") return "none"
        if (trimmed.startsWith("#")) {
            return normalizeHexColor(trimmed)
        }

        if (context != null) {
            try {
                val colorInt = ProjectResourceResolver.resolveColor(
                    context = context,
                    filePath = filePath,
                    colorRef = trimmed,
                    isDark = isDark,
                    fallbackColor = Color.TRANSPARENT
                )
                if (colorInt != Color.TRANSPARENT) {
                    val a = Color.alpha(colorInt)
                    val r = Color.red(colorInt)
                    val g = Color.green(colorInt)
                    val b = Color.blue(colorInt)
                    return if (a == 255) {
                        String.format("#%02X%02X%02X", r, g, b)
                    } else if (a == 0) {
                        "none"
                    } else {
                        String.format("#%02X%02X%02X", r, g, b)
                    }
                }
            } catch (_: Throwable) {}
        }

        // Fallback common colors
        return when {
            trimmed.contains("primary", ignoreCase = true) -> if (isDark) "#D0BCFF" else "#6750A4"
            trimmed.contains("surface", ignoreCase = true) -> if (isDark) "#E6E1E5" else "#1D1B20"
            trimmed.contains("controlnormal", ignoreCase = true) || trimmed.contains("icon", ignoreCase = true) || trimmed.contains("tint", ignoreCase = true) ->
                if (isDark) "#E6E0E9" else "#1D1B20"
            trimmed.contains("white", ignoreCase = true) -> "#FFFFFF"
            trimmed.contains("black", ignoreCase = true) -> "#000000"
            else -> if (isDark) "#E6E0E9" else "#49454F"
        }
    }

    private fun normalizeHexColor(hex: String): String {
        var clean = hex.removePrefix("#")
        return when (clean.length) {
            3 -> {
                val r = clean[0].toString().repeat(2)
                val g = clean[1].toString().repeat(2)
                val b = clean[2].toString().repeat(2)
                "#$r$g$b"
            }
            4 -> {
                val a = clean[0].toString()
                val r = clean[1].toString().repeat(2)
                val g = clean[2].toString().repeat(2)
                val b = clean[3].toString().repeat(2)
                if (a == "0") "none" else "#$r$g$b"
            }
            8 -> {
                val a = clean.substring(0, 2)
                val rgb = clean.substring(2)
                if (a.equals("00", ignoreCase = true)) "none" else "#$rgb"
            }
            6 -> "#$clean"
            else -> "#000000"
        }
    }

    private fun dpToPx(dpStr: String): Int {
        val num = dpStr.replace(Regex("[^0-9.]"), "").toFloatOrNull() ?: 24f
        val density = context?.resources?.displayMetrics?.density ?: 2.5f
        return (num * density).toInt()
    }

    private fun getAttr(element: Element, vararg names: String): String {
        for (name in names) {
            val v = element.getAttribute(name)
            if (!v.isNullOrBlank()) return v
        }
        return ""
    }

    /**
     * Renders this VectorDrawable directly into an Android Bitmap.
     * Scales smoothly to fill target dimensions while keeping aspect ratio.
     */
    fun renderBitmap(targetWidthPx: Int = 512, targetHeightPx: Int = 512, isDark: Boolean = true): Bitmap? {
        return try {
            val svgStr = toSvg(isDark)
            val svgObj = SVG.getFromString(svgStr)
            svgObj.setDocumentWidth("100%")
            svgObj.setDocumentHeight("100%")

            val bitmap = Bitmap.createBitmap(targetWidthPx, targetHeightPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val pic = svgObj.renderToPicture(targetWidthPx, targetHeightPx)
            canvas.drawPicture(pic)
            bitmap
        } catch (e: Throwable) {
            Log.e(TAG, "Error rendering vector to bitmap: ${e.message}", e)
            null
        }
    }

    companion object {
        fun isVectorXml(content: String): Boolean {
            val lower = content.lowercase(Locale.ROOT)
            return lower.contains("<vector") && (lower.contains("viewportwidth") || lower.contains("pathdata") || lower.contains("path"))
        }

        fun isDrawableXml(content: String): Boolean {
            val lower = content.lowercase(Locale.ROOT)
            return isVectorXml(content) || lower.contains("<shape") || lower.contains("<layer-list") || lower.contains("<selector")
        }

        fun getResourceBaseName(fileName: String): String {
            val name = File(fileName).name
            return name.substringBeforeLast('.')
        }
    }
}
