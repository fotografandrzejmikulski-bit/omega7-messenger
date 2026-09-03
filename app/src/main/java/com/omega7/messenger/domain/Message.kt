package com.omega7.messenger.domain

/** Immutable domain object. Validation prevents malformed local state from entering the store. */
data class Message(
    val id: String,
    val sender: String,
    val body: String,
    val timestampMillis: Long,
    val status: Status = Status.QUEUED
) {
    init {
        require(id.isNotBlank() && id.length <= 128) { "Nieprawidłowe ID wiadomości." }
        require(sender.isNotBlank() && sender.length <= 128) { "Nieprawidłowy nadawca." }
        require(body.isNotBlank() && body.length <= MAX_BODY_LENGTH) { "Nieprawidłowa treść wiadomości." }
        require(timestampMillis > 0) { "Nieprawidłowy czas wiadomości." }
    }

    enum class Status { LOCAL_ONLY, QUEUED, SENT, DELIVERED, READ, FAILED }

    companion object {
        const val MAX_BODY_LENGTH = 16_384
    }
}
