package com.fintrack.shared.feature.auth.data.local

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager

class TinkCryptoManager(context: Context) {
    init {
        AeadConfig.register()
    }

    private val aead: Aead = try {
        createAead(context)
    } catch (e: Exception) {
        // If Keystore is corrupted or key is lost, clear existing keyset and try once more
        try {
            context.getSharedPreferences("fintrack_tink_prefs", Context.MODE_PRIVATE)
                .edit()
                .remove("fintrack_tink_keyset")
                .apply()
            createAead(context)
        } catch (e2: Exception) {
            // Last resort: fallback to a non-Keystore backed AEAD if Keystore is completely broken
            // In a production app, you might want to handle this differently
            throw e2
        }
    }

    private fun createAead(context: Context): Aead {
        return AndroidKeysetManager.Builder()
            .withSharedPref(context, "fintrack_tink_keyset", "fintrack_tink_prefs")
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri("android-keystore://fintrack_master_key")
            .build()
            .keysetHandle
            .getPrimitive(Aead::class.java)
    }

    fun encrypt(data: String): String {
        val encrypted = aead.encrypt(data.toByteArray(Charsets.UTF_8), null)
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }

    fun decrypt(encryptedBase64: String): String? {
        return try {
            val encrypted = Base64.decode(encryptedBase64, Base64.DEFAULT)
            val decrypted = aead.decrypt(encrypted, null)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
