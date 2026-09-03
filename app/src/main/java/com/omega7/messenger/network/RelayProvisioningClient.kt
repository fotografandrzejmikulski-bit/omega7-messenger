package com.omega7.messenger.network

import com.omega7.messenger.e2ee.SignalE2eeEngine
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** Control-plane client for first registration, invites, revocation and prekey publication. */
class RelayProvisioningClient(
    private val baseUrl: String,
    private val authToken: String? = null,
) : AutoCloseable {
    private val executor = Executors.newCachedThreadPool()

    init { require(baseUrl.startsWith("https://")) { "Relay musi używać HTTPS." } }

    suspend fun register(
        groupId: String,
        deviceId: Int,
        inviteToken: String,
        bundle: SignalE2eeEngine.DeviceBundle,
        preKeys: List<RelayKeyClient.PreKey>,
    ): Result<String> = async {
        require(deviceId == bundle.deviceId && deviceId in 1..127)
        require(preKeys.size <= 64)
        val keys = JSONArray()
        preKeys.forEach { keys.put(JSONObject().put("id", it.id).put("key", b64(it.key))) }
        val body = JSONObject()
            .put("groupId", groupId)
            .put("deviceId", deviceId)
            .put("inviteToken", inviteToken)
            .put("registrationId", bundle.registrationId)
            .put("identityKey", b64(bundle.identityKey))
            .put("signedPreKeyId", bundle.signedPreKeyId)
            .put("signedPreKey", b64(bundle.signedPreKey))
            .put("signedPreKeySignature", b64(bundle.signedPreKeySignature))
            .put("kyberPreKeyId", bundle.kyberPreKeyId)
            .put("kyberPreKey", b64(bundle.kyberPreKey))
            .put("kyberPreKeySignature", b64(bundle.kyberPreKeySignature))
            .put("preKeys", keys)
        request("POST", "/v1/devices/register", body.toString(), false)
            .let { JSONObject(it).getString("authToken") }
    }

    suspend fun createInvite(groupId: String, ownerDeviceId: Int): Result<Pair<String, String>> = async {
        require(!authToken.isNullOrBlank()) { "Brak tokenu właściciela relay." }
        val response = request(
            "POST", "/v1/pair/invites",
            JSONObject().put("groupId", groupId).put("ownerDeviceId", ownerDeviceId).toString(), true,
        )
        val json = JSONObject(response)
        Pair(json.getString("inviteId"), json.getString("inviteToken"))
    }

    suspend fun revoke(groupId: String, ownerDeviceId: Int, targetDeviceId: Int): Result<Unit> = async {
        require(!authToken.isNullOrBlank()) { "Brak tokenu właściciela relay." }
        request(
            "POST", "/v1/devices/revoke",
            JSONObject().put("groupId", groupId).put("ownerDeviceId", ownerDeviceId).put("targetDeviceId", targetDeviceId).toString(), true,
        )
        Unit
    }

    suspend fun uploadPreKeys(groupId: String, deviceId: Int, preKeys: List<RelayKeyClient.PreKey>): Result<Unit> = async {
        require(!authToken.isNullOrBlank()) { "Brak tokenu relay." }
        require(preKeys.isNotEmpty() && preKeys.size <= 64)
        val array = JSONArray()
        preKeys.forEach { array.put(JSONObject().put("id", it.id).put("key", b64(it.key))) }
        request("POST", "/v1/keys/prekeys", JSONObject().put("groupId", groupId).put("deviceId", deviceId).put("preKeys", array).toString(), true)
        Unit
    }

    private suspend fun <T> async(block: () -> T): Result<T> = suspendCoroutine { continuation ->
        executor.execute { runCatching { block() }.fold({ continuation.resume(Result.success(it)) }, { continuation.resume(Result.failure(it)) }) }
    }

    private fun request(method: String, path: String, body: String?, authenticated: Boolean): String {
        val connection = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            require(url.protocol == "https") { "Niedozwolony transport bez TLS." }
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 20_000
            useCaches = false
            doInput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-store")
            if (authenticated) setRequestProperty("Authorization", "Bearer ${requireNotNull(authToken)}")
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
        } finally { connection.disconnect() }
    }

    private fun b64(value: ByteArray): String = android.util.Base64.encodeToString(value, android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING)
    override fun close() { executor.shutdownNow() }
}
