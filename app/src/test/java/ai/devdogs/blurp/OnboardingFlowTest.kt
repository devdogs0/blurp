package ai.devdogs.blurp

import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingFlowTest {
    private fun screen(
        supported: Boolean = true,
        consented: Boolean = true,
        serviceEnabled: Boolean = true,
        tutorialDone: Boolean = true,
        allSetSeen: Boolean = true,
    ) = OnboardingFlow.screenFor(supported, consented, serviceEnabled, tutorialDone, allSetSeen)

    @Test
    fun unsupportedWins() {
        assertEquals(Screen.Unsupported, screen(supported = false))
    }

    @Test
    fun freshInstallStartsAtDisclosure() {
        assertEquals(Screen.Disclosure, screen(consented = false, serviceEnabled = false, tutorialDone = false, allSetSeen = false))
    }

    @Test
    fun consentSurvivesRestart() {
        assertEquals(Screen.EnableService, screen(serviceEnabled = false, tutorialDone = false, allSetSeen = false))
    }

    @Test
    fun enabledGoesToTutorial() {
        assertEquals(Screen.Tutorial, screen(tutorialDone = false, allSetSeen = false))
    }

    /** The first fold ends the tutorial, wherever it happened; the user then hears they're done. */
    @Test
    fun aFoldShowsAllSet() {
        assertEquals(Screen.AllSet, screen(allSetSeen = false))
    }

    @Test
    fun allSetOnceThenHome() {
        assertEquals(Screen.Home, screen())
    }

    @Test
    fun serviceTurnedOffReturnsToEnableStep() {
        assertEquals(Screen.EnableService, screen(serviceEnabled = false))
        assertEquals(Screen.EnableService, screen(serviceEnabled = false, allSetSeen = false))
    }

    // Play: a non-accessibility use of the Accessibility service waits for the in-app disclosure,
    // even when someone switches Blurp on straight from Settings without opening it.
    @Test
    fun noFrostBeforeTheDisclosureIsAccepted() {
        assertEquals(false, OnboardingFlow.frostShown(consented = false, frostEnabled = true))
        assertEquals(true, OnboardingFlow.frostShown(consented = true, frostEnabled = true))
        assertEquals(false, OnboardingFlow.frostShown(consented = true, frostEnabled = false))
    }

    @Test
    fun aFoldBeforeConsentDoesNotEndSetup() {
        assertEquals(false, OnboardingFlow.foldEndsSetup(consented = false, tutorialDone = false))
        assertEquals(true, OnboardingFlow.foldEndsSetup(consented = true, tutorialDone = false))
        assertEquals(false, OnboardingFlow.foldEndsSetup(consented = true, tutorialDone = true))
    }
}
