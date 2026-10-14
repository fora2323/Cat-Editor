package org.cat.editor.ui.preview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Android Studio-grade viewport specifically dedicated to XML Layout previews.
 * Features natural vertical scrolling, device frame simulation, Light/Dark theme rendering,
 * and high-fidelity XML inspect overlay displaying authentic XML tags and IDs.
 */
class XmlPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var isShowDeviceFrame: Boolean = false
        private set
    var isDarkMode: Boolean = false
        private set
    private var currentSurfaceColor = Color.parseColor("#FBFDFB")

    // Containers
    var deviceFrameContainer: FrameLayout? = null
        private set
    private var phoneShell: LinearLayout? = null
    private var screenContainer: LinearLayout? = null
    private var statusBarView: View? = null
    private var timeTextView: TextView? = null
    private var iconsTextView: TextView? = null
    var viewportContent: FrameLayout? = null
        private set
    private var navigationBarView: View? = null
    private var pillView: View? = null

    // Inspection overlay
    var inspectedView: View? = null
        private set
    private val fillHighlightPaint = Paint().apply {
        color = Color.parseColor("#333871E0")
    }
    private val strokeHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3871E0")
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2).toFloat()
    }
    private val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3871E0")
    }
    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = dpToPx(11).toFloat()
        typeface = android.graphics.Typeface.MONOSPACE
    }

    var isInspectMode: Boolean = true
        private set
    var onNodeTapListener: XmlLayoutRenderer.OnXmlNodeSelectedListener? = null

    init {
        setWillNotDraw(false)
        setupDeviceFrame(context)
    }

    private fun setupDeviceFrame(context: Context) {
        removeAllViews()

        val frameContainer = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        deviceFrameContainer = frameContainer

        val shell = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        phoneShell = shell

        val screen = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    if (isShowDeviceFrame) {
                        outline.setRoundRect(0, 0, view.width, view.height, dpToPx(20).toFloat())
                    } else {
                        outline.setRect(0, 0, view.width, view.height)
                    }
                }
            }
        }
        screenContainer = screen

        // Status Bar
        val sb = createStatusBar(context)
        statusBarView = sb
        screen.addView(sb)

        // Viewport
        val vp = object : FrameLayout(context) {
            override fun dispatchDraw(canvas: Canvas) {
                super.dispatchDraw(canvas)
                drawInspectOverlay(canvas)
            }
        }
        vp.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1.0f
        )
        vp.clipToPadding = true
        vp.clipChildren = true
        viewportContent = vp
        screen.addView(vp)

        // Navigation Bar
        val nb = createNavigationBar(context)
        navigationBarView = nb
        screen.addView(nb)

        shell.addView(screen)
        frameContainer.addView(shell)
        addView(frameContainer)

        updateDeviceShellAppearance()
        updateSystemUiAppearance()
        updateSystemUiVisibility()
    }

    private fun createStatusBar(context: Context): View {
        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(16), dpToPx(6), dpToPx(16), dpToPx(4))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(28)
            )
        }

        val timeTv = TextView(context).apply {
            text = "9:41"
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
        }
        timeTextView = timeTv
        bar.addView(timeTv)

        val iconsTv = TextView(context).apply {
            text = "●●● LTE 100%"
            textSize = 10f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        iconsTextView = iconsTv
        bar.addView(iconsTv)

        return bar
    }

    private fun createNavigationBar(context: Context): View {
        val nav = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(20)
            )
        }

        val pill = View(context).apply {
            val h = dpToPx(4)
            val w = dpToPx(72)
            layoutParams = LinearLayout.LayoutParams(w, h)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = h / 2f
            }
        }
        pillView = pill
        nav.addView(pill)
        return nav
    }

    fun setShowDeviceFrame(show: Boolean) {
        if (this.isShowDeviceFrame != show) {
            this.isShowDeviceFrame = show
            updateDeviceShellAppearance()
            updateSystemUiVisibility()
            screenContainer?.invalidateOutline()
            requestLayout()
            invalidate()
        }
    }

    fun applyThemeConfig(config: XmlLayoutRenderer.XmlRenderConfig) {
        this.isDarkMode = config.isDarkMode
        this.currentSurfaceColor = config.surface
        updateDeviceShellAppearance()
        updateSystemUiAppearance()
    }

    private fun updateSystemUiVisibility() {
        val vis = if (isShowDeviceFrame) View.VISIBLE else View.GONE
        statusBarView?.visibility = vis
        navigationBarView?.visibility = vis
    }

    private fun updateSystemUiAppearance() {
        val textColor = if (isDarkMode) Color.WHITE else Color.parseColor("#1C1B1F")
        timeTextView?.setTextColor(textColor)
        iconsTextView?.setTextColor(textColor)

        val pillColor = if (isDarkMode) Color.parseColor("#66FFFFFF") else Color.parseColor("#66000000")
        (pillView?.background as? GradientDrawable)?.setColor(pillColor)

        statusBarView?.setBackgroundColor(currentSurfaceColor)
        navigationBarView?.setBackgroundColor(currentSurfaceColor)
    }

    private fun updateDeviceShellAppearance() {
        if (isShowDeviceFrame) {
            val padH = dpToPx(10)
            val padV = dpToPx(10)
            phoneShell?.setPadding(padH, padV, padH, padV)
            phoneShell?.background = GradientDrawable().apply {
                setColor(if (isDarkMode) Color.parseColor("#1A1A1A") else Color.parseColor("#E0E0E0"))
                cornerRadius = dpToPx(28).toFloat()
                setStroke(dpToPx(2), if (isDarkMode) Color.parseColor("#333333") else Color.parseColor("#C4C4C4"))
            }
        } else {
            phoneShell?.setPadding(0, 0, 0, 0)
            phoneShell?.background = null
        }
        screenContainer?.setBackgroundColor(currentSurfaceColor)
        viewportContent?.setBackgroundColor(currentSurfaceColor)
    }

    fun setContent(renderedContent: View) {
        viewportContent?.removeAllViews()

        val viewToAdd = if (isScrollableView(renderedContent)) {
            renderedContent.apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            }
        } else {
            ComposePreviewView.InspectableScrollView(context).apply {
                isFillViewport = true
                isVerticalScrollBarEnabled = true
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                addView(renderedContent)
            }
        }
        viewportContent?.addView(viewToAdd)
        requestLayout()
        invalidate()
    }

    private fun isScrollableView(v: View): Boolean {
        if (v is ScrollView || v is androidx.core.widget.NestedScrollView ||
            v is android.widget.AbsListView || v is androidx.recyclerview.widget.RecyclerView) {
            return true
        }
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) {
                if (isScrollableView(v.getChildAt(i))) return true
            }
        }
        return false
    }

    fun setInspectMode(enabled: Boolean) {
        this.isInspectMode = enabled
        if (!enabled) {
            inspectedView = null
        }
        viewportContent?.invalidate()
        invalidate()
    }

    fun setInspectedView(view: View?) {
        this.inspectedView = view
        viewportContent?.invalidate()
        invalidate()
        if (view != null) {
            post {
                scrollToTargetView(view)
            }
        }
    }

    fun scrollToTargetView(view: View) {
        val scroll = findParentScrollView(view) ?: findFirstScrollView(viewportContent)
        val action = Runnable {
            if (scroll != null && (view.isShown || view.visibility == View.VISIBLE)) {
                val rect = Rect()
                view.getDrawingRect(rect)
                try {
                    scroll.offsetDescendantRectToMyCoords(view, rect)
                    val viewYInContent = rect.top + scroll.scrollY
                    val targetY = maxOf(0, viewYInContent - ((scroll.height - view.height) / 2).coerceAtLeast(scroll.height / 4))
                    scroll.smoothScrollTo(0, targetY)
                } catch (_: Exception) {
                    try {
                        view.requestRectangleOnScreen(rect, true)
                    } catch (_: Exception) {}
                }
            }

            val hScroll = findParentHorizontalScrollView(view)
            if (hScroll != null && (view.isShown || view.visibility == View.VISIBLE)) {
                val hRect = Rect()
                view.getDrawingRect(hRect)
                try {
                    hScroll.offsetDescendantRectToMyCoords(view, hRect)
                    val viewXInContent = hRect.left + hScroll.scrollX
                    val targetX = maxOf(0, viewXInContent - ((hScroll.width - view.width) / 2).coerceAtLeast(0))
                    hScroll.smoothScrollTo(targetX, 0)
                } catch (_: Exception) {}
            }
        }

        if (scroll != null && scroll.height > 0) {
            scroll.post(action)
        } else if (scroll != null) {
            scroll.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    scroll.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    scroll.post(action)
                }
            })
        }
    }

    private fun findParentScrollView(view: View?): ScrollView? {
        var p = view?.parent
        while (p != null) {
            if (p is ScrollView) return p
            p = p.parent
        }
        return null
    }

    private fun findParentHorizontalScrollView(view: View?): android.widget.HorizontalScrollView? {
        var p = view?.parent
        while (p != null) {
            if (p is android.widget.HorizontalScrollView) return p
            p = p.parent
        }
        return null
    }

    private fun findFirstScrollView(view: View?): ScrollView? {
        if (view == null) return null
        if (view is ScrollView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val s = findFirstScrollView(view.getChildAt(i))
                if (s != null) return s
            }
        }
        return null
    }

    private fun drawInspectOverlay(canvas: Canvas) {
        val view = inspectedView
        if (view != null && view.isShown) {
            val bounds = Rect()
            view.getDrawingRect(bounds)
            try {
                viewportContent?.offsetDescendantRectToMyCoords(view, bounds)
                canvas.drawRect(bounds, fillHighlightPaint)
                canvas.drawRect(bounds, strokeHighlightPaint)

                val tag = view.tag
                if (tag is XmlNode) {
                    val name = if (tag.shortId != null) {
                        "@id/${tag.shortId}"
                    } else {
                        "<${tag.simpleName}>"
                    }
                    val textWidth = badgeTextPaint.measureText(name)
                    val badgeH = dpToPx(20)
                    val badgeW = textWidth.toInt() + dpToPx(14)
                    val badgeTop = maxOf(0, bounds.top - badgeH - dpToPx(2))
                    val badgeRect = RectF(
                        bounds.left.toFloat(),
                        badgeTop.toFloat(),
                        (bounds.left + badgeW).toFloat(),
                        (badgeTop + badgeH).toFloat()
                    )
                    canvas.drawRoundRect(badgeRect, dpToPx(4).toFloat(), dpToPx(4).toFloat(), badgeBgPaint)
                    canvas.drawText(name, (bounds.left + dpToPx(7)).toFloat(), (badgeTop + dpToPx(14)).toFloat(), badgeTextPaint)
                }
            } catch (_: Exception) {}
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
