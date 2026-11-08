package com.lunova.blurp

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Live hinge angle plus a frost amount.
 *
 * Android reports 0° closed and 180° flat, and on this Fold only the steps
 * 0 / 90 / 180. A fold is in progress from the moment the hinge leaves its
 * pose until it reaches the pose it is traveling toward. That is the Duo:
 * frost for the whole swing, sharp again once it is closed or open.
 *
 * The values the overlay reads every frame are plain fields; only
 * [foldsCompleted] is Compose state, for the tutorial.
 */
class HingeController private constructor(context: Context) {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
    val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE, false)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)
        ?: sensors.firstOrNull { candidate ->
            candidate.name.contains("hinge", ignoreCase = true) ||
                candidate.stringType.contains("hinge", ignoreCase = true)
        }
    private val folding: Sensor? = sensors.firstOrNull { candidate ->
        candidate.stringType == "com.samsung.sensor.folding_angle" ||
            candidate.name.equals("Folding Angle", ignoreCase = true)
    }

    var angle = 180f
        private set

    /** Eased fold amount: 0 flat open, 1 closed. Drives the Duo fold angle. */
    var reach = 0f
        private set

    /** Frost on the cover: full while opening from it, then a short fade after closing onto it. */
    var coverFrost = 0f
        private set

    /** Each time the phone comes back to flat open after folding to 90 degrees or less. */
    var foldsCompleted by mutableIntStateOf(0)
        private set
    private var foldedSinceCount = false

    /** 1 the moment the phone opens onto the inner screen, easing to 0 as the Duo clears. */
    var innerFade = 0f
        private set

    private var lastSampleAngle = 180f
    private var lastSampleNs = 0L
    private var primed = false
    private var lastFrameNs = 0L
    private var manual = sensor == null && folding == null
    private var useFolding = false
    private var foldingLogs = 0
    private var phase = Phase.Idle
    private var originAngle = 180f
    private var closing = false
    private var departed = false
    private var foldSinceNs = 0L
    private var swapAtNs = 0L
    private var swapToCover = false
    private var holdReach = false

    /**
     * The hinge sensor only reports a reopen once the phone is nearly flat. The
     * gyroscope in the swinging half sees the swing reverse right away.
     */
    private val gyro: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private var gyroNs = 0L
    private val turn = FloatArray(3)
    private class Turn(val ns: Long, val x: Float, val y: Float, val z: Float)
    private val turnHistory = ArrayDeque<Turn>()
    private val swingOrigin = FloatArray(3)
    private var swingPeak = 0f
    private var swingTrough = 0f
    private var gyroReopened = false
    private var reopenAnchor = 0f
    private var reopenSwing = 0f
    private var gyroReach = 0f

    private val gyroListener = object : SensorEventListener2 {
        override fun onFlushCompleted(sensor: Sensor?) = goLive()

        override fun onSensorChanged(event: SensorEvent) {
            if (manual) return
            val ns = event.timestamp
            val dt = if (gyroNs == 0L) 0f else ((ns - gyroNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
            gyroNs = ns
            for (axis in 0..2) turn[axis] += event.values[axis] * dt
            turnHistory.addLast(Turn(ns, turn[0], turn[1], turn[2]))
            while (turnHistory.size > 1 && ns - turnHistory.first().ns > GYRO_HISTORY_NS) turnHistory.removeFirst()
            trackSwing()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private fun swing(): Float {
        val x = turn[0] - swingOrigin[0]
        val y = turn[1] - swingOrigin[1]
        val z = turn[2] - swingOrigin[2]
        return sqrt(x * x + y * y + z * z)
    }

    /** Measure this fold's swing from shortly before the hinge sensor noticed it. */
    private fun armSwing() {
        val start = turnHistory.firstOrNull()
        swingOrigin[0] = start?.x ?: turn[0]
        swingOrigin[1] = start?.y ?: turn[1]
        swingOrigin[2] = start?.z ?: turn[2]
        swingPeak = swing()
        gyroReopened = false
    }

    /**
     * Once the swing reverses, the frost eases off in step with the swing back,
     * so it is gone when the phone is flat again rather than when the hinge
     * sensor finally says so.
     */
    private fun trackSwing() {
        if (armPending) return
        val swing = swing()
        if (gyroReopened) {
            swingTrough = min(swingTrough, swing)
            val back = if (reopenSwing <= 0f) 0f else (swing / reopenSwing).coerceIn(0f, 1f)
            val goal = reopenAnchor * back
            if (abs(goal - gyroReach) >= 0.002f) {
                gyroReach = goal
                wake()
            }
            if (swing >= swingTrough + GYRO_REVERSE_RAD) {
                gyroReopened = false
                holdReach = false
                swingPeak = swing
                engage(angle, towardClosed = true, reason = "gyro fold again")
            }
            return
        }
        val folding = phase == Phase.Folding && closing
        // Fully closed is not a half-fold: opening from it is the hinge's and the screen swap's to see.
        val restingFolded = phase != Phase.Folding && lastSampleAngle > 8f && lastSampleAngle < 172f
        if (onCover || !(folding || restingFolded)) return
        swingPeak = max(swingPeak, swing)
        if (swingPeak >= GYRO_ARM_RAD && swing <= swingPeak - GYRO_REVERSE_RAD) {
            gyroReopened = true
            reopenAnchor = reach
            reopenSwing = swing
            gyroReach = reach
            swingTrough = swing
            closing = false
            settle("gyro reopen peak=${Math.toDegrees(swingPeak.toDouble()).toInt()} reach=$reach")
        }
    }

    private var onCover = false
    private var openedAtNs = 0L
    private var coverStage = 0f

    /** Which screen the app is drawn on right now. */
    fun noteScreen(cover: Boolean) {
        onCover = cover
    }

    /** The phone just opened onto the inner screen: frost it, then clear on the Duo's timing. */
    private fun startInnerOpen(reason: String) {
        openedAtNs = SystemClock.elapsedRealtimeNanos()
        swapAtNs = openedAtNs
        swapToCover = false
        innerFade = 1f
        holdReach = true
        reach = 0f
        gyroReopened = false
        settle(reason)
        wake()
    }

    /** The hinge's latest reading, kept while a debug preview overrides it. */
    private var hingeReading = Float.NaN

    private fun normalized(rawAngle: Float): Float =
        rawAngle.coerceIn(0f, 360f).let { raw -> if (raw > 180f) 360f - raw else raw }

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (useFolding) return
            hingeReading = normalized(event.values[0])
            if (manual) return
            accept(event.values[0], event.timestamp)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val foldingListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (manual) return
            if (foldingLogs < 4) {
                foldingLogs++
                Log.i(TAG, "folding ${event.values.joinToString()}")
            }
            useFolding = true
            hingeReading = normalized(event.values[0])
            accept(event.values[0], event.timestamp)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private fun accept(rawAngle: Float, timestamp: Long) {
        val next = normalized(rawAngle)
        if (!primed) {
            primed = true
            lastSampleAngle = next
            lastSampleNs = timestamp
            angle = next
            return
        }
        val sinceOpenNs = SystemClock.elapsedRealtimeNanos() - openedAtNs
        if (!onCover && openedAtNs != 0L && sinceOpenNs < LATE_STEP_NS && next < lastSampleAngle) {
            Log.i(TAG, "late ${lastSampleAngle.toInt()} -> ${next.toInt()} right after opening")
            return
        }
        val delta = abs(next - lastSampleAngle)
        val previous = lastSampleAngle
        lastSampleNs = timestamp
        lastSampleAngle = next
        if (next <= 90f) foldedSinceCount = true
        if (next >= 172f && foldedSinceCount) {
            foldedSinceCount = false
            foldsCompleted++
        }
        if (delta >= 0.3f) {
            angle = next
            wake()
        }
        if (delta < 8f) return

        val towardClosed = next < previous
        if (!onCover && previous <= 8f && !towardClosed && !holdReach) {
            startInnerOpen("opened ${previous.toInt()} -> ${next.toInt()}")
            return
        }
        if (next >= 172f) gyroReopened = false
        if (gyroReopened && towardClosed) {
            Log.i(TAG, "late ${previous.toInt()} -> ${next.toInt()} after gyro reopen")
            return
        }
        if (towardClosed || next >= 172f) holdReach = false
        when (phase) {
            Phase.Idle, Phase.Settling -> engage(previous, towardClosed, "jump ${previous.toInt()} -> ${next.toInt()}")
            Phase.Folding -> {
                closing = towardClosed
                Log.i(TAG, "folding ${previous.toInt()} -> ${next.toInt()}")
            }
        }
        if (phase == Phase.Folding && abs(next - originAngle) >= 8f) departed = true
        if (phase == Phase.Folding && departed && atRest(next)) settle("arrived ${next.toInt()}")
    }

    /**
     * The cover and the inner screen swapped. The swing is over by now, and the
     * hinge sensor reports it late, so the frost after a swap runs on a timer.
     */
    fun onScreenSwap(nowCover: Boolean) {
        if (!nowCover) {
            startInnerOpen("screen swap cover=false")
            return
        }
        swapAtNs = SystemClock.elapsedRealtimeNanos()
        swapToCover = true
        gyroReopened = false
        settle("screen swap cover=true")
        wake()
    }

    private fun engage(from: Float, towardClosed: Boolean, reason: String) {
        originAngle = from
        closing = towardClosed
        departed = abs(angle - from) >= 8f
        val arm = towardClosed && phase != Phase.Folding
        if (gyroBatched) {
            // The batched readings from just before this fold are still in the sensor's buffer.
            armPending = arm
            if (!sensorManager.flush(gyroListener)) goLive()
        } else if (arm) {
            armSwing()
        }
        phase = Phase.Folding
        foldSinceNs = SystemClock.elapsedRealtimeNanos()
        Log.i(TAG, "frost $reason")
        wake()
    }

    private fun settle(reason: String) {
        if (phase == Phase.Settling) return
        phase = Phase.Settling
        Log.i(TAG, "clear $reason")
        wake()
    }

    /** True when the hinge has reached the end of this swing, including a reversal home. */
    private fun atRest(value: Float): Boolean {
        val closed = value <= 8f
        val open = value >= 172f
        if (!closed && !open) return false
        if (closing && closed) return true
        if (!closing && open) return true
        if (closed && originAngle <= 8f) return true
        if (open && originAngle >= 172f) return true
        return false
    }

    private val frameCallback = object : FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            val now = SystemClock.elapsedRealtimeNanos()
            val dt = if (lastFrameNs == 0L) {
                0.016f
            } else {
                ((frameTimeNanos - lastFrameNs) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
            }
            lastFrameNs = frameTimeNanos
            val foldTarget = ((180f - angle) / 180f).coerceIn(0f, 1f)
            val reachGoal = when {
                holdReach -> 0f
                gyroReopened -> gyroReach
                else -> foldTarget
            }
            val reachTau = when {
                gyroReopened -> 0.05f
                reachGoal < reach -> 0.1f
                else -> 0.18f
            }
            val reachFollow = 1f - exp(-dt / reachTau)
            val stepped = (reach + (reachGoal - reach) * reachFollow).coerceIn(0f, 1f)
            val nextReach = if (holdReach || abs(reachGoal - stepped) < 0.004f) reachGoal else stepped
            if (nextReach != reach) reach = nextReach
            updateSwapFrost(now, dt)
            if (phase == Phase.Folding && foldSinceNs != 0L && now - foldSinceNs > FOLD_SAFETY_NS) {
                settle("safety")
            } else if (phase == Phase.Settling) {
                // Settling only marks the frame a fold ended, so a second settle() in it logs nothing.
                phase = Phase.Idle
            }
            if (!frames) return
            if (still(now, reachGoal)) {
                // Nothing moves until a sensor or screen change; wake() picks the frames back up.
                // A half-open phone keeps its frost, like the Duo's, without drawing every frame.
                if (!gyroBatched && listening && (lastSampleAngle >= 172f || onCover)) registerGyro(batched = true)
                frames = false
                lastFrameNs = 0L
                idle = true
                if (phase == Phase.Folding) {
                    main.removeCallbacks(safety)
                    main.postDelayed(safety, (FOLD_SAFETY_NS - (now - foldSinceNs)) / 1_000_000L + 1L)
                }
                return
            }
            FrameClock.postFrameCallback(this)
        }
    }

    /** Nothing eases and no swap timer runs. */
    private fun still(now: Long, reachGoal: Float): Boolean {
        val swapping = swapAtNs != 0L && DuoFrostModel.revealLeft(sinceSwap(now), swapToCover) > 0f
        return !swapping && reach == reachGoal && innerFade == 0f &&
            coverStage == coverGoal && coverFrost == coverGoal
    }

    private val main = Handler(Looper.getMainLooper())

    /** A fold the hinge never finishes reporting ends after [FOLD_SAFETY_NS], drawing or not. */
    private val safety = Runnable {
        if (phase == Phase.Folding && foldSinceNs != 0L &&
            SystemClock.elapsedRealtimeNanos() - foldSinceNs >= FOLD_SAFETY_NS
        ) {
            settle("safety")
        }
    }

    /** True while nothing is easing, so callers can stop drawing every frame. */
    var idle = false
        private set
    private val wakeListeners = ArrayList<() -> Unit>()

    /** Called on the main thread whenever the controller leaves [idle]. */
    fun addWakeListener(listener: () -> Unit) {
        wakeListeners += listener
    }

    fun removeWakeListener(listener: () -> Unit) {
        wakeListeners -= listener
    }

    private fun wake() {
        if (users == 0) return
        idle = false
        if (!frames) {
            frames = true
            lastFrameNs = 0L
            FrameClock.postFrameCallback(frameCallback)
        }
        wakeListeners.forEach { it() }
    }

    /**
     * The cover frost runs through two smoothing stages so it eases on like
     * the inner screen does while closing, instead of snapping on, and eases
     * off when an opening stops short. A reveal's fade after a swap is smooth
     * already, so the frost follows it straight down, as fast as the Duo's.
     */
    private fun sinceSwap(now: Long): Float = (now - swapAtNs) / 1_000_000_000f

    private fun updateSwapFrost(now: Long, dt: Float) {
        val fade = if (swapAtNs == 0L) 0f else DuoFrostModel.revealLeft(sinceSwap(now), swapToCover)
        val openingNow = phase == Phase.Folding && !closing && onCover
        coverGoal = maxOf(if (swapToCover) fade else 0f, if (openingNow) 1f else 0f)
        val follow = 1f - exp(-dt / COVER_EASE_SECONDS)
        val revealing = swapToCover && !openingNow
        coverStage = if (revealing && coverGoal < coverStage) coverGoal else coverStage + (coverGoal - coverStage) * follow
        if (abs(coverGoal - coverStage) < 0.004f) coverStage = coverGoal
        val cover = if (revealing && coverStage < coverFrost) coverStage else coverFrost + (coverStage - coverFrost) * follow
        val nextCover = if (abs(coverStage - cover) < 0.004f) coverStage else cover
        if (nextCover != coverFrost) coverFrost = nextCover
        val inner = if (swapToCover) 0f else fade
        if (abs(inner - innerFade) >= 0.002f || inner == 0f) innerFade = inner
    }

    private var coverGoal = 0f
    private var listening = false
    private var frames = false

    private var users = 0

    private var screenOn = true
    private var gyroBatched = false
    private var armPending = false

    /** Each [start] needs a matching [stop]; the app and the overlay service share one controller. */
    fun start() {
        users++
        if (screenOn) listen()
        wake()
    }

    fun stop() {
        users = (users - 1).coerceAtLeast(0)
        if (users > 0) return
        unlisten()
        frames = false
        FrameClock.removeFrameCallback(frameCallback)
        main.removeCallbacks(safety)
        lastFrameNs = 0L
    }

    /** Nothing shows with the screen off, so the sensors rest too. */
    fun setScreenOn(on: Boolean) {
        if (screenOn == on) return
        screenOn = on
        if (!on) {
            unlisten()
            // The gyro sleeps with the screen, so a reopen it was tracking can't finish.
            gyroReopened = false
        } else if (users > 0) {
            primed = false
            listen()
            wake()
        }
    }

    private fun listen() {
        if (listening) return
        listening = true
        Log.i(TAG, "hinge sensor=${sensor?.name} wakeUp=${sensor?.isWakeUpSensor}")
        sensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        folding?.let { sensorManager.registerListener(foldingListener, it, SensorManager.SENSOR_DELAY_GAME) }
        registerGyro(batched = false)
    }

    private fun unlisten() {
        if (!listening) return
        listening = false
        sensorManager.unregisterListener(listener)
        sensorManager.unregisterListener(foldingListener)
        sensorManager.unregisterListener(gyroListener)
        gyroBatched = false
        armPending = false
    }

    /**
     * At rest the gyroscope fills its hardware buffer and reports about once
     * a second, so the phone can sleep. A fold flushes it and goes live.
     */
    private fun registerGyro(batched: Boolean) {
        val g = gyro ?: return
        sensorManager.unregisterListener(gyroListener)
        sensorManager.registerListener(gyroListener, g, GYRO_PERIOD_US, if (batched) GYRO_BATCH_US else 0)
        gyroBatched = batched
    }

    private fun goLive() {
        if (armPending) {
            armPending = false
            armSwing()
        }
        if (gyroBatched && listening) registerGyro(batched = false)
    }

    fun debugState(): String =
        "angle=$angle reach=$reach phase=$phase idle=$idle frames=$frames users=$users " +
            "cover=$coverFrost/$coverStage inner=$innerFade hold=$holdReach gyro=$gyroReopened manual=$manual"

    /**
     * Hand the angle back to the hinge sensor after [setPreviewAngle]. It picks
     * up the hinge's real reading: re-priming would swallow the next real fold.
     */
    fun clearPreview() {
        if (sensor == null && folding == null) return
        manual = false
        primed = !hingeReading.isNaN()
        angle = if (primed) hingeReading else 180f
        lastSampleAngle = angle
        wake()
    }

    fun setPreviewAngle(target: Float) {
        manual = true
        val next = target.coerceIn(0f, 180f)
        // As with a hinge reading, closing or reaching flat ends an opening's hold.
        if (next < angle || next >= 172f) holdReach = false
        lastSampleNs = SystemClock.elapsedRealtimeNanos()
        lastSampleAngle = next
        angle = next
        wake()
    }

    companion object {
        private const val TAG = "Blurp"
        private const val GYRO_PERIOD_US = 20_000
        private const val GYRO_BATCH_US = 800_000

        /** Each of the two cover smoothing stages. */
        private const val COVER_EASE_SECONDS = 0.05f

        /** The hinge sensor's steps can trail an opening by this long. */
        private const val LATE_STEP_NS = 700_000_000L
        private const val FOLD_SAFETY_NS = 20_000_000_000L

        private const val GYRO_HISTORY_NS = 800_000_000L
        private const val GYRO_ARM_RAD = 0.6f
        private const val GYRO_REVERSE_RAD = 0.3f

        private enum class Phase { Idle, Folding, Settling }

        @Volatile
        private var instance: HingeController? = null

        fun get(context: Context): HingeController {
            return instance ?: synchronized(this) {
                instance ?: HingeController(context.applicationContext).also { instance = it }
            }
        }
    }
}
