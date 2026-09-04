package com.omega7.messenger.data

import android.content.Context
import com.omega7.messenger.domain.OutboundMessage
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

/**
 * Durable encrypted outbox. Ciphertext is persisted after E2EE encryption so a
 * network retry never advances the Double Ratchet a second time for the same
 * logical message.
 */
class OutboundQueueRepository(context: Context) {
    data class Entry(
        val message: OutboundMessage,
        val attempts: Int,
        val nextAttemptAtMillis: Long,
    )

    private val store = EncryptedLocalStore(context)
    private val lock = Any()
    private val entries = linkedMapOf<String, Entry>()

    init { load() }

    fun list(): List<Entry> = synchronized(lock) { entries.values.toList() }

    fun enqueue(message: OutboundMessage) = synchronized(lock) {
        require(entries[message.messageId] == null) { "Duplikat wiadomości w kolejce." }
        entries[message.messageId] = Entry(message, attempts = 0, nextAttemptAtMillis = 0L)
        persistLocked()
    }

    fun due(nowMillis: Long = System.currentTimeMillis()): List<Entry> = synchronized(lock) {
        entries.values.filter { it.nextAttemptAtMillis <= nowMillis }
    }

    fun markSent(messageId: String) = synchronized(lock) {
        entries.remove(messageId)
        persistLocked()
    }

    fun markRetry(messageId: String, nowMillis: Long = System.currentTimeMillis()): Entry? = synchronized(lock) {
        val current = entries[messageId] ?: return@synchronized null
        val attempts = current.attempts + 1
        val delay = RETRY_DELAYS_MILLIS[minOf(attempts - 1, RETRY_DELAYS_MILLIS.lastIndex)]
        val updated = current.copy(attempts = attempts, nextAttemptAtMillis = nowMillis + delay)
        entries[messageId] = updated
        persistLocked()
        updated
    }

    fun clear() = synchronized(lock) {
        entries.clear()
        store.delete()
    }

    private fun persistLocked() {
        val array = JSONArray()
        entries.values.forEach { e ->
            array.put(JSONObject().apply {
                put("messageId", e.message.messageId)
                put("groupId", e.message.groupId)
                put("senderDeviceId", e.message.senderDeviceId)
                put("ciphertext", Base64.getEncoder().encodeToString(e.message.ciphertext))
                put("createdAtMillis", e.message.createdAtMillis)
                put("idempotencyKey", e.message.idempotencyKey)
                put("attempts", e.attempts)
                put("nextAttemptAtMillis", e.nextAttemptAtMillis)
            })
        }
        store.save(array.toString().toByteArray(Charsets.UTF_8))
    }

    private fun load() {
        val bytes = store.load() ?: return
        synchronized(lock) {
            runCatching {
                val array = JSONArray(String(bytes, Charsets.UTF_8))
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    val cipher = Base64.getDecoder().decode(o.getString("ciphertext"))
                    val message = OutboundMessage(
                        o.getString("messageId"), o.getString("groupId"), o.getString("senderDeviceId"),
                        cipher, o.getLong("createdAtMillis"), o.getString("idempotencyKey")
                    )
                    entries[message.messageId] = Entry(message, o.getInt("attempts"), o.getLong("nextAttemptAtMillis"))
                }
            }.onFailure { entries.clear() }
        }
        bytes.fill(0)
    }

    companion object {
        const val MAX_RETRY_ATTEMPTS = 6
        private val RETRY_DELAYS_MILLIS = longArrayOf(2_000L, 5_000L, 15_000L, 30_000L, 60_000L, 120_000L)
    }
}
