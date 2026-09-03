package com.omega7.messenger.security

import android.content.Context
import java.util.UUID

/** Random installation identifier. It is an identifier, not a secret or an authentication token. */
class DeviceIdentity(context: Context) {
    private val prefs = context.getSharedPreferences("omega7_identity", Context.MODE_PRIVATE)
    val deviceId: String
        get() = prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("device_id", it).commit()
        }

    fun destroy() { prefs.edit().clear().commit() }
}
