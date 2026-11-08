package com.lunova.blurp

import android.util.Log
import android.view.View
import java.lang.reflect.Constructor
import java.lang.reflect.Method

/**
 * One UI's compositor blur, the one its own panels use. It blurs whatever is
 * on screen behind the view, live, including other apps and the lock screen.
 * Android's public cross-window blur is switched off on this Fold.
 *
 * Measured on the Fold: sigma is about 0.43 x radius, uncapped to at least 400.
 * The gradient is full blur from its start edge to [fullTo], easing to none at
 * [zeroAt], both as fractions of the view's extent. The angle picks the start
 * edge: 90 left, 270 right, 180 top, 0 bottom.
 */
object SamsungBlur {
    private const val TAG = "FoldBlur"
    private const val SIGMA_PER_RADIUS = 0.43f

    private class Api(
        val mode: Int,
        val builder: Constructor<*>,
        val setRadius: Method,
        val setBackgroundColor: Method,
        val setBlurGradient: Method,
        val build: Method,
        val apply: Method,
    )

    private val api: Api? by lazy {
        try {
            val info = Class.forName("android.view.SemBlurInfo")
            val builder = Class.forName("android.view.SemBlurInfo\$Builder")
            val int = Int::class.javaPrimitiveType
            val float = Float::class.javaPrimitiveType
            Api(
                mode = info.getField("BLUR_MODE_WINDOW").getInt(null),
                builder = builder.getConstructor(int),
                setRadius = builder.getMethod("setRadius", int),
                setBackgroundColor = builder.getMethod("setBackgroundColor", int),
                setBlurGradient = builder.getMethod("setBlurGradient", float, float, float),
                build = builder.getMethod("build"),
                apply = View::class.java.getMethod("semSetBlurInfo", info),
            )
        } catch (t: Throwable) {
            Log.w(TAG, "One UI blur unavailable", t)
            null
        }
    }

    val available: Boolean get() = api != null

    fun radiusFor(sigma: Float): Int = (sigma / SIGMA_PER_RADIUS).toInt()

    data class Gradient(val zeroAt: Float, val fullTo: Float, val angle: Float)

    fun apply(view: View, radius: Int, gradient: Gradient?) {
        val api = api ?: return
        try {
            val b = api.builder.newInstance(api.mode)
            api.setRadius.invoke(b, radius)
            api.setBackgroundColor.invoke(b, 0)
            if (gradient != null) api.setBlurGradient.invoke(b, gradient.zeroAt, gradient.fullTo, gradient.angle)
            api.apply.invoke(view, api.build.invoke(b))
        } catch (t: Throwable) {
            Log.w(TAG, "blur apply failed", t)
        }
    }
}
