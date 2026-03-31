package com.masterpaper.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesRepository(private val context: Context) {

    private val darkModeKey = booleanPreferencesKey("dark_mode")
    private val followSystemKey = booleanPreferencesKey("follow_system")
    private val notificationsKey = booleanPreferencesKey("notifications")

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[darkModeKey] ?: false
    }

    val isFollowSystem: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[followSystemKey] ?: true
    }

    val isNotificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[notificationsKey] ?: false
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[darkModeKey] = enabled
        }
    }

    suspend fun setFollowSystem(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[followSystemKey] = enabled
        }
    }

    suspend fun setNotifications(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[notificationsKey] = enabled
        }
    }
}
