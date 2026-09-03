package com.omega7.messenger.pairing

import android.util.Base64
import org.json.JSONObject
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/** Device-side response to an invitation. It is signed by the joining device identity. */
data class PairingRequest(
    val groupId: String,
    val inviteId: String,
    val deviceId: String,
    val displayName: String,
    val devicePublicKey: String,
    val signature: String
) {
    companion object {
        private const val VERSION = 1

        private fun canonical(r: PairingRequest): String =
            listOf(VERSION, r.groupId, r.inviteId, r.deviceId, r.displayName, r.devicePublicKey).joinToString("|")

        fun create(groupId: String, inviteId: String, deviceId: String, displayName: String, publicKey: String, sign: (ByteArray) -> ByteArray): PairingRequest {
            val unsigned = PairingRequest(groupId, inviteId, deviceId, displayName.take(80), publicKey, "")
            val sig = Base64.encodeToString(sign(canonical(unsigned).toByteArray(Charsets.UTF_8)), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            return unsigned.copy(signature = sig)
        }

        fun encode(request: PairingRequest): String = Base64.encodeToString(JSONObject().apply {
            put("v", VERSION); put("g", request.groupId); put("i", request.inviteId); put("d", request.deviceId)
            put("n", request.displayName); put("k", request.devicePublicKey); put("s", request.signature)
        }.toString().toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

        fun parse(encoded: String): PairingRequest {
            val o = JSONObject(String(Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING), Charsets.UTF_8))
            require(o.getInt("v") == VERSION) { "Nieobsługiwana wersja żądania." }
            return PairingRequest(o.getString("g"), o.getString("i"), o.getString("d"), o.getString("n"), o.getString("k"), o.getString("s"))
                .also { require(it.deviceId.length in 8..128 && it.displayName.length <= 80) { "Nieprawidłowe żądanie urządzenia." } }
        }

        fun verify(request: PairingRequest): Boolean {
            val key: PublicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(Base64.getUrlDecoder().decode(request.devicePublicKey)))
            return Signature.getInstance("SHA256withRSA").run {
                initVerify(key)
                update(canonical(request.copy(signature = "")).toByteArray(Charsets.UTF_8))
                verify(Base64.getUrlDecoder().decode(request.signature))
            }
        }
    }
}
