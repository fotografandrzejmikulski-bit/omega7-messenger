package com.omega7.messenger.data

import android.content.Context
import com.omega7.messenger.crypto.LocalCipher
import com.omega7.messenger.security.DeviceTrust
import org.json.JSONArray
import org.json.JSONObject

/** Trust registry is encrypted at rest and is destroyed together with the local key. */
class DeviceTrustRepository(context: Context) {
    private val store = EncryptedLocalStore(context, LocalCipher())
    private val lock = Any()

    fun list(): List<DeviceTrust.TrustedDevice> = synchronized(lock) {
        val bytes = store.load() ?: return@synchronized emptyList()
        try {
            val array = JSONArray(String(bytes, Charsets.UTF_8))
            buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(DeviceTrust.TrustedDevice(
                        o.getString("id"), o.getString("name"), o.getString("fingerprint"),
                        DeviceTrust.State.valueOf(o.getString("state"))
                    ))
                }
            }
        } catch (_: Exception) { emptyList() } finally { bytes.fill(0) }
    }

    fun put(device: DeviceTrust.TrustedDevice) = synchronized(lock) {
        val all = list().filterNot { it.deviceId == device.deviceId } + device
        val array = JSONArray()
        all.forEach { d -> array.put(JSONObject().apply {
            put("id", d.deviceId); put("name", d.displayName)
            put("fingerprint", d.fingerprint); put("state", d.state.name)
        }) }
        val bytes = array.toString().toByteArray(Charsets.UTF_8)
        try { store.save(bytes) } finally { bytes.fill(0) }
    }

    fun revoke(deviceId: String) { list().find { it.deviceId == deviceId }?.let { put(it.copy(state = DeviceTrust.State.REVOKED)) } }
    fun clear() = synchronized(lock) { store.delete() }
}
