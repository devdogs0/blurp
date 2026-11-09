package ai.devdogs.blurp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Bundle
import android.provider.Settings

/** Where someone who installed Blurp from a file stands with Android's restricted settings. */
enum class RestrictedSettingsStep {
    /** Installed from Play or over USB: nothing is held back. */
    None,

    /** Step 1, try the switch: Android refuses it once, and only then offers the way past. */
    TryFirst,

    /** Step 2, refused once: "Allow restricted settings" is now in App info's ⋮ menu. */
    AllowNow,

    /** Step 3, allowed in App info: the switch works now. */
    SwitchOn,
}

/** Where Blurp last sent the user to switch it on. What Android answered there, Blurp can't see. */
enum class SetupTrip {
    None,
    Settings,
    AppInfo;

    companion object {
        fun fromStored(name: String?): SetupTrip = entries.firstOrNull { it.name == name } ?: None
    }
}

/**
 * Android holds back accessibility for an app installed from a downloaded or picked file (on
 * Android 15 and up through Enhanced Confirmation; One UI 9 says "App was denied access"). Apps
 * cannot read that refusal: the app op behind it is closed to them. So the step follows
 * [lastTrip]: back from Settings with the switch still off, Android refused it and App info is
 * next; back from App info, Settings again. If the allow didn't happen there, Settings refuses
 * again and the trip after it points back to App info.
 */
fun restrictedSettingsStep(packageSource: Int, lastTrip: SetupTrip, setupDone: Boolean): RestrictedSettingsStep {
    val fromAFile = packageSource == PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE ||
        packageSource == PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE
    // Setup only finishes with Blurp switched on, so by then Android has let it through for good.
    if (!fromAFile || setupDone) return RestrictedSettingsStep.None
    return when (lastTrip) {
        SetupTrip.None -> RestrictedSettingsStep.TryFirst
        SetupTrip.Settings -> RestrictedSettingsStep.AllowNow
        SetupTrip.AppInfo -> RestrictedSettingsStep.SwitchOn
    }
}

fun restrictedSettingsStep(context: Context, lastTrip: SetupTrip, setupDone: Boolean): RestrictedSettingsStep {
    val source = runCatching { context.packageManager.getInstallSourceInfo(context.packageName).packageSource }
        .getOrDefault(PackageInstaller.PACKAGE_SOURCE_UNSPECIFIED)
    return restrictedSettingsStep(source, lastTrip, setupDone)
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

/**
 * Settings' key for the Installed apps row on One UI 9's Accessibility page (`sec_accessibility_settings.xml`),
 * where Blurp is listed. Apps can't open Blurp's own page there: that needs
 * OPEN_ACCESSIBILITY_DETAILS_SETTINGS, which only system apps and app stores can hold.
 */
private const val INSTALLED_APPS_KEY = "accessibility_installed_services"

/** Opens Accessibility with the Installed apps row highlighted, the way a Settings search result lands. */
fun openAccessibilitySettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            putExtra(":settings:fragment_args_key", INSTALLED_APPS_KEY)
            putExtra(":settings:show_fragment_args", Bundle().apply { putString(":settings:fragment_args_key", INSTALLED_APPS_KEY) })
        },
    )
}
