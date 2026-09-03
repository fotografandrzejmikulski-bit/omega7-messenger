package com.omega7.messenger.pairing

import android.util.Base64
import org.json.JSONObject
import java.net.URI
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.UUID

/** Short-lived, single-use invitation. Relay registration capability and endpoint are integrity-bound by the owner signature. */
data class PairingInvite(
    val groupId: String,
    val inviteId: String,
    val expiresAtMillis: Long,
    val ownerDeviceId: String,
    val ownerName: String,
    val ownerPublicKey: String,
    val signature: String,
    /** Server-issued one-time registration capability. Never used for normal relay calls. */
    val inviteToken: String? = null,
    /** HTTPS relay endpoint authenticated by the owner signature. */
    val relayBaseUrl: String? = null,
) {
    companion object {
        private const val VERSION = 3
        private const val TTL_MS = 5 * 60 * 1000L

        fun create(groupId: String, ownerDeviceId: String, ownerName: String, ownerPublicKey: String, sign: (ByteArray) -> ByteArray): PairingInvite =
            create(groupId, ownerDeviceId, ownerName, ownerPublicKey, null, null, sign)

        fun create(
            groupId: String,
            ownerDeviceId: String,
            ownerName: String,
            ownerPublicKey: String,
            inviteToken: String?,
            relayBaseUrl: String?,
            sign: (ByteArray) -> ByteArray,
        ): PairingInvite {
            val id = UUID.randomUUID().toString()
            val expires = System.currentTimeMillis() + TTL_MS
            validateRelayUrl(relayBaseUrl)
            val unsigned = PairingInvite(groupId, id, expires, ownerDeviceId, ownerName, ownerPublicKey, "", inviteToken, relayBaseUrl?.trimEnd('/'))
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
                invite.inviteToken ?: "-", invite.relayBaseUrl ?: "-",
            ).joinToString("|")

        fun parse(encoded: String): PairingInvite {
            val json = JSONObject(String(Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING), Charsets.UTF_8))
            require(json.getInt("v") == VERSION) { "Nieobsługiwana wersja zaproszenia." }
            val token = if (json.has("t") && !json.isNull("t")) json.getString("t") else null
            val relay = if (json.has("r") && !json.isNull("r")) json.getString("r") else null
            validateRelayUrl(relay)
            val invite = PairingInvite(
                json.getString("g"), json.getString("i"), json.getLong("e"),
                json.getString("o"), json.getString("n"), json.getString("k"), json.getString("s"), token, relay,
            )
            require(invite.expiresAtMillis > System.currentTimeMillis()) { "Zaproszenie wygasło." }
            require(invite.inviteId.length <= 64 && invite.groupId.length <= 128) { "Nieprawidłowe zaproszenie." }
            require(invite.signature.length <= 1024 && invite.ownerPublicKey.length <= 4096) { "Nieprawidłowe zaproszenie." }
            require(invite.inviteToken == null || invite.inviteToken.length in 16..4096) { "Nieprawidłowy token zaproszenia." }
            return invite
        }

        fun encode(invite: PairingInvite): String = Base64.encodeToString(
            JSONObject().apply {
                put("v", VERSION); put("g", invite.groupId); put("i", invite.inviteId)
                put("e", invite.expiresAtMillis); put("o", invite.ownerDeviceId); put("n", invite.ownerName)
                put("k", invite.ownerPublicKey); put("s", invite.signature)
                invite.inviteToken?.let { put("t", it) }
                invite.relayBaseUrl?.let { put("r", it) }
            }.toString().toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )

        fun verify(invite: PairingInvite): Boolean {
            validateRelayUrl(invite.relayBaseUrl)
            val keyBytes = Base64.getUrlDecoder().decode(invite.ownerPublicKey)
            val signatureBytes = Base64.getUrlDecoder().decode(invite.signature)
            val key: PublicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(keyBytes))
            return Signature.getInstance("SHA256withRSA").run {
                initVerify(key)
                update(canonical(invite).toByteArray(Charsets.UTF_8))
                verify(signatureBytes)
            }
        }

        private fun validateRelayUrl(value: String?) {
            if (value == null) return
            val uri = URI(value)
            require(uri.scheme.equals("https", ignoreCase = true)) { "Relay musi używać HTTPS." }
            require(uri.userInfo == null) { "Adres relay nie może zawierać danych uwierzytelniających." }
            require(uri.fragment == null) { "Adres relay nie może zawierać fragmentu." }
            require(!uri.host.isNullOrBlank()) { "Adres relay musi zawierać poprawny host." }
        }
    }
}
