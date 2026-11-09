package ai.devdogs.blurp

import android.app.AppOpsManager
import android.content.pm.PackageInstaller
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Android holds back accessibility for an app installed from a downloaded file (on Android 15
 * and up through Enhanced Confirmation; One UI 9 says "App was denied access"). The way past it,
 * "Allow restricted settings" in App info's ⋮ menu, only appears after Android has refused the
 * switch once, and the refusal shows as the app op reading ignored. Seen on a Fold8, 2026-11-08:
 * before any refusal the op read allowed, so 1.1's helper never showed.
 */
class RestrictedSettingsHelpTest {
    private val downloaded = PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE
    private val pickedFile = PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE

    @Test
    fun beforeAnyRefusalADownloadedInstallTriesTheSwitchFirst() {
        for (mode in listOf(AppOpsManager.MODE_ALLOWED, AppOpsManager.MODE_DEFAULT, null)) {
            assertEquals("mode $mode", RestrictedSettingsStep.TryFirst, restrictedSettingsStep(downloaded, mode))
        }
    }

    @Test
    fun afterARefusalItAllowsRestrictedSettingsInAppInfo() {
        assertEquals(RestrictedSettingsStep.AllowNow, restrictedSettingsStep(downloaded, AppOpsManager.MODE_IGNORED))
        assertEquals(RestrictedSettingsStep.AllowNow, restrictedSettingsStep(downloaded, AppOpsManager.MODE_ERRORED))
    }

    @Test
    fun anApkPickedFromFilesIsHeldBackToo() {
        assertEquals(RestrictedSettingsStep.TryFirst, restrictedSettingsStep(pickedFile, AppOpsManager.MODE_ALLOWED))
        assertEquals(RestrictedSettingsStep.AllowNow, restrictedSettingsStep(pickedFile, AppOpsManager.MODE_IGNORED))
    }

    @Test
    fun playAndUsbInstallsAreNotHeldBack() {
        for (source in listOf(
            PackageInstaller.PACKAGE_SOURCE_STORE,
            PackageInstaller.PACKAGE_SOURCE_OTHER,
            PackageInstaller.PACKAGE_SOURCE_UNSPECIFIED,
        )) {
            assertEquals("source $source", RestrictedSettingsStep.None, restrictedSettingsStep(source, AppOpsManager.MODE_IGNORED))
        }
    }
}
