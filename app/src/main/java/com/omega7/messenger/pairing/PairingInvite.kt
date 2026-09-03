package com.omega7.messenger.pairing

import android.util.Base64
import org.json.JSONObject
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.UUID

/** Short-lived, single-use invitation envelope. QR never contains a private key or message data. */
data class PairingInvite(
    val groupId: String,
    val inviteId: String,
    val expiresAtMillis: Long,
    val ownerDeviceId: String,
    val ownerName: String,
    val ownerPublicKey: String,
    val signature: String,
    /** Server-issued one-time registration capability. Never used as a bearer token for normal relay calls. */
    val inviteToken: String? = null,
) {
    companion object {
        private const val VERSION = 2
        private const val TTL_MS = 5 * 60 * 1000L

        fun create(groupId: String, ownerDeviceId: String, ownerName: String, ownerPublicKey: String, sign: (ByteArray) -> ByteArray): PairingInvite =
            create(groupId, ownerDeviceId, ownerName, ownerPublicKey, null, sign)

        fun create(
            groupId: String,
            ownerDeviceId: String,
            ownerName: String,
            ownerPublicKey: String,
            inviteToken: String?,
            sign: (ByteArray) -> ByteArray,
        ): PairingInvite {
            val id = UUID.randomUUID().toString()
            val expires = System.currentTimeMillis() + TTL_MS
            val unsigned = PairingInvite(groupId, id, expires, ownerDeviceId, ownerName, ownerPublicKey, "", inviteToken)
            val signature = Base64.encodeToString(
                sign(canonical(unsigned).toByteArray(Charsets.UTF_8)),
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
            )
            return unsigned.copy(signature = signature)
        }

        private fun canonical(invite: PairingInvite): String =
            listOf(
                VERSION, invite.groupId, invite.inviteId, invite.expiresAtMillis,
                invite.ownerDeviceId, invite.ownerName, invite.ownerPublicKey,
                invite.inviteToken ?: "-",
            ).joinToString("|")

        fun parse(encoded: String): PairingInvite {
            val json = JSONObject(String(Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING), Charsets.UTF_8))
            require(json.getInt("v") == VERSION) { "Nieobsługiwana wersja zaproszenia." }
            val token = if (json.has("t") && !json.isNull("t")) json.getString("t") else null
            val invite = PairingInvite(
                json.getString("g"), json.getString("i"), json.getLong("e"),
                json.getString("o"), json.getString("n"), json.getString("k"), json.getString("s"), token,
            )
            require(invite.expiresAtMillis > System.currentTimeMillis()) { "Zaproszenie wygasło." }
            require(invite.inviteId.length <= 64 && invite.groupId.length <= 128) { "Nieprawidłowe zaproszenie." }
            require(invite.signature.length <= 1024 && invite.ownerPublicKey.length <= 4096) { "Nieprawidłowe zaproszenie." }
            require(invite.inviteToken == null || invite.inviteToken.length in 16..4096) { "Nieprawidłowy token zaproszenia." }
            return invite
        }

        fun encode(invite: PairingInvite): String {
            val json = JSONObject().apply {
                put("v", VERSION); put("g", invite.groupId); put("i", invite.inviteId)
                put("e", invite.expiresAtMillis); put("o", invite.ownerDeviceId)
                put("n", invite.ownerName); put("k", invite.ownerPublicKey); put("s", invite.signature)
                invite.inviteToken?.let { put("t", it) }
            }
            return Base64.encodeToString(json.toString().toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        }

        fun verify(invite: PairingInvite): Boolean {
            val keyBytes = Base64.getUrlDecoder().decode(invite.ownerPublicKey)
            val signatureBytes = Base64.getUrlDecoder().decode(invite.signature)
            val key: PublicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(keyBytes))
            val verifier = Signature.getInstance("SHA256withRSA")
            verifier.initVerify(key)
            verifier.update(canonical(invite).toByteArray(Charsets.UTF_8))
            return verifier.verify(signatureBytes)
        }
    }
}
