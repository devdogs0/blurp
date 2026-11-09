package ai.devdogs.blurp

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Process
import android.provider.Settings

/**
 * Android holds back accessibility for an app installed from a downloaded or picked file until
 * the user allows restricted settings in its App info (One UI 9: "App was denied access"). Play
 * and USB installs are not held back.
 */
fun needsRestrictedSettingsHelp(packageSource: Int, restrictedSettingsMode: Int?): Boolean {
    val fromAFile = packageSource == PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE ||
        packageSource == PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE
    return fromAFile && restrictedSettingsMode != AppOpsManager.MODE_ALLOWED
}

/** The app op behind "Allow restricted settings" (AppOpsManager.OPSTR_ACCESS_RESTRICTED_SETTINGS, hidden). */
private const val RESTRICTED_SETTINGS_OP = "android:access_restricted_settings"

fun restrictedSettingsHelpNeeded(context: Context): Boolean {
    val source = runCatching { context.packageManager.getInstallSourceInfo(context.packageName).packageSource }
        .getOrDefault(PackageInstaller.PACKAGE_SOURCE_UNSPECIFIED)
    val mode = runCatching {
        context.getSystemService(AppOpsManager::class.java)
            .unsafeCheckOpNoThrow(RESTRICTED_SETTINGS_OP, Process.myUid(), context.packageName)
    }.getOrNull()
    return needsRestrictedSettingsHelp(source, mode)
}

/** Blurp's App info, whose ⋮ menu holds "Allow restricted settings". */
fun openAppInfo(context: Context) {
    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
}

fun isBlurpServiceEnabled(context: Context): Boolean {
    val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        ?: return false
    val blurp = ComponentName(context, FoldBlurService::class.java)
    return enabled.split(':').any { ComponentName.unflattenFromString(it) == blurp }
}

/** Settings reads this extra as a string (`flattenToString`), not a parcelled ComponentName. */
fun accessibilityDetailsExtra(packageName: String, className: String): String = "$packageName/$className"

fun openAccessibilitySettings(context: Context) {
    val blurp = ComponentName(context, FoldBlurService::class.java)
    val details = Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS").apply {
        putExtra(Intent.EXTRA_COMPONENT_NAME, accessibilityDetailsExtra(blurp.packageName, blurp.className))
    }
    try {
        context.startActivity(details)
    } catch (_: Exception) {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}
