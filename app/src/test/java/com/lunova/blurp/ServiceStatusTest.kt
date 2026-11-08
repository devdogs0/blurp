package com.lunova.blurp

import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceStatusTest {
    @Test
    fun detailsExtraIsFlattenedComponentName() {
        assertEquals(
            "com.lunova.blurp/com.lunova.blurp.FoldBlurService",
            accessibilityDetailsExtra("com.lunova.blurp", "com.lunova.blurp.FoldBlurService"),
        )
    }
}
