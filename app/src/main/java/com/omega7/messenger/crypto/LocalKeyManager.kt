package com.omega7.messenger.crypto
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
class LocalKeyManager {
 companion object { private const val STORE="AndroidKeyStore"; private const val ALIAS="omega7.local.aes256" }
 fun getOrCreateKey(): SecretKey {
  val ks=KeyStore.getInstance(STORE).apply{load(null)}
  (ks.getKey(ALIAS,null) as? SecretKey)?.let{return it}
  val g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,STORE)
  g.init(KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
  return g.generateKey()
 }
 fun destroyKey(){val ks=KeyStore.getInstance(STORE).apply{load(null)};if(ks.containsAlias(ALIAS))ks.deleteEntry(ALIAS)}
}
