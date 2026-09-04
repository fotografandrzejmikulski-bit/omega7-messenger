package com.omega7.messenger.pairing

import android.util.Base64
import org.json.JSONObject
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/** One-time owner authorization for the joining device to complete relay registration. */
data class PairingApproval(
    val groupId: String,
    val inviteId: String,
    val signalDeviceId: Int,
    val joiningDeviceId: String,
    val joiningDisplayName: String,
    val joiningDevicePublicKey: String,
    val joiningRequestSignature: String,
    val signalBundle: String,
    val ownerDeviceId: String,
    val ownerPublicKey: String,
    val ownerSignature: String,
) {
    companion object {
        private const val VERSION = 2

        fun create(
            invite: PairingInvite,
            request: PairingRequest,
            sign: (ByteArray) -> ByteArray,
        ): PairingApproval {
            val deviceId = requireNotNull(request.signalDeviceId) { "Brak Signal DeviceID." }
            val bundle = requireNotNull(request.signalBundle) { "Brak bundla Signal." }
            require(invite.groupId == request.groupId && invite.inviteId == request.inviteId)
            val unsigned = PairingApproval(
                invite.groupId,
                invite.inviteId,
                deviceId,
                request.deviceId,
                request.displayName,
                request.devicePublicKey,
                request.signature,
                bundle,
                invite.ownerDeviceId,
                invite.ownerPublicKey,
                "",
            )
            val sig = Base64.encodeToString(
                sign(canonical(unsigned).toByteArray(Charsets.UTF_8)),
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
            )
            return unsigned.copy(ownerSignature = sig)
        }

        private fun canonical(a: PairingApproval): String = listOf(
            VERSION,
            a.groupId,
            a.inviteId,
            a.signalDeviceId,
            a.joiningDeviceId,
            a.joiningDisplayName,
            a.joiningDevicePublicKey,
            a.joiningRequestSignature,
            a.signalBundle,
            a.ownerDeviceId,
            a.ownerPublicKey,
        ).joinToString("|")

        fun encode(a: PairingApproval): String = Base64.encodeToString(
            JSONObject().apply {
                put("v", VERSION)
                put("g", a.groupId)
                put("i", a.inviteId)
                put("d", a.signalDeviceId)
                put("jd", a.joiningDeviceId)
                put("jn", a.joiningDisplayName)
                put("k", a.joiningDevicePublicKey)
                put("rs", a.joiningRequestSignature)
                put("b", a.signalBundle)
                put("o", a.ownerDeviceId)
                put("ok", a.ownerPublicKey)
                put("s", a.ownerSignature)
            }.toString().toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )

        fun parse(encoded: String): PairingApproval {
            val j = JSONObject(String(Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING), Charsets.UTF_8))
            require(j.getInt("v") == VERSION) { "Nieobsługiwana wersja zatwierdzenia." }
            val a = PairingApproval(
                j.getString("g"),
                j.getString("i"),
                j.getInt("d"),
                j.getString("jd"),
                j.getString("jn"),
                j.getString("k"),
                j.getString("rs"),
                j.getString("b"),
                j.getString("o"),
                j.getString("ok"),
                j.getString("s"),
            )
            require(a.groupId.length <= 128 && a.inviteId.length <= 64)
            require(a.signalDeviceId in 1..127 && a.signalBundle.length <= 128 * 1024)
            require(a.joiningDeviceId.length in 8..128 && a.joiningDisplayName.length <= 80)
            require(a.joiningDevicePublicKey.length <= 4096 && a.joiningRequestSignature.length <= 1024)
            require(a.ownerPublicKey.length <= 4096 && a.ownerSignature.length <= 1024)
            return a
        }

        fun verify(a: PairingApproval): Boolean {
            val key = KeyFactory.getInstance("RSA").generatePublic(
                X509EncodedKeySpec(Base64.getUrlDecoder().decode(a.ownerPublicKey))
            )
            return Signature.getInstance("SHA256withRSA").run {
                initVerify(key)
                update(canonical(a).toByteArray(Charsets.UTF_8))
                verify(Base64.getUrlDecoder().decode(a.ownerSignature))
            }
        }

        fun verifyAgainst(invite: PairingInvite, request: PairingRequest, approval: PairingApproval): Boolean {
            return invite.groupId == approval.groupId &&
                invite.inviteId == approval.inviteId &&
                invite.ownerDeviceId == approval.ownerDeviceId &&
                invite.ownerPublicKey == approval.ownerPublicKey &&
                request.groupId == approval.groupId &&
                request.inviteId == approval.inviteId &&
                request.deviceId == approval.joiningDeviceId &&
                request.displayName == approval.joiningDisplayName &&
                request.devicePublicKey == approval.joiningDevicePublicKey &&
                request.signature == approval.joiningRequestSignature &&
                request.signalDeviceId == approval.signalDeviceId &&
                request.signalBundle == approval.signalBundle &&
                PairingRequest.verify(request) &&
                verify(approval)
        }
    }
}
