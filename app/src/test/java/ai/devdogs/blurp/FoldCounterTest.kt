package ai.devdogs.blurp

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A fold is the hinge at 90 degrees or less, then back to 172 or more. The
 * service listens for it so setup finishes even when Blurp's own screen is
 * gone (a user who closes Settings from Recents closes Blurp with it).
 */
class FoldCounterTest {
    private fun folds(vararg angles: Float): Int {
        var count = 0
        val counter = FoldCounter { count++ }
        angles.forEach(counter::onAngle)
        return count
    }

    @Test
    fun foldingToNinetyAndOpeningCounts() {
        assertEquals(1, folds(180f, 90f, 180f))
    }

    @Test
    fun closingAndOpeningCountsOnce() {
        assertEquals(1, folds(180f, 90f, 0f, 90f, 180f))
    }

    @Test
    fun aShallowFoldDoesNotCount() {
        assertEquals(0, folds(180f, 100f, 180f))
    }

    @Test
    fun itCountsOnlyOnceTheHingeIsBackOpen() {
        assertEquals(0, folds(180f, 0f, 90f, 160f))
        assertEquals(1, folds(180f, 0f, 90f, 160f, 175f))
    }

    @Test
    fun everyFoldCounts() {
        assertEquals(2, folds(180f, 0f, 180f, 90f, 180f))
    }

    @Test
    fun stayingOpenDoesNotCountAgain() {
        assertEquals(1, folds(180f, 0f, 180f, 180f, 176f, 180f))
    }
}
