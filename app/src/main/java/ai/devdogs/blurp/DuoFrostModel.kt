package ai.devdogs.blurp

import kotlin.math.max
import kotlin.math.pow

/**
 * The fold frost of the "Duo" reference animation, measured frame by frame.
 * Shared by the in-app preview and the system-wide overlay.
 *
 * Inner screen: sigma = A * d^0.7, where d runs from 0 at the hinge to 1 at
 * the moving panel's free edge. A is about 100 px half open and past 200 px
 * near closed. The moving panel darkens toward its free edge; the panel in
 * hand barely dims.
 */
object DuoFrostModel {
    const val SigmaExponent = 0.7f
    const val PeakSigma = 210f

    /** Peak blur on the cover's outer edge while the hinge is moving. */
    const val CoverPeakSigma = 150f

    /** The cover stays sharp along this share of its hinge side. */
    const val CoverSharpEdge = 0.12f
    const val CoverSigmaExponent = 1.2f

    /**
     * Duo, opening: the inner screen appears dark all over and frosted on the
     * moving half. The half in hand is only dimmed: its icons stay sharp
     * (measured on the owner's Duo video, 2026-11-08). Both clear within
     * about 0.15 s of appearing.
     */
    const val InnerSwapStrength = 0.75f
    const val InnerSwapFlash = 0.6f

    /** Share of the panel in hand, measured from the hinge, that a light frost spills into. */
    const val InHandSpill = 0.2f

    /** Spill blur at the hinge as a share of the peak. */
    const val SpillStrength = 0.06f

    /**
     * The reveal frost after a screen swap holds this long, in seconds after
     * Blurp sees the swap. The new panel lit 0.14 to 0.30 s after that in 12
     * real folds (2026-11-08), so it lights frosted and clears about as fast
     * as the Duo's. In 26 more folds it lit 0.15 to 0.36 s after, the frost
     * gone 0.05 to 0.26 s later, and the owner saw the Duo's reveal. Screen
     * recordings can't time this: they stay black through One UI's fold
     * animation, up to 0.6 s, while the panel shows the app and the frost.
     * See `RevealTimingTest`.
     */
    const val RevealHoldSeconds = 0.24f

    /** Duo video, measured: the inner screen is sharp about 0.13 s after it appears. */
    const val InnerRevealSeconds = 0.16f

    /** Duo video, measured: the cover is sharp about 0.08 s after it appears. */
    const val CoverRevealSeconds = 0.1f

    /** Share of a swap's reveal frost left [sinceSwap] seconds after Blurp saw the swap: 1 held, easing to 0. */
    fun revealLeft(sinceSwap: Float, cover: Boolean): Float {
        val seconds = if (cover) CoverRevealSeconds else InnerRevealSeconds
        val t = ((sinceSwap - RevealHoldSeconds) / seconds).coerceIn(0f, 1f)
        return 1f - t * t * (3f - 2f * t)
    }

    /** How strong the frost is right now. [peak] is a Gaussian sigma in pixels, [flash] a dimming share. */
    data class Level(val e: Float, val peak: Float, val flash: Float) {
        val visible: Boolean get() = peak >= 1f || flash >= 0.01f
    }

    /** Fold progress eased into the 0..1 strength the measurements were fit against. */
    fun foldEase(reach: Float): Float {
        val r = 1f - reach.coerceIn(0f, 1f)
        return 1f - r * r
    }

    fun level(cover: Boolean, reach: Float, coverFrost: Float, innerFade: Float): Level {
        if (cover) {
            val e = coverFrost.coerceIn(0f, 1f)
            return Level(e, CoverPeakSigma * e, 0f)
        }
        if (innerFade > 0f) {
            val e = max(InnerSwapStrength * innerFade, foldEase(reach))
            return Level(
                e = e,
                peak = PeakSigma * e,
                flash = InnerSwapFlash * innerFade * innerFade * innerFade,
            )
        }
        val e = foldEase(reach)
        return Level(e, PeakSigma * e, 0f)
    }

    /** Inner screen: sigma at distance [d] from the hinge toward the free edge. */
    fun innerSigma(peak: Float, d: Float): Float {
        return if (d <= 0f) 0f else peak * d.coerceAtMost(1f).pow(SigmaExponent)
    }

    /** Cover: distance into the frosted part, from [u] = 0 on the hinge side to 1 on the outer edge. */
    fun coverDistance(u: Float): Float {
        return ((u - CoverSharpEdge) / (1f - CoverSharpEdge)).coerceIn(0f, 1f)
    }

    fun coverSigma(peak: Float, d: Float): Float {
        return if (d <= 0f) 0f else peak * d.coerceAtMost(1f).pow(CoverSigmaExponent)
    }

    /** Brightness on the inner screen. [d] is null on the panel in hand. */
    fun innerGain(e: Float, d: Float?, flash: Float): Float {
        val stationary = 1f - 0.32f * e
        val gain = if (d == null) stationary else stationary * (1f - 0.85f * e * d)
        return gain * (1f - flash)
    }

    /** Brightness on the cover, from [u] = 0 on the hinge side to 1 on the outer edge. */
    fun coverGain(e: Float, u: Float): Float = 1f - e * (0.1f + 0.2f * u)
}
