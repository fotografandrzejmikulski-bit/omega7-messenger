package com.omega7.messenger.pairing

import android.content.Context
import com.omega7.messenger.data.RelayConfigStore
import com.omega7.messenger.e2ee.SignalE2eeEngine
import com.omega7.messenger.network.RelayProvisioningClient

/** Coordinates the complete owner -> joiner provisioning handshake without marking a device trusted early. */
class DeviceProvisioningCoordinator(context: Context) {
    private val app = context.applicationContext
    private val relayConfig = RelayConfigStore(app)

    data class JoinMaterial(val invite: PairingInvite, val request: PairingRequest)

    fun createOwnerInvite(): Result<PairingInvite> = runCatching {
        val config = requireNotNull(relayConfig.load()) { "Brak skonfigurowanego relay." }
        val engine = SignalE2eeEngine.open(app)
        RelayProvisioningClient(config.baseUrl, config.authToken).use { client ->
            val created = client.createInvite("omega7-main", engine.deviceId()).getOrThrow()
            PairingInvite.create(
                groupId = "omega7-main",
                ownerDeviceId = engine.deviceId().toString(),
                ownerName = android.os.Build.MODEL,
                ownerPublicKey = engine.identityFingerprint(),
                inviteToken = created.second,
                relayBaseUrl = config.baseUrl,
                sign = { bytes -> throw UnsupportedOperationException("Owner invite signing must use DeviceKeyManager; coordinator requires signing adapter.") },
            )
        }
    }

    /** Build the join request locally. No private Signal material is placed into the QR. */
    fun createJoinMaterial(invite: PairingInvite, sign: (ByteArray) -> ByteArray): Result<JoinMaterial> = runCatching {
        require(invite.expiresAtMillis > System.currentTimeMillis()) { "Zaproszenie wygasło." }
        require(!invite.inviteToken.isNullOrBlank()) { "Zaproszenie nie zawiera rejestracyjnego tokenu relay." }
        val engine = SignalE2eeEngine.open(app)
        val request = PairingRequest.create(
            groupId = invite.groupId,
            inviteId = invite.inviteId,
            deviceId = java.util.UUID.randomUUID().toString(),
            displayName = android.os.Build.MODEL,
            publicKey = com.omega7.messenger.security.DeviceKeyManager().publicKeyDerBase64Url(),
            signalDeviceId = engine.deviceId(),
            signalBundle = engine.localBundle(),
            sign = sign,
        )
        JoinMaterial(invite, request)
    }

    fun createApproval(invite: PairingInvite, request: PairingRequest, sign: (ByteArray) -> ByteArray): Result<PairingApproval> = runCatching {
        require(invite.expiresAtMillis > System.currentTimeMillis()) { "Zaproszenie wygasło." }
        require(invite.groupId == request.groupId && invite.inviteId == request.inviteId) { "Żądanie nie pasuje do zaproszenia." }
        require(PairingRequest.verify(request)) { "Podpis urządzenia jest nieprawidłowy." }
        require(request.signalDeviceId != null && request.signalBundle != null) { "Żądanie nie zawiera kompletnego bundla Signal." }
        PairingApproval.create(invite, request, sign)
    }

    /** Complete registration only after owner approval has been cryptographically verified. */
    suspend fun completeRegistration(invite: PairingInvite, request: PairingRequest, approval: PairingApproval): Result<String> {
        return runCatching {
            require(invite.expiresAtMillis > System.currentTimeMillis()) { "Zaproszenie wygasło." }
            require(!invite.inviteToken.isNullOrBlank() && !invite.relayBaseUrl.isNullOrBlank()) { "Brak danych provisioning relay." }
            require(PairingRequest.verify(request)) { "Nieprawidłowy podpis urządzenia." }
            require(PairingApproval.verifyAgainst(invite, request, approval)) { "Zatwierdzenie właściciela jest nieprawidłowe." }
            val bundle = SignalE2eeEngine.DeviceBundle.fromJson(requireNotNull(request.signalBundle))
            require(bundle.deviceId == requireNotNull(request.signalDeviceId)) { "Signal DeviceID nie zgadza się z bundle." }
            require(bundle.deviceId in 1..127) { "Nieprawidłowy Signal DeviceID." }

            val preKeys = SignalE2eeEngine.open(app).localPreKeys().map { (id, key) ->
                com.omega7.messenger.network.RelayKeyClient.PreKey(id, key)
            }
            require(preKeys.isNotEmpty()) { "Brak lokalnych one-time prekeys." }
            RelayProvisioningClient(requireNotNull(invite.relayBaseUrl)).use { client ->
                val token = client.register(
                    groupId = invite.groupId,
                    deviceId = bundle.deviceId,
                    inviteToken = requireNotNull(invite.inviteToken),
                    bundle = bundle,
                    preKeys = preKeys,
                ).getOrThrow()
                relayConfig.save(RelayConfigStore.Config(requireNotNull(invite.relayBaseUrl), token))
                token
            }
        }
    }
}
