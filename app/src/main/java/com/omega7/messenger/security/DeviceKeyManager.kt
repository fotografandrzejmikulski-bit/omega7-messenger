package com.omega7.messenger.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.util.Base64

/** Non-exportable device signing identity used to authenticate pairing invitations. */
class DeviceKeyManager {
    companion object {
        private const val STORE = "AndroidKeyStore"
        private const val ALIAS = "omega7.device.signing.v1"
    }

    private fun keyPair(): KeyPair {
        val ks = KeyStore.getInstance(STORE).apply { load(null) }
        val existing = ks.getEntry(ALIAS, null) as? KeyStore.PrivateKeyEntry
        if (existing != null) return KeyPair(existing.certificate.publicKey, existing.privateKey)
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, STORE)
        generator.initialize(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setKeySize(2048)
                .build()
        )
        return generator.generateKeyPair()
    }

    fun publicKeyDerBase64Url(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(keyPair().public.encoded)

    fun sign(data: ByteArray): ByteArray {
        val privateKey: PrivateKey = keyPair().private
        return Signature.getInstance("SHA256withRSA").run {
            initSign(privateKey)
            update(data)
            sign()
        }
    }

    fun destroy() {
        val ks = KeyStore.getInstance(STORE).apply { load(null) }
        if (ks.containsAlias(ALIAS)) ks.deleteEntry(ALIAS)
    }
}
