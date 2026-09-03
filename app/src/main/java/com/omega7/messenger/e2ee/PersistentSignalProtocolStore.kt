package com.omega7.messenger.e2ee

import android.content.Context
import android.util.Base64
import com.omega7.messenger.data.EncryptedLocalStore
import org.json.JSONObject
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.InvalidKeyIdException
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord
import org.signal.libsignal.protocol.state.KyberPreKeyRecord
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SessionRecord
import org.signal.libsignal.protocol.state.SignalProtocolStore
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.state.impl.InMemorySignalProtocolStore
import java.util.UUID

/** Durable Signal state encrypted with Ω7's Android Keystore-backed local AES-GCM store. */
class PersistentSignalProtocolStore private constructor(
    private val stateStore: EncryptedLocalStore,
    identityKeyPair: IdentityKeyPair,
    registrationId: Int,
) : InMemorySignalProtocolStore(identityKeyPair, registrationId) {

    private val knownPreKeyIds = linkedSetOf<Int>()
    private val knownSessionAddresses = linkedMapOf<String, SignalProtocolAddress>()
    private val trustedIdentityBytes = linkedMapOf<String, ByteArray>()
    private val lock = Any()

    init { restore() }

    val identityKeyPair: IdentityKeyPair get() = getIdentityKeyPair()
    val localRegistrationId: Int get() = getLocalRegistrationId()
    fun firstAvailablePreKeyId(): Int = synchronized(lock) {
        knownPreKeyIds.firstOrNull { containsPreKey(it) } ?: -1
    }

    override fun saveIdentity(address: SignalProtocolAddress, identityKey: IdentityKey): SignalProtocolStore.IdentityChange {
        synchronized(lock) {
            val change = super.saveIdentity(address, identityKey)
            trustedIdentityBytes[address.toString()] = identityKey.serialize().clone()
            persistLocked()
            return change
        }
    }

    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) {
        synchronized(lock) {
            super.storePreKey(preKeyId, record)
            knownPreKeyIds += preKeyId
            persistLocked()
        }
    }

    override fun removePreKey(preKeyId: Int) {
        synchronized(lock) {
            super.removePreKey(preKeyId)
            knownPreKeyIds -= preKeyId
            persistLocked()
        }
    }

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) {
        synchronized(lock) {
            super.storeSignedPreKey(signedPreKeyId, record)
            persistLocked()
        }
    }

    override fun removeSignedPreKey(signedPreKeyId: Int) {
        synchronized(lock) {
            super.removeSignedPreKey(signedPreKeyId)
            persistLocked()
        }
    }

    override fun storeKyberPreKey(kyberPreKeyId: Int, record: KyberPreKeyRecord) {
        synchronized(lock) {
            super.storeKyberPreKey(kyberPreKeyId, record)
            persistLocked()
        }
    }

    override fun markKyberPreKeyUsed(kyberPreKeyId: Int, signedPreKeyId: Int, baseKey: ECPublicKey) {
        synchronized(lock) {
            super.markKyberPreKeyUsed(kyberPreKeyId, signedPreKeyId, baseKey)
            persistLocked()
        }
    }

    override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) {
        synchronized(lock) {
            super.storeSession(address, record)
            knownSessionAddresses[address.toString()] = SignalProtocolAddress(address.getName(), address.getDeviceId())
            persistLocked()
        }
    }

    override fun deleteSession(address: SignalProtocolAddress) {
        synchronized(lock) {
            super.deleteSession(address)
            knownSessionAddresses.remove(address.toString())?.close()
            persistLocked()
        }
    }

    override fun deleteAllSessions(name: String) {
        synchronized(lock) {
            super.deleteAllSessions(name)
            val doomed = knownSessionAddresses.filterValues { it.getName() == name }.keys.toList()
            doomed.forEach { knownSessionAddresses.remove(it)?.close() }
            persistLocked()
        }
    }

    override fun storeSenderKey(sender: SignalProtocolAddress, distributionId: UUID, record: SenderKeyRecord) {
        // Ω7 uses pairwise sessions for the seven-device group; sender-key state is not used.
        super.storeSenderKey(sender, distributionId, record)
    }

    private fun restore() {
        val blob = stateStore.load() ?: return
        synchronized(lock) {
            try {
                val root = JSONObject(String(blob, Charsets.UTF_8))
                val preKeys = root.optJSONObject("preKeys") ?: JSONObject()
                preKeys.keys().forEach { idText ->
                    val id = idText.toInt()
                    super.storePreKey(id, PreKeyRecord(decode(preKeys.getString(idText))))
                    knownPreKeyIds += id
                }
                val signed = root.optJSONObject("signedPreKeys") ?: JSONObject()
                signed.keys().forEach { idText ->
                    val id = idText.toInt()
                    super.storeSignedPreKey(id, SignedPreKeyRecord(decode(signed.getString(idText))))
                }
                val kyber = root.optJSONObject("kyberPreKeys") ?: JSONObject()
                kyber.keys().forEach { idText ->
                    val id = idText.toInt()
                    super.storeKyberPreKey(id, KyberPreKeyRecord(decode(kyber.getString(idText))))
                }
                val sessions = root.optJSONObject("sessions") ?: JSONObject()
                sessions.keys().forEach { addressText ->
                    val name = addressText.substringBeforeLast('.')
                    val deviceId = addressText.substringAfterLast('.').toInt()
                    val address = SignalProtocolAddress(name, deviceId)
                    super.storeSession(address, SessionRecord(decode(sessions.getString(addressText))))
                    knownSessionAddresses[addressText] = address
                }
                val trusted = root.optJSONObject("trustedIdentities") ?: JSONObject()
                trusted.keys().forEach { addressText ->
                    val name = addressText.substringBeforeLast('.')
                    val deviceId = addressText.substringAfterLast('.').toInt()
                    val address = SignalProtocolAddress(name, deviceId)
                    val bytes = decode(trusted.getString(addressText))
                    super.saveIdentity(address, IdentityKey(bytes))
                    trustedIdentityBytes[addressText] = bytes.clone()
                    bytes.fill(0)
                }
            } catch (e: Exception) {
                throw IllegalStateException("Nie można odtworzyć stanu Signal Protocol; E2EE zostaje zablokowane.", e)
            } finally { blob.fill(0) }
        }
    }

    private fun persistLocked() {
        val root = JSONObject()
            .put("format", 1)
            .put("identityKeyPair", encode(getIdentityKeyPair().serialize()))
            .put("registrationId", getLocalRegistrationId())
        val preKeys = JSONObject()
        knownPreKeyIds.sorted().forEach { id -> if (containsPreKey(id)) preKeys.put(id.toString(), encode(loadPreKey(id).serialize())) }
        root.put("preKeys", preKeys)
        val signed = JSONObject()
        loadSignedPreKeys().forEach { record -> signed.put(record.getId().toString(), encode(record.serialize())) }
        root.put("signedPreKeys", signed)
        val kyber = JSONObject()
        loadKyberPreKeys().forEach { record -> kyber.put(record.getId().toString(), encode(record.serialize())) }
        root.put("kyberPreKeys", kyber)
        val sessions = JSONObject()
        knownSessionAddresses.forEach { (addressText, address) -> if (containsSession(address)) sessions.put(addressText, encode(loadSession(address).serialize())) }
        root.put("sessions", sessions)
        val trusted = JSONObject()
        trustedIdentityBytes.forEach { (addressText, bytes) -> trusted.put(addressText, encode(bytes)) }
        root.put("trustedIdentities", trusted)
        stateStore.save(root.toString().toByteArray(Charsets.UTF_8))
    }

    private fun encode(value: ByteArray): String = Base64.encodeToString(value, Base64.NO_WRAP or Base64.URL_SAFE)
    private fun decode(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP or Base64.URL_SAFE)

    companion object {
        private const val FILE_NAME = "omega7_signal_protocol.bin"
        fun open(context: Context): PersistentSignalProtocolStore {
            val encrypted = EncryptedLocalStore(context.applicationContext, fileName = FILE_NAME)
            val existing = encrypted.load()
            if (existing == null) {
                val store = PersistentSignalProtocolStore(
                    encrypted,
                    IdentityKeyPair.generate(),
                    org.signal.libsignal.protocol.util.KeyHelper.generateRegistrationId(true)
                )
                store.persistLocked()
                return store
            }
            val root = try { JSONObject(String(existing, Charsets.UTF_8)) } finally { existing.fill(0) }
            val identityBytes = Base64.decode(root.getString("identityKeyPair"), Base64.NO_WRAP or Base64.URL_SAFE)
            val identity = try { IdentityKeyPair(identityBytes) } finally { identityBytes.fill(0) }
            return PersistentSignalProtocolStore(encrypted, identity, root.getInt("registrationId"))
        }
    }
}
