package com.omega7.messenger.domain

/** Metadata passed to the transport after an external E2EE provider produces ciphertext. */
data class OutboundMessage(
    val messageId: String,
    val groupId: String,
    val senderDeviceId: String,
    val ciphertext: ByteArray,
    val createdAtMillis: Long,
    val idempotencyKey: String
) {
    init {
        require(messageId.isNotBlank() && messageId.length <= 128)
        require(groupId.isNotBlank() && groupId.length <= 128)
        require(senderDeviceId.isNotBlank() && senderDeviceId.length <= 128)
        require(ciphertext.isNotEmpty() && ciphertext.size <= MAX_CIPHERTEXT)
        require(createdAtMillis > 0)
        require(idempotencyKey.isNotBlank() && idempotencyKey.length <= 128)
    }

    companion object { const val MAX_CIPHERTEXT = 256 * 1024 }
}
