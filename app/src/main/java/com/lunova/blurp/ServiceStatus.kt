package com.lunova.blurp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

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
