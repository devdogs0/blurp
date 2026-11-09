package ai.devdogs.blurp

import android.view.Surface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrostGeometryTest {
    @Test
    fun fold8InnerScreenIsNotTheCoverInEitherOrientation() {
        assertFalse(FrostGeometry.isCover(2448, 1848))
        assertFalse(FrostGeometry.isCover(1848, 2448))
    }

    @Test
    fun fold8CoverScreenIsTheCoverInEitherOrientation() {
        assertTrue(FrostGeometry.isCover(1248, 1972))
        assertTrue(FrostGeometry.isCover(1972, 1248))
    }

    @Test
    fun aLowerScreenResolutionDoesNotChangeWhichScreenIsWhich() {
        // Settings > Display > Screen resolution scales both sides alike.
        assertFalse(FrostGeometry.isCover(1836, 1386))
        assertTrue(FrostGeometry.isCover(936, 1479))
    }

    @Test
    fun innerScreenFrostsFromTheMovingHalfsFreeEdge() {
        assertEquals(FrostEdge.LEFT, FrostGeometry.of(2448, 1848, Surface.ROTATION_0).edge)
        assertEquals(FrostEdge.BOTTOM, FrostGeometry.of(1848, 2448, Surface.ROTATION_90).edge)
        assertEquals(FrostEdge.RIGHT, FrostGeometry.of(2448, 1848, Surface.ROTATION_180).edge)
        assertEquals(FrostEdge.TOP, FrostGeometry.of(1848, 2448, Surface.ROTATION_270).edge)
    }

    @Test
    fun coverFrostsFromItsOuterEdge() {
        assertEquals(FrostEdge.RIGHT, FrostGeometry.of(1248, 1972, Surface.ROTATION_0).edge)
        assertEquals(FrostEdge.TOP, FrostGeometry.of(1972, 1248, Surface.ROTATION_90).edge)
        assertEquals(FrostEdge.LEFT, FrostGeometry.of(1248, 1972, Surface.ROTATION_180).edge)
        assertEquals(FrostEdge.BOTTOM, FrostGeometry.of(1972, 1248, Surface.ROTATION_270).edge)
    }

    /**
     * Fold7: 8.0" inner screen 1968 x 2184, 6.5" cover 1080 x 2520. The Fold8
     * Ultra has the same screen sizes; retailers list its inner screen at
     * 2256 x 2504. Both inner screens are taller than wide at rotation 0,
     * where the Fold8's is wider than tall, but the hinge runs top to bottom
     * on all three.
     */
    private val fold7Inner = 1968 to 2184
    private val ultraInner = 2256 to 2504
    private val tallCover = 1080 to 2520

    @Test
    fun fold7AndUltraInnerScreensAreNotTheCover() {
        for ((w, h) in listOf(fold7Inner, ultraInner)) {
            assertFalse("$w x $h", FrostGeometry.isCover(w, h))
            assertFalse("$h x $w", FrostGeometry.isCover(h, w))
        }
    }

    @Test
    fun fold7AndUltraCoversAreTheCover() {
        val (w, h) = tallCover
        assertTrue(FrostGeometry.isCover(w, h))
        assertTrue(FrostGeometry.isCover(h, w))
    }

    @Test
    fun fold7AndUltraInnerScreensFrostFromTheMovingHalfsFreeEdge() {
        for ((w, h) in listOf(fold7Inner, ultraInner)) {
            assertEquals(FrostEdge.LEFT, FrostGeometry.of(w, h, Surface.ROTATION_0).edge)
            assertEquals(FrostEdge.BOTTOM, FrostGeometry.of(h, w, Surface.ROTATION_90).edge)
            assertEquals(FrostEdge.RIGHT, FrostGeometry.of(w, h, Surface.ROTATION_180).edge)
            assertEquals(FrostEdge.TOP, FrostGeometry.of(h, w, Surface.ROTATION_270).edge)
        }
    }

    @Test
    fun fold7AndUltraCoversFrostFromTheirOuterEdge() {
        val (w, h) = tallCover
        assertEquals(FrostEdge.RIGHT, FrostGeometry.of(w, h, Surface.ROTATION_0).edge)
        assertEquals(FrostEdge.TOP, FrostGeometry.of(h, w, Surface.ROTATION_90).edge)
    }

    @Test
    fun fold7AndUltraFrostCoversTheMovingHalfOnly() {
        // The frost runs from the free edge to the hinge, halfway across.
        for ((w, h) in listOf(fold7Inner, ultraInner)) {
            val geo = FrostGeometry.of(w, h, Surface.ROTATION_0)
            val level = DuoFrostModel.level(cover = false, reach = 0.5f, coverFrost = 0f, innerFade = 0f)
            planPanes(geo, level).filter { it.sigma >= 1f }.forEach { pane ->
                assertTrue("$w x $h: $pane", pane.rect.right <= w / 2 + DuoFrostModel.InHandSpill * w / 2 + 1)
                assertTrue("$w x $h: $pane", pane.rect.bottom <= h && pane.rect.left >= 0)
            }
        }
    }

    @Test
    fun geometryKeepsTheScreenSize() {
        val geo = FrostGeometry.of(1248, 1972, Surface.ROTATION_0)
        assertEquals(1248, geo.width)
        assertEquals(1972, geo.height)
        assertTrue(geo.cover)
    }
}
