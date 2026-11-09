package ai.devdogs.blurp

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Process
import android.provider.Settings

/** Where someone who installed Blurp from a file stands with Android's restricted settings. */
enum class RestrictedSettingsStep {
    /** Installed from Play or over USB: nothing is held back. */
    None,

    /** Try the switch: Android refuses it once, and only then offers the way past. */
    TryFirst,

    /** Refused once: "Allow restricted settings" is now in App info's ⋮ menu. */
    AllowNow,
}

/**
 * Android holds back accessibility for an app installed from a downloaded or picked file (on
 * Android 15 and up through Enhanced Confirmation; One UI 9 says "App was denied access"). The
 * app op reads allowed until the first refusal and ignored after it, so it tells the two steps
 * apart; it cannot say whether the switch is still held back before that refusal.
 */
fun restrictedSettingsStep(packageSource: Int, restrictedSettingsMode: Int?): RestrictedSettingsStep {
    val fromAFile = packageSource == PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE ||
        packageSource == PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE
    if (!fromAFile) return RestrictedSettingsStep.None
    val refused = restrictedSettingsMode == AppOpsManager.MODE_IGNORED || restrictedSettingsMode == AppOpsManager.MODE_ERRORED
    return if (refused) RestrictedSettingsStep.AllowNow else RestrictedSettingsStep.TryFirst
}

/** The app op behind "Allow restricted settings" (AppOpsManager.OPSTR_ACCESS_RESTRICTED_SETTINGS, hidden). */
private const val RESTRICTED_SETTINGS_OP = "android:access_restricted_settings"

fun restrictedSettingsStep(context: Context): RestrictedSettingsStep {
    val source = runCatching { context.packageManager.getInstallSourceInfo(context.packageName).packageSource }
        .getOrDefault(PackageInstaller.PACKAGE_SOURCE_UNSPECIFIED)
    val mode = runCatching {
        context.getSystemService(AppOpsManager::class.java)
            .unsafeCheckOpNoThrow(RESTRICTED_SETTINGS_OP, Process.myUid(), context.packageName)
    }.getOrNull()
    return restrictedSettingsStep(source, mode)
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
