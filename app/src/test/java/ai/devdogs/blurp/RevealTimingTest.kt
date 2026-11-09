package ai.devdogs.blurp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * After the phone opens or closes, the new screen should light up frosted
 * and clear about as fast as the Duo's: the inner screen is sharp about
 * 0.13 s after it appears, the cover about 0.08 s.
 */
class RevealTimingTest {
    /**
     * When the new panel lit, in seconds after Blurp saw the screens switch:
     * 12 real folds on the Fold8 on 2026-11-08 ("Unblocked screen on" in
     * logcat). Screen recordings stay black for up to 0.6 s after a switch
     * while One UI's fold animation runs, but the panel doesn't: mid-animation,
     * SurfaceFlinger was compositing the app and the frost onto it.
     */
    private val opens = listOf(0.291f, 0.202f, 0.144f, 0.292f, 0.152f, 0.281f)
    private val closes = listOf(0.222f, 0.168f, 0.184f, 0.149f, 0.300f, 0.294f)

    @Test
    fun innerScreenLightsFrosted() {
        opens.forEach { lit ->
            assertTrue("frost when lit at $lit s", DuoFrostModel.revealLeft(lit, cover = false) > 0.7f)
        }
    }

    @Test
    fun coverLightsFrosted() {
        closes.forEach { lit ->
            assertTrue("frost when lit at $lit s", DuoFrostModel.revealLeft(lit, cover = true) > 0.3f)
        }
    }

    /** The screen-on time varies by 0.16 s, so no one hold matches the Duo on every fold. */
    @Test
    fun innerScreenClearsSoonAfterLighting() {
        opens.forEach { lit ->
            assertEquals("frost 0.26 s after lighting at $lit s", 0f, DuoFrostModel.revealLeft(lit + 0.26f, cover = false), 0f)
        }
    }

    @Test
    fun coverClearsSoonAfterLighting() {
        closes.forEach { lit ->
            assertEquals("frost 0.2 s after lighting at $lit s", 0f, DuoFrostModel.revealLeft(lit + 0.2f, cover = true), 0f)
        }
    }

    @Test
    fun revealEasesOffWithoutJumps() {
        for (cover in listOf(false, true)) {
            var last = DuoFrostModel.revealLeft(0f, cover)
            assertEquals(1f, last, 0f)
            var t = 0f
            while (t < 1f) {
                t += 0.002f
                val now = DuoFrostModel.revealLeft(t, cover)
                assertTrue("never grows back (cover=$cover, $t s)", now <= last)
                assertTrue("no jump at $t s (cover=$cover)", last - now < 0.06f)
                last = now
            }
            assertEquals(0f, last, 0f)
        }
    }
}
