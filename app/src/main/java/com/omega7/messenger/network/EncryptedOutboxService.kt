package com.omega7.messenger.network

import com.omega7.messenger.data.OutboundQueueRepository
import com.omega7.messenger.domain.E2eeEngine
import com.omega7.messenger.domain.OutboundMessage
import java.util.UUID

/**
 * Encrypts exactly once, persists the resulting ciphertext, then delivers it.
 * Retries reuse the persisted ciphertext and idempotency key; the ratchet is
 * never advanced merely because a network attempt failed.
 */
class EncryptedOutboxService(
    private val e2ee: E2eeEngine,
    private val transport: AuthenticatedTransport,
    private val queue: OutboundQueueRepository,
) {
    suspend fun submit(
        messageId: String = UUID.randomUUID().toString(),
        groupId: String,
        senderDeviceId: String,
        plaintext: ByteArray,
        idempotencyKey: String = messageId,
    ): Result<Unit> {
        if (e2ee is com.omega7.messenger.domain.E2eeNotConfigured) {
            return Result.failure(IllegalStateException("E2EE nie jest skonfigurowane — wysyłanie zablokowane."))
        }
        return runCatching {
            val ciphertext = e2ee.encrypt(groupId, plaintext)
            require(ciphertext.isNotEmpty()) { "E2EE zwróciło pusty szyfrogram." }
            require(ciphertext.size <= OutboundMessage.MAX_CIPHERTEXT) { "Szyfrogram jest zbyt duży." }
            val message = OutboundMessage(messageId, groupId, senderDeviceId, ciphertext, System.currentTimeMillis(), idempotencyKey)
            queue.enqueue(message)
            drain(message.messageId).getOrThrow()
        }
    }

    suspend fun drainDue(nowMillis: Long = System.currentTimeMillis()): Int {
        var sent = 0
        queue.due(nowMillis).forEach { entry ->
            if (drain(entry.message.messageId).isSuccess) sent++
        }
        return sent
    }

    suspend fun drain(messageId: String): Result<Unit> {
        val entry = queue.list().firstOrNull { it.message.messageId == messageId }
            ?: return Result.failure(IllegalArgumentException("Wiadomości nie ma w kolejce."))
        return transport.send(entry.message.groupId, entry.message.ciphertext, entry.message.idempotencyKey)
            .onSuccess { queue.markSent(messageId) }
            .onFailure { queue.markRetry(messageId) }
    }
}
