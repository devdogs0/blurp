package ai.devdogs.blurp

enum class Screen { Unsupported, Disclosure, EnableService, Tutorial, AllSet, Home }

/** Which screen the app opens on, from what is true right now. */
object OnboardingFlow {
    /**
     * [tutorialDone] turns true at the first fold, recorded by the service even
     * when the app is closed; [allSetSeen] once the user has tapped Done on the
     * "You're all set" screen that follows.
     */
    fun screenFor(
        supported: Boolean,
        consented: Boolean,
        serviceEnabled: Boolean,
        tutorialDone: Boolean,
        allSetSeen: Boolean,
    ): Screen = when {
        !supported -> Screen.Unsupported
        !consented -> Screen.Disclosure
        !serviceEnabled -> Screen.EnableService
        !tutorialDone -> Screen.Tutorial
        !allSetSeen -> Screen.AllSet
        else -> Screen.Home
    }
}
