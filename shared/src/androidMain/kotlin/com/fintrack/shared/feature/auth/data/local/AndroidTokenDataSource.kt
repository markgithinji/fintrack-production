package com.fintrack.shared.feature.auth.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fintrack.shared.feature.auth.domain.datasource.TokenDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.tokenDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_tokens")

class AndroidTokenDataSource(
    private val context: Context
) : TokenDataSource {

    private val cryptoManager = TinkCryptoManager(context)
    
    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    }

    override val accessToken: Flow<String?> = context.tokenDataStore.data
        .map { prefs -> 
            prefs[ACCESS_TOKEN_KEY]?.let { cryptoManager.decrypt(it) } 
        }
        .distinctUntilChanged()

    override val refreshToken: Flow<String?> = context.tokenDataStore.data
        .map { prefs -> 
            prefs[REFRESH_TOKEN_KEY]?.let { cryptoManager.decrypt(it) } 
        }
        .distinctUntilChanged()

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.tokenDataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = cryptoManager.encrypt(accessToken)
            prefs[REFRESH_TOKEN_KEY] = cryptoManager.encrypt(refreshToken)
        }
    }

    override suspend fun clearTokens() {
        context.tokenDataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN_KEY)
            prefs.remove(REFRESH_TOKEN_KEY)
        }
    }
}
