package com.omega7.messenger.network

import com.omega7.messenger.e2ee.SignalE2eeEngine
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Authenticated Signal bundle discovery and membership directory client.
 * Directory discovery never consumes one-time prekeys; bundle retrieval may consume one.
 */
class RelayKeyClient(
    private val baseUrl: String,
    private val authToken: String,
    private val deviceId: Int,
) : AutoCloseable {
    private val executor = Executors.newCachedThreadPool()

    init {
        require(baseUrl.startsWith("https://")) { "Relay musi używać HTTPS." }
        require(authToken.isNotBlank()) { "Brak tokenu relay." }
        require(deviceId in 1..127) { "Nieprawidłowy DeviceID." }
    }

    data class DeviceDirectoryEntry(
        val deviceId: Int,
        val identityKeyBase64: String,
        val createdAt: String,
        val updatedAt: String,
    ) {
        init {
            require(deviceId in 1..127)
            require(identityKeyBase64.isNotBlank())
        }
    }

    suspend fun listDevices(groupId: String): Result<List<DeviceDirectoryEntry>> = async {
        require(groupId.isNotBlank() && groupId.length <= 128)
        val response = request(
            "GET",
            "/v1/devices?groupId=${encode(groupId)}&requesterDeviceId=$deviceId",
            null,
            authenticated = true,
        )
        val root = JSONObject(response)
        require(root.getString("groupId") == groupId)
        require(root.getInt("requesterDeviceId") == deviceId)
        val array = root.getJSONArray("devices")
        require(array.length() in 1..7)
        buildList(array.length()) {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(DeviceDirectoryEntry(
                    deviceId = item.getInt("deviceId"),
                    identityKeyBase64 = item.getString("identityKey"),
                    createdAt = item.getString("createdAt"),
                    updatedAt = item.getString("updatedAt"),
                ))
            }
        }.also {
            require(it.map { d -> d.deviceId }.distinct().size == it.size) { "Relay zwrócił duplikaty urządzeń." }
        }
    }

    suspend fun fetchBundle(groupId: String, remoteDeviceId: Int): Result<SignalE2eeEngine.DeviceBundle> =
        async {
            require(groupId.isNotBlank() && groupId.length <= 128)
            require(remoteDeviceId in 1..127 && remoteDeviceId != deviceId)
            val response = request(
                "GET",
                "/v1/keys/${encode(groupId)}/$remoteDeviceId?requesterDeviceId=$deviceId",
                null,
                authenticated = true,
            )
            SignalE2eeEngine.DeviceBundle.fromJson(response)
        }

    suspend fun uploadPreKeys(groupId: String, preKeys: List<PreKey>): Result<Unit> =
        async {
            require(preKeys.isNotEmpty() && preKeys.size <= 64) { "Nieprawidłowa liczba prekeys." }
            val array = JSONArray()
            preKeys.forEach {
                require(it.id > 0 && it.key.isNotEmpty() && it.key.size <= 4096) { "Nieprawidłowy prekey." }
                array.put(JSONObject().put("id", it.id).put("key", b64(it.key)))
            }
            request(
                "POST",
                "/v1/keys/prekeys",
                JSONObject().put("groupId", groupId).put("deviceId", deviceId).put("preKeys", array).toString(),
                authenticated = true,
            )
        }.map { Unit }

    data class PreKey(val id: Int, val key: ByteArray)

    private suspend fun <T> async(block: () -> T): Result<T> = suspendCoroutine { continuation ->
        executor.execute {
            runCatching { block() }.fold(
                { continuation.resume(Result.success(it)) },
                { continuation.resume(Result.failure(it)) },
            )
        }
    }

    private fun request(method: String, path: String, body: String?, authenticated: Boolean): String {
        val url = URL(baseUrl.trimEnd('/') + path)
        require(url.protocol == "https") { "Niedozwolony transport bez TLS." }
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 20_000
            useCaches = false
            doInput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-store")
            if (authenticated) setRequestProperty("Authorization", "Bearer $authToken")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        return try {
            if (body != null) connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
            if (status !in 200..299) throw IllegalStateException("Relay HTTP $status: ${response.take(256)}")
            response
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String = java.net.URLEncoder.encode(value, Charsets.UTF_8.name())
    private fun b64(value: ByteArray): String = android.util.Base64.encodeToString(value, android.util.Base64.NO_WRAP)

    override fun close() { executor.shutdownNow() }
}
