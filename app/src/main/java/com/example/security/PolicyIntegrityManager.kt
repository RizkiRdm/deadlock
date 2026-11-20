package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.example.domain.model.LockedAppPolicy
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class PolicyIntegrityManager {

    companion object {
        private const val KEY_ALIAS = "deadlock_policy_integrity_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val HMAC_ALGO = "HmacSHA256"
    }

    private var fallbackKey: SecretKey? = null

    private fun getOrCreateSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.getKey(KEY_ALIAS, null) as SecretKey
            } else {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, ANDROID_KEYSTORE)
                val keyGenSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                ).build()
                keyGenerator.init(keyGenSpec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            // Fallback for JVM unit tests or environments where AndroidKeyStore is not initialized
            fallbackKey ?: synchronized(this) {
                fallbackKey ?: run {
                    val keyBytes = "deadlock_local_integrity_seed_key_32bytes!!".toByteArray(Charsets.UTF_8)
                    SecretKeySpec(keyBytes, HMAC_ALGO).also { fallbackKey = it }
                }
            }
        }
    }

    private fun buildPayload(policy: LockedAppPolicy): ByteArray {
        val raw = "${policy.id}|${policy.packageName}|${policy.startedAt}|${policy.expiresAt}|${policy.durationYears}"
        return raw.toByteArray(Charsets.UTF_8)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef"
        val result = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val i = b.toInt() and 0xFF
            result.append(hexChars[i shr 4])
            result.append(hexChars[i and 0x0F])
        }
        return result.toString()
    }

    fun signPolicy(policy: LockedAppPolicy): String {
        return try {
            val secretKey = getOrCreateSecretKey()
            val mac = Mac.getInstance(HMAC_ALGO)
            mac.init(secretKey)
            val bytes = mac.doFinal(buildPayload(policy))
            bytesToHex(bytes)
        } catch (e: Exception) {
            ""
        }
    }

    fun verifyPolicy(policy: LockedAppPolicy): Boolean {
        if (policy.integritySignature.isBlank()) return false
        return try {
            val calculated = signPolicy(policy)
            calculated.isNotEmpty() && calculated == policy.integritySignature
        } catch (e: Exception) {
            false
        }
    }
}
