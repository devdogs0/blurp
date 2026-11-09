package ai.devdogs.blurp

import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceStatusTest {
    @Test
    fun detailsExtraIsFlattenedComponentName() {
        assertEquals(
            "ai.devdogs.blurp/ai.devdogs.blurp.FoldBlurService",
            accessibilityDetailsExtra("ai.devdogs.blurp", "ai.devdogs.blurp.FoldBlurService"),
        )
    }
}
