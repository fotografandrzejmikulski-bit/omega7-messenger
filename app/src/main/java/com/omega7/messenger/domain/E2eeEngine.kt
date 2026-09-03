package com.omega7.messenger.domain

/**
 * Granica dla audytowanej implementacji E2EE.
 * Ω7 nie implementuje własnego protokołu kryptograficznego.
 */
interface E2eeEngine {
    fun encrypt(groupId: String, plaintext: ByteArray): ByteArray
    fun decrypt(groupId: String, ciphertext: ByteArray): ByteArray
}

class E2eeNotConfigured : E2eeEngine {
    override fun encrypt(groupId: String, plaintext: ByteArray): ByteArray =
        error("Warstwa E2EE nie została jeszcze podłączona do audytowanej implementacji.")

    override fun decrypt(groupId: String, ciphertext: ByteArray): ByteArray =
        error("Warstwa E2EE nie została jeszcze podłączona do audytowanej implementacji.")
}
