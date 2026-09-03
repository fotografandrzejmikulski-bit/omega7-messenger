package com.omega7.messenger.network

import com.omega7.messenger.domain.E2eeEngine
import com.omega7.messenger.domain.E2eeNotConfigured
import com.omega7.messenger.domain.OutboundMessage

/**
 * Fail-closed network boundary. No transport call is possible until an audited
 * E2EE implementation is explicitly injected.
 */
class SecureSendGate(
    private val e2ee: E2eeEngine,
    private val transport: AuthenticatedTransport
) {
    suspend fun send(message: OutboundMessage, plaintext: ByteArray): Result<Unit> {
        if (e2ee is E2eeNotConfigured) {
            return Result.failure(IllegalStateException("E2EE nie jest skonfigurowane — wysyłanie zablokowane."))
        }
        return runCatching {
            val ciphertext = e2ee.encrypt(message.groupId, plaintext)
            require(ciphertext.isNotEmpty()) { "E2EE zwróciło pusty szyfrogram." }
            require(ciphertext.size <= OutboundMessage.MAX_CIPHERTEXT) { "Szyfrogram jest zbyt duży." }
            transport.send(message.groupId, ciphertext, message.idempotencyKey).getOrThrow()
        }
    }
}
