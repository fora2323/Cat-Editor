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
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Android Studio-grade Jetpack Compose & XML Preview Viewport.
 * Features full-width/height responsive rendering, natural vertical scrolling,
 * auto-scroll to inspected elements, inspect-mode scroll locking,
 * optional device frame with authentic demo status bar, Light/Dark system simulation,
 * and high-fidelity component inspection overlays.
 */
class ComposePreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    /**
     * Custom ScrollView that can disable scroll interception during inspect mode.
     */
    class InspectableScrollView(context: Context) : ScrollView(context) {
        var isScrollDisabled: Boolean = false

        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
            if (isScrollDisabled) {
                return false
            }
            return super.onInterceptTouchEvent(ev)
        }

        override fun onTouchEvent(ev: MotionEvent): Boolean {
            if (isScrollDisabled) {
                return false
            }
            return super.onTouchEvent(ev)
        }
    }

    enum class DevicePreset(val label: String, val widthDp: Int, val heightDp: Int) {
        RESPONSIVE("Fit to Screen", -1, -1),
        PHONE("Phone", 412, 915)
    }

    var isShowDeviceFrame: Boolean = false
        private set
    var isShowSystemUi: Boolean = false
        private set
    var devicePreset: DevicePreset = DevicePreset.RESPONSIVE
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
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    var isInspectMode: Boolean = false
        private set
    var onNodeTapListener: ComposeRenderer.OnNodeSelectedListener? = null

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

        // Demo Status Bar
        val sb = createStatusBar(context)
        statusBarView = sb
        screen.addView(sb)

        // Viewport takes all remaining vertical space
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

        // Demo Navigation Bar
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
        val sb = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(28))
            setPadding(dpToPx(16), 0, dpToPx(16), 0)
        }

        val timeTv = TextView(context).apply {
            text = "9:41"
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        timeTextView = timeTv
        sb.addView(timeTv)

        val spacer = View(context)
        sb.addView(spacer, LinearLayout.LayoutParams(0, 1, 1.0f))

        val cameraHole = View(context).apply {
            val holeSize = dpToPx(10)
            layoutParams = LinearLayout.LayoutParams(holeSize, holeSize)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#111312"))
            }
        }
        sb.addView(cameraHole)

        val spacer2 = View(context)
        sb.addView(spacer2, LinearLayout.LayoutParams(0, 1, 1.0f))

        val iconsTv = TextView(context).apply {
            text = "5G  100%"
            textSize = 11f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        iconsTextView = iconsTv
        sb.addView(iconsTv)

        return sb
    }

    private fun createNavigationBar(context: Context): View {
        val nb = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(22))
        }

        val pill = View(context).apply {
            layoutParams = LayoutParams(dpToPx(100), dpToPx(4), Gravity.CENTER)
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(2).toFloat()
            }
        }
        pillView = pill
        nb.addView(pill)
        return nb
    }

    private fun updateDeviceShellAppearance() {
        if (isShowDeviceFrame) {
            val shellBg = GradientDrawable().apply {
                cornerRadius = dpToPx(24).toFloat()
                setColor(if (isDarkMode) Color.parseColor("#1C201F") else Color.parseColor("#E0E3E1"))
                setStroke(dpToPx(3), if (isDarkMode) Color.parseColor("#343A38") else Color.parseColor("#B0B5B3"))
            }
            phoneShell?.background = shellBg
            val pad = dpToPx(6)
            phoneShell?.setPadding(pad, pad, pad, pad)
            val lp = phoneShell?.layoutParams as? MarginLayoutParams
            val m = dpToPx(8)
            lp?.setMargins(m, m, m, m)
            phoneShell?.layoutParams = lp
        } else {
            phoneShell?.background = null
            phoneShell?.setPadding(0, 0, 0, 0)
            val lp = phoneShell?.layoutParams as? MarginLayoutParams
            lp?.setMargins(0, 0, 0, 0)
            phoneShell?.layoutParams = lp
        }
        screenContainer?.invalidateOutline()
    }

    private fun updateSystemUiAppearance() {
        val screenBg = currentSurfaceColor
        val barBg = if (isDarkMode) Color.parseColor("#1C201F") else Color.parseColor("#EAEAEA")

        screenContainer?.setBackgroundColor(screenBg)
        statusBarView?.setBackgroundColor(barBg)
        viewportContent?.setBackgroundColor(screenBg)
        navigationBarView?.setBackgroundColor(barBg)

        val textColor = if (isDarkMode) Color.parseColor("#E0E3E1") else Color.parseColor("#191C1B")
        timeTextView?.setTextColor(textColor)
        iconsTextView?.setTextColor(textColor)

        val pillColor = if (isDarkMode) Color.parseColor("#80E0E3E1") else Color.parseColor("#80191C1B")
        pillView?.background = GradientDrawable().apply {
            cornerRadius = dpToPx(2).toFloat()
            setColor(pillColor)
        }
    }

    private fun updateSystemUiVisibility() {
        // When device frame is shown, always show demo status bar & navigation bar
        statusBarView?.visibility = if (isShowDeviceFrame) VISIBLE else GONE
        navigationBarView?.visibility = if (isShowDeviceFrame) VISIBLE else GONE
    }

    fun setContent(renderedContent: View) {
        viewportContent?.removeAllViews()

        // If renderedContent is not already scrollable, wrap it in an InspectableScrollView
        val viewToAdd = if (isScrollableView(renderedContent)) {
            renderedContent
        } else {
            InspectableScrollView(context).apply {
                isFillViewport = true
                isVerticalScrollBarEnabled = true
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                addView(renderedContent)
            }
        }
        viewportContent?.addView(viewToAdd)
        updateScrollDisabling()
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

    private fun updateScrollDisabling() {
        setScrollDisabledRecursive(viewportContent, isInspectMode)
    }

    private fun setScrollDisabledRecursive(view: View?, disabled: Boolean) {
        if (view == null) return
        if (view is InspectableScrollView) {
            view.isScrollDisabled = disabled
        } else if (view is ScrollView) {
            if (disabled) {
                view.setOnTouchListener { _, event ->
                    // In inspect mode, consume scroll moves to disable scrolling
                    event.action == MotionEvent.ACTION_MOVE
                }
            } else {
                view.setOnTouchListener(null)
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setScrollDisabledRecursive(view.getChildAt(i), disabled)
            }
        }
    }

    fun applyThemeConfig(config: ComposeRenderer.RenderConfig) {
        this.isDarkMode = config.isDarkMode
        this.currentSurfaceColor = config.surface
        updateDeviceShellAppearance()
        updateSystemUiAppearance()
        invalidate()
    }

    fun setShowDeviceFrame(show: Boolean) {
        this.isShowDeviceFrame = show
        this.isShowSystemUi = show
        updateDeviceShellAppearance()
        updateSystemUiVisibility()
        requestLayout()
        invalidate()
    }

    fun setShowSystemUi(show: Boolean) {
        this.isShowSystemUi = show
        updateSystemUiVisibility()
        requestLayout()
        invalidate()
    }

    fun setDevicePreset(preset: DevicePreset) {
        this.devicePreset = preset
        invalidate()
    }

    private var touchStartX = 0f
    private var touchStartY = 0f

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (isInspectMode) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartX = ev.x
                    touchStartY = ev.y
                }
                MotionEvent.ACTION_UP -> {
                    val slop = dpToPx(8).toFloat()
                    val dx = Math.abs(ev.x - touchStartX)
                    val dy = Math.abs(ev.y - touchStartY)
                    if (dx < slop && dy < slop) {
                        handleInspectTap(ev.x, ev.y)
                    }
                }
            }
            // In inspect mode, intercept all touches to disable canvas scrolling and prevent underlying widget clicks
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (isInspectMode) {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartX = ev.x
                    touchStartY = ev.y
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    val slop = dpToPx(8).toFloat()
                    val dx = Math.abs(ev.x - touchStartX)
                    val dy = Math.abs(ev.y - touchStartY)
                    if (dx < slop && dy < slop) {
                        handleInspectTap(ev.x, ev.y)
                    }
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    // Consume move events to completely disable scrolling
                    return true
                }
            }
            return true
        }
        return super.onTouchEvent(ev)
    }

    private fun handleInspectTap(x: Float, y: Float) {
        val vp = viewportContent ?: return
        val location = IntArray(2)
        getLocationOnScreen(location)
        val globalX = (location[0] + x).toInt()
        val globalY = (location[1] + y).toInt()

        val target = findDeepestTaggedView(vp, globalX, globalY)
        if (target != null) {
            setInspectedView(target)
            val node = target.tag as? ComposeNode
            if (node != null) {
                onNodeTapListener?.onNodeSelected(node, target)
            }
        }
    }

    private fun findDeepestTaggedView(view: View, globalX: Int, globalY: Int): View? {
        if (!view.isShown) return null
        val r = Rect()
        view.getGlobalVisibleRect(r)
        if (!r.contains(globalX, globalY)) return null

        if (view is ViewGroup) {
            for (i in view.childCount - 1 downTo 0) {
                val child = view.getChildAt(i)
                val found = findDeepestTaggedView(child, globalX, globalY)
                if (found != null) return found
            }
        }

        if (view.tag is ComposeNode) {
            return view
        }
        return null
    }

    fun setInspectMode(inspectMode: Boolean) {
        this.isInspectMode = inspectMode
        if (!inspectMode) {
            setInspectedView(null)
        }
        updateScrollDisabling()
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

    /**
     * Automatically scrolls the viewport so the inspected element is brought into view.
     */
    fun scrollToTargetView(view: View) {
        post {
            val scroll = findParentScrollView(view) ?: findFirstScrollView(viewportContent)
            if (scroll != null) {
                val rect = Rect()
                view.getDrawingRect(rect)
                try {
                    scroll.offsetDescendantRectToMyCoords(view, rect)
                    val targetY = maxOf(0, rect.top - (scroll.height / 3))
                    scroll.smoothScrollTo(0, targetY)
                } catch (_: Exception) {
                    view.requestRectangleOnScreen(rect, true)
                }
            }
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
                if (tag is ComposeNode) {
                    val name = tag.type
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

    fun zoomFit() {
        requestLayout()
        invalidate()
    }

    fun zoomIn() {}
    fun zoomOut() {}

    fun captureSnapshot(): android.graphics.Bitmap? {
        val target = if (isShowDeviceFrame) (deviceFrameContainer ?: this) else (viewportContent ?: this)
        val w = target.width
        val h = target.height
        if (w <= 0 || h <= 0) return null
        val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        target.draw(canvas)
        return bitmap
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
