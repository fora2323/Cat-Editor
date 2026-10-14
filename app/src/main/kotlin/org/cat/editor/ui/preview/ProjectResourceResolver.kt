package org.cat.editor.ui.preview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.core.graphics.PathParser
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader
import java.util.Locale
import java.util.regex.Pattern

/**
 * Intelligent resource resolver for Android projects.
 * Locates and decodes drawables (XML vector drawables, shape gradients, PNG, JPG, WEBP),
 * theme attributes (?attr/colorSurface, ?attr/colorSurfaceContainerHigh, etc.),
 * colors from colors.xml, and strings from strings.xml.
 * Strictly distinguishes color backgrounds from image assets, preventing unintended fallback icon backgrounds.
 */
object ProjectResourceResolver {

    private val colorCache = mutableMapOf<String, Int>()
    private val stringCache = mutableMapOf<String, String>()

    /**
     * Resolves the nearest Android `res/` directory by traversing parent directories.
     */
    fun findResDir(filePath: String): File? {
        if (filePath.isBlank()) return null
        var curr: File? = File(filePath).parentFile
        var count = 0
        while (curr != null && count < 8) {
            val directRes = File(curr, "res")
            if (directRes.exists() && directRes.isDirectory) return directRes
            val srcMainRes = File(curr, "src/main/res")
            if (srcMainRes.exists() && srcMainRes.isDirectory) return srcMainRes
            val appSrcMainRes = File(curr, "app/src/main/res")
            if (appSrcMainRes.exists() && appSrcMainRes.isDirectory) return appSrcMainRes
            curr = curr.parentFile
            count++
        }
        return null
    }

    /**
     * Checks if a string represents a color or theme attribute rather than a drawable asset.
     */
    fun isColorExpression(ref: String?): Boolean {
        if (ref.isNullOrBlank()) return false
        val trimmed = ref.trim()
        return trimmed.startsWith("#") ||
                trimmed.startsWith("?") ||
                trimmed.startsWith("@color/") ||
                trimmed.startsWith("@android:color/") ||
                trimmed.contains("Color.", ignoreCase = true) ||
                trimmed.startsWith("0x", ignoreCase = true) ||
                trimmed.equals("@null", ignoreCase = true) ||
                trimmed.equals("@android:color/transparent", ignoreCase = true)
    }

    /**
     * Resolves a drawable reference (e.g. "@drawable/puzzle", "puzzle", "R.drawable.puzzle")
     * into a high-fidelity Drawable.
     * When isBackground is true, strictly avoids falling back to menu icons.
     */
    fun resolveDrawable(
        context: Context,
        filePath: String,
        drawableRef: String?,
        isDark: Boolean = false,
        tintColor: Int? = null,
        isBackground: Boolean = false
    ): Drawable? {
        if (drawableRef.isNullOrBlank()) return null
        val trimmed = drawableRef.trim()

        // 1. If it's a color attribute or hex color, return a solid color/gradient drawable
        if (isColorExpression(trimmed)) {
            val color = resolveColor(context, filePath, trimmed, isDark, Color.TRANSPARENT)
            return if (color != Color.TRANSPARENT) {
                GradientDrawable().apply { setColor(color) }
            } else null
        }

        val cleanName = cleanResourceName(trimmed)

        // 2. Special handling for circular accent shapes (e.g. @drawable/shape_circle_accent)
        if (cleanName.contains("shape_circle") || cleanName.contains("circle_accent") || cleanName == "circle") {
            val circleColor = if (isDark) Color.parseColor("#4F378B") else Color.parseColor("#EADDFF")
            return GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(circleColor)
            }
        }

        // 3. Check for actual drawable file on disk
        val resDir = findResDir(filePath)
        if (resDir != null && resDir.exists()) {
            val drawableFile = findDrawableFile(resDir, cleanName)
            if (drawableFile != null && drawableFile.exists()) {
                val decoded = decodeDrawableFile(context, drawableFile, isDark, tintColor)
                if (decoded != null) return decoded
            }
        }

        // 4. If this is a background and not found on disk, NEVER return a menu icon!
        if (isBackground) {
            return null
        }

        // 5. For image/icon content, check custom authentic vector icons first
        val customVector = createCustomVectorDrawable(context, cleanName, tintColor, isDark)
        if (customVector != null) return customVector

        // 6. Fallback to basic Android platform icons
        return getFallbackAndroidIcon(context, cleanName, tintColor)
    }

    /**
     * Creates authentic Material Design vector icons matching Android Studio preview.
     */
    private fun createCustomVectorDrawable(
        context: Context,
        name: String,
        tintColor: Int?,
        isDark: Boolean
    ): Drawable? {
        val lower = name.lowercase(Locale.ROOT)
        val defaultTint = if (isDark) Color.parseColor("#EADDFF") else Color.parseColor("#4F378B")
        val color = tintColor ?: defaultTint

        return when {
            // Manage Blocks: Official Material jigsaw puzzle piece (extension)
            lower.contains("puzzle") || lower.contains("block") -> {
                renderVectorPath(
                    context = context,
                    pathData = "M20.5,11 H19 V7 c0,-1.1 -0.9,-2 -2,-2 h-4 V3.5 C13,2.12 11.88,1 10.5,1 S8,2.12 8,3.5 V5 H4 C2.9,5 2.01,5.9 2.01,7 v3.8 H3.5 c1.49,0 2.7,1.21 2.7,2.7 s-1.21,2.7 -2.7,2.7 H2 V20 c0,1.1 0.9,2 2,2 h3.8 v-1.5 c0,-1.49 1.21,-2.7 2.7,-2.7 1.49,0 2.7,1.21 2.7,2.7 V22 H17 c1.1,0 2,-0.9 2,-2 v-4 h1.5 c1.38,0 2.5,-1.12 2.5,-2.5 s-1.12,-2.5 -2.5,-2.5 z",
                    strokeColor = Color.TRANSPARENT,
                    strokeWidth = 0f,
                    fillColor = color
                )
            }
            // Manage Categories: 4 rounded squares in 2x2 grid (category_2)
            lower.contains("category") -> {
                renderVectorPath(
                    context = context,
                    pathData = "M5,3 h4 c1.1,0 2,0.9 2,2 v4 c0,1.1 -0.9,2 -2,2 H5 c-1.1,0 -2,-0.9 -2,-2 V5 c0,-1.1 0.9,-2 2,-2 z m10,0 h4 c1.1,0 2,0.9 2,2 v4 c0,1.1 -0.9,2 -2,2 h-4 c-1.1,0 -2,-0.9 -2,-2 V5 c0,-1.1 0.9,-2 2,-2 z M5,13 h4 c1.1,0 2,0.9 2,2 v4 c0,1.1 -0.9,2 -2,2 H5 c-1.1,0 -2,-0.9 -2,-2 v-4 c0,-1.1 0.9,-2 2,-2 z m10,0 h4 c1.1,0 2,0.9 2,2 v4 c0,1.1 -0.9,2 -2,2 h-4 c-1.1,0 -2,-0.9 -2,-2 v-4 c0,-1.1 0.9,-2 2,-2 z",
                    strokeColor = Color.TRANSPARENT,
                    strokeWidth = 0f,
                    fillColor = color
                )
            }
            // Manage Widgets: 3 rounded squares in 2x2 grid + plus sign at bottom right (layout_grid_add)
            lower.contains("widget") || lower.contains("layout_grid") -> {
                renderVectorPath(
                    context = context,
                    pathData = "M5,3 h4 c1.1,0 2,0.9 2,2 v4 c0,1.1 -0.9,2 -2,2 H5 c-1.1,0 -2,-0.9 -2,-2 V5 c0,-1.1 0.9,-2 2,-2 z m10,0 h4 c1.1,0 2,0.9 2,2 v4 c0,1.1 -0.9,2 -2,2 h-4 c-1.1,0 -2,-0.9 -2,-2 V5 c0,-1.1 0.9,-2 2,-2 z M5,13 h4 c1.1,0 2,0.9 2,2 v4 c0,1.1 -0.9,2 -2,2 H5 c-1.1,0 -2,-0.9 -2,-2 v-4 c0,-1.1 0.9,-2 2,-2 z M17,13 v3 h-3 v2 h3 v3 h2 v-3 h3 v-2 h-3 v-3 z",
                    strokeColor = Color.TRANSPARENT,
                    strokeWidth = 0f,
                    fillColor = color
                )
            }
            else -> null
        }
    }

    private fun renderVectorPath(
        context: Context,
        pathData: String,
        strokeColor: Int,
        strokeWidth: Float,
        fillColor: Int
    ): Drawable? {
        return try {
            val density = context.resources.displayMetrics.density
            val sizePx = (32 * density).toInt()
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val scale = sizePx / 24f
            canvas.scale(scale, scale)

            val androidPath = PathParser.createPathFromPathData(pathData)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            if (fillColor != Color.TRANSPARENT) {
                paint.style = Paint.Style.FILL
                paint.color = fillColor
                canvas.drawPath(androidPath, paint)
            }

            if (strokeWidth > 0f && strokeColor != Color.TRANSPARENT) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = strokeWidth
                paint.color = strokeColor
                canvas.drawPath(androidPath, paint)
            }

            BitmapDrawable(context.resources, bitmap)
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Searches drawable folders for matching files (xml, png, jpg, webp, svg).
     */
    private fun findDrawableFile(resDir: File, name: String): File? {
        val candidateFolders = resDir.listFiles { f ->
            f.isDirectory && (f.name.startsWith("drawable") || f.name.startsWith("mipmap"))
        } ?: return null

        val extensions = listOf(".xml", ".png", ".webp", ".jpg", ".jpeg", ".svg")
        for (folder in candidateFolders) {
            for (ext in extensions) {
                val f = File(folder, name + ext)
                if (f.exists() && f.isFile) return f
            }
        }
        return null
    }

    /**
     * Decodes a drawable file from disk (Vector XML, Shape XML, or Bitmap image).
     */
    private fun decodeDrawableFile(
        context: Context,
        file: File,
        isDark: Boolean,
        tintColor: Int?
    ): Drawable? {
        val ext = file.extension.lowercase(Locale.ROOT)
        if (ext in listOf("png", "jpg", "jpeg", "webp")) {
            try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
                val d = BitmapDrawable(context.resources, bitmap)
                if (tintColor != null) {
                    d.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
                }
                return d
            } catch (_: Throwable) {
                return null
            }
        }

        if (ext == "xml") {
            try {
                val content = file.readText()
                if (content.contains("<vector")) {
                    return renderVectorDrawable(context, content, tintColor)
                } else if (content.contains("<shape")) {
                    return renderShapeDrawable(context, content, isDark)
                }
            } catch (_: Throwable) {
                return null
            }
        }

        return null
    }

    /**
     * Renders an Android Vector Drawable (<vector><path.../></vector>) to a BitmapDrawable
     * using AndroidX PathParser.
     */
    private fun renderVectorDrawable(context: Context, xmlContent: String, tintColor: Int?): Drawable? {
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))

            var viewportWidth = 24f
            var viewportHeight = 24f
            var widthDp = 24f
            var heightDp = 24f

            data class PathData(
                val pathString: String,
                val fillColor: Int,
                val strokeColor: Int,
                val strokeWidth: Float
            )

            val paths = mutableListOf<PathData>()
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val tagName = parser.name
                    if (tagName == "vector") {
                        val vw = parser.getAttributeValue(null, "android:viewportWidth")
                            ?: parser.getAttributeValue(null, "viewportWidth")
                        val vh = parser.getAttributeValue(null, "android:viewportHeight")
                            ?: parser.getAttributeValue(null, "viewportHeight")
                        val w = parser.getAttributeValue(null, "android:width")
                            ?: parser.getAttributeValue(null, "width")
                        val h = parser.getAttributeValue(null, "android:height")
                            ?: parser.getAttributeValue(null, "height")

                        vw?.toFloatOrNull()?.let { viewportWidth = it }
                        vh?.toFloatOrNull()?.let { viewportHeight = it }
                        w?.replace(Regex("[^0-9.]"), "")?.toFloatOrNull()?.let { widthDp = it }
                        h?.replace(Regex("[^0-9.]"), "")?.toFloatOrNull()?.let { heightDp = it }
                    } else if (tagName == "path") {
                        val pathStr = parser.getAttributeValue(null, "android:pathData")
                            ?: parser.getAttributeValue(null, "pathData")
                        if (!pathStr.isNullOrBlank()) {
                            val fillStr = parser.getAttributeValue(null, "android:fillColor")
                                ?: parser.getAttributeValue(null, "fillColor")
                            val strokeStr = parser.getAttributeValue(null, "android:strokeColor")
                                ?: parser.getAttributeValue(null, "strokeColor")
                            val strokeWStr = parser.getAttributeValue(null, "android:strokeWidth")
                                ?: parser.getAttributeValue(null, "strokeWidth")

                            val fill = if (!fillStr.isNullOrBlank()) parseHexColor(fillStr, Color.TRANSPARENT) else Color.TRANSPARENT
                            val stroke = if (!strokeStr.isNullOrBlank()) parseHexColor(strokeStr, Color.TRANSPARENT) else Color.TRANSPARENT
                            val strokeW = strokeWStr?.toFloatOrNull() ?: 0f

                            paths.add(PathData(pathStr, fill, stroke, strokeW))
                        }
                    }
                }
                eventType = parser.next()
            }

            if (paths.isEmpty()) return null

            val density = context.resources.displayMetrics.density
            val targetW = (widthDp * density).toInt().coerceIn(32, 256)
            val targetH = (heightDp * density).toInt().coerceIn(32, 256)

            val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val scaleX = targetW / viewportWidth
            val scaleY = targetH / viewportHeight
            canvas.scale(scaleX, scaleY)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            for (p in paths) {
                try {
                    val androidPath = PathParser.createPathFromPathData(p.pathString)
                    var fColor = p.fillColor
                    if (tintColor != null && fColor != Color.TRANSPARENT) {
                        fColor = tintColor
                    } else if (fColor == Color.TRANSPARENT && p.strokeColor == Color.TRANSPARENT) {
                        fColor = tintColor ?: Color.parseColor("#757575")
                    }

                    if (fColor != Color.TRANSPARENT) {
                        paint.style = Paint.Style.FILL
                        paint.color = fColor
                        canvas.drawPath(androidPath, paint)
                    }

                    if (p.strokeWidth > 0 && p.strokeColor != Color.TRANSPARENT) {
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = p.strokeWidth
                        paint.color = tintColor ?: p.strokeColor
                        canvas.drawPath(androidPath, paint)
                    }
                } catch (_: Throwable) {}
            }

            return BitmapDrawable(context.resources, bitmap)
        } catch (_: Throwable) {
            return null
        }
    }

    /**
     * Renders an Android Shape Drawable (<shape><gradient.../><corners.../></shape>) to a GradientDrawable.
     */
    private fun renderShapeDrawable(context: Context, xmlContent: String, isDark: Boolean): Drawable? {
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))

            var startColor: Int? = null
            var endColor: Int? = null
            var centerColor: Int? = null
            var angle = 0
            var solidColor: Int? = null
            var cornerRadius = 0f
            var strokeColor: Int? = null
            var strokeWidth = 0

            val density = context.resources.displayMetrics.density
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "gradient" -> {
                            val start = parser.getAttributeValue(null, "android:startColor") ?: parser.getAttributeValue(null, "startColor")
                            val end = parser.getAttributeValue(null, "android:endColor") ?: parser.getAttributeValue(null, "endColor")
                            val center = parser.getAttributeValue(null, "android:centerColor") ?: parser.getAttributeValue(null, "centerColor")
                            val ang = parser.getAttributeValue(null, "android:angle") ?: parser.getAttributeValue(null, "angle")

                            start?.let { startColor = parseHexColor(it, Color.TRANSPARENT) }
                            end?.let { endColor = parseHexColor(it, Color.TRANSPARENT) }
                            center?.let { centerColor = parseHexColor(it, Color.TRANSPARENT) }
                            ang?.toIntOrNull()?.let { angle = it }
                        }
                        "solid" -> {
                            val color = parser.getAttributeValue(null, "android:color") ?: parser.getAttributeValue(null, "color")
                            color?.let { solidColor = parseHexColor(it, Color.TRANSPARENT) }
                        }
                        "corners" -> {
                            val radius = parser.getAttributeValue(null, "android:radius") ?: parser.getAttributeValue(null, "radius")
                            radius?.replace(Regex("[^0-9.]"), "")?.toFloatOrNull()?.let {
                                cornerRadius = it * density
                            }
                        }
                        "stroke" -> {
                            val color = parser.getAttributeValue(null, "android:color") ?: parser.getAttributeValue(null, "color")
                            val width = parser.getAttributeValue(null, "android:width") ?: parser.getAttributeValue(null, "width")
                            color?.let { strokeColor = parseHexColor(it, Color.TRANSPARENT) }
                            width?.replace(Regex("[^0-9.]"), "")?.toFloatOrNull()?.let {
                                strokeWidth = (it * density).toInt()
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

            val gd: GradientDrawable
            if (startColor != null && endColor != null) {
                val orientation = when (angle) {
                    0 -> GradientDrawable.Orientation.LEFT_RIGHT
                    45 -> GradientDrawable.Orientation.BL_TR
                    90 -> GradientDrawable.Orientation.BOTTOM_TOP
                    135 -> GradientDrawable.Orientation.BR_TL
                    180 -> GradientDrawable.Orientation.RIGHT_LEFT
                    225 -> GradientDrawable.Orientation.TR_BL
                    270 -> GradientDrawable.Orientation.TOP_BOTTOM
                    315 -> GradientDrawable.Orientation.TL_BR
                    else -> GradientDrawable.Orientation.TOP_BOTTOM
                }
                val colors = if (centerColor != null) {
                    intArrayOf(startColor!!, centerColor!!, endColor!!)
                } else {
                    intArrayOf(startColor!!, endColor!!)
                }
                gd = GradientDrawable(orientation, colors)
            } else {
                gd = GradientDrawable()
                if (solidColor != null) {
                    gd.setColor(solidColor!!)
                }
            }

            if (cornerRadius > 0f) {
                gd.cornerRadius = cornerRadius
            }
            if (strokeWidth > 0 && strokeColor != null) {
                gd.setStroke(strokeWidth, strokeColor!!)
            }

            return gd
        } catch (_: Throwable) {
            return null
        }
    }

    /**
     * Fallback to Android platform basic icons based on keyword matching.
     */
    fun getFallbackAndroidIcon(context: Context, name: String, tintColor: Int? = null): Drawable? {
        val lower = name.lowercase(Locale.ROOT)
        val resId = when {
            lower.contains("block") || lower.contains("puzzle") -> android.R.drawable.ic_menu_agenda
            lower.contains("widget") || lower.contains("grid") || lower.contains("layout") -> android.R.drawable.ic_menu_view
            lower.contains("category") || lower.contains("palette") -> android.R.drawable.ic_menu_gallery
            lower.contains("edit") || lower.contains("write") || lower.contains("pen") -> android.R.drawable.ic_menu_edit
            lower.contains("search") || lower.contains("find") -> android.R.drawable.ic_menu_search
            lower.contains("setting") || lower.contains("pref") || lower.contains("config") -> android.R.drawable.ic_menu_preferences
            lower.contains("delete") || lower.contains("trash") -> android.R.drawable.ic_menu_delete
            lower.contains("add") || lower.contains("plus") -> android.R.drawable.ic_input_add
            lower.contains("check") || lower.contains("done") -> android.R.drawable.checkbox_on_background
            lower.contains("close") || lower.contains("cancel") -> android.R.drawable.ic_menu_close_clear_cancel
            lower.contains("info") || lower.contains("help") -> android.R.drawable.ic_dialog_info
            lower.contains("share") -> android.R.drawable.ic_menu_share
            lower.contains("save") -> android.R.drawable.ic_menu_save
            lower.contains("favorite") || lower.contains("heart") || lower.contains("star") -> android.R.drawable.star_on
            lower.contains("home") -> android.R.drawable.ic_menu_myplaces
            lower.contains("person") || lower.contains("user") -> android.R.drawable.ic_menu_myplaces
            else -> android.R.drawable.ic_menu_agenda
        }

        try {
            val d = context.getDrawable(resId) ?: return null
            val mutate = d.mutate()
            if (tintColor != null) {
                mutate.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
            }
            return mutate
        } catch (_: Throwable) {
            return null
        }
    }

    /**
     * Resolves a color string from theme attributes (?attr/colorSurface, ?attr/colorOnSurface, etc.),
     * resource colors (@color/...), or hex values.
     */
    fun resolveColor(
        context: Context,
        filePath: String,
        colorRef: String?,
        isDark: Boolean = false,
        fallbackColor: Int = Color.GRAY
    ): Int {
        if (colorRef.isNullOrBlank()) return fallbackColor
        val trimmed = colorRef.trim()

        if (trimmed.startsWith("#")) {
            return parseHexColor(trimmed, fallbackColor)
        }

        // Resolve Material 3 Theme Attributes (?attr/... or ?android:attr/...)
        if (trimmed.startsWith("?")) {
            val attrName = trimmed.substringAfter("attr/").substringAfter('?')
            val themeColor = resolveThemeAttributeColor(attrName, isDark)
            if (themeColor != null) return themeColor
        }

        val cacheKey = "$filePath:$trimmed:$isDark"
        colorCache[cacheKey]?.let { return it }

        if (trimmed.startsWith("@color/") || trimmed.startsWith("@android:color/")) {
            val colorName = trimmed.substringAfterLast('/')
            val resDir = findResDir(filePath)
            if (resDir != null) {
                val colorVal = readColorFromProject(resDir, colorName, isDark)
                if (colorVal != null) {
                    colorCache[cacheKey] = colorVal
                    return colorVal
                }
            }
        }

        val parsed = parseHexColor(trimmed, fallbackColor)
        colorCache[cacheKey] = parsed
        return parsed
    }

    /**
     * Canonical Google Material 3 Expressive theme attribute resolution.
     */
    private fun resolveThemeAttributeColor(attrName: String, isDark: Boolean): Int? {
        val clean = attrName.lowercase(Locale.ROOT)
        return when {
            clean.contains("colorsurfacecontainerhigh") ->
                if (isDark) Color.parseColor("#2B2930") else Color.parseColor("#ECE6F0")
            clean.contains("colorsurfacecontainerlow") ->
                if (isDark) Color.parseColor("#1D1B20") else Color.parseColor("#F7F2FA")
            clean.contains("colorsurfacecontainer") ->
                if (isDark) Color.parseColor("#211F26") else Color.parseColor("#F3EDF7")
            clean.contains("coloronsurfacevariant") || clean.contains("textcolorsecondary") ->
                if (isDark) Color.parseColor("#CAC4D0") else Color.parseColor("#49454F")
            clean.contains("coloronsurface") || clean.contains("textcolorprimary") ->
                if (isDark) Color.parseColor("#E6E0E9") else Color.parseColor("#1D1B20")
            clean.contains("colorsurface") ->
                if (isDark) Color.parseColor("#141218") else Color.parseColor("#FAF8FD")
            clean.contains("coloronprimarycontainer") ->
                if (isDark) Color.parseColor("#EADDFF") else Color.parseColor("#4F378B")
            clean.contains("colorprimarycontainer") ->
                if (isDark) Color.parseColor("#4F378B") else Color.parseColor("#EADDFF")
            clean.contains("coloronprimary") ->
                if (isDark) Color.parseColor("#381E72") else Color.parseColor("#FFFFFF")
            clean.contains("colorprimary") ->
                if (isDark) Color.parseColor("#D0BCFF") else Color.parseColor("#6750A4")
            clean.contains("coloronsecondarycontainer") ->
                if (isDark) Color.parseColor("#E8DEF8") else Color.parseColor("#1D192B")
            clean.contains("colorsecondarycontainer") ->
                if (isDark) Color.parseColor("#4A4458") else Color.parseColor("#E8DEF8")
            clean.contains("coloronsecondary") ->
                if (isDark) Color.parseColor("#332D41") else Color.parseColor("#FFFFFF")
            clean.contains("colorsecondary") ->
                if (isDark) Color.parseColor("#CCC2DC") else Color.parseColor("#625B71")
            clean.contains("coloroutlinevariant") ->
                if (isDark) Color.parseColor("#49454F") else Color.parseColor("#CAC4D0")
            clean.contains("coloroutline") ->
                if (isDark) Color.parseColor("#938F99") else Color.parseColor("#79747E")
            clean.contains("windowbackground") || clean.contains("background") ->
                if (isDark) Color.parseColor("#141218") else Color.parseColor("#FEF7FF")
            else -> null
        }
    }

    private fun readColorFromProject(resDir: File, colorName: String, isDark: Boolean): Int? {
        val folder = if (isDark) File(resDir, "values-night") else File(resDir, "values")
        val fallbackFolder = File(resDir, "values")

        fun scanFolder(f: File): Int? {
            val colorsFile = File(f, "colors.xml")
            if (colorsFile.exists()) {
                val text = colorsFile.readText()
                val m = Pattern.compile("<color\\s+name\\s*=\\s*\"$colorName\"[^>]*>([^<]+)</color>").matcher(text)
                if (m.find()) {
                    val hex = m.group(1)?.trim() ?: ""
                    return parseHexColor(hex, Color.TRANSPARENT)
                }
            }
            return null
        }

        return scanFolder(folder) ?: scanFolder(fallbackFolder)
    }

    /**
     * Resolves a string reference (e.g. "@string/manage_blocks") from strings.xml.
     */
    fun resolveString(context: Context, filePath: String, stringRef: String?): String {
        if (stringRef.isNullOrBlank()) return ""
        val trimmed = stringRef.trim()

        if (!trimmed.startsWith("@string/")) return trimmed

        val stringName = trimmed.substringAfter("@string/")
        stringCache[stringName]?.let { return it }

        val resDir = findResDir(filePath)
        if (resDir != null) {
            val stringsFile = File(resDir, "values/strings.xml")
            if (stringsFile.exists()) {
                try {
                    val text = stringsFile.readText()
                    val m = Pattern.compile("<string\\s+name\\s*=\\s*\"$stringName\"[^>]*>([\\s\\S]*?)</string>").matcher(text)
                    if (m.find()) {
                        val v = m.group(1)?.trim()?.replace("\\'", "'")?.replace("\\\"", "\"") ?: ""
                        stringCache[stringName] = v
                        return v
                    }
                } catch (_: Throwable) {}
            }
        }

        val fallback = stringName.replace('_', ' ').split(' ')
            .joinToString(" ") { it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() } }
        stringCache[stringName] = fallback
        return fallback
    }

    fun parseHexColor(hex: String?, fallback: Int): Int {
        if (hex.isNullOrBlank()) return fallback
        var clean = hex.trim()
        if (clean.startsWith("#")) clean = clean.substring(1)

        return try {
            when (clean.length) {
                3 -> { // #RGB
                    val r = clean[0].toString().repeat(2)
                    val g = clean[1].toString().repeat(2)
                    val b = clean[2].toString().repeat(2)
                    Color.parseColor("#$r$g$b")
                }
                4 -> { // #ARGB
                    val a = clean[0].toString().repeat(2)
                    val r = clean[1].toString().repeat(2)
                    val g = clean[2].toString().repeat(2)
                    val b = clean[3].toString().repeat(2)
                    Color.parseColor("#$a$r$g$b")
                }
                6 -> Color.parseColor("#$clean")
                8 -> Color.parseColor("#$clean")
                else -> fallback
            }
        } catch (_: Throwable) {
            fallback
        }
    }

    private fun cleanResourceName(name: String): String {
        return name.substringAfterLast('/').substringAfterLast('.').trim()
    }
}
