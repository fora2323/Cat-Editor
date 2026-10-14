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
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Android Studio-grade Jetpack Compose & XML Preview Viewport.
 * Features authentic device framing (Phone, Tablet, Foldable, Desktop),
 * pixel-perfect Light/Dark system UI simulation, zoom/pan navigation,
 * and high-fidelity component inspection overlays.
 */
class ComposePreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    enum class DevicePreset(val label: String, val widthDp: Int, val heightDp: Int) {
        PHONE("Pixel 7 (Phone)", 412, 915),
        COMPACT("Compact Phone", 360, 640),
        TABLET("10\" Tablet (Landscape)", 1280, 800),
        TABLET_PORTRAIT("Tablet (Portrait)", 800, 1280),
        FOLDABLE("Foldable (Unfolded)", 841, 673),
        FOLDABLE_CLOSED("Foldable (Outer)", 412, 915),
        DESKTOP("Desktop (1080p)", 1920, 1080),
        RESPONSIVE("Wrap Content (No Frame)", -1, -1)
    }

    enum class CanvasBackground {
        DEFAULT, WHITE, DARK, GRID
    }

    private var scaleDetector: ScaleGestureDetector? = null
    var scaleFactor = 1.0f
        private set
    private var posX = 0f
    private var posY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var downTouchX = 0f
    private var downTouchY = 0f
    private var hasAutoFitted = false
    private var isDragging = false

    var isShowDeviceFrame: Boolean = true
        private set
    var isShowSystemUi: Boolean = true
        private set
    var devicePreset: DevicePreset = DevicePreset.PHONE
        private set
    var canvasBackground: CanvasBackground = CanvasBackground.DEFAULT
        private set

    var isDarkMode: Boolean = false
        private set
    private var currentSurfaceColor = Color.parseColor("#FBFDFB")

    // Device shell containers
    private var deviceFrameContainer: FrameLayout? = null
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
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
        setupDeviceFrame(context)
    }

    private fun setupDeviceFrame(context: Context) {
        removeAllViews()

        val frameContainer = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        }
        deviceFrameContainer = frameContainer

        val shell = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }
        phoneShell = shell

        val screen = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    if (isShowDeviceFrame) {
                        outline.setRoundRect(0, 0, view.width, view.height, dpToPx(26).toFloat())
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
        val w = if (devicePreset.widthDp > 0) dpToPx(devicePreset.widthDp) else LayoutParams.WRAP_CONTENT
        val h = if (devicePreset.heightDp > 0) dpToPx(devicePreset.heightDp) else LayoutParams.WRAP_CONTENT
        vp.layoutParams = LinearLayout.LayoutParams(w, h)
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
        val sb = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(28))
            setPadding(dpToPx(16), 0, dpToPx(16), 0)
        }

        val timeTv = TextView(context).apply {
            text = "9:41"
            textSize = 11f
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
            textSize = 10f
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
            layoutParams = LayoutParams(dpToPx(108), dpToPx(4), Gravity.CENTER)
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
                cornerRadius = dpToPx(34).toFloat()
                setColor(Color.parseColor("#1C201F"))
                setStroke(dpToPx(5), Color.parseColor("#343A38"))
            }
            phoneShell?.background = shellBg
            val pad = dpToPx(8)
            phoneShell?.setPadding(pad, pad, pad, pad)
        } else {
            phoneShell?.background = null
            phoneShell?.setPadding(0, 0, 0, 0)
        }
        screenContainer?.invalidateOutline()
    }

    private fun updateSystemUiAppearance() {
        val screenBg = currentSurfaceColor
        screenContainer?.setBackgroundColor(screenBg)
        statusBarView?.setBackgroundColor(screenBg)
        viewportContent?.setBackgroundColor(screenBg)
        navigationBarView?.setBackgroundColor(screenBg)

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
        statusBarView?.visibility = if (isShowSystemUi) VISIBLE else GONE
        navigationBarView?.visibility = if (isShowSystemUi) VISIBLE else GONE
    }

    fun setContent(renderedContent: View) {
        viewportContent?.removeAllViews()
        viewportContent?.addView(renderedContent)
        requestLayout()
        invalidate()
    }

    fun applyThemeConfig(config: ComposeRenderer.RenderConfig) {
        this.isDarkMode = config.isDarkMode
        this.currentSurfaceColor = config.surface
        updateSystemUiAppearance()
        invalidate()
    }

    fun setShowDeviceFrame(show: Boolean) {
        this.isShowDeviceFrame = show
        updateDeviceShellAppearance()
        updateSystemUiAppearance()
        post { zoomFit() }
    }

    fun setShowSystemUi(show: Boolean) {
        this.isShowSystemUi = show
        updateSystemUiVisibility()
        post { zoomFit() }
    }

    fun setDevicePreset(preset: DevicePreset) {
        this.devicePreset = preset
        val isWrap = (preset.widthDp <= 0 || preset.heightDp <= 0)
        val w = if (isWrap) LayoutParams.WRAP_CONTENT else dpToPx(preset.widthDp)
        val h = if (isWrap) LayoutParams.WRAP_CONTENT else dpToPx(preset.heightDp)

        viewportContent?.layoutParams = LinearLayout.LayoutParams(w, h)
        if (isWrap) {
            isShowDeviceFrame = false
            isShowSystemUi = false
            updateDeviceShellAppearance()
            updateSystemUiVisibility()
        }
        post { zoomFit() }
    }

    fun setInspectMode(inspectMode: Boolean) {
        this.isInspectMode = inspectMode
        if (!inspectMode) {
            setInspectedView(null)
        }
        invalidate()
    }

    fun setInspectedView(view: View?) {
        this.inspectedView = view
        viewportContent?.invalidate()
        invalidate()
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

    fun zoomIn() {
        scaleFactor = minOf(scaleFactor + 0.2f, 3.5f)
        applyScale()
    }

    fun zoomOut() {
        scaleFactor = maxOf(scaleFactor - 0.2f, 0.15f)
        applyScale()
    }

    fun zoomFit() {
        if (width <= 0 || height <= 0 || phoneShell == null) {
            scaleFactor = 1.0f
            posX = 0f
            posY = 0f
            applyScale()
            return
        }

        phoneShell?.measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )

        var shellW = phoneShell?.measuredWidth ?: 0
        var shellH = phoneShell?.measuredHeight ?: 0

        if (shellW <= 0) {
            val wDp = if (devicePreset.widthDp > 0) devicePreset.widthDp else 320
            shellW = dpToPx(wDp + 24)
        }
        if (shellH <= 0) {
            val hDp = if (devicePreset.heightDp > 0) devicePreset.heightDp else 480
            shellH = dpToPx(hDp + 64)
        }

        val availableW = width - dpToPx(36)
        val availableH = height - dpToPx(36)
        val sX = availableW.toFloat() / shellW.toFloat()
        val sY = availableH.toFloat() / shellH.toFloat()
        val bestScale = minOf(sX, sY)

        scaleFactor = if (devicePreset == DevicePreset.RESPONSIVE || devicePreset.widthDp <= 0) {
            maxOf(0.3f, minOf(bestScale, 1.25f))
        } else {
            maxOf(0.12f, minOf(bestScale, 3.0f))
        }
        posX = 0f
        posY = 0f
        applyScale()
    }

    private fun applyScale() {
        deviceFrameContainer?.let {
            it.scaleX = scaleFactor
            it.scaleY = scaleFactor
            it.translationX = posX
            it.translationY = posY
        }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!hasAutoFitted && w > 0 && h > 0) {
            hasAutoFitted = true
            post { zoomFit() }
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        scaleDetector?.onTouchEvent(event)
        if (scaleDetector?.isInProgress == true) return true

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downTouchX = event.x
                downTouchY = event.y
                lastTouchX = event.x
                lastTouchY = event.y
                isDragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = Math.abs(event.x - downTouchX)
                val dy = Math.abs(event.y - downTouchY)
                if (dx > dpToPx(8) || dy > dpToPx(8)) {
                    isDragging = true
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector?.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (scaleDetector?.isInProgress != true) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    posX += dx
                    posY += dy
                    applyScale()
                }
                lastTouchX = event.x
                lastTouchY = event.y
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
        }
        return true
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = maxOf(0.15f, minOf(scaleFactor, 3.5f))
            applyScale()
            return true
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
