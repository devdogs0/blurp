package com.lunova.blurp

import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingFlowTest {
    @Test
    fun unsupportedWins() {
        assertEquals(Screen.Unsupported, OnboardingFlow.screenFor(false, true, true, true))
    }

    @Test
    fun freshInstallStartsAtDisclosure() {
        assertEquals(Screen.Disclosure, OnboardingFlow.screenFor(true, false, false, false))
    }

    @Test
    fun consentSurvivesRestart() {
        assertEquals(Screen.EnableService, OnboardingFlow.screenFor(true, true, false, false))
    }

    @Test
    fun enabledGoesToTutorial() {
        assertEquals(Screen.Tutorial, OnboardingFlow.screenFor(true, true, true, false))
    }

    @Test
    fun doneGoesHome() {
        assertEquals(Screen.Home, OnboardingFlow.screenFor(true, true, true, true))
    }

    @Test
    fun serviceTurnedOffReturnsToEnableStep() {
        assertEquals(Screen.EnableService, OnboardingFlow.screenFor(true, true, false, true))
    }
}
