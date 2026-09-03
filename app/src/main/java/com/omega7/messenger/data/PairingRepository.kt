package com.omega7.messenger.data

import android.content.Context
import com.omega7.messenger.pairing.PairingInvite
import org.json.JSONArray
import org.json.JSONObject

/** Local queue of invitations awaiting server-side acceptance/approval. */
class PairingRepository(context: Context) {
    private val store = EncryptedLocalStore(context, fileName = "omega7_pairing.bin")

    @Synchronized fun pending(): List<PairingInvite> = runCatching {
        val array = JSONArray(store.load()?.toString(Charsets.UTF_8) ?: "[]")
        buildList(array.length()) { for (i in 0 until array.length()) add(fromJson(array.getJSONObject(i))) }
    }.getOrDefault(emptyList())

    @Synchronized fun add(invite: PairingInvite) {
        val all = (pending().filterNot { it.inviteId == invite.inviteId } + invite).takeLast(6)
        save(all)
    }

    @Synchronized fun remove(inviteId: String) {
        val all = pending().filterNot { it.inviteId == inviteId }
        save(all)
    }

    @Synchronized fun clear() { store.delete() }

    private fun save(all: List<PairingInvite>) {
        val json = JSONArray().apply { all.forEach { put(toJson(it)) } }.toString()
        store.save(json.toByteArray(Charsets.UTF_8))
    }

    private fun toJson(i: PairingInvite) = JSONObject().apply {
        put("g", i.groupId); put("i", i.inviteId); put("e", i.expiresAtMillis); put("o", i.ownerDeviceId)
        put("n", i.ownerName); put("k", i.ownerPublicKey); put("s", i.signature)
    }
    private fun fromJson(o: JSONObject) = PairingInvite(o.getString("g"), o.getString("i"), o.getLong("e"), o.getString("o"), o.getString("n"), o.getString("k"), o.getString("s"))
}
