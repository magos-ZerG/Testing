package com.z23u184.studymate.data.session

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.z23u184.studymate.domain.model.UserMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val SESSION_DATASTORE_NAME = "studymate_session"

private val Context.sessionDataStore by preferencesDataStore(
    name = SESSION_DATASTORE_NAME
)

class SessionPreferencesDataSource(
    context: Context,
) : SessionLocalDataSource, AuthTokenLocalDataSource {

    private val appContext = context.applicationContext
    private val dataStore = appContext.sessionDataStore

    override suspend fun getUserMode(): UserMode = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences ->
            preferences[Keys.USER_MODE]?.let(UserMode::valueOf) ?: UserMode.LOCAL
        }
        .first()

    fun observeUserMode(): Flow<UserMode> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences ->
            preferences[Keys.USER_MODE]?.let(UserMode::valueOf) ?: UserMode.LOCAL
        }

    override suspend fun setUserMode(mode: UserMode) {
        dataStore.edit { it[Keys.USER_MODE] = mode.name }
    }

    override suspend fun getAuthorizedUserId(): String? = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.AUTHORIZED_USER_ID] }
        .first()

    override suspend fun setAuthorizedUserId(value: String?) {
        dataStore.edit {
            if (value == null) it.remove(Keys.AUTHORIZED_USER_ID)
            else it[Keys.AUTHORIZED_USER_ID] = value
        }
    }

    override suspend fun getLastSyncAtEpochMs(): Long? = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.LAST_SYNC_AT] }
        .first()

    override suspend fun setLastSyncAtEpochMs(value: Long) {
        dataStore.edit { it[Keys.LAST_SYNC_AT] = value }
    }

    override suspend fun clearLastSyncAtEpochMs() {
        dataStore.edit { it.remove(Keys.LAST_SYNC_AT) }
    }

    suspend fun getLastSuccessfulSyncAtEpochMs(): Long? = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.LAST_SUCCESSFUL_SYNC_AT] }
        .first()

    suspend fun setLastSuccessfulSyncAtEpochMs(value: Long?) {
        dataStore.edit {
            if (value == null) it.remove(Keys.LAST_SUCCESSFUL_SYNC_AT)
            else it[Keys.LAST_SUCCESSFUL_SYNC_AT] = value
        }
    }

    override suspend fun getAccessToken(): String? = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.ACCESS_TOKEN] }
        .first()

    override suspend fun setAccessToken(value: String?) {
        dataStore.edit {
            if (value == null) it.remove(Keys.ACCESS_TOKEN)
            else it[Keys.ACCESS_TOKEN] = value
        }
    }

    override suspend fun getRefreshToken(): String? = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.REFRESH_TOKEN] }
        .first()

    override suspend fun setRefreshToken(value: String?) {
        dataStore.edit {
            if (value == null) it.remove(Keys.REFRESH_TOKEN)
            else it[Keys.REFRESH_TOKEN] = value
        }
    }

    override suspend fun clearTokens() {
        dataStore.edit {
            it.remove(Keys.ACCESS_TOKEN)
            it.remove(Keys.REFRESH_TOKEN)
        }
    }

    private object Keys {
        val USER_MODE: Preferences.Key<String> = stringPreferencesKey("user_mode")
        val AUTHORIZED_USER_ID: Preferences.Key<String> = stringPreferencesKey("authorized_user_id")
        val LAST_SYNC_AT: Preferences.Key<Long> = longPreferencesKey("last_sync_at")
        val LAST_SUCCESSFUL_SYNC_AT: Preferences.Key<Long> = longPreferencesKey("last_successful_sync_at")
        val ACCESS_TOKEN: Preferences.Key<String> = stringPreferencesKey("access_token")
        val REFRESH_TOKEN: Preferences.Key<String> = stringPreferencesKey("refresh_token")
    }
}
