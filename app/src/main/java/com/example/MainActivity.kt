package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.configureAppCheck
import com.example.data.local.ThemePreferences
import com.example.ui.navigation.CricketProApp
import com.example.ui.theme.CricketProTheme
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Must configure App Check before any Firebase service calls or setContent
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            val prefs = remember { ThemePreferences(applicationContext) }
            val scope = rememberCoroutineScope()
            val themeMode by prefs.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.DARK)
            val hapticsEnabled by prefs.hapticsEnabled.collectAsStateWithLifecycle(initialValue = true)

            CricketProTheme(themeMode = themeMode) {
                CricketProApp(
                    themeMode = themeMode,
                    hapticsEnabled = hapticsEnabled,
                    onSetThemeMode = { mode ->
                        scope.launch { prefs.setThemeMode(mode) }
                    },
                    onSetHapticsEnabled = { enabled ->
                        scope.launch { prefs.setHapticsEnabled(enabled) }
                    }
                )
            }
        }
    }
}
