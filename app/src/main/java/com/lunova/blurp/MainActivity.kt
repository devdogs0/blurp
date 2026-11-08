package com.lunova.blurp

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lunova.blurp.ui.BlurpActions
import com.lunova.blurp.ui.BlurpApp

class MainActivity : ComponentActivity() {
    private lateinit var prefs: BlurpPrefs
    private var screen by mutableStateOf(Screen.Disclosure)
    private var serviceEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        prefs = BlurpPrefs.get(this)
        refresh()
        val actions = BlurpActions(
            onConsent = {
                prefs.consented = true
                refresh()
            },
            onOpenSettings = { openAccessibilitySettings(this) },
            onTutorialDone = {
                prefs.tutorialDone = true
                refresh()
            },
            onFrostChange = { prefs.setFrostEnabled(it) },
        )
        setContent {
            val frostEnabled by prefs.frostEnabled.collectAsState()
            BlurpApp(screen, serviceEnabled, frostEnabled, actions)
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        serviceEnabled = isBlurpServiceEnabled(this)
        screen = OnboardingFlow.screenFor(
            supported = SamsungBlur.available,
            consented = prefs.consented,
            serviceEnabled = serviceEnabled,
            tutorialDone = prefs.tutorialDone,
        )
    }
}
