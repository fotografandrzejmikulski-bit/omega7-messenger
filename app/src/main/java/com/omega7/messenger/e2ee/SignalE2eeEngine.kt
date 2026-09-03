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

/** Ω7 E2EE engine backed exclusively by libsignal. No custom ratchet/KDF is implemented here. */
class SignalE2eeEngine private constructor(
    private val context: Context,
    private val store: PersistentSignalProtocolStore,
    private val localDeviceId: Int,
) {
    data class DeviceBundle(
        val deviceId: Int, val registrationId: Int, val identityKey: ByteArray,
        val preKeyId: Int, val preKey: ByteArray?, val signedPreKeyId: Int,
        val signedPreKey: ByteArray, val signedPreKeySignature: ByteArray,
        val kyberPreKeyId: Int, val kyberPreKey: ByteArray, val kyberPreKeySignature: ByteArray,
    ) {
        fun toJson(): String = JSONObject().put("deviceId", deviceId).put("registrationId", registrationId)
            .put("identityKey", b64(identityKey)).put("preKeyId", preKeyId).put("preKey", preKey?.let(::b64))
            .put("signedPreKeyId", signedPreKeyId).put("signedPreKey", b64(signedPreKey))
            .put("signedPreKeySignature", b64(signedPreKeySignature)).put("kyberPreKeyId", kyberPreKeyId)
            .put("kyberPreKey", b64(kyberPreKey)).put("kyberPreKeySignature", b64(kyberPreKeySignature)).toString()

        companion object {
            fun fromJson(value: String): DeviceBundle { val j=JSONObject(value); return DeviceBundle(
                j.getInt("deviceId"),j.getInt("registrationId"),unb64(j.getString("identityKey")),j.getInt("preKeyId"),
                if(j.isNull("preKey"))null else unb64(j.getString("preKey")),j.getInt("signedPreKeyId"),unb64(j.getString("signedPreKey")),
                unb64(j.getString("signedPreKeySignature")),j.getInt("kyberPreKeyId"),unb64(j.getString("kyberPreKey")),unb64(j.getString("kyberPreKeySignature"))) }
        }
    }

    fun localBundle(): DeviceBundle {
        if (store.loadSignedPreKeys().isEmpty() || store.loadKyberPreKeys().isEmpty() || store.firstAvailablePreKeyId() < 0) generatePreKeys()
        val signed=store.loadSignedPreKeys().maxByOrNull{it.getTimestamp()} ?: error("Brak signed prekey")
        val kyber=store.loadKyberPreKeys().maxByOrNull{it.getTimestamp()} ?: error("Brak Kyber prekey")
        val pre=store.firstAvailablePreKeyId()
        return DeviceBundle(localDeviceId,store.localRegistrationId,store.identityKeyPair.publicKey.serialize(),pre,store.loadPreKey(pre).keyPair.publicKey.serialize(),signed.id,signed.keyPair.publicKey.serialize(),signed.signature,kyber.id,kyber.keyPair.publicKey.serialize(),kyber.signature)
    }

    @Synchronized fun generatePreKeys(preKeyCount:Int=24){
        require(preKeyCount in 8..64){"Nieprawidłowa liczba prekeys."}; val idk=store.identityKeyPair; val r=SecureRandom()
        val sid=r.nextInt(0x7FFFFF)+1; val sp=ECKeyPair.generate(); val ss=idk.privateKey.calculateSignature(sp.publicKey.serialize())
        store.storeSignedPreKey(sid,SignedPreKeyRecord(sid,System.currentTimeMillis(),sp,ss))
        val kid=r.nextInt(0x7FFFFF)+1; val kp=KEMKeyPair.generate(KEMKeyType.KYBER_1024); val ks=idk.privateKey.calculateSignature(kp.publicKey.serialize())
        store.storeKyberPreKey(kid,KyberPreKeyRecord(kid,System.currentTimeMillis(),kp,ks))
        repeat(preKeyCount){var pid:Int;do{pid=r.nextInt(0x7FFFFF)+1}while(store.containsPreKey(pid));store.storePreKey(pid,PreKeyRecord(pid,ECKeyPair.generate()))}
    }

    /** Register a device only after the application's QR/fingerprint approval has succeeded. */
    @Synchronized fun registerVerifiedDevice(groupId:String,bundle:DeviceBundle){
        require(bundle.deviceId in 1..127 && bundle.deviceId!=localDeviceId)
        val current=loadVerifiedBundles().filterNot{it.deviceId==bundle.deviceId}.toMutableList();require(current.size<6){"Osiągnięto limit 7 urządzeń."}
        val remote=IdentityKey(bundle.identityKey);val address=SignalProtocolAddress(groupId,bundle.deviceId)
        try{
            val change=store.saveIdentity(address,remote)
            require(change==org.signal.libsignal.protocol.state.IdentityKeyStore.IdentityChange.NEW_OR_UNCHANGED){"Zmiana klucza tożsamości wymaga ponownej weryfikacji."}
            val pb=PreKeyBundle(bundle.registrationId,bundle.deviceId,bundle.preKeyId,bundle.preKey?.let(::ECPublicKey),bundle.signedPreKeyId,ECPublicKey(bundle.signedPreKey),bundle.signedPreKeySignature,remote,bundle.kyberPreKeyId,KEMPublicKey(bundle.kyberPreKey),bundle.kyberPreKeySignature)
            val local=SignalProtocolAddress(groupId,localDeviceId);try{SessionBuilder(store,address,local).process(pb)}finally{local.close()};current+=bundle.copy(identityKey=bundle.identityKey.clone());saveVerifiedBundles(current)
        }catch(e:InvalidKeyException){throw SecurityException("Nieprawidłowy klucz urządzenia zdalnego.",e)}finally{address.close()}
    }

    @Synchronized fun encrypt(groupId:String,plaintext:ByteArray):ByteArray{
        require(plaintext.size<=MAX_PLAINTEXT){"Wiadomość jest zbyt duża."};val recipients=loadVerifiedBundles();require(recipients.isNotEmpty()){"Brak zweryfikowanych urządzeń docelowych."}
        val a=JSONArray();recipients.forEach{b->a.put(JSONObject().put("deviceId",b.deviceId).put("ciphertext",b64(encryptForDevice(groupId,b.deviceId,plaintext))))}
        return JSONObject().put("version",1).put("senderDeviceId",localDeviceId).put("recipients",a).toString().toByteArray(Charsets.UTF_8)
    }

    @Synchronized fun encryptForDevice(groupId:String,remoteDeviceId:Int,plaintext:ByteArray):ByteArray{
        val remote=SignalProtocolAddress(groupId,remoteDeviceId);val local=SignalProtocolAddress(groupId,localDeviceId);return try{encodeEnvelope(localDeviceId,SessionCipher(store,local,remote).encrypt(plaintext))}finally{local.close();remote.close()}
    }

    @Synchronized fun decrypt(groupId:String,ciphertext:ByteArray):ByteArray{
        require(ciphertext.size<=MAX_ENVELOPE*6){"Koperta grupowa jest zbyt duża."};val root=JSONObject(String(ciphertext,Charsets.UTF_8));require(root.getInt("version")==1){"Nieobsługiwana wersja koperty grupowej."};val sender=root.getInt("senderDeviceId");require(sender in 1..127&&sender!=localDeviceId){"Nieprawidłowy nadawca."};val r=root.getJSONArray("recipients");for(i in 0 until r.length()){val item=r.getJSONObject(i);if(item.getInt("deviceId")==localDeviceId)return decryptFromDevice(groupId,sender,unb64(item.getString("ciphertext")))};throw SecurityException("Koperta nie jest przeznaczona dla tego urządzenia.")
    }

    @Synchronized fun decryptFromDevice(groupId:String,senderDeviceId:Int,envelope:ByteArray):ByteArray{
        require(envelope.size<=MAX_ENVELOPE){"Koperta jest zbyt duża."};val p=decodeEnvelope(envelope);require(p.first==senderDeviceId){"Adres nadawcy nie zgadza się z kopertą."};val remote=SignalProtocolAddress(groupId,senderDeviceId);val local=SignalProtocolAddress(groupId,localDeviceId);return try{val c=SessionCipher(store,local,remote);when(p.second){CiphertextMessage.PREKEY_TYPE->c.decrypt(PreKeySignalMessage(p.third));CiphertextMessage.WHISPER_TYPE->c.decrypt(SignalMessage(p.third));else->throw SecurityException("Niedozwolony typ koperty E2EE.")}}finally{local.close();remote.close()}
    }

    fun revokeDevice(groupId:String,remoteDeviceId:Int){saveVerifiedBundles(loadVerifiedBundles().filterNot{it.deviceId==remoteDeviceId});store.deleteAllSessions(groupId)}
    fun identityFingerprint():String=store.identityKeyPair.publicKey.getFingerprint(); fun deviceId():Int=localDeviceId

    private fun loadVerifiedBundles():List<DeviceBundle>{val f=EncryptedLocalStore(context,fileName=VERIFIED_FILE);val b=f.load()?:return emptyList();return try{val a=JSONArray(String(b,Charsets.UTF_8));(0 until a.length()).map{DeviceBundle.fromJson(a.getString(it))}}finally{b.fill(0)}}
    private fun saveVerifiedBundles(v:List<DeviceBundle>){require(v.size<=6){"Limit zaufanych urządzeń przekroczony."};val a=JSONArray();v.forEach{a.put(it.toJson())};EncryptedLocalStore(context,fileName=VERIFIED_FILE).save(a.toString().toByteArray(Charsets.UTF_8))}
    private fun encodeEnvelope(sender:Int,m:CiphertextMessage):ByteArray{val b=m.serialize();require(b.size<=MAX_ENVELOPE);return ByteArray(6+b.size).also{it[0]=VERSION;it[1]=m.type.toByte();it[2]=(sender ushr 24).toByte();it[3]=(sender ushr 16).toByte();it[4]=(sender ushr 8).toByte();it[5]=sender.toByte();b.copyInto(it,6)}}
    private fun decodeEnvelope(v:ByteArray):Triple<Int,Int,ByteArray>{require(v.size>=7&&v[0]==VERSION){"Uszkodzona koperta E2EE."};val id=((v[2].toInt()and 255)shl 24)or((v[3].toInt()and 255)shl 16)or((v[4].toInt()and 255)shl 8)or(v[5].toInt()and 255);require(id in 1..127);return Triple(id,v[1].toInt()and 255,v.copyOfRange(6,v.size))}

    companion object{private const val PREFS="omega7_signal_identity";private const val KEY_DEVICE_ID="signal_device_id";private const val VERIFIED_FILE="omega7_verified_devices.bin";private const val VERSION:Byte=1;private const val MAX_PLAINTEXT=16*1024;private const val MAX_ENVELOPE=256*1024
        fun open(context:Context):SignalE2eeEngine{val app=context.applicationContext;val p=app.getSharedPreferences(PREFS,Context.MODE_PRIVATE);val id=p.getInt(KEY_DEVICE_ID,0).takeIf{it in 1..127}?:run{val x=SecureRandom().nextInt(127)+1;check(p.edit().putInt(KEY_DEVICE_ID,x).commit());x};return SignalE2eeEngine(app,PersistentSignalProtocolStore.open(app),id)}
        private fun b64(v:ByteArray)=Base64.encodeToString(v,Base64.NO_WRAP or Base64.URL_SAFE);private fun unb64(v:String)=Base64.decode(v,Base64.NO_WRAP or Base64.URL_SAFE)
    }
}
