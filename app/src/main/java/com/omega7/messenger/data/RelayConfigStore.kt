package com.omega7.messenger.data

import android.content.Context
import org.json.JSONObject

/** Encrypted local storage for the relay endpoint and per-device bearer token. */
class RelayConfigStore(private val context: Context) {
    data class Config(val baseUrl: String, val authToken: String)

    fun save(config: Config) {
        require(config.baseUrl.startsWith("https://")) { "Relay musi używać HTTPS." }
        require(config.authToken.isNotBlank()) { "Brak tokenu relay." }
        val json = JSONObject().put("baseUrl", config.baseUrl.trimEnd('/')).put("authToken", config.authToken)
        EncryptedLocalStore(context, fileName = FILE).save(json.toString().toByteArray(Charsets.UTF_8))
    }

    fun load(): Config? {
        val bytes = EncryptedLocalStore(context, fileName = FILE).load() ?: return null
        return try {
            val json = JSONObject(String(bytes, Charsets.UTF_8))
            val baseUrl = json.getString("baseUrl")
            val token = json.getString("authToken")
            if (!baseUrl.startsWith("https://") || token.isBlank()) null else Config(baseUrl.trimEnd('/'), token)
        } finally { bytes.fill(0) }
    }

    fun clear() { EncryptedLocalStore(context, fileName = FILE).delete() }

    companion object { private const val FILE = "omega7_relay_config.bin" }
}
