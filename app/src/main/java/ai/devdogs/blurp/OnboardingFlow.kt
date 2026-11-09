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

    /**
     * Google Play: an app using the Accessibility service for something other than accessibility
     * must get consent through its own disclosure first. The service can also be switched on
     * straight from Settings, so it draws nothing until the disclosure is accepted.
     */
    fun frostShown(consented: Boolean, frostEnabled: Boolean): Boolean = consented && frostEnabled

    /** A fold ends setup only once the frost could be seen, so after consent. */
    fun foldEndsSetup(consented: Boolean, tutorialDone: Boolean): Boolean = consented && !tutorialDone
}
