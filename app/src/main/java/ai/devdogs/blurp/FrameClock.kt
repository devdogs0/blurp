package ai.devdogs.blurp

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Choreographer

fun interface FrameCallback {
    fun doFrame(frameTimeNanos: Long)
}

/**
 * Vsync ticks for the frost, delivered on the main thread.
 *
 * One UI 9 changed `Choreographer.doFrame` on an app's main thread: once the
 * app has been in the background for 4 s with no visible window, frame
 * callbacks are skipped and wait until the app comes back to the front
 * (logcat: "SYSPERF_ACTIVE_APP_BBA_ENABLE : stop animation in background
 * states"). Blurp is always in the background, so every fold's first frame
 * was swallowed. One UI only switches off the main thread's Choreographer,
 * so the vsync comes from a Choreographer on a thread of our own and the
 * frame itself runs on the main thread, where the windows and sensors live.
 *
 * Main thread only. Asks for no vsync while nothing is posted.
 */
object FrameClock {
    private val main = Handler.createAsync(Looper.getMainLooper())
    private val queue = FrameQueue { requestVsync() }
    private var vsync: Handler? = null
    private lateinit var choreographer: Choreographer

    private val onVsync = Choreographer.FrameCallback { frameTimeNanos ->
        main.post { queue.tick(frameTimeNanos) }
    }

    fun postFrameCallback(callback: FrameCallback) = queue.post(callback)

    fun removeFrameCallback(callback: FrameCallback) = queue.remove(callback)

    private fun requestVsync() {
        val handler = vsync ?: Handler.createAsync(
            HandlerThread("BlurpVsync").apply { start() }.looper,
        ).also { handler ->
            vsync = handler
            handler.post { choreographer = Choreographer.getInstance() }
        }
        handler.post { choreographer.postFrameCallback(onVsync) }
    }
}

/** Which callbacks run on the next tick. A callback posted during a tick runs on the following one. */
internal class FrameQueue(private val requestTick: () -> Unit) {
    private var pending = ArrayList<FrameCallback>()
    private var running = ArrayList<FrameCallback>()
    private var requested = false

    fun post(callback: FrameCallback) {
        if (callback in pending) return
        pending += callback
        if (!requested) {
            requested = true
            requestTick()
        }
    }

    fun remove(callback: FrameCallback) {
        pending -= callback
        running -= callback
    }

    fun tick(frameTimeNanos: Long) {
        requested = false
        val now = pending
        pending = running
        running = now
        while (running.isNotEmpty()) running.removeAt(0).doFrame(frameTimeNanos)
    }
}
