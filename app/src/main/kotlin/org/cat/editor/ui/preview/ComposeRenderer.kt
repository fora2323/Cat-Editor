package org.cat.editor.ui.preview

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.regex.Pattern

/**
 * Transforms a ComposeNode AST tree into an authentic, interactive Material 3 Android View
 * hierarchy with support for gradients, corner radius clipping, typography, and UI inspection.
 */
object ComposeRenderer {

    fun interface OnNodeSelectedListener {
        fun onNodeSelected(node: ComposeNode, view: View)
    }

    class RenderConfig {
        var isDarkMode: Boolean = false
        var filePath: String = ""
        var fontScale: Float = 1.0f
        var isInteractive: Boolean = true
        var isInspectMode: Boolean = false
        var inspectListener: OnNodeSelectedListener? = null

        // Material 3 Expressive Color Palette
        var primary: Int = 0
        var onPrimary: Int = 0
        var primaryContainer: Int = 0
        var onPrimaryContainer: Int = 0
        var secondary: Int = 0
        var secondaryContainer: Int = 0
        var tertiary: Int = 0
        var tertiaryContainer: Int = 0
        var surface: Int = 0
        var onSurface: Int = 0
        var onSurfaceVariant: Int = 0
        var surfaceContainer: Int = 0
        var surfaceContainerLow: Int = 0
        var surfaceContainerHigh: Int = 0
        var background: Int = 0
        var outline: Int = 0
        var outlineVariant: Int = 0

        companion object {
            fun create(darkMode: Boolean, filePath: String = "", fontScale: Float = 1.0f): RenderConfig {
                val c = RenderConfig()
                c.isDarkMode = darkMode
                c.filePath = filePath
                c.fontScale = fontScale

                if (darkMode) {
                    c.primary = Color.parseColor("#53DBC9")
                    c.onPrimary = Color.parseColor("#003731")
                    c.primaryContainer = Color.parseColor("#005048")
                    c.onPrimaryContainer = Color.parseColor("#74F8E5")
                    c.secondary = Color.parseColor("#B1CCC6")
                    c.secondaryContainer = Color.parseColor("#334B47")
                    c.tertiary = Color.parseColor("#ADCAE6")
                    c.tertiaryContainer = Color.parseColor("#2D4960")
                    c.surface = Color.parseColor("#111413")
                    c.onSurface = Color.parseColor("#E0E3E1")
                    c.onSurfaceVariant = Color.parseColor("#C4C7C5")
                    c.surfaceContainer = Color.parseColor("#1D2120")
                    c.surfaceContainerLow = Color.parseColor("#171A19")
                    c.surfaceContainerHigh = Color.parseColor("#282B2A")
                    c.background = Color.parseColor("#0F1413")
                    c.outline = Color.parseColor("#899390")
                    c.outlineVariant = Color.parseColor("#3F4946")
                } else {
                    c.primary = Color.parseColor("#006A60")
                    c.onPrimary = Color.parseColor("#FFFFFF")
                    c.primaryContainer = Color.parseColor("#74F8E5")
                    c.onPrimaryContainer = Color.parseColor("#00201C")
                    c.secondary = Color.parseColor("#4A635E")
                    c.secondaryContainer = Color.parseColor("#CCE8E2")
                    c.tertiary = Color.parseColor("#46617A")
                    c.tertiaryContainer = Color.parseColor("#CDE5FF")
                    c.surface = Color.parseColor("#FBFDFB")
                    c.onSurface = Color.parseColor("#191C1B")
                    c.onSurfaceVariant = Color.parseColor("#444746")
                    c.surfaceContainer = Color.parseColor("#EDF0ED")
                    c.surfaceContainerLow = Color.parseColor("#F3F5F2")
                    c.surfaceContainerHigh = Color.parseColor("#E7EAE7")
                    c.background = Color.parseColor("#F8FAF8")
                    c.outline = Color.parseColor("#6F7976")
                    c.outlineVariant = Color.parseColor("#BFC9C5")
                }
                return c
            }
        }
    }

    fun render(context: Context, rootNode: ComposeNode, config: RenderConfig): View {
        return renderNode(context, rootNode, config) ?: View(context)
    }

    private fun renderNode(context: Context, node: ComposeNode, config: RenderConfig): View? {
        val type = node.type
        var view: View? = when (type) {
            "Column", "LazyColumn" -> renderColumn(context, node, config)
            "Row", "LazyRow" -> renderRow(context, node, config)
            "Box", "Surface" -> renderBox(context, node, config)
            "Card", "ElevatedCard", "OutlinedCard" -> renderCard(context, node, config)
            "Scaffold" -> renderScaffold(context, node, config)
            "Text", "BasicText" -> renderText(context, node, config)
            "Icon" -> renderIcon(context, node, config)
            "Image" -> renderImage(context, node, config)
            "Button", "ElevatedButton", "FilledTonalButton", "OutlinedButton", "TextButton" ->
                renderButton(context, node, config)
            "IconButton" -> renderIconButton(context, node, config)
            "FloatingActionButton" -> renderFab(context, node, config)
            "BasicTextField" -> renderBasicTextField(context, node, config)
            "TextField", "OutlinedTextField" -> renderTextField(context, node, config)
            "Checkbox" -> renderCheckbox(context, node, config)
            "Switch" -> renderSwitch(context, node, config)
            "RadioButton" -> renderRadioButton(context, node, config)
            "Slider" -> renderSlider(context, node, config)
            "Spacer" -> renderSpacer(context, node, config)
            "Divider", "HorizontalDivider" -> renderDivider(context, node, config)
            "LinearProgressIndicator" -> renderLinearProgress(context, node, config)
            "CircularProgressIndicator" -> renderCircularProgress(context, node, config)
            "Chip", "AssistChip", "FilterChip", "SuggestionChip" -> renderChip(context, node, config)
            "TopAppBar", "CenterAlignedTopAppBar", "SmallTopAppBar", "MediumTopAppBar", "LargeTopAppBar" ->
                renderTopAppBar(context, node, config)
            "NavigationBar", "BottomAppBar" -> renderNavigationBar(context, node, config)
            else -> {
                if (type.endsWith("Theme") || node.children.isNotEmpty()) {
                    renderColumn(context, node, config)
                } else {
                    renderCustomComposableBadge(context, node, config)
                }
            }
        }

        if (view != null) {
            applyModifiers(context, view, node, config)
            setupInspectListener(view, node, config)
        }

        return view
    }

    private fun renderCustomComposableBadge(context: Context, node: ComposeNode, config: RenderConfig): View {
        val card = MaterialCardView(context).apply {
            setCardBackgroundColor(config.surfaceContainer)
            strokeColor = config.outlineVariant
            strokeWidth = dpToPx(context, 1)
            radius = dpToPx(context, 8).toFloat()
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val pad = dpToPx(context, 8)
            setPadding(pad, pad, pad, pad)
        }

        val icon = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_agenda)
            setColorFilter(config.primary)
            val s = dpToPx(context, 18)
            layoutParams = LinearLayout.LayoutParams(s, s)
        }
        row.addView(icon)

        val textLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val tlp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                leftMargin = dpToPx(context, 8)
                marginStart = dpToPx(context, 8)
            }
            layoutParams = tlp
        }

        val title = TextView(context).apply {
            text = node.type + "()"
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(config.onSurface)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12 * config.fontScale)
        }
        textLayout.addView(title)

        if (node.attributes.isNotEmpty()) {
            val sub = TextView(context).apply {
                text = node.attributes.entries.joinToString(", ") { "${it.key}: ${it.value}" }
                setTextColor(config.outline)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10 * config.fontScale)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }
            textLayout.addView(sub)
        }

        row.addView(textLayout)
        card.addView(row)

        val clp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val margin = dpToPx(context, 4)
        clp.setMargins(margin, margin, margin, margin)
        card.layoutParams = clp
        return card
    }

    private fun renderColumn(context: Context, node: ComposeNode, config: RenderConfig): LinearLayout {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        for (child in node.children) {
            val childView = renderNode(context, child, config)
            if (childView != null) {
                if (childView.layoutParams == null) {
                    childView.layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                layout.addView(childView)
            }
        }
        return layout
    }

    private fun renderRow(context: Context, node: ComposeNode, config: RenderConfig): LinearLayout {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        for (child in node.children) {
            val childView = renderNode(context, child, config)
            if (childView != null) {
                if (childView.layoutParams == null) {
                    childView.layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                layout.addView(childView)
            }
        }
        return layout
    }

    private fun renderBox(context: Context, node: ComposeNode, config: RenderConfig): FrameLayout {
        val layout = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        if (node.type == "Surface") {
            var surfaceColor = config.surface
            val colorAttr = node.getAttribute("color")
            if (colorAttr.isNotEmpty()) {
                surfaceColor = parseColorExpression(colorAttr, config.surface)
            }
            var cornerRadius = 0
            val shapeAttr = node.getAttribute("shape")
            if (shapeAttr.isNotEmpty()) {
                cornerRadius = parseCornerRadius(context, shapeAttr)
            }
            if (cornerRadius > 0) {
                val gd = GradientDrawable().apply {
                    setColor(surfaceColor)
                    this.cornerRadius = cornerRadius.toFloat()
                }
                layout.background = gd
                layout.clipToOutline = true
            } else {
                layout.setBackgroundColor(surfaceColor)
            }
        }

        for (child in node.children) {
            val childView = renderNode(context, child, config)
            if (childView != null) {
                if (childView.layoutParams == null) {
                    childView.layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                layout.addView(childView)
            }
        }
        return layout
    }

    private fun renderCard(context: Context, node: ComposeNode, config: RenderConfig): MaterialCardView {
        val card = MaterialCardView(context).apply {
            radius = dpToPx(context, 16).toFloat()
            cardElevation = dpToPx(context, if (node.type == "ElevatedCard") 4 else 1).toFloat()

            if (node.type == "OutlinedCard") {
                strokeColor = config.outlineVariant
                strokeWidth = dpToPx(context, 1)
                setCardBackgroundColor(config.surface)
            } else {
                setCardBackgroundColor(config.surfaceContainer)
            }

            val colorsAttr = node.getAttribute("colors", node.getAttribute("containerColor", ""))
            if (colorsAttr.isNotEmpty()) {
                val bg = parseColorExpression(colorsAttr, config.surfaceContainer)
                setCardBackgroundColor(bg)
            }

            val shapeAttr = node.getAttribute("shape", "")
            if (shapeAttr.isNotEmpty()) {
                val r = parseCornerRadius(context, shapeAttr)
                if (r > 0) radius = r.toFloat()
            }
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dpToPx(context, 16)
            setPadding(pad, pad, pad, pad)
        }

        for (child in node.children) {
            val childView = renderNode(context, child, config)
            if (childView != null) {
                content.addView(childView)
            }
        }
        card.addView(content)
        return card
    }

    private fun renderTopAppBar(context: Context, node: ComposeNode, config: RenderConfig): View {
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(config.surfaceContainer)
            val h = dpToPx(context, if (node.type.contains("Large")) 112 else (if (node.type.contains("Medium")) 88 else 56))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h)
            setPadding(dpToPx(context, 16), 0, dpToPx(context, 16), 0)
        }

        val titleText = node.getAttribute("title", node.getAttribute("text", "Top App Bar"))
        val tvTitle = TextView(context).apply {
            text = titleText
            setTextSize(TypedValue.COMPLEX_UNIT_SP, (if (node.type.contains("Large")) 22f else 18f) * config.fontScale)
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(config.onSurface)
            if (node.type.contains("CenterAligned")) {
                gravity = Gravity.CENTER
            }
        }
        bar.addView(tvTitle, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f))

        for (child in node.children) {
            val childView = renderNode(context, child, config)
            if (childView != null) {
                bar.addView(childView)
            }
        }
        return bar
    }

    private fun renderNavigationBar(context: Context, node: ComposeNode, config: RenderConfig): View {
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(config.surfaceContainer)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(context, 68))
            val padH = dpToPx(context, 8)
            val padV = dpToPx(context, 4)
            setPadding(padH, padV, padH, padV)
        }

        for (child in node.children) {
            val childView = renderNode(context, child, config)
            if (childView != null) {
                val llp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                    gravity = Gravity.CENTER
                }
                childView.layoutParams = llp
                bar.addView(childView)
            }
        }
        return bar
    }

    private fun renderScaffold(context: Context, node: ComposeNode, config: RenderConfig): View {
        val scaffold = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(config.background)
        }

        var hasTopBar = false
        for (child in node.children) {
            if (child.type.contains("TopAppBar")) {
                val topBarView = renderNode(context, child, config)
                if (topBarView != null) {
                    scaffold.addView(topBarView)
                    hasTopBar = true
                }
            }
        }

        if (!hasTopBar) {
            val defaultBar = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(config.surfaceContainer)
                val p = dpToPx(context, 16)
                setPadding(p, p / 2, p, p / 2)
            }
            val title = TextView(context).apply {
                text = "Compose App"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18 * config.fontScale)
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(config.onSurface)
            }
            defaultBar.addView(title)
            scaffold.addView(defaultBar)
        }

        val scrollView = ScrollView(context).apply { isFillViewport = true }
        val bodyContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val bodyPad = dpToPx(context, 16)
            setPadding(bodyPad, bodyPad, bodyPad, bodyPad)
        }

        for (child in node.children) {
            if (!child.type.contains("TopAppBar") && !child.type.contains("BottomAppBar")) {
                val childView = renderNode(context, child, config)
                if (childView != null) {
                    bodyContainer.addView(childView)
                }
            }
        }
        scrollView.addView(bodyContainer)

        val scrollParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f)
        scaffold.addView(scrollView, scrollParams)
        return scaffold
    }

    private fun renderText(context: Context, node: ComposeNode, config: RenderConfig): TextView {
        val tv = TextView(context)
        var text = node.getAttribute("text", "")
        if (text.isEmpty()) {
            text = node.getAttribute("arg_0", "Text")
        }
        tv.text = text
        tv.setTextColor(config.onSurface)

        val colorAttr = node.getAttribute("color", "")
        if (colorAttr.isNotEmpty()) {
            val textColor = parseColorExpression(colorAttr, config.onSurface)
            tv.setTextColor(textColor)
        }

        var baseSize = 14f
        val style = node.getAttribute("style", "")
        if (style.contains("display") || style.contains("headline")) {
            baseSize = 24f
            tv.typeface = Typeface.DEFAULT_BOLD
        } else if (style.contains("title")) {
            baseSize = 18f
            tv.typeface = Typeface.DEFAULT_BOLD
        } else if (style.contains("label")) {
            baseSize = 12f
        }

        val fontSize = node.getAttribute("fontSize", "")
        if (fontSize.isNotEmpty()) {
            try {
                baseSize = fontSize.replace(Regex("[^0-9.]"), "").toFloat()
            } catch (_: Exception) {}
        }

        val fontWeight = node.getAttribute("fontWeight", "")
        if (fontWeight.contains("Bold") || fontWeight.contains("W700") || fontWeight.contains("SemiBold")) {
            tv.typeface = Typeface.DEFAULT_BOLD
        } else if (fontWeight.contains("Medium") || fontWeight.contains("W500")) {
            tv.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }

        val maxLines = node.getAttribute("maxLines", "")
        if (maxLines.isNotEmpty()) {
            try {
                val ml = maxLines.trim().toInt()
                tv.maxLines = ml
                tv.ellipsize = TextUtils.TruncateAt.END
            } catch (_: Exception) {}
        }

        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, baseSize * config.fontScale)
        return tv
    }

    private fun renderIcon(context: Context, node: ComposeNode, config: RenderConfig): ImageView {
        val iv = ImageView(context)
        val s = dpToPx(context, 24)
        iv.layoutParams = ViewGroup.LayoutParams(s, s)

        val vector = node.getAttribute("imageVector", node.getAttribute("painter", node.getAttribute("arg_0", "")))
        val tintAttr = node.getAttribute("tint", "")
        val tintColor = if (tintAttr.isNotEmpty()) {
            parseColorExpression(tintAttr, config.primary)
        } else {
            config.primary
        }

        val resolvedDrawable = ProjectResourceResolver.resolveDrawable(
            context = context,
            filePath = config.filePath,
            drawableRef = vector,
            isDark = config.isDarkMode,
            tintColor = tintColor
        )

        if (resolvedDrawable != null) {
            iv.setImageDrawable(resolvedDrawable)
        } else {
            iv.setImageDrawable(ProjectResourceResolver.getFallbackAndroidIcon(context, vector, tintColor))
        }

        return iv
    }

    private fun renderImage(context: Context, node: ComposeNode, config: RenderConfig): ImageView {
        val iv = ImageView(context)
        iv.scaleType = ImageView.ScaleType.CENTER_CROP

        val src = node.getAttribute("painter", node.getAttribute("src", node.getAttribute("imageVector", node.getAttribute("arg_0", ""))))
        val resolvedDrawable = ProjectResourceResolver.resolveDrawable(
            context = context,
            filePath = config.filePath,
            drawableRef = src,
            isDark = config.isDarkMode
        )

        if (resolvedDrawable != null) {
            iv.setImageDrawable(resolvedDrawable)
        } else {
            iv.setImageDrawable(ProjectResourceResolver.getFallbackAndroidIcon(context, src))
        }

        val s = dpToPx(context, 120)
        iv.layoutParams = ViewGroup.LayoutParams(s, s)
        return iv
    }

    private fun renderButton(context: Context, node: ComposeNode, config: RenderConfig): MaterialButton {
        val btn = MaterialButton(context)
        var label = "Button"
        var fontSize = 14f
        var isBold = false

        for (child in node.children) {
            if (child.type == "Text") {
                label = child.getAttribute("text", child.getAttribute("arg_0", "Button"))
                val fs = child.getAttribute("fontSize", "")
                if (fs.isNotEmpty()) {
                    try { fontSize = fs.replace(Regex("[^0-9.]"), "").toFloat() } catch (_: Exception) {}
                }
                val fw = child.getAttribute("fontWeight", "")
                if (fw.contains("Bold") || fw.contains("SemiBold")) isBold = true
                break
            }
        }
        btn.text = label
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize * config.fontScale)
        if (isBold) btn.typeface = Typeface.DEFAULT_BOLD

        var btnColor = config.primary
        val btnTextColor = config.onPrimary

        val colorsAttr = node.getAttribute("colors", node.getAttribute("containerColor", ""))
        if (colorsAttr.isNotEmpty()) {
            btnColor = parseColorExpression(colorsAttr, config.primary)
        }

        when (node.type) {
            "OutlinedButton" -> {
                btn.strokeColor = ColorStateList.valueOf(config.outline)
                btn.strokeWidth = dpToPx(context, 1)
                btn.setBackgroundColor(Color.TRANSPARENT)
                btn.setTextColor(config.primary)
            }
            "TextButton" -> {
                btn.setBackgroundColor(Color.TRANSPARENT)
                btn.setTextColor(config.primary)
            }
            "FilledTonalButton" -> {
                btn.setBackgroundColor(config.secondaryContainer)
                btn.setTextColor(config.secondary)
            }
            else -> {
                btn.setBackgroundColor(btnColor)
                btn.backgroundTintList = ColorStateList.valueOf(btnColor)
                btn.setTextColor(btnTextColor)
            }
        }

        val shapeAttr = node.getAttribute("shape", "")
        if (shapeAttr.isNotEmpty()) {
            val cornerRadius = parseCornerRadius(context, shapeAttr)
            if (cornerRadius > 0) btn.cornerRadius = cornerRadius
        }

        if (!config.isInspectMode && config.isInteractive) {
            btn.setOnClickListener { Toast.makeText(context, "Clicked: " + btn.text, Toast.LENGTH_SHORT).show() }
        } else {
            btn.setOnClickListener(null)
            btn.isClickable = false
        }
        return btn
    }

    private fun renderIconButton(context: Context, node: ComposeNode, config: RenderConfig): View {
        val iv = ImageView(context)
        val p = dpToPx(context, 8)
        iv.setPadding(p, p, p, p)
        iv.setImageResource(android.R.drawable.ic_menu_manage)
        iv.setColorFilter(config.primary)

        for (child in node.children) {
            if (child.type == "Icon") {
                return renderIcon(context, child, config)
            }
        }
        return iv
    }

    private fun renderFab(context: Context, node: ComposeNode, config: RenderConfig): FloatingActionButton {
        val fab = FloatingActionButton(context)
        fab.backgroundTintList = ColorStateList.valueOf(config.primaryContainer)
        fab.imageTintList = ColorStateList.valueOf(config.onPrimaryContainer)
        fab.setImageResource(android.R.drawable.ic_input_add)
        return fab
    }

    private fun renderBasicTextField(context: Context, node: ComposeNode, config: RenderConfig): View {
        val et = EditText(context)
        var value = node.getAttribute("value", "")
        var placeholder = node.getAttribute("placeholder", node.getAttribute("hint", ""))

        value = ComposeAstParser.cleanValueExpression(value)
        placeholder = ComposeAstParser.cleanValueExpression(placeholder)

        et.setText(value)
        if (placeholder.isNotEmpty()) {
            et.hint = placeholder
        } else if (value.isEmpty()) {
            et.hint = "Enter text..."
        }

        et.setTextColor(config.onSurface)
        et.setHintTextColor(ColorUtils.setAlphaComponent(config.onSurfaceVariant, 140))

        val gd = GradientDrawable().apply {
            setColor(ColorUtils.setAlphaComponent(config.surfaceContainerLow, 120))
            setStroke(dpToPx(context, 1), config.outlineVariant)
            cornerRadius = dpToPx(context, 6).toFloat()
        }
        et.background = gd

        val px = dpToPx(context, 10)
        val py = dpToPx(context, 8)
        et.setPadding(px, py, px, py)

        val interactive = !config.isInspectMode && config.isInteractive
        et.isFocusable = interactive
        et.isFocusableInTouchMode = interactive
        et.isCursorVisible = interactive
        et.isEnabled = interactive
        return et
    }

    private fun renderTextField(context: Context, node: ComposeNode, config: RenderConfig): TextInputLayout {
        val til = TextInputLayout(context)
        var label = node.getAttribute("label", "Enter text")
        var value = node.getAttribute("value", "")
        var placeholder = node.getAttribute("placeholder", "")

        label = ComposeAstParser.cleanValueExpression(label)
        value = ComposeAstParser.cleanValueExpression(value)
        placeholder = ComposeAstParser.cleanValueExpression(placeholder)

        til.hint = label
        til.boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        val r = dpToPx(context, 8).toFloat()
        til.setBoxCornerRadii(r, r, r, r)

        val et = TextInputEditText(context)
        et.setText(value)
        if (placeholder.isNotEmpty()) {
            et.hint = placeholder
        }
        val interactive = !config.isInspectMode && config.isInteractive
        et.isFocusable = interactive
        et.isFocusableInTouchMode = interactive
        et.isCursorVisible = interactive
        et.isEnabled = interactive

        til.addView(et)
        return til
    }

    private fun renderCheckbox(context: Context, node: ComposeNode, config: RenderConfig): CheckBox {
        val cb = CheckBox(context)
        cb.isChecked = "true".equals(node.getAttribute("checked", "false"), ignoreCase = true)
        cb.text = node.getAttribute("text", "")
        cb.isEnabled = config.isInteractive
        return cb
    }

    private fun renderSwitch(context: Context, node: ComposeNode, config: RenderConfig): MaterialSwitch {
        val sw = MaterialSwitch(context)
        sw.isChecked = "true".equals(node.getAttribute("checked", "false"), ignoreCase = true)
        sw.isEnabled = config.isInteractive
        return sw
    }

    private fun renderRadioButton(context: Context, node: ComposeNode, config: RenderConfig): RadioButton {
        val rb = RadioButton(context)
        rb.isChecked = "true".equals(node.getAttribute("selected", "false"), ignoreCase = true)
        rb.isEnabled = config.isInteractive
        return rb
    }

    private fun renderSlider(context: Context, node: ComposeNode, config: RenderConfig): Slider {
        val slider = Slider(context)
        slider.valueFrom = 0f
        slider.valueTo = 100f
        slider.value = 50f
        slider.isEnabled = config.isInteractive
        return slider
    }

    private fun renderSpacer(context: Context, node: ComposeNode, config: RenderConfig): View {
        val spacer = View(context)
        var w = 0
        var h = 0
        var weight = 0f

        val height = node.getModifierValue("height")
        val width = node.getModifierValue("width")
        val size = node.getModifierValue("size")
        val weightStr = node.getModifierValue("weight")

        if (weightStr != null) {
            try {
                weight = weightStr.replace(Regex("[^0-9.]"), "").toFloat()
            } catch (_: Exception) {}
        }

        if (size != null) {
            w = parseDp(context, size)
            h = w
        } else {
            if (height != null) h = parseDp(context, height)
            if (width != null) w = parseDp(context, width)
        }

        if (w == 0 && h == 0 && weight == 0f) {
            h = dpToPx(context, 16)
        }

        val lp = LinearLayout.LayoutParams(
            if (w > 0) w else (if (weight > 0) 0 else ViewGroup.LayoutParams.WRAP_CONTENT),
            if (h > 0) h else (if (weight > 0) ViewGroup.LayoutParams.WRAP_CONTENT else ViewGroup.LayoutParams.WRAP_CONTENT),
            weight
        )
        spacer.layoutParams = lp
        return spacer
    }

    private fun renderDivider(context: Context, node: ComposeNode, config: RenderConfig): MaterialDivider {
        val divider = MaterialDivider(context)
        divider.dividerColor = config.outlineVariant
        divider.dividerThickness = dpToPx(context, 1)
        return divider
    }

    private fun renderLinearProgress(context: Context, node: ComposeNode, config: RenderConfig): LinearProgressIndicator {
        val p = LinearProgressIndicator(context)
        p.isIndeterminate = true
        p.setIndicatorColor(config.primary)
        return p
    }

    private fun renderCircularProgress(context: Context, node: ComposeNode, config: RenderConfig): CircularProgressIndicator {
        val p = CircularProgressIndicator(context)
        p.isIndeterminate = true
        p.setIndicatorColor(config.primary)
        return p
    }

    private fun renderChip(context: Context, node: ComposeNode, config: RenderConfig): Chip {
        val chip = Chip(context)
        chip.text = node.getAttribute("label", node.getAttribute("text", "Chip"))
        chip.setChipBackgroundColorResource(android.R.color.transparent)
        chip.chipStrokeColor = ColorStateList.valueOf(config.outlineVariant)
        chip.chipStrokeWidth = dpToPx(context, 1).toFloat()
        chip.setTextColor(config.onSurface)
        return chip
    }

    private fun applyModifiers(context: Context, view: View, node: ComposeNode, config: RenderConfig) {
        var width = ViewGroup.LayoutParams.WRAP_CONTENT
        var height = ViewGroup.LayoutParams.WRAP_CONTENT
        var weight = 0f
        val padding = intArrayOf(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom)
        var cornerRadius = 0

        for (m in node.modifiers) {
            when {
                m.startsWith("fillMaxWidth") -> width = ViewGroup.LayoutParams.MATCH_PARENT
                m.startsWith("fillMaxHeight") -> height = ViewGroup.LayoutParams.MATCH_PARENT
                m.startsWith("fillMaxSize") -> {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                    height = ViewGroup.LayoutParams.MATCH_PARENT
                }
                m.startsWith("width(") -> width = parseDp(context, extractParenthesized(m) ?: "")
                m.startsWith("height(") -> height = parseDp(context, extractParenthesized(m) ?: "")
                m.startsWith("size(") -> {
                    val s = parseDp(context, extractParenthesized(m) ?: "")
                    width = s
                    height = s
                }
                m.startsWith("weight(") -> {
                    try {
                        weight = (extractParenthesized(m) ?: "").replace(Regex("[^0-9.]"), "").toFloat()
                    } catch (_: Exception) {}
                }
                m.startsWith("padding(") -> {
                    val pVal = extractParenthesized(m) ?: ""
                    applyPaddingValues(context, pVal, padding)
                }
                m.startsWith("clip(") -> {
                    cornerRadius = parseCornerRadius(context, extractParenthesized(m))
                    if (cornerRadius > 0) {
                        view.clipToOutline = true
                    }
                }
                m.startsWith("background(") -> {
                    val bgVal = extractParenthesized(m) ?: ""
                    val d = parseBackgroundDrawable(context, bgVal, cornerRadius, config)
                    if (d != null) view.background = d
                }
            }
        }

        view.setPadding(padding[0], padding[1], padding[2], padding[3])

        var lp = view.layoutParams
        if (lp == null) {
            lp = LinearLayout.LayoutParams(width, height, weight)
        } else {
            lp.width = width
            lp.height = height
            if (lp is LinearLayout.LayoutParams && weight > 0) {
                lp.weight = weight
            }
        }
        view.layoutParams = lp
    }

    private fun setupInspectListener(view: View, node: ComposeNode, config: RenderConfig) {
        view.tag = node
        if (config.isInspectMode && config.inspectListener != null) {
            view.setOnClickListener {
                config.inspectListener?.onNodeSelected(node, view)
            }
        }
    }

    fun findViewForNode(root: View?, target: ComposeNode?): View? {
        if (root == null || target == null) return null
        if (root.tag === target || root.tag == target) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val found = findViewForNode(root.getChildAt(i), target)
                if (found != null) return found
            }
        }
        return null
    }

    fun parseCornerRadius(context: Context, expr: String?): Int {
        if (expr.isNullOrBlank()) return 0
        if (expr.contains("CircleShape")) return dpToPx(context, 999)
        if (expr.contains("RoundedCornerShape")) {
            val m = Pattern.compile("RoundedCornerShape\\s*\\(\\s*([0-9.]+)(?:\\s*\\.\\s*dp)?").matcher(expr)
            if (m.find()) {
                val v = m.group(1)?.toFloatOrNull() ?: 0f
                return dpToPx(context, v.toInt())
            }
        }
        return 0
    }

    fun parseColorExpression(expr: String?, defaultColor: Int): Int {
        if (expr.isNullOrBlank()) return defaultColor
        val trimmed = expr.trim()

        if (trimmed.contains("Color.White") || trimmed.contains("White")) return Color.WHITE
        if (trimmed.contains("Color.Black") || trimmed.contains("Black")) return Color.BLACK
        if (trimmed.contains("Color.Transparent") || trimmed.contains("Transparent")) return Color.TRANSPARENT
        if (trimmed.contains("Color.Gray") || trimmed.contains("Gray")) return Color.GRAY
        if (trimmed.contains("Color.Red") || trimmed.contains("Red")) return Color.RED
        if (trimmed.contains("Color.Green") || trimmed.contains("Green")) return Color.parseColor("#4CAF50")
        if (trimmed.contains("Color.Blue") || trimmed.contains("Blue")) return Color.parseColor("#2196F3")

        if (trimmed.contains("0x", ignoreCase = true)) {
            try {
                val m = Pattern.compile("0[xX]([0-9a-fA-F]+)").matcher(trimmed)
                if (m.find()) {
                    val hex = m.group(1) ?: ""
                    val v = hex.toLong(16)
                    var color = v.toInt()
                    if (hex.length <= 6) {
                        color = (0xFF000000L or v).toInt()
                    }
                    return color
                }
            } catch (_: Exception) {}
        }
        return defaultColor
    }

    fun parseBackgroundDrawable(context: Context, bgExpr: String?, cornerRadius: Int, config: RenderConfig): Drawable? {
        if (bgExpr.isNullOrBlank()) return null
        val trimmed = bgExpr.trim()

        // 1. Check for Compose gradient expressions: Brush.linearGradient, Brush.verticalGradient, Brush.horizontalGradient
        if (trimmed.contains("linearGradient", ignoreCase = true) ||
            trimmed.contains("verticalGradient", ignoreCase = true) ||
            trimmed.contains("horizontalGradient", ignoreCase = true)
        ) {
            val colorsList = mutableListOf<Int>()
            val m = Pattern.compile("(?:0[xX][0-9a-fA-F]{6,8}|Color\\s*\\.\\s*[A-Za-z]+|Color\\s*\\(\\s*0[xX][0-9a-fA-F]+\\s*\\)|#[0-9a-fA-F]{6,8})").matcher(trimmed)
            while (m.find()) {
                colorsList.add(parseColorExpression(m.group(), config.primary))
            }

            if (colorsList.size >= 2) {
                val orientation = when {
                    trimmed.contains("verticalGradient", ignoreCase = true) -> GradientDrawable.Orientation.TOP_BOTTOM
                    trimmed.contains("horizontalGradient", ignoreCase = true) -> GradientDrawable.Orientation.LEFT_RIGHT
                    else -> GradientDrawable.Orientation.TL_BR
                }
                return GradientDrawable(orientation, colorsList.toIntArray()).apply {
                    if (cornerRadius > 0) {
                        this.cornerRadius = cornerRadius.toFloat()
                    }
                }
            }
        }

        // 2. Check for drawable resource reference: @drawable/... or R.drawable.
        if (trimmed.startsWith("@drawable/") || trimmed.contains("R.drawable.")) {
            val d = ProjectResourceResolver.resolveDrawable(context, config.filePath, trimmed, config.isDarkMode)
            if (d != null) return d
        }

        // 3. Fallback to solid color
        val color = parseColorExpression(bgExpr, config.surfaceContainer)
        val gd = GradientDrawable().apply {
            setColor(color)
            if (cornerRadius > 0) {
                this.cornerRadius = cornerRadius.toFloat()
            }
        }
        return gd
    }

    private fun extractParenthesized(s: String?): String? {
        if (s == null) return null
        val open = s.indexOf('(')
        val close = s.lastIndexOf(')')
        return if (open != -1 && close > open) {
            s.substring(open + 1, close).trim()
        } else null
    }

    private fun applyPaddingValues(context: Context, valStr: String, padding: IntArray) {
        val dpMatch = Pattern.compile("([0-9.]+)(?:\\s*\\.\\s*dp)?").matcher(valStr)
        if (dpMatch.find()) {
            val p = dpToPx(context, dpMatch.group(1)?.toFloatOrNull()?.toInt() ?: 8)
            padding[0] = p
            padding[1] = p
            padding[2] = p
            padding[3] = p
        }
    }

    private fun parseDp(context: Context, expr: String): Int {
        val m = Pattern.compile("([0-9.]+)").matcher(expr)
        return if (m.find()) {
            val v = m.group(1)?.toFloatOrNull()?.toInt() ?: 0
            dpToPx(context, v)
        } else 0
    }

    fun dpToPx(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
