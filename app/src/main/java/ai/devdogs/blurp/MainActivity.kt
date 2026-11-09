package ai.devdogs.blurp

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
import ai.devdogs.blurp.ui.BlurpActions
import ai.devdogs.blurp.ui.BlurpApp

class MainActivity : ComponentActivity() {
    private lateinit var prefs: BlurpPrefs
    private var screen by mutableStateOf(Screen.Disclosure)
    private var serviceEnabled by mutableStateOf(false)
    private var restrictedStep by mutableStateOf(RestrictedSettingsStep.None)

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
            // Blurp can't see what Android answers in Settings or App info, so it remembers where it
            // sent the user and shows the step after that one (see restrictedSettingsStep).
            onOpenSettings = {
                prefs.lastSetupTrip = SetupTrip.Settings
                openAccessibilitySettings(this)
            },
            onOpenAppInfo = {
                prefs.lastSetupTrip = SetupTrip.AppInfo
                openAppInfo(this)
            },
            onTutorialDone = {
                prefs.tutorialDone = true
                refresh()
            },
            onAllSetDone = {
                // Setup is over: go back to wherever the user was. The toggle is for later visits.
                prefs.allSetSeen = true
                finishAndRemoveTask()
            },
            onFrostChange = { prefs.setFrostEnabled(it) },
        )
        setContent {
            val frostEnabled by prefs.frostEnabled.collectAsState()
            BlurpApp(screen, serviceEnabled, frostEnabled, restrictedStep, actions)
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        serviceEnabled = isBlurpServiceEnabled(this)
        // Read again on every resume: coming back from Settings with the switch off moves to the next step.
        restrictedStep = restrictedSettingsStep(this, prefs.lastSetupTrip, setupDone = prefs.tutorialDone)
        screen = OnboardingFlow.screenFor(
            supported = SamsungBlur.available,
            consented = prefs.consented,
            serviceEnabled = serviceEnabled,
            tutorialDone = prefs.tutorialDone,
            allSetSeen = prefs.allSetSeen,
        )
    }
}
