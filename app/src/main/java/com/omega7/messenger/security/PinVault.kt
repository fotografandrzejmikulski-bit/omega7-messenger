package com.omega7.messenger.security

import android.content.Context
import android.util.Base64
import com.omega7.messenger.crypto.AuthKeyManager
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Local authentication vault.
 * The persisted verifier is salted and additionally peppered by a non-exportable
 * Android Keystore HMAC key, reducing the value of an extracted app-data database.
 */
class PinVault(private val context: Context) {
    companion object {
        const val MAX_ATTEMPTS = 3
        private const val MIN_LENGTH = 6
        private const val ITERATIONS = 210_000
        private const val KEY_BITS = 256
        private const val FORMAT = 2
    }

    private val prefs = context.getSharedPreferences("omega7_auth", Context.MODE_PRIVATE)
    private val random = SecureRandom()
    private val authKey = AuthKeyManager()

    init {
        if (prefs.contains("salt") && prefs.getInt("format", 0) != FORMAT) prefs.edit().clear().commit()
    }

    fun isConfigured(): Boolean = prefs.getInt("format", 0) == FORMAT && prefs.contains("salt") && prefs.contains("verifier")
    fun failedAttempts(): Int = prefs.getInt("failed_attempts", 0).coerceIn(0, MAX_ATTEMPTS)
    fun remainingAttempts(): Int = (MAX_ATTEMPTS - failedAttempts()).coerceAtLeast(0)

    fun setPin(pin: CharArray) {
        require(pin.size >= MIN_LENGTH) { "Kod musi mieć co najmniej 6 znaków." }
        val salt = ByteArray(32).also(random::nextBytes)
        val verifier = derive(pin, salt)
        try {
            prefs.edit().putInt("format", FORMAT)
                .putString("salt", b64(salt))
                .putString("verifier", b64(verifier))
                .putInt("failed_attempts", 0).commit()
        } finally {
            pin.fill('\u0000'); verifier.fill(0); salt.fill(0)
        }
    }

    fun verify(pin: CharArray): Boolean {
        if (!isConfigured() || failedAttempts() >= MAX_ATTEMPTS) { pin.fill('\u0000'); return false }
        val salt = runCatching { prefs.getString("salt", null)?.let(::unb64) }.getOrNull()
        val expected = runCatching { prefs.getString("verifier", null)?.let(::unb64) }.getOrNull()
        if (salt == null || expected == null) { pin.fill('\u0000'); return false }
        val actual = try { derive(pin, salt) } finally { pin.fill('\u0000') }
        val ok = MessageDigest.isEqual(expected, actual)
        actual.fill(0); salt.fill(0); expected.fill(0)
        if (ok) resetFailedAttempts() else incrementFailedAttempts()
        return ok
    }

    fun changePin(current: CharArray, newPin: CharArray): Boolean {
        if (newPin.size < MIN_LENGTH) { current.fill('\u0000'); newPin.fill('\u0000'); return false }
        if (!verify(current)) { newPin.fill('\u0000'); return false }
        setPin(newPin)
        return true
    }

    fun resetFailedAttempts() { prefs.edit().putInt("failed_attempts", 0).commit() }

    private fun incrementFailedAttempts() {
        prefs.edit().putInt("failed_attempts", (failedAttempts() + 1).coerceAtMost(MAX_ATTEMPTS)).commit()
    }

    private fun derive(pin: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin, salt, ITERATIONS, KEY_BITS)
        val stretched = try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(authKey.getOrCreateKey())
            mac.doFinal(stretched)
        } finally { stretched.fill(0) }
    }

    private fun b64(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun unb64(value: String) = Base64.decode(value, Base64.NO_WRAP)
}
