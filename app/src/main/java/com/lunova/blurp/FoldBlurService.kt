package com.lunova.blurp

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.Display
import android.view.Surface
import android.view.accessibility.AccessibilityEvent

/**
 * Runs the Duo fold frost system-wide: over the home screen, other apps and
 * the lock screen, Blurp's own screens included. Turned on under
 * Settings > Accessibility > Installed apps > Blurp.
 */
class FoldBlurService : AccessibilityService() {
    private val main = Handler(Looper.getMainLooper())
    private lateinit var controller: HingeController
    private var overlay: FrostOverlay? = null
    private var geometry: FrostGeometry? = null
    private var running = false
    private var looping = false
    private val onWake: () -> Unit = { loop() }
    private lateinit var prefs: BlurpPrefs
    private val onFrost: (Boolean) -> Unit = { loop() }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) refreshGeometry()
        }

        override fun onDisplayAdded(displayId: Int) = Unit

        override fun onDisplayRemoved(displayId: Int) = Unit
    }

    /** Debug builds: adb shell am broadcast -a com.lunova.blurp.PREVIEW --ef angle 90 (no extra: live; --ez frost false; --ez state true). */
    private val preview = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.hasExtra("state")) {
                val geo = geometry
                val level = geo?.let {
                    DuoFrostModel.level(it.cover, controller.reach, controller.coverFrost, controller.innerFade)
                }
                Log.i(TAG, "state ${controller.debugState()} looping=$looping resting=${overlay?.resting} $level")
            } else if (intent.hasExtra("frost")) {
                prefs.setFrostEnabled(intent.getBooleanExtra("frost", true))
            } else if (intent.hasExtra("angle")) {
                controller.setPreviewAngle(intent.getFloatExtra("angle", 180f))
            } else {
                controller.clearPreview()
            }
        }
    }

    private val screen = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            controller.setScreenOn(intent.action == Intent.ACTION_SCREEN_ON)
        }
    }

    private val frame = object : FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) {
                looping = false
                return
            }
            draw(frameTimeNanos)
            // Once the controller is idle the frost holds still, hidden or not,
            // and this last draw already shows it.
            if (controller.idle) {
                looping = false
                return
            }
            FrameClock.postFrameCallback(this)
        }
    }

    private fun loop() {
        if (!running || looping) return
        looping = true
        FrameClock.postFrameCallback(frame)
    }

    override fun onServiceConnected() {
        if (!SamsungBlur.available) {
            Log.w(TAG, "One UI blur missing; the system-wide frost stays off")
            return
        }
        controller = HingeController.get(this)
        prefs = BlurpPrefs.get(this)
        controller.setScreenOn(getSystemService(PowerManager::class.java).isInteractive)
        controller.start()
        registerReceiver(
            screen,
            IntentFilter(Intent.ACTION_SCREEN_OFF).apply { addAction(Intent.ACTION_SCREEN_ON) },
            RECEIVER_NOT_EXPORTED,
        )
        overlay = FrostOverlay(this).also { it.prepare() }
        getSystemService(DisplayManager::class.java).registerDisplayListener(displayListener, main)
        refreshGeometry()
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            registerReceiver(preview, IntentFilter(ACTION_PREVIEW), RECEIVER_EXPORTED)
        }
        running = true
        controller.addWakeListener(onWake)
        prefs.addFrostListener(onFrost)
        loop()
        Log.i(TAG, "system-wide frost on, ${geometry}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (running) {
            running = false
            looping = false
            controller.removeWakeListener(onWake)
            prefs.removeFrostListener(onFrost)
            FrameClock.removeFrameCallback(frame)
            getSystemService(DisplayManager::class.java).unregisterDisplayListener(displayListener)
            runCatching { unregisterReceiver(preview) }
            unregisterReceiver(screen)
            overlay?.release()
            controller.stop()
        }
        super.onDestroy()
    }

    private fun draw(nowNs: Long) {
        val geo = geometry ?: return
        val level = if (prefs.frostEnabled.value) {
            DuoFrostModel.level(geo.cover, controller.reach, controller.coverFrost, controller.innerFade)
        } else {
            DuoFrostModel.Level(0f, 0f, 0f)
        }
        overlay?.render(geo, level, nowNs)
    }

    /**
     * The overlay lives on display 0, which Samsung maps to the cover or the
     * inner panel as the phone closes and opens. Its size comes from the
     * display mode: this service's own window metrics only catch up after the
     * swap. The Fold has no screen-resolution setting, so mode pixels are the
     * pixels windows are laid out in.
     */
    private fun refreshGeometry() {
        val display = getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY) ?: return
        val mode = display.mode
        val rotation = display.rotation
        val sideways = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270
        val width = if (sideways) mode.physicalHeight else mode.physicalWidth
        val height = if (sideways) mode.physicalWidth else mode.physicalHeight
        val next = FrostGeometry.of(width, height, rotation)
        val cover = next.cover
        val previous = geometry
        geometry = next
        if (previous == null) {
            controller.noteScreen(cover)
            return
        }
        if (previous.cover != cover) {
            controller.onScreenSwap(cover)
            controller.noteScreen(cover)
        }
        if (previous != next) {
            Log.i(TAG, "geometry $next")
            loop()
        }
    }

    companion object {
        private const val TAG = "FoldBlur"
        private const val ACTION_PREVIEW = "com.lunova.blurp.PREVIEW"
    }
}
