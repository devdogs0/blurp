package ai.devdogs.blurp

import android.content.pm.PackageInstaller
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Android holds back accessibility for an app installed from a downloaded file (on Android 15
 * and up through Enhanced Confirmation; One UI 9 says "App was denied access"). The way past it,
 * "Allow restricted settings" in App info's ⋮ menu, only appears after Android has refused the
 * switch once. Apps cannot read that refusal (the app op is closed to them: seen on a Fold8,
 * 2026-11-08), so Blurp remembers where it last sent the user and lights the step after it:
 * back from Settings with the switch still off, Android refused it, so App info is next; back
 * from App info, Settings again.
 */
class RestrictedSettingsHelpTest {
    private val downloaded = PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE
    private val pickedFile = PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE

    @Test
    fun aDownloadedInstallTriesTheSwitchFirst() {
        assertEquals(RestrictedSettingsStep.TryFirst, restrictedSettingsStep(downloaded, SetupTrip.None, setupDone = false))
    }

    @Test
    fun backFromSettingsWithTheSwitchOffItAllowsRestrictedSettingsInAppInfo() {
        assertEquals(RestrictedSettingsStep.AllowNow, restrictedSettingsStep(downloaded, SetupTrip.Settings, setupDone = false))
    }

    @Test
    fun backFromAppInfoItSwitchesBlurpOnInSettings() {
        assertEquals(RestrictedSettingsStep.SwitchOn, restrictedSettingsStep(downloaded, SetupTrip.AppInfo, setupDone = false))
    }

    @Test
    fun anApkPickedFromFilesIsHeldBackToo() {
        assertEquals(RestrictedSettingsStep.TryFirst, restrictedSettingsStep(pickedFile, SetupTrip.None, setupDone = false))
        assertEquals(RestrictedSettingsStep.AllowNow, restrictedSettingsStep(pickedFile, SetupTrip.Settings, setupDone = false))
        assertEquals(RestrictedSettingsStep.SwitchOn, restrictedSettingsStep(pickedFile, SetupTrip.AppInfo, setupDone = false))
    }

    @Test
    fun playAndUsbInstallsAreNotHeldBack() {
        for (source in listOf(
            PackageInstaller.PACKAGE_SOURCE_STORE,
            PackageInstaller.PACKAGE_SOURCE_OTHER,
            PackageInstaller.PACKAGE_SOURCE_UNSPECIFIED,
        )) {
            for (trip in SetupTrip.entries) {
                assertEquals("source $source trip $trip", RestrictedSettingsStep.None, restrictedSettingsStep(source, trip, setupDone = false))
            }
        }
    }

    // Setup only finishes with Blurp switched on, so Android has let it through by then. Turned
    // off later, it switches straight back on: no refusal, no App info.
    @Test
    fun afterSetupSwitchingBackOnIsThePlainStep() {
        for (trip in SetupTrip.entries) {
            assertEquals("trip $trip", RestrictedSettingsStep.None, restrictedSettingsStep(downloaded, trip, setupDone = true))
        }
    }

    @Test
    fun aStoredTripReadsBackAndAnythingElseIsNone() {
        for (trip in SetupTrip.entries) assertEquals(trip, SetupTrip.fromStored(trip.name))
        assertEquals(SetupTrip.None, SetupTrip.fromStored(null))
        assertEquals(SetupTrip.None, SetupTrip.fromStored("Somewhere"))
    }
}
