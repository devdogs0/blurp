package ai.devdogs.blurp

enum class Screen { Unsupported, Disclosure, EnableService, Tutorial, Home }

/** Which screen the app opens on, from what is true right now. */
object OnboardingFlow {
    fun screenFor(supported: Boolean, consented: Boolean, serviceEnabled: Boolean, tutorialDone: Boolean): Screen = when {
        !supported -> Screen.Unsupported
        !consented -> Screen.Disclosure
        !serviceEnabled -> Screen.EnableService
        !tutorialDone -> Screen.Tutorial
        else -> Screen.Home
    }
}
