package com.omega7.messenger.e2ee

import android.content.Context
import android.util.Base64
import com.omega7.messenger.data.EncryptedLocalStore
import org.json.JSONObject
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord
import org.signal.libsignal.protocol.state.IdentityKeyStore
import org.signal.libsignal.protocol.state.KyberPreKeyRecord
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SessionRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.state.impl.InMemorySignalProtocolStore
import java.util.UUID

/** Durable Signal state encrypted with Ω7's Android Keystore-backed local AES-GCM store. */
class PersistentSignalProtocolStore private constructor(private val stateStore:EncryptedLocalStore,identityKeyPair:IdentityKeyPair,registrationId:Int):InMemorySignalProtocolStore(identityKeyPair,registrationId){
 private val knownPreKeyIds=linkedSetOf<Int>();private val knownSessionAddresses=linkedMapOf<String,SignalProtocolAddress>();private val trustedIdentityBytes=linkedMapOf<String,ByteArray>();private val lock=Any()
 init{restore()}
 val identityKeyPair:IdentityKeyPair get()=getIdentityKeyPair();val localRegistrationId:Int get()=getLocalRegistrationId();fun firstAvailablePreKeyId():Int=synchronized(lock){knownPreKeyIds.firstOrNull{containsPreKey(it)}?:-1}
 override fun saveIdentity(a:SignalProtocolAddress,k:IdentityKey):IdentityKeyStore.IdentityChange=synchronized(lock){val c=super.saveIdentity(a,k);trustedIdentityBytes[a.toString()]=k.serialize().clone();persistLocked();c}
 override fun storePreKey(id:Int,r:PreKeyRecord)=synchronized(lock){super.storePreKey(id,r);knownPreKeyIds+=id;persistLocked()}
 override fun removePreKey(id:Int)=synchronized(lock){super.removePreKey(id);knownPreKeyIds-=id;persistLocked()}
 override fun storeSignedPreKey(id:Int,r:SignedPreKeyRecord)=synchronized(lock){super.storeSignedPreKey(id,r);persistLocked()}
 override fun removeSignedPreKey(id:Int)=synchronized(lock){super.removeSignedPreKey(id);persistLocked()}
 override fun storeKyberPreKey(id:Int,r:KyberPreKeyRecord)=synchronized(lock){super.storeKyberPreKey(id,r);persistLocked()}
 override fun markKyberPreKeyUsed(id:Int,signedId:Int,baseKey:ECPublicKey)=synchronized(lock){super.markKyberPreKeyUsed(id,signedId,baseKey);persistLocked()}
 override fun storeSession(a:SignalProtocolAddress,r:SessionRecord)=synchronized(lock){super.storeSession(a,r);knownSessionAddresses[a.toString()]=SignalProtocolAddress(a.getName(),a.getDeviceId());persistLocked()}
 override fun deleteSession(a:SignalProtocolAddress)=synchronized(lock){super.deleteSession(a);knownSessionAddresses.remove(a.toString())?.close();persistLocked()}
 override fun deleteAllSessions(name:String)=synchronized(lock){super.deleteAllSessions(name);knownSessionAddresses.filterValues{it.getName()==name}.keys.toList().forEach{knownSessionAddresses.remove(it)?.close()};persistLocked()}
 override fun storeSenderKey(sender:SignalProtocolAddress,distributionId:UUID,record:SenderKeyRecord){super.storeSenderKey(sender,distributionId,record)}
 private fun restore(){val blob=stateStore.load()?:return;synchronized(lock){try{val root=JSONObject(String(blob,Charsets.UTF_8));val pk=root.optJSONObject("preKeys")?:JSONObject();pk.keys().forEach{idText->val id=idText.toInt();super.storePreKey(id,PreKeyRecord(decode(pk.getString(idText))));knownPreKeyIds+=id};val sp=root.optJSONObject("signedPreKeys")?:JSONObject();sp.keys().forEach{idText->val id=idText.toInt();super.storeSignedPreKey(id,SignedPreKeyRecord(decode(sp.getString(idText))))};val ky=root.optJSONObject("kyberPreKeys")?:JSONObject();ky.keys().forEach{idText->val id=idText.toInt();super.storeKyberPreKey(id,KyberPreKeyRecord(decode(ky.getString(idText))))};val ss=root.optJSONObject("sessions")?:JSONObject();ss.keys().forEach{t->val name=t.substringBeforeLast('.');val id=t.substringAfterLast('.').toInt();val a=SignalProtocolAddress(name,id);super.storeSession(a,SessionRecord(decode(ss.getString(t))));knownSessionAddresses[t]=a};val tr=root.optJSONObject("trustedIdentities")?:JSONObject();tr.keys().forEach{t->val name=t.substringBeforeLast('.');val id=t.substringAfterLast('.').toInt();val a=SignalProtocolAddress(name,id);val b=decode(tr.getString(t));super.saveIdentity(a,IdentityKey(b));trustedIdentityBytes[t]=b.clone();b.fill(0)}}catch(e:Exception){throw IllegalStateException("Nie można odtworzyć stanu Signal Protocol; E2EE zostaje zablokowane.",e)}finally{blob.fill(0)}}}
 private fun persistLocked(){val root=JSONObject().put("format",1).put("identityKeyPair",encode(getIdentityKeyPair().serialize())).put("registrationId",getLocalRegistrationId());val pk=JSONObject();knownPreKeyIds.sorted().forEach{id->if(containsPreKey(id))pk.put(id.toString(),encode(loadPreKey(id).serialize()))};root.put("preKeys",pk);val sp=JSONObject();loadSignedPreKeys().forEach{r->sp.put(r.getId().toString(),encode(r.serialize()))};root.put("signedPreKeys",sp);val ky=JSONObject();loadKyberPreKeys().forEach{r->ky.put(r.getId().toString(),encode(r.serialize()))};root.put("kyberPreKeys",ky);val ss=JSONObject();knownSessionAddresses.forEach{(t,a)->if(containsSession(a))ss.put(t,encode(loadSession(a).serialize()))};root.put("sessions",ss);val tr=JSONObject();trustedIdentityBytes.forEach{(t,b)->tr.put(t,encode(b))};root.put("trustedIdentities",tr);stateStore.save(root.toString().toByteArray(Charsets.UTF_8))}
 private fun encode(v:ByteArray)=Base64.encodeToString(v,Base64.NO_WRAP or Base64.URL_SAFE);private fun decode(v:String)=Base64.decode(v,Base64.NO_WRAP or Base64.URL_SAFE)
 companion object{private const val FILE_NAME="omega7_signal_protocol.bin";fun open(context:Context):PersistentSignalProtocolStore{val encrypted=EncryptedLocalStore(context.applicationContext,fileName=FILE_NAME);val existing=encrypted.load();if(existing==null){val s=PersistentSignalProtocolStore(encrypted,IdentityKeyPair.generate(),org.signal.libsignal.protocol.util.KeyHelper.generateRegistrationId(true));s.persistLocked();return s};val root=try{JSONObject(String(existing,Charsets.UTF_8))}finally{existing.fill(0)};val b=Base64.decode(root.getString("identityKeyPair"),Base64.NO_WRAP or Base64.URL_SAFE);val id=try{IdentityKeyPair(b)}finally{b.fill(0)};return PersistentSignalProtocolStore(encrypted,id,root.getInt("registrationId"))}}
}
