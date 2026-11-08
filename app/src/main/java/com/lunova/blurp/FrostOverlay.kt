package com.lunova.blurp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Shader
import android.util.Log
import android.view.Gravity
import android.view.Surface
import android.view.View
import android.view.WindowManager
import kotlin.math.sqrt

/** Which screen edge the frost is heaviest at: the moving panel's free edge. */
enum class FrostEdge(val angle: Float) {
    LEFT(90f), RIGHT(270f), TOP(180f), BOTTOM(0f);

    val horizontal: Boolean get() = this == LEFT || this == RIGHT

    val opposite: FrostEdge
        get() = when (this) {
            LEFT -> RIGHT
            RIGHT -> LEFT
            TOP -> BOTTOM
            BOTTOM -> TOP
        }
}

data class FrostGeometry(val width: Int, val height: Int, val cover: Boolean, val edge: FrostEdge) {
    val span: Int get() = if (edge.horizontal) width else height

    companion object {
        /**
         * Covers are 1.58 (Fold8) and 2.33 (Fold7, Fold8 Ultra) times taller
         * than wide; inner screens 1.32 (Fold8) and about 1.11 (Fold7, Ultra).
         */
        private const val COVER_MIN_ASPECT = 1.45f

        /**
         * The cover is a tall phone screen and the inner screen nearly square.
         * Judged by shape so display zoom and screen resolution don't matter.
         */
        fun isCover(width: Int, height: Int): Boolean =
            maxOf(width, height) >= COVER_MIN_ASPECT * minOf(width, height)

        /** [width] and [height] as the screen is now, [rotation] a `Surface.ROTATION_*`. */
        fun of(width: Int, height: Int, rotation: Int): FrostGeometry {
            val cover = isCover(width, height)
            val edge = if (cover) {
                when (rotation) {
                    Surface.ROTATION_90 -> FrostEdge.TOP
                    Surface.ROTATION_180 -> FrostEdge.LEFT
                    Surface.ROTATION_270 -> FrostEdge.BOTTOM
                    else -> FrostEdge.RIGHT
                }
            } else {
                when (rotation) {
                    Surface.ROTATION_90 -> FrostEdge.BOTTOM
                    Surface.ROTATION_180 -> FrostEdge.RIGHT
                    Surface.ROTATION_270 -> FrostEdge.TOP
                    else -> FrostEdge.LEFT
                }
            }
            return FrostGeometry(width, height, cover, edge)
        }
    }

    /** Screen rect of the strip [from]..[to] pixels in from [edge]. */
    fun strip(from: Float, to: Float): PaneRect {
        val a = from.toInt().coerceIn(0, span)
        val b = to.toInt().coerceIn(0, span)
        return when (edge) {
            FrostEdge.LEFT -> PaneRect(a, 0, b, height)
            FrostEdge.RIGHT -> PaneRect(width - b, 0, width - a, height)
            FrostEdge.TOP -> PaneRect(0, a, width, b)
            FrostEdge.BOTTOM -> PaneRect(0, height - b, width, height - a)
        }
    }
}

/** A blur pane's place on screen, in pixels. */
data class PaneRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/** One blur window: where it goes, how strong, and which way its blur fades. */
data class PaneSpec(val rect: PaneRect, val sigma: Float, val gradient: SamsungBlur.Gradient?)

/**
 * The panes for [level] on [geo]. The measured blur grows from nothing at the
 * hinge to its peak at the free edge. Each layer covers the strip from the free
 * edge to where it starts, easing in over its own stretch, so the layers stack
 * toward the edge and their blurs add up to the measured curve.
 */
fun planPanes(geo: FrostGeometry, level: DuoFrostModel.Level): List<PaneSpec> {
    val span = geo.span.toFloat()
    val frosted = if (geo.cover) span * (1f - DuoFrostModel.CoverSharpEdge) else span / 2f
    val sigmaAt = { d: Float ->
        if (geo.cover) DuoFrostModel.coverSigma(level.peak, d) else DuoFrostModel.innerSigma(level.peak, d)
    }
    val specs = ArrayList<PaneSpec>(FrostOverlay.PANES)
    for (k in 0 until FrostOverlay.LAYERS) {
        val near = k / FrostOverlay.LAYERS.toFloat()
        val far = (k + 1) / FrostOverlay.LAYERS.toFloat()
        val sigma = sqrt((sigmaAt(far).let { it * it } - sigmaAt(near).let { it * it }).coerceAtLeast(0f))
        val extent = (1f - near) * frosted
        specs += PaneSpec(
            rect = geo.strip(0f, extent),
            sigma = sigma,
            gradient = SamsungBlur.Gradient(zeroAt = 1f, fullTo = (1f - far) / (1f - near), angle = geo.edge.angle),
        )
    }
    if (!geo.cover) {
        val hinge = span / 2f
        val reach = DuoFrostModel.InHandSpill * (span - hinge)
        val sigma = level.peak * DuoFrostModel.SpillStrength
        // One pane across the hinge: two panes meeting there leave a seam.
        specs += PaneSpec(geo.strip(hinge - reach, hinge + reach), sigma, SamsungBlur.Gradient(1f, 0.5f, geo.edge.angle))
    }
    return specs
}

/**
 * The Duo frost drawn over everything on screen with live One UI blur: the
 * blur panes from [planPanes], a light spill across the hinge, and a dimming
 * layer on top.
 */
class FrostOverlay(private val context: Context) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val panes = List(PANES) { BlurPane(context) }
    private val shade = ShadeView(context)
    private val shadeParams = overlayParams(Rect())

    /**
     * One UI's process freezer (Freecess) freezes a background app a few
     * minutes after it was last opened, accessibility services included, and
     * hinge events then wait until a fold is over. It leaves an app alone while
     * one of its windows is on screen. This window is always on screen, 1 px
     * and fully transparent: SurfaceFlinger skips alpha-0 layers, so it is
     * never drawn.
     */
    private val anchor = View(context)
    private val anchorParams = overlayParams(Rect(0, 0, 1, 1)).apply {
        alpha = 0f
        title = "BlurpAnchor"
    }
    private var geometry: FrostGeometry? = null
    private var attached = false
    private var hidden = true

    /** True once the frost is hidden, so nothing needs drawing until it shows again. */
    val resting: Boolean get() = hidden

    fun render(geo: FrostGeometry, level: DuoFrostModel.Level, nowNs: Long) {
        if (!level.visible) {
            if (!hidden) {
                hidden = true
                panes.forEach { it.hide() }
                shade.visibility = View.INVISIBLE
            }
            return
        }
        hidden = false
        val specs = planPanes(geo, level)
        if (!attached) attach()
        val relayout = geo != geometry
        geometry = geo
        panes.forEachIndexed { i, pane -> pane.show(specs.getOrNull(i), relayout) }
        if (relayout) {
            shadeParams.apply { x = 0; y = 0; width = geo.width; height = geo.height }
            windowManager.updateViewLayout(shade, shadeParams)
        }
        shade.visibility = View.VISIBLE
        shade.update(geo, level)
    }

    /**
     * Adding windows takes a few frames, which would swallow the start of the
     * fade in. The windows stay attached and hidden between folds instead.
     */
    fun prepare() {
        if (attached) return
        attach()
        panes.forEach { it.hide() }
        shade.visibility = View.INVISIBLE
    }

    fun release() {
        detach()
    }

    private fun attach() {
        windowManager.addView(anchor, anchorParams)
        panes.forEach { windowManager.addView(it.view, it.params) }
        windowManager.addView(shade, shadeParams)
        attached = true
        geometry = null
    }

    private fun detach() {
        if (!attached) return
        panes.forEach { runCatching { windowManager.removeViewImmediate(it.view) } }
        runCatching { windowManager.removeViewImmediate(shade) }
        runCatching { windowManager.removeViewImmediate(anchor) }
        panes.forEach { it.reset() }
        attached = false
        geometry = null
        hidden = true
    }

    private inner class BlurPane(context: Context) {
        val view = View(context)
        val params = overlayParams(Rect())
        private var radius = -1
        private var gradient: SamsungBlur.Gradient? = null

        fun show(spec: PaneSpec?, relayout: Boolean) {
            if (spec == null) {
                hide()
                return
            }
            if (relayout || params.width != spec.rect.width || params.height != spec.rect.height ||
                params.x != spec.rect.left || params.y != spec.rect.top
            ) {
                params.x = spec.rect.left
                params.y = spec.rect.top
                params.width = spec.rect.width.coerceAtLeast(1)
                params.height = spec.rect.height.coerceAtLeast(1)
                windowManager.updateViewLayout(view, params)
            }
            val nextRadius = SamsungBlur.radiusFor(spec.sigma)
            if (nextRadius < 1) {
                hide()
                return
            }
            view.visibility = View.VISIBLE
            if (nextRadius != radius || spec.gradient != gradient) {
                radius = nextRadius
                gradient = spec.gradient
                SamsungBlur.apply(view, nextRadius, spec.gradient)
            }
        }

        fun hide() {
            if (radius > 0) {
                SamsungBlur.apply(view, 0, null)
                radius = 0
                gradient = null
            }
            view.visibility = View.INVISIBLE
        }

        fun reset() {
            radius = -1
            gradient = null
        }
    }

    private class ShadeView(context: Context) : View(context) {
        private val paint = Paint()
        private var geo: FrostGeometry? = null
        private val colors = IntArray(STOPS + 1)
        private val positions = FloatArray(STOPS + 1) { it / STOPS.toFloat() }
        private var key = 0f

        fun update(geo: FrostGeometry, level: DuoFrostModel.Level) {
            val nextKey = level.e * 1000f + level.flash
            if (geo == this.geo && nextKey == key) return
            this.geo = geo
            key = nextKey
            val span = geo.span.toFloat()
            for (i in 0..STOPS) {
                val s = positions[i]
                val gain = if (geo.cover) {
                    DuoFrostModel.coverGain(level.e, 1f - s)
                } else {
                    val x = s * span
                    val hinge = span / 2f
                    DuoFrostModel.innerGain(level.e, if (x < hinge) 1f - x / hinge else null, level.flash)
                }
                colors[i] = Color.argb(((1f - gain).coerceIn(0f, 1f) * 255).toInt(), 0, 0, 0)
            }
            val w = geo.width.toFloat()
            val h = geo.height.toFloat()
            paint.shader = when (geo.edge) {
                FrostEdge.LEFT -> LinearGradient(0f, 0f, w, 0f, colors, positions, Shader.TileMode.CLAMP)
                FrostEdge.RIGHT -> LinearGradient(w, 0f, 0f, 0f, colors, positions, Shader.TileMode.CLAMP)
                FrostEdge.TOP -> LinearGradient(0f, 0f, 0f, h, colors, positions, Shader.TileMode.CLAMP)
                FrostEdge.BOTTOM -> LinearGradient(0f, h, 0f, 0f, colors, positions, Shader.TileMode.CLAMP)
            }
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            canvas.drawPaint(paint)
        }
    }

    companion object {
        const val LAYERS = 5
        const val PANES = LAYERS + 1
        private const val STOPS = 24

        /**
         * When the phone opens or closes, One UI hides every overlay it can for
         * the screen switch and fades it back in over 350 ms only once the new
         * screen has lit, so the screen lit up sharp and the frost blurred in
         * after it. This One UI SDK flag keeps a window in the switch itself:
         * the new screen lights with the frost already on it, as on the Duo.
         * Check: `tools/check-fold-fade.sh`.
         */
        private val skipFoldFade: ((WindowManager.LayoutParams) -> Unit)? by lazy {
            runCatching {
                val type = WindowManager.LayoutParams::class.java
                val flag = type.getField("SEM_EXTENSION_FLAG2_SKIP_ASYNC_ROTATION").getInt(null)
                val add = type.getMethod("semAddExtensionFlags2", Int::class.javaPrimitiveType)
                val skip: (WindowManager.LayoutParams) -> Unit = { params -> add.invoke(params, flag) }
                skip
            }.onFailure { Log.w("FoldBlur", "One UI's fold-fade flag unavailable", it) }.getOrNull()
        }

        private fun overlayParams(rect: Rect) = WindowManager.LayoutParams(
            rect.width().coerceAtLeast(1),
            rect.height().coerceAtLeast(1),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = rect.left
            y = rect.top
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            setFitInsetsTypes(0)
            title = "BlurpFrost"
            skipFoldFade?.invoke(this)
        }
    }
}
