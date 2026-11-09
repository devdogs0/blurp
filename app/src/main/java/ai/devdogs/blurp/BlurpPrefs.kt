package ai.devdogs.blurp

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** First-run progress and the frost switch. Shared by the app and the service, which run in one process. */
class BlurpPrefs private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("blurp", Context.MODE_PRIVATE)
    private val frost = MutableStateFlow(prefs.getBoolean(FROST_ENABLED, true))

    var consented: Boolean
        get() = prefs.getBoolean(CONSENTED, false)
        set(value) = prefs.edit().putBoolean(CONSENTED, value).apply()

    var tutorialDone: Boolean
        get() = prefs.getBoolean(TUTORIAL_DONE, false)
        set(value) = prefs.edit().putBoolean(TUTORIAL_DONE, value).apply()

    val frostEnabled: StateFlow<Boolean> = frost.asStateFlow()
    private val frostListeners = ArrayList<(Boolean) -> Unit>()

    /** Main thread only, like [setFrostEnabled]. */
    fun addFrostListener(listener: (Boolean) -> Unit) {
        frostListeners += listener
    }

    fun removeFrostListener(listener: (Boolean) -> Unit) {
        frostListeners -= listener
    }

    fun setFrostEnabled(enabled: Boolean) {
        if (frost.value == enabled) return
        prefs.edit().putBoolean(FROST_ENABLED, enabled).apply()
        frost.value = enabled
        frostListeners.forEach { it(enabled) }
    }

    companion object {
        private const val CONSENTED = "consented"
        private const val TUTORIAL_DONE = "tutorial_done"
        private const val FROST_ENABLED = "frost_enabled"

        @Volatile
        private var instance: BlurpPrefs? = null

        fun get(context: Context): BlurpPrefs =
            instance ?: synchronized(this) {
                instance ?: BlurpPrefs(context.applicationContext).also { instance = it }
            }
    }
}
