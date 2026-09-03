package com.omega7.messenger.network

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** HTTPS-only relay client. The relay receives ciphertext and routing metadata, never plaintext. */
class RelayHttpTransport(
    private val baseUrl: String,
    private val authToken: String,
    private val deviceId: Int,
    private val groupId: String = "omega7-main",
) : AuthenticatedTransport, AutoCloseable {
    private val executor = Executors.newCachedThreadPool()

    init {
        require(baseUrl.startsWith("https://"))
        require(deviceId in 1..127)
        require(authToken.isNotBlank())
        require(groupId.isNotBlank() && groupId.length <= 128)
    }

    override suspend fun send(groupId: String, encryptedPayload: ByteArray, idempotencyKey: String): Result<Unit> = suspendCoroutine { c ->
        executor.execute {
            runCatching {
                require(groupId == this.groupId) { "Nieprawidłowa grupa transportu." }
                require(encryptedPayload.size <= 384 * 1024) { "Pakiet jest zbyt duży." }
                val root = JSONObject(String(encryptedPayload, Charsets.UTF_8))
                require(root.getInt("version") == 1)
                require(root.getInt("senderDeviceId") == deviceId)
                val r = root.getJSONArray("recipients")
                require(r.length() in 1..6)
                for (i in 0 until r.length()) {
                    val item = r.getJSONObject(i)
                    val recipient = item.getInt("deviceId")
                    require(recipient in 1..127 && recipient != deviceId)
                    val ciphertext = item.getString("ciphertext")
                    require(ciphertext.isNotBlank() && ciphertext.length <= 360_000)
                    postJson("/v1/messages", JSONObject()
                        .put("groupId", groupId)
                        .put("senderDeviceId", deviceId)
                        .put("recipientDeviceId", recipient)
                        .put("idempotencyKey", "$idempotencyKey:$recipient")
                        .put("ciphertext", ciphertext))
                }
            }.fold({ c.resume(Result.success(Unit)) }, { c.resume(Result.failure(it)) })
        }
    }

    override suspend fun sync(cursor: String?): Result<SyncBatch> = suspendCoroutine { c ->
        executor.execute {
            runCatching {
                val safeCursor = cursor?.takeIf { it.matches(Regex("[0-9]{1,20}")) } ?: "0"
                val json = request("GET", "/v1/sync?groupId=${encode(groupId)}&deviceId=$deviceId&cursor=$safeCursor", null)
                val root = JSONObject(json)
                val a = root.getJSONArray("messages")
                require(a.length() <= 100)
                val events = (0 until a.length()).map { i ->
                    val x = a.getJSONObject(i)
                    JSONObject().put("senderDeviceId", x.getInt("senderDeviceId"))
                        .put("idempotencyKey", x.getString("idempotencyKey"))
                        .put("ciphertext", x.getString("ciphertext"))
                        .toString().toByteArray(Charsets.UTF_8)
                }
                SyncBatch(root.optString("cursor", null), events)
            }.fold({ c.resume(Result.success(it)) }, { c.resume(Result.failure(it)) })
        }
    }

    private fun postJson(path: String, body: JSONObject) { request("POST", path, body.toString()) }

    private fun request(method: String, path: String, body: String?): String {
        val u = URL(baseUrl.trimEnd('/') + path)
        require(u.protocol == "https")
        val x = (u.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 20_000
            useCaches = false
            doInput = true
            setRequestProperty("Authorization", "Bearer $authToken")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-store")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        return try {
            if (body != null) x.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = x.responseCode
            val stream = if (status in 200..299) x.inputStream else x.errorStream
            val response = stream?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
            if (status !in 200..299) throw IllegalStateException("Relay HTTP $status: ${response.take(256)}")
            response
        } finally { x.disconnect() }
    }

    private fun encode(v: String) = java.net.URLEncoder.encode(v, Charsets.UTF_8.name())
    override fun close() { executor.shutdownNow() }
}
