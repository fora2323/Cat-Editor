package org.cat.editor.ui.preview

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.File
import java.util.Locale
import java.util.regex.Pattern

/**
 * High-fidelity renderer that constructs authentic Android Views from an XmlNode AST.
 * Integrates ProjectResourceResolver for vector drawables, gradients, colors, strings, and icons.
 */
object XmlLayoutRenderer {

    fun interface OnXmlNodeSelectedListener {
        fun onNodeSelected(node: XmlNode, view: View)
    }

    class XmlRenderConfig {
        var isDarkMode: Boolean = false
        var filePath: String = ""
        var fontScale: Float = 1.0f
        var isInspectMode: Boolean = true
        var inspectListener: OnXmlNodeSelectedListener? = null

        var primary: Int = 0
        var onPrimary: Int = 0
        var primaryContainer: Int = 0
        var onPrimaryContainer: Int = 0
        var secondary: Int = 0
        var secondaryContainer: Int = 0
        var onSecondaryContainer: Int = 0
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
            fun create(darkMode: Boolean, filePath: String = "", fontScale: Float = 1.0f): XmlRenderConfig {
                val c = XmlRenderConfig()
                c.isDarkMode = darkMode
                c.filePath = filePath
                c.fontScale = fontScale

                if (darkMode) {
                    c.primary = Color.parseColor("#D0BCFF")
                    c.onPrimary = Color.parseColor("#381E72")
                    c.primaryContainer = Color.parseColor("#4F378B")
                    c.onPrimaryContainer = Color.parseColor("#EADDFF")
                    c.secondary = Color.parseColor("#CCC2DC")
                    c.secondaryContainer = Color.parseColor("#4A4458")
                    c.onSecondaryContainer = Color.parseColor("#E8DEF8")
                    c.surface = Color.parseColor("#141218")
                    c.onSurface = Color.parseColor("#E6E0E9")
                    c.onSurfaceVariant = Color.parseColor("#CAC4D0")
                    c.surfaceContainer = Color.parseColor("#211F26")
                    c.surfaceContainerLow = Color.parseColor("#1D1B20")
                    c.surfaceContainerHigh = Color.parseColor("#2B2930")
                    c.background = Color.parseColor("#141218")
                    c.outline = Color.parseColor("#938F99")
                    c.outlineVariant = Color.parseColor("#49454F")
                } else {
                    c.primary = Color.parseColor("#6750A4")
                    c.onPrimary = Color.parseColor("#FFFFFF")
                    c.primaryContainer = Color.parseColor("#EADDFF")
                    c.onPrimaryContainer = Color.parseColor("#21005D")
                    c.secondary = Color.parseColor("#625B71")
                    c.secondaryContainer = Color.parseColor("#E8DEF8")
                    c.onSecondaryContainer = Color.parseColor("#1D192B")
                    c.surface = Color.parseColor("#FAF8FD")
                    c.onSurface = Color.parseColor("#1D1B20")
                    c.onSurfaceVariant = Color.parseColor("#49454F")
                    c.surfaceContainer = Color.parseColor("#F3EDF7")
                    c.surfaceContainerLow = Color.parseColor("#F7F2FA")
                    c.surfaceContainerHigh = Color.parseColor("#ECE6F0")
                    c.background = Color.parseColor("#FEF7FF")
                    c.outline = Color.parseColor("#79747E")
                    c.outlineVariant = Color.parseColor("#CAC4D0")
                }
                return c
            }
        }
    }

    fun render(context: Context, rootNode: XmlNode, config: XmlRenderConfig): View {
        val themedContext = ContextThemeWrapper(
            context,
            com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar
        )
        return renderNode(themedContext, rootNode, config) ?: View(themedContext)
    }

    private fun renderNode(context: Context, node: XmlNode, config: XmlRenderConfig): View? {
        val simpleTag = node.simpleName
        val view: View? = when (simpleTag) {
            "LinearLayout" -> renderLinearLayout(context, node, config)
            "ScrollView" -> renderScrollView(context, node, config, isHorizontal = false)
            "HorizontalScrollView" -> renderScrollView(context, node, config, isHorizontal = true)
            "RelativeLayout", "FrameLayout", "CoordinatorLayout", "ConstraintLayout" ->
                renderFrameLayout(context, node, config)
            "CardView", "MaterialCardView" -> renderCardView(context, node, config)
            "Toolbar", "MaterialToolbar", "com.google.android.material.appbar.MaterialToolbar" ->
                renderToolbar(context, node, config)
            "TextView", "AppCompatTextView", "MaterialTextView" -> renderTextView(context, node, config)
            "ImageView", "AppCompatImageView" -> renderImageView(context, node, config)
            "Button", "AppCompatButton", "MaterialButton" -> renderButton(context, node, config)
            "EditText", "AppCompatEditText", "TextInputEditText" -> renderEditText(context, node, config)
            "TextInputLayout" -> renderTextInputLayout(context, node, config)
            "CheckBox", "AppCompatCheckBox", "MaterialCheckBox" -> renderCheckBox(context, node, config)
            "RadioButton", "AppCompatRadioButton", "MaterialRadioButton" -> renderRadioButton(context, node, config)
            "RadioGroup" -> renderRadioGroup(context, node, config)
            "Switch", "SwitchCompat", "MaterialSwitch" -> renderSwitch(context, node, config)
            "Chip" -> renderChip(context, node, config)
            "ChipGroup" -> renderChipGroup(context, node, config)
            "TabLayout" -> renderTabLayout(context, node, config)
            "BottomNavigationView", "NavigationBarView" -> renderBottomNavigationView(context, node, config)
            "Slider" -> renderSlider(context, node, config)
            "RecyclerView", "ListView" -> renderRecyclerView(context, node, config)
            "ProgressBar" -> renderProgressBar(context, node, config)
            "FloatingActionButton" -> renderFab(context, node, config)
            "ExtendedFloatingActionButton" -> renderExtendedFab(context, node, config)
            "include" -> renderInclude(context, node, config)
            "View", "MaterialDivider", "Divider" -> renderDividerView(context, node, config)
            else -> {
                when {
                    simpleTag.contains("Toolbar", ignoreCase = true) -> renderToolbar(context, node, config)
                    simpleTag.contains("Card", ignoreCase = true) -> renderCardView(context, node, config)
                    simpleTag.contains("InputLayout", ignoreCase = true) -> renderTextInputLayout(context, node, config)
                    simpleTag.contains("Button", ignoreCase = true) -> renderButton(context, node, config)
                    simpleTag.contains("Edit", ignoreCase = true) -> renderEditText(context, node, config)
                    simpleTag.contains("ChipGroup", ignoreCase = true) -> renderChipGroup(context, node, config)
                    simpleTag.contains("Chip", ignoreCase = true) -> renderChip(context, node, config)
                    simpleTag.contains("Tab", ignoreCase = true) -> renderTabLayout(context, node, config)
                    simpleTag.contains("Slider", ignoreCase = true) -> renderSlider(context, node, config)
                    simpleTag.contains("Recycler", ignoreCase = true) || simpleTag.contains("List", ignoreCase = true) ->
                        renderRecyclerView(context, node, config)
                    node.isViewGroup() || node.children.isNotEmpty() -> renderLinearLayout(context, node, config)
                    else -> renderTextView(context, node, config)
                }
            }
        }

        if (view != null) {
            applyCommonViewAttributes(context, view, node, config)
            setupInspectListener(view, node, config)
        }

        return view
    }

    private fun renderLinearLayout(context: Context, node: XmlNode, config: XmlRenderConfig): LinearLayout {
        val layout = LinearLayout(context)
        val orientation = node.getAttribute("orientation", "vertical")
        layout.orientation = if (orientation.equals("horizontal", ignoreCase = true)) {
            LinearLayout.HORIZONTAL
        } else {
            LinearLayout.VERTICAL
        }

        val gravityStr = node.getAttribute("gravity", "")
        if (gravityStr.isNotEmpty()) {
            layout.gravity = parseGravity(gravityStr)
        }

        for (childNode in node.children) {
            val childView = renderNode(context, childNode, config)
            if (childView != null) {
                val lp = createLinearLayoutParams(context, childNode)
                layout.addView(childView, lp)
            }
        }

        return layout
    }

    private fun renderScrollView(context: Context, node: XmlNode, config: XmlRenderConfig, isHorizontal: Boolean): View {
        val scroll = ComposePreviewView.InspectableScrollView(context).apply {
            isFillViewport = true
            clipToPadding = false
            clipChildren = false
        }

        if (node.children.size == 1) {
            val singleChild = renderNode(context, node.children[0], config)
            if (singleChild != null) {
                val lp = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                scroll.addView(singleChild, lp)
            }
        } else {
            val container = LinearLayout(context).apply {
                orientation = if (isHorizontal) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                clipToPadding = false
                clipChildren = false
            }

            for (childNode in node.children) {
                val childView = renderNode(context, childNode, config)
                if (childView != null) {
                    val lp = createLinearLayoutParams(context, childNode)
                    container.addView(childView, lp)
                }
            }

            scroll.addView(container)
        }

        return scroll
    }

    private fun renderFrameLayout(context: Context, node: XmlNode, config: XmlRenderConfig): FrameLayout {
        val frame = FrameLayout(context).apply {
            clipToPadding = false
            clipChildren = false
        }
        for (childNode in node.children) {
            val childView = renderNode(context, childNode, config)
            if (childView != null) {
                val lp = createFrameLayoutParams(context, childNode)
                frame.addView(childView, lp)
            }
        }
        return frame
    }

    private fun renderToolbar(context: Context, node: XmlNode, config: XmlRenderConfig): View {
        val toolbar = MaterialToolbar(context)
        val title = node.getAttribute("title", node.getAttribute("text", ""))
        if (title.isNotEmpty()) {
            toolbar.title = ProjectResourceResolver.resolveString(context, config.filePath, title)
        }

        val subtitle = node.getAttribute("subtitle", "")
        if (subtitle.isNotEmpty()) {
            toolbar.subtitle = ProjectResourceResolver.resolveString(context, config.filePath, subtitle)
        }

        val titleColorAttr = node.getAttribute("titleTextColor")
        val titleColor = if (titleColorAttr.isNotEmpty()) {
            ProjectResourceResolver.resolveColor(context, config.filePath, titleColorAttr, config.isDarkMode, config.onSurface)
        } else {
            config.onSurface
        }

        try {
            toolbar.setTitleTextAppearance(context, com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
        } catch (_: Throwable) {
            try {
                toolbar.setTitleTextAppearance(context, androidx.appcompat.R.style.TextAppearance_AppCompat_Widget_ActionBar_Title)
            } catch (_: Throwable) {}
        }
        toolbar.setTitleTextColor(titleColor)

        val bgAttr = node.getAttribute("background")
        val bgColor = if (bgAttr.isNotEmpty()) {
            ProjectResourceResolver.resolveColor(context, config.filePath, bgAttr, config.isDarkMode, config.surfaceContainerHigh)
        } else {
            config.surfaceContainerHigh
        }
        toolbar.setBackgroundColor(bgColor)

        val minH = dpToPx(context, 56)
        toolbar.minimumHeight = minH
        toolbar.setContentInsetsRelative(dpToPx(context, 16), dpToPx(context, 16))

        // Overflow 3-dots icon matching Android Studio layout preview
        try {
            if (toolbar.menu.size() == 0) {
                toolbar.menu.add(0, 1, 0, "More").apply {
                    setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_NEVER)
                }
            }
            toolbar.overflowIcon?.mutate()?.setColorFilter(titleColor, android.graphics.PorterDuff.Mode.SRC_IN)
        } catch (_: Throwable) {}

        return toolbar
    }

    private fun renderCardView(context: Context, node: XmlNode, config: XmlRenderConfig): MaterialCardView {
        val card = MaterialCardView(context)

        // Corner radius (defaults to 16dp for authentic Material 3 elevated card)
        val radiusAttr = node.getAttribute("cardCornerRadius", node.getAttribute("cornerRadius", ""))
        val radiusDp = if (radiusAttr.isNotEmpty()) parseDimensionDp(radiusAttr, 16) else 16
        card.radius = dpToPx(context, radiusDp).toFloat()

        // Elevation (defaults to 3dp for elevated card)
        val elevAttr = node.getAttribute("cardElevation", node.getAttribute("elevation", ""))
        val elevDp = if (elevAttr.isNotEmpty()) parseDimensionDp(elevAttr, 3) else 3
        card.cardElevation = dpToPx(context, elevDp).toFloat()

        // Stroke (Only set if explicitly specified; elevated cards have 0 stroke width by default)
        val strokeWAttr = node.getAttribute("strokeWidth")
        val strokeCAttr = node.getAttribute("strokeColor")
        val strokeW = if (strokeWAttr.isNotEmpty()) {
            parseDimensionDp(strokeWAttr, 1)
        } else if (strokeCAttr.isNotEmpty()) {
            1
        } else {
            0
        }
        card.strokeWidth = dpToPx(context, strokeW)
        if (strokeW > 0) {
            val strokeColor = if (strokeCAttr.isNotEmpty()) {
                ProjectResourceResolver.resolveColor(context, config.filePath, strokeCAttr, config.isDarkMode, config.outlineVariant)
            } else {
                config.outlineVariant
            }
            card.strokeColor = strokeColor
        } else {
            card.strokeColor = Color.TRANSPARENT
        }

        // Background color (defaults to surfaceContainerLow for clean elevated card look)
        val bgAttr = node.getAttribute("cardBackgroundColor", node.getAttribute("background", ""))
        val bgColor = if (bgAttr.isNotEmpty()) {
            ProjectResourceResolver.resolveColor(context, config.filePath, bgAttr, config.isDarkMode, config.surfaceContainerLow)
        } else {
            config.surfaceContainerLow
        }
        card.setCardBackgroundColor(bgColor)

        // Content padding
        val contentPaddingAttr = node.getAttribute("contentPadding")
        if (contentPaddingAttr.isNotEmpty()) {
            val p = dpToPx(context, parseDimensionDp(contentPaddingAttr, 0))
            card.setContentPadding(p, p, p, p)
        }

        // Card children
        if (node.children.size == 1) {
            val child = renderNode(context, node.children[0], config)
            if (child != null) card.addView(child)
        } else if (node.children.size > 1) {
            val innerLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            for (childNode in node.children) {
                val childView = renderNode(context, childNode, config)
                if (childView != null) {
                    innerLayout.addView(childView, createLinearLayoutParams(context, childNode))
                }
            }
            card.addView(innerLayout)
        }

        return card
    }

    private fun renderTextView(context: Context, node: XmlNode, config: XmlRenderConfig): TextView {
        val tv = TextView(context)

        // Text
        val rawText = node.getAttribute("text")
        val resolvedText = ProjectResourceResolver.resolveString(context, config.filePath, rawText)
        tv.text = resolvedText

        // Hint
        val hint = node.getAttribute("hint")
        if (hint.isNotEmpty()) {
            tv.hint = ProjectResourceResolver.resolveString(context, config.filePath, hint)
        }

        // Text Size
        val sizeAttr = node.getAttribute("textSize")
        val sizeSp = if (sizeAttr.isNotEmpty()) parseDimensionDp(sizeAttr, 14).toFloat() else 14f
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp * config.fontScale)

        // Text Color
        val colorAttr = node.getAttribute("textColor")
        val color = if (colorAttr.isNotEmpty()) {
            ProjectResourceResolver.resolveColor(context, config.filePath, colorAttr, config.isDarkMode, config.onSurface)
        } else {
            config.onSurface
        }
        tv.setTextColor(color)

        // Text Style
        val styleAttr = node.getAttribute("textStyle")
        when {
            styleAttr.contains("bold") && styleAttr.contains("italic") -> tv.setTypeface(null, Typeface.BOLD_ITALIC)
            styleAttr.contains("bold") -> tv.setTypeface(null, Typeface.BOLD)
            styleAttr.contains("italic") -> tv.setTypeface(null, Typeface.ITALIC)
            else -> tv.setTypeface(null, Typeface.NORMAL)
        }

        // Gravity
        val grav = node.getAttribute("gravity")
        if (grav.isNotEmpty()) tv.gravity = parseGravity(grav)

        // Max lines & Ellipsize
        val maxLinesAttr = node.getAttribute("maxLines")
        if (maxLinesAttr.isNotEmpty()) {
            maxLinesAttr.toIntOrNull()?.let {
                tv.maxLines = it
                tv.ellipsize = TextUtils.TruncateAt.END
            }
        }

        return tv
    }

    private fun renderImageView(context: Context, node: XmlNode, config: XmlRenderConfig): ImageView {
        val iv = ImageView(context)

        // Background (e.g. @drawable/shape_circle_accent)
        val bgAttr = node.getAttribute("background")
        if (bgAttr.isNotEmpty()) {
            val bg = ProjectResourceResolver.resolveDrawable(
                context = context,
                filePath = config.filePath,
                drawableRef = bgAttr,
                isDark = config.isDarkMode,
                isBackground = true
            )
            if (bg != null) {
                iv.background = bg
            } else if (ProjectResourceResolver.isColorExpression(bgAttr)) {
                val color = ProjectResourceResolver.resolveColor(context, config.filePath, bgAttr, config.isDarkMode, Color.TRANSPARENT)
                if (color != Color.TRANSPARENT) iv.setBackgroundColor(color)
            }
        }

        // Source drawable (resolves vector XML, shapes, PNG, with fallback to basic Android icons)
        val srcAttr = node.getAttribute("src", node.getAttribute("srcCompat", ""))
        val tintAttr = node.getAttribute("tint", node.getAttribute("tintColor", ""))
        val tint = if (tintAttr.isNotEmpty()) {
            ProjectResourceResolver.resolveColor(context, config.filePath, tintAttr, config.isDarkMode, config.onPrimaryContainer)
        } else {
            config.onPrimaryContainer
        }

        val drawable = ProjectResourceResolver.resolveDrawable(
            context = context,
            filePath = config.filePath,
            drawableRef = srcAttr,
            isDark = config.isDarkMode,
            tintColor = tint,
            isBackground = false
        )

        if (drawable != null) {
            iv.setImageDrawable(drawable)
        } else {
            iv.setImageDrawable(ProjectResourceResolver.getFallbackAndroidIcon(context, node.shortId ?: "image", tint))
        }

        // Apply native ImageView image tinting
        iv.imageTintList = ColorStateList.valueOf(tint)
        iv.imageTintMode = android.graphics.PorterDuff.Mode.SRC_IN

        // Scale type
        val scaleAttr = node.getAttribute("scaleType")
        iv.scaleType = when (scaleAttr.lowercase(Locale.ROOT)) {
            "centercrop" -> ImageView.ScaleType.CENTER_CROP
            "centerinside" -> ImageView.ScaleType.CENTER_INSIDE
            "fitxy" -> ImageView.ScaleType.FIT_XY
            "fitstart" -> ImageView.ScaleType.FIT_START
            "fitend" -> ImageView.ScaleType.FIT_END
            "center" -> ImageView.ScaleType.CENTER
            else -> ImageView.ScaleType.FIT_CENTER
        }

        return iv
    }

    private fun renderButton(context: Context, node: XmlNode, config: XmlRenderConfig): MaterialButton {
        val btn = MaterialButton(context)
        val rawText = node.getAttribute("text", "Button")
        btn.text = ProjectResourceResolver.resolveString(context, config.filePath, rawText)

        val sizeAttr = node.getAttribute("textSize")
        val sizeSp = if (sizeAttr.isNotEmpty()) parseDimensionDp(sizeAttr, 14).toFloat() else 14f
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp * config.fontScale)

        val cornerAttr = node.getAttribute("cornerRadius")
        val corner = if (cornerAttr.isNotEmpty()) parseDimensionDp(cornerAttr, 20) else 20
        btn.cornerRadius = dpToPx(context, corner)

        // Check Button Style: Outlined, Tonal, Elevated, Text, Filled
        val styleAttr = node.getAttribute("style")
        val isOutlined = styleAttr.contains("Outlined", ignoreCase = true)
        val isTonal = styleAttr.contains("Tonal", ignoreCase = true)
        val isTextBtn = styleAttr.contains("TextButton", ignoreCase = true)
        val isElevated = styleAttr.contains("Elevated", ignoreCase = true)

        val bgTintAttr = node.getAttribute("backgroundTint")
        val textColorAttr = node.getAttribute("textColor")

        when {
            isOutlined -> {
                btn.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                val strokeC = if (node.hasAttribute("strokeColor")) {
                    ProjectResourceResolver.resolveColor(context, config.filePath, node.getAttribute("strokeColor"), config.isDarkMode, config.outline)
                } else config.outline
                btn.strokeColor = ColorStateList.valueOf(strokeC)
                btn.strokeWidth = dpToPx(context, parseDimensionDp(node.getAttribute("strokeWidth", "1dp"), 1))
                val tc = if (textColorAttr.isNotEmpty()) {
                    ProjectResourceResolver.resolveColor(context, config.filePath, textColorAttr, config.isDarkMode, config.primary)
                } else config.primary
                btn.setTextColor(tc)
                btn.iconTint = ColorStateList.valueOf(tc)
            }
            isTonal -> {
                val bg = if (bgTintAttr.isNotEmpty()) {
                    ProjectResourceResolver.resolveColor(context, config.filePath, bgTintAttr, config.isDarkMode, config.secondaryContainer)
                } else config.secondaryContainer
                btn.backgroundTintList = ColorStateList.valueOf(bg)
                val tc = if (textColorAttr.isNotEmpty()) {
                    ProjectResourceResolver.resolveColor(context, config.filePath, textColorAttr, config.isDarkMode, config.onSecondaryContainer)
                } else config.onSecondaryContainer
                btn.setTextColor(tc)
                btn.iconTint = ColorStateList.valueOf(tc)
            }
            isTextBtn -> {
                btn.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                btn.strokeWidth = 0
                val tc = if (textColorAttr.isNotEmpty()) {
                    ProjectResourceResolver.resolveColor(context, config.filePath, textColorAttr, config.isDarkMode, config.primary)
                } else config.primary
                btn.setTextColor(tc)
                btn.iconTint = ColorStateList.valueOf(tc)
            }
            isElevated -> {
                val bg = if (bgTintAttr.isNotEmpty()) {
                    ProjectResourceResolver.resolveColor(context, config.filePath, bgTintAttr, config.isDarkMode, config.surfaceContainerLow)
                } else config.surfaceContainerLow
                btn.backgroundTintList = ColorStateList.valueOf(bg)
                val tc = if (textColorAttr.isNotEmpty()) {
                    ProjectResourceResolver.resolveColor(context, config.filePath, textColorAttr, config.isDarkMode, config.primary)
                } else config.primary
                btn.setTextColor(tc)
                btn.iconTint = ColorStateList.valueOf(tc)
                btn.elevation = dpToPx(context, 2).toFloat()
            }
            else -> {
                val bg = if (bgTintAttr.isNotEmpty()) {
                    ProjectResourceResolver.resolveColor(context, config.filePath, bgTintAttr, config.isDarkMode, config.primary)
                } else config.primary
                btn.backgroundTintList = ColorStateList.valueOf(bg)
                val tc = if (textColorAttr.isNotEmpty()) {
                    ProjectResourceResolver.resolveColor(context, config.filePath, textColorAttr, config.isDarkMode, config.onPrimary)
                } else config.onPrimary
                btn.setTextColor(tc)
                btn.iconTint = ColorStateList.valueOf(tc)
            }
        }

        // Icon
        val iconAttr = node.getAttribute("icon", node.getAttribute("drawableStart", node.getAttribute("drawableLeft", "")))
        if (iconAttr.isNotEmpty()) {
            val d = ProjectResourceResolver.resolveDrawable(context, config.filePath, iconAttr, config.isDarkMode)
            if (d != null) {
                btn.icon = d
                val iconGravityAttr = node.getAttribute("iconGravity").lowercase(Locale.ROOT)
                btn.iconGravity = when (iconGravityAttr) {
                    "textstart", "text_start" -> MaterialButton.ICON_GRAVITY_TEXT_START
                    "textend", "text_end" -> MaterialButton.ICON_GRAVITY_TEXT_END
                    "end" -> MaterialButton.ICON_GRAVITY_END
                    "texttop", "text_top" -> MaterialButton.ICON_GRAVITY_TEXT_TOP
                    else -> MaterialButton.ICON_GRAVITY_START
                }
                val iconPaddingAttr = node.getAttribute("iconPadding")
                if (iconPaddingAttr.isNotEmpty()) {
                    btn.iconPadding = dpToPx(context, parseDimensionDp(iconPaddingAttr, 8))
                }
            }
        }

        return btn
    }

    private fun renderEditText(context: Context, node: XmlNode, config: XmlRenderConfig): TextInputEditText {
        val et = TextInputEditText(context)
        val text = node.getAttribute("text")
        if (text.isNotEmpty()) et.setText(ProjectResourceResolver.resolveString(context, config.filePath, text))

        val hint = node.getAttribute("hint")
        if (hint.isNotEmpty()) et.hint = ProjectResourceResolver.resolveString(context, config.filePath, hint)

        val colorAttr = node.getAttribute("textColor")
        val color = if (colorAttr.isNotEmpty()) {
            ProjectResourceResolver.resolveColor(context, config.filePath, colorAttr, config.isDarkMode, config.onSurface)
        } else {
            config.onSurface
        }
        et.setTextColor(color)
        et.setHintTextColor(config.onSurfaceVariant)

        val sizeAttr = node.getAttribute("textSize")
        val sizeSp = if (sizeAttr.isNotEmpty()) parseDimensionDp(sizeAttr, 16).toFloat() else 16f
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp * config.fontScale)

        val inputTypeAttr = node.getAttribute("inputType").lowercase(Locale.ROOT)
        if (inputTypeAttr.contains("password")) {
            et.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
        }
        if (inputTypeAttr.contains("multiline")) {
            et.isSingleLine = false
            et.minLines = 3
        } else {
            et.isSingleLine = true
        }

        val linesAttr = node.getAttribute("lines")
        linesAttr.toIntOrNull()?.let { et.setLines(it) }
        val maxLinesAttr = node.getAttribute("maxLines")
        maxLinesAttr.toIntOrNull()?.let { et.maxLines = it }
        val minLinesAttr = node.getAttribute("minLines")
        minLinesAttr.toIntOrNull()?.let { et.minLines = it }

        return et
    }

    private fun renderTextInputLayout(context: Context, node: XmlNode, config: XmlRenderConfig): TextInputLayout {
        val til = TextInputLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Style & Box Mode (Outlined vs Filled)
        val styleAttr = node.getAttribute("style")
        val boxModeAttr = node.getAttribute("boxBackgroundMode").lowercase(Locale.ROOT)
        when {
            boxModeAttr == "outline" || boxModeAttr == "outlined" || styleAttr.contains("OutlinedBox", ignoreCase = true) -> {
                til.boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            }
            boxModeAttr == "filled" || styleAttr.contains("FilledBox", ignoreCase = true) -> {
                til.boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_FILLED
            }
            boxModeAttr == "none" -> {
                til.boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_NONE
            }
            else -> {
                // Default in Material 3 is Outlined box
                til.boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            }
        }

        // Corner Radius
        val radiusAttr = node.getAttribute("boxCornerRadiusTopStart",
            node.getAttribute("cornerRadius", node.getAttribute("boxCornerRadius", "")))
        val defaultRadius = if (til.boxBackgroundMode == TextInputLayout.BOX_BACKGROUND_OUTLINE) 8 else 4
        val rTopStart = dpToPx(context, parseDimensionDp(node.getAttribute("boxCornerRadiusTopStart", radiusAttr), defaultRadius)).toFloat()
        val rTopEnd = dpToPx(context, parseDimensionDp(node.getAttribute("boxCornerRadiusTopEnd", radiusAttr), defaultRadius)).toFloat()
        val rBottomEnd = dpToPx(context, parseDimensionDp(node.getAttribute("boxCornerRadiusBottomEnd", radiusAttr), defaultRadius)).toFloat()
        val rBottomStart = dpToPx(context, parseDimensionDp(node.getAttribute("boxCornerRadiusBottomStart", radiusAttr), defaultRadius)).toFloat()
        til.setBoxCornerRadii(rTopStart, rTopEnd, rBottomEnd, rBottomStart)

        // Box Stroke & Colors
        val strokeColorAttr = node.getAttribute("boxStrokeColor")
        val strokeColor = if (strokeColorAttr.isNotEmpty()) {
            ProjectResourceResolver.resolveColor(context, config.filePath, strokeColorAttr, config.isDarkMode, config.primary)
        } else {
            config.primary
        }
        til.boxStrokeColor = strokeColor
        val strokeColorStates = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_focused),
                intArrayOf()
            ),
            intArrayOf(
                strokeColor,
                config.outline
            )
        )
        til.setBoxStrokeColorStateList(strokeColorStates)

        val boxBgAttr = node.getAttribute("boxBackgroundColor")
        if (boxBgAttr.isNotEmpty()) {
            val bg = ProjectResourceResolver.resolveColor(context, config.filePath, boxBgAttr, config.isDarkMode, config.surfaceContainerHigh)
            til.boxBackgroundColor = bg
        } else if (til.boxBackgroundMode == TextInputLayout.BOX_BACKGROUND_FILLED) {
            til.boxBackgroundColor = config.surfaceContainerHigh
        }

        // Hint & Floating Label
        val hint = node.getAttribute("hint")
        if (hint.isNotEmpty()) {
            til.hint = ProjectResourceResolver.resolveString(context, config.filePath, hint)
            til.isHintEnabled = true
        }
        val hintColorAttr = node.getAttribute("hintTextColor")
        val hintColor = if (hintColorAttr.isNotEmpty()) {
            ProjectResourceResolver.resolveColor(context, config.filePath, hintColorAttr, config.isDarkMode, config.primary)
        } else config.primary
        til.defaultHintTextColor = ColorStateList.valueOf(config.onSurfaceVariant)
        til.hintTextColor = ColorStateList.valueOf(hintColor)

        // Helper Text
        val helperText = node.getAttribute("helperText")
        if (helperText.isNotEmpty()) {
            til.helperText = ProjectResourceResolver.resolveString(context, config.filePath, helperText)
            til.isHelperTextEnabled = true
            til.setHelperTextColor(ColorStateList.valueOf(config.onSurfaceVariant))
        }

        // Error Text
        val errorText = node.getAttribute("error")
        if (errorText.isNotEmpty()) {
            til.error = ProjectResourceResolver.resolveString(context, config.filePath, errorText)
            til.isErrorEnabled = true
        }

        // Counter
        val counterEnabled = "true".equals(node.getAttribute("counterEnabled", "false"), ignoreCase = true)
        if (counterEnabled) {
            til.isCounterEnabled = true
            val maxLen = node.getAttribute("counterMaxLength").toIntOrNull()
            if (maxLen != null && maxLen > 0) {
                til.counterMaxLength = maxLen
            }
        }

        // Prefix & Suffix
        val prefix = node.getAttribute("prefixText")
        if (prefix.isNotEmpty()) til.prefixText = prefix
        val suffix = node.getAttribute("suffixText")
        if (suffix.isNotEmpty()) til.suffixText = suffix

        // Start Icon
        val startIconAttr = node.getAttribute("startIconDrawable")
        if (startIconAttr.isNotEmpty()) {
            val d = ProjectResourceResolver.resolveDrawable(context, config.filePath, startIconAttr, config.isDarkMode)
            if (d != null) {
                til.startIconDrawable = d
                val tintAttr = node.getAttribute("startIconTint")
                val tint = if (tintAttr.isNotEmpty()) {
                    ProjectResourceResolver.resolveColor(context, config.filePath, tintAttr, config.isDarkMode, config.onSurfaceVariant)
                } else config.onSurfaceVariant
                til.setStartIconTintList(ColorStateList.valueOf(tint))
            }
        }

        // End Icon
        val endIconModeAttr = node.getAttribute("endIconMode").lowercase(Locale.ROOT)
        when {
            endIconModeAttr == "password_toggle" -> til.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            endIconModeAttr == "clear_text" -> til.endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
            endIconModeAttr == "dropdown_menu" -> til.endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
            endIconModeAttr == "custom" -> til.endIconMode = TextInputLayout.END_ICON_CUSTOM
            endIconModeAttr.isNotEmpty() -> til.endIconMode = TextInputLayout.END_ICON_CUSTOM
        }
        val endIconAttr = node.getAttribute("endIconDrawable")
        if (endIconAttr.isNotEmpty()) {
            val d = ProjectResourceResolver.resolveDrawable(context, config.filePath, endIconAttr, config.isDarkMode)
            if (d != null) {
                til.endIconDrawable = d
                val tintAttr = node.getAttribute("endIconTint")
                val tint = if (tintAttr.isNotEmpty()) {
                    ProjectResourceResolver.resolveColor(context, config.filePath, tintAttr, config.isDarkMode, config.onSurfaceVariant)
                } else config.onSurfaceVariant
                til.setEndIconTintList(ColorStateList.valueOf(tint))
            }
        }

        // Children: Render child TextInputEditText or auto-generate one like Android Studio LayoutLib
        var hasEditText = false
        for (childNode in node.children) {
            val childView = renderNode(context, childNode, config)
            if (childView != null) {
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                til.addView(childView, lp)
                if (childView is TextInputEditText || childView is android.widget.EditText) {
                    hasEditText = true
                }
            }
        }

        if (!hasEditText) {
            val autoEt = TextInputEditText(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setTextColor(config.onSurface)
                setHintTextColor(config.onSurfaceVariant)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f * config.fontScale)
            }
            til.addView(autoEt)
        }

        return til
    }

    private fun renderRadioGroup(context: Context, node: XmlNode, config: XmlRenderConfig): RadioGroup {
        val rg = RadioGroup(context)
        val orientation = node.getAttribute("orientation", "vertical")
        rg.orientation = if (orientation.equals("horizontal", ignoreCase = true)) {
            RadioGroup.HORIZONTAL
        } else {
            RadioGroup.VERTICAL
        }
        for (childNode in node.children) {
            val childView = renderNode(context, childNode, config)
            if (childView != null) rg.addView(childView, createLinearLayoutParams(context, childNode))
        }
        return rg
    }

    private fun renderChip(context: Context, node: XmlNode, config: XmlRenderConfig): View {
        val chip = Chip(context)
        val text = node.getAttribute("text", "Chip")
        chip.text = ProjectResourceResolver.resolveString(context, config.filePath, text)

        val iconAttr = node.getAttribute("chipIcon")
        if (iconAttr.isNotEmpty()) {
            val d = ProjectResourceResolver.resolveDrawable(context, config.filePath, iconAttr, config.isDarkMode)
            if (d != null) chip.chipIcon = d
        }

        chip.isCheckable = "true".equals(node.getAttribute("checkable", "false"), ignoreCase = true)
        chip.isChecked = "true".equals(node.getAttribute("checked", "false"), ignoreCase = true)
        chip.isCloseIconVisible = "true".equals(node.getAttribute("closeIconVisible", "false"), ignoreCase = true)
        chip.setTextColor(config.onSurface)
        return chip
    }

    private fun renderChipGroup(context: Context, node: XmlNode, config: XmlRenderConfig): View {
        val group = ChipGroup(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        for (childNode in node.children) {
            val childView = renderNode(context, childNode, config)
            if (childView != null) group.addView(childView)
        }
        return group
    }

    private fun renderTabLayout(context: Context, node: XmlNode, config: XmlRenderConfig): View {
        val tabLayout = TabLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(context, 48))
            setSelectedTabIndicatorColor(config.primary)
            setTabTextColors(config.onSurfaceVariant, config.primary)
        }

        if (node.children.isNotEmpty()) {
            for (child in node.children) {
                val tabText = child.getAttribute("text", "Tab")
                val tab = tabLayout.newTab().setText(ProjectResourceResolver.resolveString(context, config.filePath, tabText))
                val iconAttr = child.getAttribute("icon")
                if (iconAttr.isNotEmpty()) {
                    tab.icon = ProjectResourceResolver.resolveDrawable(context, config.filePath, iconAttr, config.isDarkMode)
                }
                tabLayout.addTab(tab)
            }
        } else {
            tabLayout.addTab(tabLayout.newTab().setText("Tab 1"), true)
            tabLayout.addTab(tabLayout.newTab().setText("Tab 2"))
            tabLayout.addTab(tabLayout.newTab().setText("Tab 3"))
        }

        val bgAttr = node.getAttribute("background")
        val bgColor = if (bgAttr.isNotEmpty()) {
            ProjectResourceResolver.resolveColor(context, config.filePath, bgAttr, config.isDarkMode, config.surface)
        } else config.surface
        tabLayout.setBackgroundColor(bgColor)

        return tabLayout
    }

    private fun renderBottomNavigationView(context: Context, node: XmlNode, config: XmlRenderConfig): View {
        val bnv = BottomNavigationView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(context, 80))
            setBackgroundColor(config.surfaceContainer)
            itemIconTintList = ColorStateList.valueOf(config.onSurface)
            itemTextColor = ColorStateList.valueOf(config.onSurface)
        }

        bnv.menu.add(0, 1, 0, "Home").setIcon(android.R.drawable.ic_menu_myplaces)
        bnv.menu.add(0, 2, 0, "Search").setIcon(android.R.drawable.ic_menu_search)
        bnv.menu.add(0, 3, 0, "Settings").setIcon(android.R.drawable.ic_menu_preferences)
        bnv.selectedItemId = 1

        return bnv
    }

    private fun renderExtendedFab(context: Context, node: XmlNode, config: XmlRenderConfig): View {
        val efab = ExtendedFloatingActionButton(context)
        val text = node.getAttribute("text", "Action")
        efab.text = ProjectResourceResolver.resolveString(context, config.filePath, text)

        val iconAttr = node.getAttribute("icon", node.getAttribute("src", ""))
        if (iconAttr.isNotEmpty()) {
            val d = ProjectResourceResolver.resolveDrawable(context, config.filePath, iconAttr, config.isDarkMode, config.onPrimary)
            if (d != null) efab.icon = d
        }
        efab.backgroundTintList = ColorStateList.valueOf(config.primary)
        efab.setTextColor(config.onPrimary)
        efab.iconTint = ColorStateList.valueOf(config.onPrimary)
        return efab
    }

    private fun renderSlider(context: Context, node: XmlNode, config: XmlRenderConfig): View {
        return try {
            val slider = Slider(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            val valFrom = node.getAttribute("valueFrom").toFloatOrNull() ?: 0f
            val valTo = node.getAttribute("valueTo").toFloatOrNull() ?: 100f
            val value = node.getAttribute("value").toFloatOrNull() ?: 50f
            slider.valueFrom = valFrom
            slider.valueTo = valTo
            slider.value = value.coerceIn(valFrom, valTo)
            slider.thumbTintList = ColorStateList.valueOf(config.primary)
            slider.trackActiveTintList = ColorStateList.valueOf(config.primary)
            slider.trackInactiveTintList = ColorStateList.valueOf(config.surfaceContainerHigh)
            slider
        } catch (_: Throwable) {
            ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                progress = 50
            }
        }
    }

    private fun renderRecyclerView(context: Context, node: XmlNode, config: XmlRenderConfig): View {
        val listLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            clipToPadding = false
            clipChildren = false
        }

        val listItemRef = node.getAttribute("listitem")
        var renderedItemAst: XmlNode? = null
        if (listItemRef.isNotEmpty() && listItemRef.startsWith("@layout/")) {
            val layoutName = listItemRef.substringAfterLast('/')
            val resDir = ProjectResourceResolver.findResDir(config.filePath)
            val candidateFolders = resDir?.listFiles { f -> f.isDirectory && f.name.startsWith("layout") }
            if (candidateFolders != null) {
                for (folder in candidateFolders) {
                    val f = File(folder, "$layoutName.xml")
                    if (f.exists() && f.isFile) {
                        try {
                            renderedItemAst = XmlLayoutParser.parse(f.readText())
                            break
                        } catch (_: Throwable) {}
                    }
                }
            }
        }

        if (renderedItemAst != null) {
            for (i in 0 until 3) {
                val itemView = renderNode(context, renderedItemAst, config)
                if (itemView != null) {
                    listLayout.addView(itemView)
                }
            }
        } else {
            // Android Studio sample list items
            for (i in 1..3) {
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dpToPx(context, 16), dpToPx(context, 12), dpToPx(context, 16), dpToPx(context, 12))
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                }
                val icon = ImageView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(context, 40), dpToPx(context, 40))
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(config.surfaceContainerHigh)
                    }
                    setImageDrawable(ProjectResourceResolver.getFallbackAndroidIcon(context, "list", config.primary))
                    val p = dpToPx(context, 8)
                    setPadding(p, p, p, p)
                }
                val textCol = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginStart = dpToPx(context, 16)
                    }
                }
                val title = TextView(context).apply {
                    text = "Item $i"
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(config.onSurface)
                }
                val sub = TextView(context).apply {
                    text = "Secondary supporting text for item $i"
                    textSize = 13f
                    setTextColor(config.onSurfaceVariant)
                }
                textCol.addView(title)
                textCol.addView(sub)
                row.addView(icon)
                row.addView(textCol)
                listLayout.addView(row)
            }
        }

        return listLayout
    }

    private fun renderInclude(context: Context, node: XmlNode, config: XmlRenderConfig): View? {
        val layoutRef = node.getAttribute("layout")
        if (layoutRef.isBlank()) return null
        val layoutName = layoutRef.substringAfterLast('/')
        val resDir = ProjectResourceResolver.findResDir(config.filePath) ?: return null
        val candidateFolders = resDir.listFiles { f -> f.isDirectory && f.name.startsWith("layout") } ?: return null
        for (folder in candidateFolders) {
            val f = File(folder, "$layoutName.xml")
            if (f.exists() && f.isFile) {
                try {
                    val xmlText = f.readText()
                    val includedAst = XmlLayoutParser.parse(xmlText)
                    return renderNode(context, includedAst, config)
                } catch (_: Throwable) {}
            }
        }
        return null
    }

    private fun renderCheckBox(context: Context, node: XmlNode, config: XmlRenderConfig): CheckBox {
        val cb = CheckBox(context)
        val text = node.getAttribute("text")
        if (text.isNotEmpty()) cb.text = ProjectResourceResolver.resolveString(context, config.filePath, text)
        cb.isChecked = "true".equals(node.getAttribute("checked", "false"), ignoreCase = true)
        cb.setTextColor(config.onSurface)
        return cb
    }

    private fun renderRadioButton(context: Context, node: XmlNode, config: XmlRenderConfig): RadioButton {
        val rb = RadioButton(context)
        val text = node.getAttribute("text")
        if (text.isNotEmpty()) rb.text = ProjectResourceResolver.resolveString(context, config.filePath, text)
        rb.isChecked = "true".equals(node.getAttribute("checked", "false"), ignoreCase = true)
        rb.setTextColor(config.onSurface)
        return rb
    }

    private fun renderSwitch(context: Context, node: XmlNode, config: XmlRenderConfig): View {
        return try {
            val sw = MaterialSwitch(context)
            val text = node.getAttribute("text")
            if (text.isNotEmpty()) sw.text = ProjectResourceResolver.resolveString(context, config.filePath, text)
            sw.isChecked = "true".equals(node.getAttribute("checked", "false"), ignoreCase = true)
            sw.setTextColor(config.onSurface)
            sw
        } catch (_: Throwable) {
            val sw = SwitchCompat(context)
            val text = node.getAttribute("text")
            if (text.isNotEmpty()) sw.text = ProjectResourceResolver.resolveString(context, config.filePath, text)
            sw.isChecked = "true".equals(node.getAttribute("checked", "false"), ignoreCase = true)
            sw.setTextColor(config.onSurface)
            sw
        }
    }

    private fun renderProgressBar(context: Context, node: XmlNode, config: XmlRenderConfig): ProgressBar {
        val pb = ProgressBar(context)
        pb.isIndeterminate = true
        return pb
    }

    private fun renderFab(context: Context, node: XmlNode, config: XmlRenderConfig): FloatingActionButton {
        val fab = FloatingActionButton(context)
        val src = node.getAttribute("src", node.getAttribute("srcCompat", ""))
        val drawable = ProjectResourceResolver.resolveDrawable(
            context = context,
            filePath = config.filePath,
            drawableRef = src,
            isDark = config.isDarkMode,
            tintColor = config.onPrimary
        )
        if (drawable != null) fab.setImageDrawable(drawable)
        fab.backgroundTintList = ColorStateList.valueOf(config.primary)
        return fab
    }

    private fun renderDividerView(context: Context, node: XmlNode, config: XmlRenderConfig): View {
        val v = View(context)
        val bgAttr = node.getAttribute("background")
        if (bgAttr.isNotEmpty()) {
            val d = ProjectResourceResolver.resolveDrawable(context, config.filePath, bgAttr, config.isDarkMode)
            if (d != null) v.background = d
            else v.setBackgroundColor(config.outlineVariant)
        } else {
            v.setBackgroundColor(config.outlineVariant)
        }
        return v
    }

    private fun applyCommonViewAttributes(context: Context, view: View, node: XmlNode, config: XmlRenderConfig) {
        // Background
        val bgAttr = node.getAttribute("background")
        if (bgAttr.isNotEmpty() && view !is MaterialCardView && view !is MaterialButton && view !is MaterialToolbar && view !is ImageView) {
            if (ProjectResourceResolver.isColorExpression(bgAttr)) {
                val color = ProjectResourceResolver.resolveColor(context, config.filePath, bgAttr, config.isDarkMode, Color.TRANSPARENT)
                if (color != Color.TRANSPARENT) view.setBackgroundColor(color)
            } else {
                val bgDrawable = ProjectResourceResolver.resolveDrawable(
                    context = context,
                    filePath = config.filePath,
                    drawableRef = bgAttr,
                    isDark = config.isDarkMode,
                    isBackground = true
                )
                if (bgDrawable != null) {
                    view.background = bgDrawable
                } else {
                    val color = ProjectResourceResolver.resolveColor(context, config.filePath, bgAttr, config.isDarkMode, Color.TRANSPARENT)
                    if (color != Color.TRANSPARENT) view.setBackgroundColor(color)
                }
            }
        }

        // Padding
        var pStart = view.paddingStart
        var pTop = view.paddingTop
        var pEnd = view.paddingEnd
        var pBottom = view.paddingBottom

        val paddingAttr = node.getAttribute("padding")
        if (paddingAttr.isNotEmpty()) {
            val p = dpToPx(context, parseDimensionDp(paddingAttr, 0))
            pStart = p; pTop = p; pEnd = p; pBottom = p
        }

        val paddingHAttr = node.getAttribute("paddingHorizontal")
        if (paddingHAttr.isNotEmpty()) {
            val p = dpToPx(context, parseDimensionDp(paddingHAttr, 0))
            pStart = p; pEnd = p
        }

        val paddingVAttr = node.getAttribute("paddingVertical")
        if (paddingVAttr.isNotEmpty()) {
            val p = dpToPx(context, parseDimensionDp(paddingVAttr, 0))
            pTop = p; pBottom = p
        }

        val pL = node.getAttribute("paddingLeft", node.getAttribute("paddingStart", ""))
        if (pL.isNotEmpty()) pStart = dpToPx(context, parseDimensionDp(pL, 0))

        val pT = node.getAttribute("paddingTop")
        if (pT.isNotEmpty()) pTop = dpToPx(context, parseDimensionDp(pT, 0))

        val pR = node.getAttribute("paddingRight", node.getAttribute("paddingEnd", ""))
        if (pR.isNotEmpty()) pEnd = dpToPx(context, parseDimensionDp(pR, 0))

        val pB = node.getAttribute("paddingBottom")
        if (pB.isNotEmpty()) pBottom = dpToPx(context, parseDimensionDp(pB, 0))

        view.setPaddingRelative(pStart, pTop, pEnd, pBottom)
    }

    private fun createLinearLayoutParams(context: Context, node: XmlNode): LinearLayout.LayoutParams {
        val w = parseLayoutDimension(context, node.getAttribute("layout_width", "wrap_content"))
        var h = parseLayoutDimension(context, node.getAttribute("layout_height", "wrap_content"))

        // Default Toolbar to 56dp (actionBarSize) when height is wrap_content
        val simpleTag = node.simpleName
        if ((simpleTag.contains("Toolbar", ignoreCase = true)) && h == ViewGroup.LayoutParams.WRAP_CONTENT) {
            h = dpToPx(context, 56)
        }

        val weightStr = node.getAttribute("layout_weight")
        var weight = weightStr.toFloatOrNull() ?: 0f

        // If a view (e.g. ScrollView) specifies match_parent height in a vertical layout without explicit weight,
        // give it weight 1f so it cleanly fills the remaining screen height below toolbars/headers
        if (weight == 0f && h == ViewGroup.LayoutParams.MATCH_PARENT) {
            weight = 1.0f
        }

        val lp = LinearLayout.LayoutParams(w, h, weight)

        // Gravity
        val gravStr = node.getAttribute("layout_gravity")
        if (gravStr.isNotEmpty()) lp.gravity = parseGravity(gravStr)

        // Margins
        applyMargins(context, node, lp)
        return lp
    }

    private fun createFrameLayoutParams(context: Context, node: XmlNode): FrameLayout.LayoutParams {
        val w = parseLayoutDimension(context, node.getAttribute("layout_width", "wrap_content"))
        val h = parseLayoutDimension(context, node.getAttribute("layout_height", "wrap_content"))
        val lp = FrameLayout.LayoutParams(w, h)

        val gravStr = node.getAttribute("layout_gravity")
        if (gravStr.isNotEmpty()) lp.gravity = parseGravity(gravStr)

        applyMargins(context, node, lp)
        return lp
    }

    private fun applyMargins(context: Context, node: XmlNode, lp: ViewGroup.MarginLayoutParams) {
        val marginAttr = node.getAttribute("layout_margin")
        if (marginAttr.isNotEmpty()) {
            val m = dpToPx(context, parseDimensionDp(marginAttr, 0))
            lp.setMargins(m, m, m, m)
        }

        val mL = node.getAttribute("layout_marginLeft", node.getAttribute("layout_marginStart", ""))
        if (mL.isNotEmpty()) {
            val px = dpToPx(context, parseDimensionDp(mL, 0))
            lp.leftMargin = px
            lp.marginStart = px
        }

        val mT = node.getAttribute("layout_marginTop")
        if (mT.isNotEmpty()) lp.topMargin = dpToPx(context, parseDimensionDp(mT, 0))

        val mR = node.getAttribute("layout_marginRight", node.getAttribute("layout_marginEnd", ""))
        if (mR.isNotEmpty()) {
            val px = dpToPx(context, parseDimensionDp(mR, 0))
            lp.rightMargin = px
            lp.marginEnd = px
        }

        val mB = node.getAttribute("layout_marginBottom")
        if (mB.isNotEmpty()) lp.bottomMargin = dpToPx(context, parseDimensionDp(mB, 0))
    }

    private fun parseLayoutDimension(context: Context, dimenStr: String): Int {
        val trimmed = dimenStr.trim().lowercase(Locale.ROOT)
        return when {
            trimmed == "match_parent" || trimmed == "fill_parent" -> ViewGroup.LayoutParams.MATCH_PARENT
            trimmed == "wrap_content" -> ViewGroup.LayoutParams.WRAP_CONTENT
            trimmed.endsWith("dp") || trimmed.endsWith("dip") -> {
                val v = parseDimensionDp(trimmed, 0)
                dpToPx(context, v)
            }
            trimmed.endsWith("px") -> trimmed.replace("px", "").toIntOrNull() ?: 0
            else -> ViewGroup.LayoutParams.WRAP_CONTENT
        }
    }

    private fun parseDimensionDp(dimenStr: String, defaultVal: Int): Int {
        val clean = dimenStr.replace(Regex("[^0-9.]"), "")
        return clean.toFloatOrNull()?.toInt() ?: defaultVal
    }

    private fun parseGravity(gravityStr: String): Int {
        var g = Gravity.NO_GRAVITY
        val parts = gravityStr.split('|')
        for (p in parts) {
            when (p.trim().lowercase(Locale.ROOT)) {
                "center" -> g = g or Gravity.CENTER
                "center_horizontal" -> g = g or Gravity.CENTER_HORIZONTAL
                "center_vertical" -> g = g or Gravity.CENTER_VERTICAL
                "start", "left" -> g = g or Gravity.START
                "end", "right" -> g = g or Gravity.END
                "top" -> g = g or Gravity.TOP
                "bottom" -> g = g or Gravity.BOTTOM
            }
        }
        return if (g == Gravity.NO_GRAVITY) Gravity.START else g
    }

    private fun setupInspectListener(view: View, node: XmlNode, config: XmlRenderConfig) {
        view.tag = node
        if (config.isInspectMode && config.inspectListener != null) {
            view.setOnClickListener {
                config.inspectListener?.onNodeSelected(node, view)
            }
        }
    }

    fun findViewForNode(root: View?, target: XmlNode?): View? {
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

    private fun dpToPx(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
