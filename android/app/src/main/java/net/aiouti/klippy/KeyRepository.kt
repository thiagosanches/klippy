package net.aiouti.klippy

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class KeyRepository(context: Context) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    
    private val sharedPreferences = EncryptedSharedPreferences.create(
        "klippy_secure_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveServerUrl(url: String) {
        sharedPreferences.edit().putString(KEY_SERVER_URL, url).apply()
    }

    fun getServerUrl(): String? {
        return sharedPreferences.getString(KEY_SERVER_URL, null)
    }

    fun savePrivateKey(key: String) {
        sharedPreferences.edit().putString(KEY_PRIVATE, key).apply()
    }

    fun getPrivateKey(): String? {
        return sharedPreferences.getString(KEY_PRIVATE, null)
    }

    fun savePublicKey(key: String) {
        sharedPreferences.edit().putString(KEY_PUBLIC, key).apply()
    }

    fun getPublicKey(): String? {
        return sharedPreferences.getString(KEY_PUBLIC, null)
    }

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_PRIVATE = "private_key"
        private const val KEY_PUBLIC = "public_key"
    }
}
