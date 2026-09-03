package com.omega7.messenger.e2ee

import android.content.Context
import android.util.Base64
import com.omega7.messenger.data.EncryptedLocalStore
import org.json.JSONArray
import org.json.JSONObject
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.InvalidKeyException
import org.signal.libsignal.protocol.NoSessionException
import org.signal.libsignal.protocol.SessionBuilder
import org.signal.libsignal.protocol.SessionCipher
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.kem.KEMKeyPair
import org.signal.libsignal.protocol.kem.KEMKeyType
import org.signal.libsignal.protocol.kem.KEMPublicKey
import org.signal.libsignal.protocol.message.CiphertextMessage
import org.signal.libsignal.protocol.message.PreKeySignalMessage
import org.signal.libsignal.protocol.message.SignalMessage
import org.signal.libsignal.protocol.state.KyberPreKeyRecord
import org.signal.libsignal.protocol.state.PreKeyBundle
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import java.security.SecureRandom

/**
 * Ω7 E2EE engine backed exclusively by libsignal. It does not implement a custom ratchet/KDF.
 * Each verified device gets an independent Signal session, matching the seven-device model.
 */
class SignalE2eeEngine private constructor(
    private val context: Context,
    private val store: PersistentSignalProtocolStore,
    private val localDeviceId: Int,
) {
    data class DeviceBundle(
        val deviceId: Int,
        val registrationId: Int,
        val identityKey: ByteArray,
        val preKeyId: Int,
        val preKey: ByteArray?,
        val signedPreKeyId: Int,
        val signedPreKey: ByteArray,
        val signedPreKeySignature: ByteArray,
        val kyberPreKeyId: Int,
        val kyberPreKey: ByteArray,
        val kyberPreKeySignature: ByteArray,
    ) {
        fun toJson(): String = JSONObject()
            .put("deviceId", deviceId)
            .put("registrationId", registrationId)
            .put("identityKey", b64(identityKey))
            .put("preKeyId", preKeyId)
            .put("preKey", preKey?.let(::b64))
            .put("signedPreKeyId", signedPreKeyId)
            .put("signedPreKey", b64(signedPreKey))
            .put("signedPreKeySignature", b64(signedPreKeySignature))
            .put("kyberPreKeyId", kyberPreKeyId)
            .put("kyberPreKey", b64(kyberPreKey))
            .put("kyberPreKeySignature", b64(kyberPreKeySignature))
            .toString()

        companion object {
            fun fromJson(value: String): DeviceBundle {
                val j = JSONObject(value)
                return DeviceBundle(
                    j.getInt("deviceId"), j.getInt("registrationId"),
                    unb64(j.getString("identityKey")), j.getInt("preKeyId"),
                    if (j.isNull("preKey")) null else unb64(j.getString("preKey")),
                    j.getInt("signedPreKeyId"), unb64(j.getString("signedPreKey")),
                    unb64(j.getString("signedPreKeySignature")), j.getInt("kyberPreKeyId"),
                    unb64(j.getString("kyberPreKey")), unb64(j.getString("kyberPreKeySignature"))
                )
            }
        }
    }

    fun localBundle(): DeviceBundle {
        if (store.loadSignedPreKeys().isEmpty() || store.loadKyberPreKeys().isEmpty() || store.firstAvailablePreKeyId() < 0) {
            generatePreKeys()
        }
        val signed = store.loadSignedPreKeys().maxByOrNull { it.getTimestamp() } ?: error("Brak signed prekey")
        val kyber = store.loadKyberPreKeys().maxByOrNull { it.getTimestamp() } ?: error("Brak Kyber prekey")
        val preKeyId = store.firstAvailablePreKeyId()
        val preKeyPublic = store.loadPreKey(preKeyId).keyPair.publicKey.serialize()
        return DeviceBundle(
            localDeviceId, store.localRegistrationId, store.identityKeyPair.publicKey.serialize(),
            preKeyId, preKeyPublic, signed.id, signed.keyPair.publicKey.serialize(), signed.signature,
            kyber.id, kyber.keyPair.publicKey.serialize(), kyber.signature
        )
    }

    @Synchronized
    fun generatePreKeys(preKeyCount: Int = 24) {
        require(preKeyCount in 8..64) { "Nieprawidłowa liczba prekeys." }
        val identity = store.identityKeyPair
        val random = SecureRandom()
        val signedId = random.nextInt(0x7FFFFF) + 1
        val signedPair = ECKeyPair.generate()
        val signedSignature = identity.privateKey.calculateSignature(signedPair.publicKey.serialize())
        store.storeSignedPreKey(signedId, SignedPreKeyRecord(signedId, System.currentTimeMillis(), signedPair, signedSignature))
        val kyberId = random.nextInt(0x7FFFFF) + 1
        val kyberPair = KEMKeyPair.generate(KEMKeyType.KYBER_1024)
        val kyberSignature = identity.privateKey.calculateSignature(kyberPair.publicKey.serialize())
        store.storeKyberPreKey(kyberId, KyberPreKeyRecord(kyberId, System.currentTimeMillis(), kyberPair, kyberSignature))
        repeat(preKeyCount) {
            var id: Int
            do { id = random.nextInt(0x7FFFFF) + 1 } while (store.containsPreKey(id))
            store.storePreKey(id, PreKeyRecord(id, ECKeyPair.generate()))
        }
    }

    /** Call only after the QR/fingerprint flow has explicitly approved the remote identity. */
    @Synchronized
    fun registerVerifiedDevice(groupId: String, bundle: DeviceBundle) {
        require(bundle.deviceId in 1..127 && bundle.deviceId != localDeviceId)
        val current = loadVerifiedBundles().filterNot { it.deviceId == bundle.deviceId }.toMutableList()
        require(current.size < 6) { "Osiągnięto limit 7 urządzeń." }
        val remoteIdentity = IdentityKey(bundle.identityKey)
        val address = SignalProtocolAddress(groupId, bundle.deviceId)
        try {
            store.saveIdentity(address, remoteIdentity)
            val preKeyBundle = PreKeyBundle(
                bundle.registrationId, bundle.deviceId, bundle.preKeyId,
                bundle.preKey?.let(::ECPublicKey), bundle.signedPreKeyId, ECPublicKey(bundle.signedPreKey),
                bundle.signedPreKeySignature, remoteIdentity, bundle.kyberPreKeyId,
                KEMPublicKey(bundle.kyberPreKey), bundle.kyberPreKeySignature
            )
            SessionBuilder(store, address, SignalProtocolAddress(groupId, localDeviceId)).process(preKeyBundle)
            current += bundle.copy(identityKey = bundle.identityKey.clone())
            saveVerifiedBundles(current)
        } catch (e: InvalidKeyException) {
            throw SecurityException("Nieprawidłowy klucz urządzenia zdalnego.", e)
        } finally { address.close() }
    }

    @Synchronized
    fun encrypt(groupId: String, plaintext: ByteArray): ByteArray {
        require(plaintext.size <= MAX_PLAINTEXT) { "Wiadomość jest zbyt duża." }
        val recipients = loadVerifiedBundles()
        require(recipients.isNotEmpty()) { "Brak zweryfikowanych urządzeń docelowych." }
        val items = JSONArray()
        recipients.forEach { bundle ->
            val encrypted = encryptForDevice(groupId, bundle.deviceId, plaintext)
            items.put(JSONObject().put("deviceId", bundle.deviceId).put("ciphertext", b64(encrypted)))
        }
        return JSONObject().put("version", 1).put("senderDeviceId", localDeviceId).put("recipients", items)
            .toString().toByteArray(Charsets.UTF_8)
    }

    @Synchronized
    fun encryptForDevice(groupId: String, remoteDeviceId: Int, plaintext: ByteArray): ByteArray {
        val remote = SignalProtocolAddress(groupId, remoteDeviceId)
        val local = SignalProtocolAddress(groupId, localDeviceId)
        return try {
            val cipher = SessionCipher(store, local, remote)
            encodeEnvelope(localDeviceId, cipher.encrypt(plaintext))
        } finally { local.close(); remote.close() }
    }

    @Synchronized
    fun decrypt(groupId: String, ciphertext: ByteArray): ByteArray {
        require(ciphertext.size <= MAX_ENVELOPE * 6) { "Koperta grupowa jest zbyt duża." }
        val root = JSONObject(String(ciphertext, Charsets.UTF_8))
        require(root.getInt("version") == 1) { "Nieobsługiwana wersja koperty grupowej." }
        val sender = root.getInt("senderDeviceId")
        require(sender in 1..127 && sender != localDeviceId)
        val recipients = root.getJSONArray("recipients")
        for (i in 0 until recipients.length()) {
            val item = recipients.getJSONObject(i)
            if (item.getInt("deviceId") == localDeviceId) {
                return decryptFromDevice(groupId, sender, unb64(item.getString("ciphertext")))
            }
        }
        throw SecurityException("Koperta nie jest przeznaczona dla tego urządzenia.")
    }

    @Synchronized
    fun decryptFromDevice(groupId: String, senderDeviceId: Int, envelope: ByteArray): ByteArray {
        require(envelope.size <= MAX_ENVELOPE) { "Koperta jest zbyt duża." }
        val parsed = decodeEnvelope(envelope)
        require(parsed.first == senderDeviceId) { "Adres nadawcy nie zgadza się z kopertą." }
        val remote = SignalProtocolAddress(groupId, senderDeviceId)
        val local = SignalProtocolAddress(groupId, localDeviceId)
        return try {
            val cipher = SessionCipher(store, local, remote)
            when (parsed.second) {
                CiphertextMessage.PREKEY_TYPE -> cipher.decrypt(PreKeySignalMessage(parsed.third))
                CiphertextMessage.WHISPER_TYPE -> cipher.decrypt(SignalMessage(parsed.third))
                else -> throw SecurityException("Niedozwolony typ koperty E2EE.")
            }
        } finally { local.close(); remote.close() }
    }

    fun revokeDevice(groupId: String, remoteDeviceId: Int) {
        saveVerifiedBundles(loadVerifiedBundles().filterNot { it.deviceId == remoteDeviceId })
        store.deleteAllSessions(groupId)
    }

    fun identityFingerprint(): String = store.identityKeyPair.publicKey.getFingerprint()
    fun deviceId(): Int = localDeviceId

    private fun loadVerifiedBundles(): List<DeviceBundle> {
        val file = EncryptedLocalStore(context, fileName = VERIFIED_FILE)
        val bytes = file.load() ?: return emptyList()
        return try {
            val array = JSONArray(String(bytes, Charsets.UTF_8))
            (0 until array.length()).map { DeviceBundle.fromJson(array.getString(it)) }
        } finally { bytes.fill(0) }
    }

    private fun saveVerifiedBundles(value: List<DeviceBundle>) {
        require(value.size <= 6) { "Limit zaufanych urządzeń przekroczony." }
        val array = JSONArray()
        value.forEach { array.put(it.toJson()) }
        EncryptedLocalStore(context, fileName = VERIFIED_FILE).save(array.toString().toByteArray(Charsets.UTF_8))
    }

    private fun encodeEnvelope(senderDeviceId: Int, message: CiphertextMessage): ByteArray {
        val body = message.serialize()
        require(body.size <= MAX_ENVELOPE)
        return ByteArray(6 + body.size).also {
            it[0] = VERSION; it[1] = message.type.toByte()
            it[2] = (senderDeviceId ushr 24).toByte(); it[3] = (senderDeviceId ushr 16).toByte()
            it[4] = (senderDeviceId ushr 8).toByte(); it[5] = senderDeviceId.toByte()
            body.copyInto(it, 6)
        }
    }

    private fun decodeEnvelope(value: ByteArray): Triple<Int, Int, ByteArray> {
        require(value.size >= 7 && value[0] == VERSION) { "Uszkodzona koperta E2EE." }
        val id = ((value[2].toInt() and 0xFF) shl 24) or ((value[3].toInt() and 0xFF) shl 16) or
            ((value[4].toInt() and 0xFF) shl 8) or (value[5].toInt() and 0xFF)
        require(id in 1..127)
        return Triple(id, value[1].toInt() and 0xFF, value.copyOfRange(6, value.size))
    }

    companion object {
        private const val PREFS = "omega7_signal_identity"
        private const val KEY_DEVICE_ID = "signal_device_id"
        private const val VERIFIED_FILE = "omega7_verified_devices.bin"
        private const val VERSION: Byte = 1
        private const val MAX_PLAINTEXT = 16 * 1024
        private const val MAX_ENVELOPE = 256 * 1024

        fun open(context: Context): SignalE2eeEngine {
            val app = context.applicationContext
            val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val deviceId = prefs.getInt(KEY_DEVICE_ID, 0).takeIf { it in 1..127 } ?: run {
                val generated = SecureRandom().nextInt(127) + 1
                check(prefs.edit().putInt(KEY_DEVICE_ID, generated).commit())
                generated
            }
            return SignalE2eeEngine(app, PersistentSignalProtocolStore.open(app), deviceId)
        }

        private fun b64(value: ByteArray): String = Base64.encodeToString(value, Base64.NO_WRAP or Base64.URL_SAFE)
        private fun unb64(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP or Base64.URL_SAFE)
    }
}
