package com.ayush.vallpaper.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ayush.vallpaper.domain.model.AppSettings
import com.ayush.vallpaper.domain.model.WallpaperStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore by preferencesDataStore(
    name = "vallpaper_settings"
)

class AppSettingsRepository(
    private val context: Context
) {

    private object Keys {
        val automaticWallpaper =
            booleanPreferencesKey("automatic_wallpaper")

        val selectedStyle =
            stringPreferencesKey("selected_style")

        val applyToHomeScreen =
            booleanPreferencesKey("apply_to_home_screen")

        val applyToLockScreen =
            booleanPreferencesKey("apply_to_lock_screen")
    }

    val settings: Flow<AppSettings> =
        context.settingsDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(androidx.datastore.preferences.core.emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->

                val style =
                    preferences[Keys.selectedStyle]
                        ?.let { value ->
                            WallpaperStyle.entries.firstOrNull {
                                it.name == value
                            }
                        }
                        ?: WallpaperStyle.AMBIENT

                AppSettings(
                    automaticWallpaper =
                        preferences[Keys.automaticWallpaper]
                            ?: true,
                    selectedStyle = style,
                    applyToHomeScreen =
                        preferences[Keys.applyToHomeScreen]
                            ?: true,
                    applyToLockScreen =
                        preferences[Keys.applyToLockScreen]
                            ?: true
                )
            }

    suspend fun setAutomaticWallpaper(
        enabled: Boolean
    ) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.automaticWallpaper] = enabled
        }
    }

    suspend fun setSelectedStyle(
        style: WallpaperStyle
    ) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.selectedStyle] = style.name
        }
    }

    suspend fun setApplyToHomeScreen(
        enabled: Boolean
    ) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.applyToHomeScreen] = enabled
        }
    }

    suspend fun setApplyToLockScreen(
        enabled: Boolean
    ) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.applyToLockScreen] = enabled
        }
    }
}
