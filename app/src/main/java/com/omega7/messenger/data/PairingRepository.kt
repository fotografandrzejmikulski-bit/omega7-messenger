package com.omega7.messenger.data

import android.content.Context
import com.omega7.messenger.pairing.PairingInvite
import org.json.JSONArray
import org.json.JSONObject

/** Local encrypted queue of invitations awaiting provisioning. */
class PairingRepository(context: Context) {
    private val store = EncryptedLocalStore(context, fileName = "omega7_pairing.bin")

    @Synchronized fun pending(): List<PairingInvite> = runCatching {
        val bytes = store.load() ?: return@runCatching emptyList()
        try {
            val array = JSONArray(bytes.toString(Charsets.UTF_8))
            buildList(array.length()) { for (i in 0 until array.length()) add(fromJson(array.getJSONObject(i))) }
        } finally { bytes.fill(0) }
    }.getOrDefault(emptyList())

    @Synchronized fun add(invite: PairingInvite) {
        val all = (pending().filterNot { it.inviteId == invite.inviteId } + invite).takeLast(6)
        save(all)
    }

    @Synchronized fun remove(inviteId: String) {
        save(pending().filterNot { it.inviteId == inviteId })
    }

    @Synchronized fun clear() { store.delete() }

    private fun save(all: List<PairingInvite>) {
        val json = JSONArray().apply { all.forEach { put(toJson(it)) } }.toString()
        store.save(json.toByteArray(Charsets.UTF_8))
    }

    private fun toJson(i: PairingInvite) = JSONObject().apply {
        put("v", 4)
        put("g", i.groupId)
        put("i", i.inviteId)
        put("e", i.expiresAtMillis)
        put("o", i.ownerDeviceId)
        put("n", i.ownerName)
        put("k", i.ownerPublicKey)
        put("s", i.signature)
        i.inviteToken?.let { put("t", it) }
        i.relayBaseUrl?.let { put("r", it) }
        i.ownerSignalDeviceId?.let { put("sd", it) }
        i.ownerSignalBundle?.let { put("sb", it) }
    }

    private fun fromJson(o: JSONObject): PairingInvite = PairingInvite(
        o.getString("g"), o.getString("i"), o.getLong("e"), o.getString("o"),
        o.getString("n"), o.getString("k"), o.getString("s"),
        if (o.has("t") && !o.isNull("t")) o.getString("t") else null,
        if (o.has("r") && !o.isNull("r")) o.getString("r") else null,
        if (o.has("sd") && !o.isNull("sd")) o.getInt("sd") else null,
        if (o.has("sb") && !o.isNull("sb")) o.getString("sb") else null,
    )
}
