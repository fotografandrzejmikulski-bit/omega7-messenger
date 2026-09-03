package com.omega7.messenger.pairing

import android.util.Base64
import com.omega7.messenger.e2ee.SignalE2eeEngine
import org.json.JSONObject
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/** Device-side response to an invitation. It is signed by the joining device identity. */
data class PairingRequest(
    val groupId: String,
    val inviteId: String,
    /** Legacy local installation identity. Kept only for QR compatibility; relay identity is signalDeviceId. */
    val deviceId: String,
    val displayName: String,
    val devicePublicKey: String,
    val signature: String,
    /** Numeric libsignal/relay DeviceID (1..127). */
    val signalDeviceId: Int? = null,
    /** Complete public Signal bundle, serialized as JSON. Contains no private key material. */
    val signalBundle: String? = null,
) {
    companion object {
        private const val VERSION = 2

        private fun canonical(r: PairingRequest): String =
            listOf(
                VERSION, r.groupId, r.inviteId, r.deviceId, r.displayName, r.devicePublicKey,
                r.signalDeviceId ?: "-", r.signalBundle ?: "-",
            ).joinToString("|")

        fun create(
            groupId: String,
            inviteId: String,
            deviceId: String,
            displayName: String,
            publicKey: String,
            sign: (ByteArray) -> ByteArray,
        ): PairingRequest = create(groupId, inviteId, deviceId, displayName, publicKey, null, null, sign)

        fun create(
            groupId: String,
            inviteId: String,
            deviceId: String,
            displayName: String,
            publicKey: String,
            signalDeviceId: Int,
            signalBundle: SignalE2eeEngine.DeviceBundle,
            sign: (ByteArray) -> ByteArray,
        ): PairingRequest = create(groupId, inviteId, deviceId, displayName, publicKey, signalDeviceId, signalBundle.toJson(), sign)

        private fun create(
            groupId: String,
            inviteId: String,
            deviceId: String,
            displayName: String,
            publicKey: String,
            signalDeviceId: Int?,
            signalBundle: String?,
            sign: (ByteArray) -> ByteArray,
        ): PairingRequest {
            require(signalDeviceId == null || signalDeviceId in 1..127) { "Nieprawidłowy Signal DeviceID." }
            if (signalDeviceId != null) require(!signalBundle.isNullOrBlank()) { "Brak publicznego bundla Signal." }
            val unsigned = PairingRequest(groupId, inviteId, deviceId, displayName.take(80), publicKey, "", signalDeviceId, signalBundle)
            val sig = Base64.encodeToString(sign(canonical(unsigned).toByteArray(Charsets.UTF_8)), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            return unsigned.copy(signature = sig)
        }

        fun encode(request: PairingRequest): String = Base64.encodeToString(JSONObject().apply {
            put("v", VERSION); put("g", request.groupId); put("i", request.inviteId); put("d", request.deviceId)
            put("n", request.displayName); put("k", request.devicePublicKey); put("s", request.signature)
            request.signalDeviceId?.let { put("sd", it) }
            request.signalBundle?.let { put("sb", it) }
        }.toString().toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

        fun parse(encoded: String): PairingRequest {
            val o = JSONObject(String(Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING), Charsets.UTF_8))
            require(o.getInt("v") == VERSION) { "Nieobsługiwana wersja żądania." }
            val signalDeviceId = if (o.has("sd") && !o.isNull("sd")) o.getInt("sd") else null
            val signalBundle = if (o.has("sb") && !o.isNull("sb")) o.getString("sb") else null
            return PairingRequest(o.getString("g"), o.getString("i"), o.getString("d"), o.getString("n"), o.getString("k"), o.getString("s"), signalDeviceId, signalBundle)
                .also {
                    require(it.deviceId.length in 8..128 && it.displayName.length <= 80) { "Nieprawidłowe żądanie urządzenia." }
                    require(it.signalDeviceId == null || it.signalDeviceId in 1..127) { "Nieprawidłowy Signal DeviceID." }
                    require(it.signalDeviceId == null || !it.signalBundle.isNullOrBlank()) { "Brak publicznego bundla Signal." }
                    require(it.signalBundle == null || it.signalBundle.length <= 128 * 1024) { "Bundle Signal jest zbyt duży." }
                }
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
