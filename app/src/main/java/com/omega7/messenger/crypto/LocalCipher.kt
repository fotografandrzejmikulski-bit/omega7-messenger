package com.omega7.messenger.crypto

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

/** AES-256-GCM envelope. The authenticated header prevents silent format confusion. */
class LocalCipher(private val keys: LocalKeyManager = LocalKeyManager()) {
    companion object {
        private val MAGIC = byteArrayOf('O'.code.toByte(), '7'.code.toByte(), 'E'.code.toByte(), '1'.code.toByte())
        private const val VERSION: Byte = 1
        private const val IV_LENGTH = 12
        private const val TAG_BITS = 128
        private const val MAX_PLAINTEXT = 8 * 1024 * 1024
    }

    private val random = SecureRandom()

    fun encrypt(plain: ByteArray): ByteArray {
        require(plain.size <= MAX_PLAINTEXT) { "Dane lokalne są zbyt duże." }
        val iv = ByteArray(IV_LENGTH).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keys.getOrCreateKey(), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(header())
        val body = cipher.doFinal(plain)
        return ByteBuffer.allocate(4 + 1 + 1 + IV_LENGTH + body.size)
            .put(MAGIC).put(VERSION).put(IV_LENGTH.toByte()).put(iv).put(body).array()
    }

    fun decrypt(blob: ByteArray): ByteArray {
        require(blob.size >= 4 + 1 + 1 + IV_LENGTH + 16) { "Uszkodzony zaszyfrowany magazyn." }
        val b = ByteBuffer.wrap(blob)
        val magic = ByteArray(4).also(b::get)
        require(magic.contentEquals(MAGIC)) { "Nieznany format magazynu." }
        require(b.get() == VERSION) { "Nieobsługiwana wersja magazynu." }
        require(b.get().toInt() == IV_LENGTH) { "Nieprawidłowy IV." }
        val iv = ByteArray(IV_LENGTH).also(b::get)
        val body = ByteArray(b.remaining()).also(b::get)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keys.getOrCreateKey(), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(header())
        return try { cipher.doFinal(body) } catch (e: AEADBadTagException) {
            throw SecurityException("Integralność magazynu została naruszona.", e)
        }
    }

    private fun header() = MAGIC + byteArrayOf(VERSION, IV_LENGTH.toByte())
}
