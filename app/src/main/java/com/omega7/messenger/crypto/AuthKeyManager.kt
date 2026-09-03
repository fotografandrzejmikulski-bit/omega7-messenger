package com.omega7.messenger.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/** Non-exportable Keystore key used as an on-device authentication pepper. */
class AuthKeyManager {
    companion object {
        private const val STORE = "AndroidKeyStore"
        private const val ALIAS = "omega7.auth.pepper.v1"
    }

    fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(STORE).apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, STORE)
        generator.init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY).build())
        return generator.generateKey()
    }

    fun destroyKey() {
        val ks = KeyStore.getInstance(STORE).apply { load(null) }
        if (ks.containsAlias(ALIAS)) ks.deleteEntry(ALIAS)
    }
}
