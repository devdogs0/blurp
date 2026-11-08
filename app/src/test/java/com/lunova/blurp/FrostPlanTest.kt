package com.lunova.blurp

import android.view.Surface
import org.junit.Assert.assertTrue
import org.junit.Test

class FrostPlanTest {
    private val inner = FrostGeometry.of(2448, 1848, Surface.ROTATION_0)

    /** The moving half is the left one; the light spill reaches this far past the hinge. */
    private val spillEnd = inner.span / 2f + DuoFrostModel.InHandSpill * (inner.span / 2f)

    private fun blurredPanes(level: DuoFrostModel.Level) =
        planPanes(inner, level).filter { it.sigma >= 1f }

    @Test
    fun openingRevealBlursOnlyTheMovingHalf() {
        // Duo video: the half in hand is dimmed as the inner screen appears, but stays sharp.
        val reveal = DuoFrostModel.level(cover = false, reach = 0f, coverFrost = 0f, innerFade = 1f)
        val panes = blurredPanes(reveal)
        assertTrue("the reveal frosts something", panes.isNotEmpty())
        panes.forEach { pane ->
            assertTrue("pane $pane reaches into the half in hand", pane.rect.right <= spillEnd + 1)
        }
    }

    @Test
    fun openingRevealStillDimsTheWholeScreen() {
        val reveal = DuoFrostModel.level(cover = false, reach = 0f, coverFrost = 0f, innerFade = 1f)
        assertTrue(reveal.flash > 0.5f)
        assertTrue(DuoFrostModel.innerGain(reveal.e, null, reveal.flash) < 0.5f)
    }

    @Test
    fun closingFrostBlursOnlyTheMovingHalf() {
        val closing = DuoFrostModel.level(cover = false, reach = 0.5f, coverFrost = 0f, innerFade = 0f)
        blurredPanes(closing).forEach { pane ->
            assertTrue("pane $pane reaches into the half in hand", pane.rect.right <= spillEnd + 1)
        }
    }

    @Test
    fun panesStayOnScreen() {
        val reveal = DuoFrostModel.level(cover = false, reach = 0f, coverFrost = 0f, innerFade = 1f)
        planPanes(inner, reveal).forEach { pane ->
            assertTrue(pane.rect.left >= 0 && pane.rect.top >= 0)
            assertTrue(pane.rect.right <= inner.width && pane.rect.bottom <= inner.height)
        }
    }
}
