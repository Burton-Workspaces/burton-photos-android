package com.burton.photos.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.burton.photos.domain.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.photosStore by preferencesDataStore("burton_photos")

data class StoredSession(
    val baseUrl: String,
    val token: String,
    val user: User?,
    val mode: String,
)

@Singleton
class LocalPrefs @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun origin(): String =
        context.photosStore.data.first()[BASE_URL].orEmpty()

    suspend fun load(): StoredSession? {
        val prefs = context.photosStore.data.first()
        val baseUrl = prefs[BASE_URL].orEmpty()
        val token = prefs[TOKEN].orEmpty()
        if (baseUrl.isBlank() || token.isBlank()) return null
        val id = prefs[USER_ID].orEmpty()
        val user = if (id.isBlank()) {
            null
        } else {
            User(
                id = id,
                email = prefs[EMAIL].orEmpty(),
                name = prefs[NAME].orEmpty(),
                role = prefs[ROLE].orEmpty().ifBlank { "user" },
            )
        }
        return StoredSession(baseUrl, token, user, prefs[MODE].orEmpty())
    }

    suspend fun saveBaseUrl(baseUrl: String) {
        context.photosStore.edit { it[BASE_URL] = baseUrl.trim().trimEnd('/') }
    }

    suspend fun saveSession(baseUrl: String, token: String, user: User, mode: String) {
        context.photosStore.edit {
            it[BASE_URL] = baseUrl.trim().trimEnd('/')
            it[TOKEN] = token
            it[USER_ID] = user.id
            it[EMAIL] = user.email
            it[NAME] = user.name
            it[ROLE] = user.role
            it[MODE] = mode
        }
    }

    suspend fun clearSession() {
        context.photosStore.edit {
            it.remove(TOKEN)
            it.remove(USER_ID)
            it.remove(EMAIL)
            it.remove(NAME)
            it.remove(ROLE)
            it.remove(MODE)
        }
    }

    companion object {
        private val BASE_URL = stringPreferencesKey("base_url")
        private val TOKEN = stringPreferencesKey("token")
        private val USER_ID = stringPreferencesKey("user_id")
        private val EMAIL = stringPreferencesKey("email")
        private val NAME = stringPreferencesKey("name")
        private val ROLE = stringPreferencesKey("role")
        private val MODE = stringPreferencesKey("mode")
    }
}
