package com.archieapps.applock.security

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "security_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

class PinManager(private val context: Context) {

    val isPinSetFlow: Flow<Boolean> = context.securityDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PIN_ENC_KEY] != null || it[PIN_HASH_KEY] != null }

    suspend fun isPinSet(): Boolean = isPinSetFlow.first()

    suspend fun savePin(pin: String): Boolean = withContext(Dispatchers.Default) {
        runCatching {
            val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
            val hash = hash(pin, salt, ITERATIONS)
            val blob = PinKeystore.encrypt(hash)
            context.securityDataStore.edit { preferences ->
                preferences[PIN_VERSION_KEY] = VERSION_KEYSTORE
                preferences[PIN_SALT_KEY] = salt.toBase64()
                preferences[PIN_ITERATIONS_KEY] = ITERATIONS
                preferences[PIN_ENC_KEY] = blob.cipherText.toBase64()
                preferences[PIN_IV_KEY] = blob.iv.toBase64()
                preferences.remove(PIN_HASH_KEY)
            }
        }.isSuccess
    }

    suspend fun verifyPin(pin: String): Boolean = withContext(Dispatchers.Default) {
        runCatching {
            val preferences = context.securityDataStore.data.first()
            val salt = preferences[PIN_SALT_KEY]?.fromBase64() ?: return@runCatching false
            val iterations = preferences[PIN_ITERATIONS_KEY] ?: return@runCatching false
            val candidate = hash(pin, salt, iterations)

            val storedHash = when (preferences[PIN_VERSION_KEY]) {
                VERSION_KEYSTORE -> {
                    val cipherText = preferences[PIN_ENC_KEY]?.fromBase64() ?: return@runCatching false
                    val iv = preferences[PIN_IV_KEY]?.fromBase64() ?: return@runCatching false
                    runCatching { PinKeystore.decrypt(EncryptedBlob(iv, cipherText)) }.getOrNull()
                        ?: return@runCatching false
                }

                else -> preferences[PIN_HASH_KEY]?.fromBase64() ?: return@runCatching false
            }

            MessageDigest.isEqual(candidate, storedHash)
        }.getOrDefault(false)
    }

    private fun hash(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    companion object {
        const val PIN_MIN_LENGTH = 4
        const val PIN_MAX_LENGTH = 8

        private val PIN_HASH_KEY = stringPreferencesKey("pin_hash")
        private val PIN_SALT_KEY = stringPreferencesKey("pin_salt")
        private val PIN_ITERATIONS_KEY = intPreferencesKey("pin_iterations")
        private val PIN_VERSION_KEY = intPreferencesKey("pin_version")
        private val PIN_ENC_KEY = stringPreferencesKey("pin_enc")
        private val PIN_IV_KEY = stringPreferencesKey("pin_iv")

        private const val VERSION_KEYSTORE = 2
        private const val SALT_LENGTH = 16
        private const val ITERATIONS = 120_000
        private const val KEY_LENGTH_BITS = 256
    }
}
