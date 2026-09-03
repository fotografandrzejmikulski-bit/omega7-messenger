package com.omega7.messenger.data

import android.content.Context
import com.omega7.messenger.domain.Message
import com.omega7.messenger.domain.MessageRepository
import org.json.JSONArray
import org.json.JSONObject

/** Thread-safe encrypted local message repository. */
class LocalMessageRepository(context: Context) : MessageRepository {
    private val store = EncryptedLocalStore(context)
    private val lock = Any()
    private val messages = mutableListOf<Message>()

    init { load() }

    override fun list(): List<Message> = synchronized(lock) { messages.toList() }

    override fun append(message: Message) {
        synchronized(lock) {
            require(messages.none { it.id == message.id }) { "Duplikat wiadomości." }
            messages += message
            persistLocked()
        }
    }

    override fun clear() {
        synchronized(lock) {
            messages.clear()
            store.delete()
        }
    }

    private fun persistLocked() {
        val array = JSONArray()
        messages.forEach {
            array.put(JSONObject().apply {
                put("id", it.id)
                put("sender", it.sender)
                put("body", it.body)
                put("time", it.timestampMillis)
                put("status", it.status.name)
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
                    messages += Message(
                        o.getString("id"), o.getString("sender"), o.getString("body"),
                        o.getLong("time"), Message.Status.valueOf(o.getString("status"))
                    )
                }
            }.onFailure { messages.clear() }
        }
        bytes.fill(0)
    }
}
