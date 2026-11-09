package pl.lublin.wsei.plan.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pl.lublin.wsei.plan.domain.model.StudentSession
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class EncryptedAuthPayload(
    val login: String,
    val password: String,
    val isDebugAccount: Boolean,
    val savedSession: StudentSession? = null,
    val encryptedAtEpochMillis: Long = System.currentTimeMillis()
)

/**
 * Hardware-backed cryptographic credential store using Android Keystore (`AndroidKeyStore`)
 * and `AES-256-GCM` (`AES/GCM/NoPadding` with 128-bit authentication tag).
 *
 * Ensures login credentials and session tokens are never stored in plaintext on the device
 * and are protected against unauthorized extraction.
 */
@Singleton
class EncryptedCredentialStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "wsei_wd_master_aes256_gcm_key_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val PREFS_FILE = "wsei_encrypted_credential_vault"
        private const val KEY_CIPHERTEXT_BLOB = "vault_aes_gcm_blob"
        private const val KEY_REMEMBER_ENABLED = "vault_remember_enabled"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existingEntry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existingEntry != null) {
            return existingEntry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts and persists the student's credentials and session using AES-256-GCM.
     */
    fun saveEncryptedCredentials(
        login: String,
        password: String,
        session: StudentSession,
        rememberCredentials: Boolean = true
    ) {
        if (!rememberCredentials) {
            clearVault()
            return
        }
        runCatching {
            val payload = EncryptedAuthPayload(
                login = login,
                password = password,
                isDebugAccount = session.isDebugAccount,
                savedSession = session
            )
            val plaintextBytes = json.encodeToString(payload).toByteArray(Charsets.UTF_8)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintextBytes)

            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val cipherBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)

            prefs.edit()
                .putString(KEY_CIPHERTEXT_BLOB, "$ivBase64:$cipherBase64")
                .putBoolean(KEY_REMEMBER_ENABLED, true)
                .apply()
        }
    }

    /**
     * Decrypts and returns the stored [EncryptedAuthPayload] from the hardware-backed vault,
     * or `null` if no credentials are stored or if authentication tag verification fails.
     */
    fun loadDecryptedCredentials(): EncryptedAuthPayload? {
        val blob = prefs.getString(KEY_CIPHERTEXT_BLOB, null) ?: return null
        val parts = blob.split(":")
        if (parts.size != 2) return null

        return runCatching {
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
            val decryptedBytes = cipher.doFinal(ciphertext)
            val jsonStr = String(decryptedBytes, Charsets.UTF_8)
            json.decodeFromString<EncryptedAuthPayload>(jsonStr)
        }.getOrNull()
    }

    fun isRememberMeEnabled(): Boolean = prefs.getBoolean(KEY_REMEMBER_ENABLED, true)

    fun clearVault() {
        prefs.edit()
            .remove(KEY_CIPHERTEXT_BLOB)
            .putBoolean(KEY_REMEMBER_ENABLED, false)
            .apply()
    }
}
