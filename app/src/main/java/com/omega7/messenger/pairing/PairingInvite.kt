package com.omega7.messenger.pairing

import android.util.Base64
import org.json.JSONObject
import java.net.URI
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.UUID

/** Short-lived, single-use invitation. Relay capability, endpoint and owner Signal bundle are integrity-bound by the owner signature. */
data class PairingInvite(
    val groupId: String,
    val inviteId: String,
    val expiresAtMillis: Long,
    val ownerDeviceId: String,
    val ownerName: String,
    val ownerPublicKey: String,
    val signature: String,
    val inviteToken: String? = null,
    val relayBaseUrl: String? = null,
    val ownerSignalDeviceId: Int? = null,
    val ownerSignalBundle: String? = null,
) {
    companion object {
        private const val VERSION = 4
        private const val TTL_MS = 5 * 60 * 1000L

        fun create(groupId: String, ownerDeviceId: String, ownerName: String, ownerPublicKey: String, sign: (ByteArray) -> ByteArray): PairingInvite =
            create(groupId, ownerDeviceId, ownerName, ownerPublicKey, null, null, null, null, sign)

        fun create(
            groupId: String,
            ownerDeviceId: String,
            ownerName: String,
            ownerPublicKey: String,
            inviteToken: String?,
            relayBaseUrl: String?,
            sign: (ByteArray) -> ByteArray,
        ): PairingInvite = create(groupId, ownerDeviceId, ownerName, ownerPublicKey, inviteToken, relayBaseUrl, null, null, sign)

        fun create(
            groupId: String,
            ownerDeviceId: String,
            ownerName: String,
            ownerPublicKey: String,
            inviteToken: String?,
            relayBaseUrl: String?,
            ownerSignalDeviceId: Int?,
            ownerSignalBundle: SignalBundleString?,
            sign: (ByteArray) -> ByteArray,
        ): PairingInvite {
            require(ownerSignalDeviceId == null || ownerSignalDeviceId in 1..127) { "Nieprawidłowy Signal DeviceID właściciela." }
            require(ownerSignalDeviceId == null || !ownerSignalBundle.isNullOrBlank()) { "Brak bundla Signal właściciela." }
            validateRelayUrl(relayBaseUrl)
            val id = UUID.randomUUID().toString()
            val expires = System.currentTimeMillis() + TTL_MS
            val unsigned = PairingInvite(groupId, id, expires, ownerDeviceId, ownerName, ownerPublicKey, "", inviteToken, relayBaseUrl?.trimEnd('/'), ownerSignalDeviceId, ownerSignalBundle)
            val signature = Base64.encodeToString(
                sign(canonical(unsigned).toByteArray(Charsets.UTF_8)),
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
            )
            return unsigned.copy(signature = signature)
        }

        private fun canonical(invite: PairingInvite): String = listOf(
            VERSION, invite.groupId, invite.inviteId, invite.expiresAtMillis,
            invite.ownerDeviceId, invite.ownerName, invite.ownerPublicKey,
            invite.inviteToken ?: "-", invite.relayBaseUrl ?: "-",
            invite.ownerSignalDeviceId ?: "-", invite.ownerSignalBundle ?: "-",
        ).joinToString("|")

        fun parse(encoded: String): PairingInvite {
            val json = JSONObject(String(Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING), Charsets.UTF_8))
            require(json.getInt("v") == VERSION) { "Nieobsługiwana wersja zaproszenia." }
            val token = if (json.has("t") && !json.isNull("t")) json.getString("t") else null
            val relay = if (json.has("r") && !json.isNull("r")) json.getString("r") else null
            val ownerSignalDeviceId = if (json.has("sd") && !json.isNull("sd")) json.getInt("sd") else null
            val ownerSignalBundle = if (json.has("sb") && !json.isNull("sb")) json.getString("sb") else null
            validateRelayUrl(relay)
            val invite = PairingInvite(
                json.getString("g"), json.getString("i"), json.getLong("e"),
                json.getString("o"), json.getString("n"), json.getString("k"), json.getString("s"),
                token, relay, ownerSignalDeviceId, ownerSignalBundle,
            )
            require(invite.expiresAtMillis > System.currentTimeMillis()) { "Zaproszenie wygasło." }
            require(invite.inviteId.length <= 64 && invite.groupId.length <= 128) { "Nieprawidłowe zaproszenie." }
            require(invite.signature.length <= 1024 && invite.ownerPublicKey.length <= 4096) { "Nieprawidłowe zaproszenie." }
            require(invite.inviteToken == null || invite.inviteToken.length in 16..4096) { "Nieprawidłowy token zaproszenia." }
            require(ownerSignalDeviceId == null || ownerSignalDeviceId in 1..127) { "Nieprawidłowy Signal DeviceID właściciela." }
            require(ownerSignalDeviceId == null || !ownerSignalBundle.isNullOrBlank()) { "Brak bundla Signal właściciela." }
            require(ownerSignalBundle == null || ownerSignalBundle.length <= 128 * 1024) { "Bundle Signal właściciela jest zbyt duży." }
            return invite
        }

        fun encode(invite: PairingInvite): String = Base64.encodeToString(
            JSONObject().apply {
                put("v", VERSION); put("g", invite.groupId); put("i", invite.inviteId)
                put("e", invite.expiresAtMillis); put("o", invite.ownerDeviceId); put("n", invite.ownerName)
                put("k", invite.ownerPublicKey); put("s", invite.signature)
                invite.inviteToken?.let { put("t", it) }
                invite.relayBaseUrl?.let { put("r", it) }
                invite.ownerSignalDeviceId?.let { put("sd", it) }
                invite.ownerSignalBundle?.let { put("sb", it) }
            }.toString().toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )

        fun verify(invite: PairingInvite): Boolean {
            validateRelayUrl(invite.relayBaseUrl)
            require(invite.ownerSignalDeviceId == null || invite.ownerSignalDeviceId in 1..127)
            require(invite.ownerSignalDeviceId == null || !invite.ownerSignalBundle.isNullOrBlank())
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

    /** Alias used to keep the create overload unambiguous without coupling pairing to the E2EE implementation class. */
    private typealias SignalBundleString = String?
}
