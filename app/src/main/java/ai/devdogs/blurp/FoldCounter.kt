package ai.devdogs.blurp

/** Counts completed folds: the hinge at 90 degrees or less, then back to 172 or more. */
class FoldCounter(private val onFold: () -> Unit) {
    private var folded = false

    fun onAngle(degrees: Float) {
        if (degrees <= 90f) folded = true
        if (degrees >= 172f && folded) {
            folded = false
            onFold()
        }
    }
}
