package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.cricketPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "cricketpro_prefs")

class ThemePreferences(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    }

    val themeMode: Flow<ThemeMode> = context.cricketPrefsDataStore.data.map { prefs ->
        val raw = prefs[Keys.THEME_MODE] ?: ThemeMode.DARK.name
        enumValues<ThemeMode>().firstOrNull { it.name == raw } ?: ThemeMode.DARK
    }

    val hapticsEnabled: Flow<Boolean> = context.cricketPrefsDataStore.data.map { prefs ->
        prefs[Keys.HAPTICS_ENABLED] ?: true
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.cricketPrefsDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.cricketPrefsDataStore.edit { prefs ->
            prefs[Keys.HAPTICS_ENABLED] = enabled
        }
    }
}
