package ai.devdogs.blurp

import android.app.AppOpsManager
import android.content.pm.PackageInstaller
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Android holds back accessibility for an app installed from a downloaded file until the user
 * allows restricted settings in its App info; One UI 9 says "App was denied access". Blurp's
 * turn-on step explains that only to the people it happens to.
 */
class RestrictedSettingsHelpTest {
    private val downloaded = PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE
    private val pickedFile = PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE

    @Test
    fun aDownloadedInstallNeedsHelpUntilRestrictedSettingsAreAllowed() {
        assertTrue(needsRestrictedSettingsHelp(downloaded, AppOpsManager.MODE_IGNORED))
        assertTrue(needsRestrictedSettingsHelp(downloaded, AppOpsManager.MODE_DEFAULT))
        assertFalse(needsRestrictedSettingsHelp(downloaded, AppOpsManager.MODE_ALLOWED))
    }

    @Test
    fun anApkPickedFromFilesIsHeldBackToo() {
        assertTrue(needsRestrictedSettingsHelp(pickedFile, AppOpsManager.MODE_IGNORED))
    }

    /** When Android won't say, a downloaded install still gets the steps: they do no harm. */
    @Test
    fun anUnknownSettingStillShowsTheSteps() {
        assertTrue(needsRestrictedSettingsHelp(downloaded, null))
    }

    @Test
    fun playAndUsbInstallsAreNotHeldBack() {
        for (source in listOf(
            PackageInstaller.PACKAGE_SOURCE_STORE,
            PackageInstaller.PACKAGE_SOURCE_OTHER,
            PackageInstaller.PACKAGE_SOURCE_UNSPECIFIED,
        )) {
            assertFalse("source $source", needsRestrictedSettingsHelp(source, AppOpsManager.MODE_IGNORED))
        }
    }
}
