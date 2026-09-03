package com.omega7.messenger.domain

import android.content.Context
import com.omega7.messenger.e2ee.SignalE2eeEngine

/** Stable application boundary for audited Signal Protocol E2EE. */
interface E2eeEngine {
    fun encrypt(groupId: String, plaintext: ByteArray): ByteArray
    fun decrypt(groupId: String, ciphertext: ByteArray): ByteArray
}

/** Production adapter. It exposes only the application-level boundary; crypto remains in libsignal. */
class SignalE2eeEngineAdapter(context: Context) : E2eeEngine {
    private val engine = SignalE2eeEngine.open(context.applicationContext)

    override fun encrypt(groupId: String, plaintext: ByteArray): ByteArray =
        engine.encrypt(groupId, plaintext)

    override fun decrypt(groupId: String, ciphertext: ByteArray): ByteArray =
        engine.decrypt(groupId, ciphertext)

    fun implementation(): SignalE2eeEngine = engine
}

/** Explicit fail-closed implementation used until a production E2EE adapter is injected. */
class E2eeNotConfigured : E2eeEngine {
    override fun encrypt(groupId: String, plaintext: ByteArray): ByteArray =
        error("Warstwa E2EE nie została skonfigurowana.")

    override fun decrypt(groupId: String, ciphertext: ByteArray): ByteArray =
        error("Warstwa E2EE nie została skonfigurowana.")
}
