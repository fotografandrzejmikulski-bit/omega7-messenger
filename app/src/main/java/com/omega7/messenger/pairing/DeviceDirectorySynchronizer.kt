package com.omega7.messenger.pairing

import android.content.Context
import com.omega7.messenger.data.DeviceTrustRepository
import com.omega7.messenger.network.RelayKeyClient
import com.omega7.messenger.security.DeviceTrust
import java.security.MessageDigest
import java.util.Base64

/**
 * Reconciles relay membership with the local trust registry without silently
 * promoting newly discovered devices to VERIFIED.
 */
class DeviceDirectorySynchronizer(context: Context) {
    private val trust = DeviceTrustRepository(context.applicationContext)

    fun reconcile(entries: List<RelayKeyClient.DeviceDirectoryEntry>): Result<List<DeviceTrust.TrustedDevice>> = runCatching {
        require(entries.size in 1..7) { "Nieprawidłowa liczba urządzeń." }
        require(entries.map { it.deviceId }.distinct().size == entries.size) { "Duplikaty urządzeń." }

        val existing = trust.list().associateBy { it.deviceId }
        val activeIds = entries.map { it.deviceId.toString() }.toSet()
        val result = entries.map { entry ->
            val id = entry.deviceId.toString()
            val fingerprint = fingerprint(entry.identityKeyBase64)
            when (val old = existing[id]) {
                null -> DeviceTrust.TrustedDevice(id, "Urządzenie #${entry.deviceId}", fingerprint, DeviceTrust.State.UNKNOWN)
                else -> when {
                    old.fingerprint == fingerprint && old.state == DeviceTrust.State.VERIFIED -> old
                    old.fingerprint == fingerprint && old.state != DeviceTrust.State.CHANGED -> old
                    old.fingerprint == fingerprint -> old.copy(state = DeviceTrust.State.UNKNOWN)
                    else -> old.copy(fingerprint = fingerprint, state = DeviceTrust.State.CHANGED)
                }
            }
        }

        result.forEach { trust.put(it) }
        existing.values.filter { it.deviceId !in activeIds && it.state != DeviceTrust.State.REVOKED }
            .forEach { trust.put(it.copy(state = DeviceTrust.State.REVOKED)) }
        result
    }

    private fun fingerprint(identityKeyBase64: String): String {
        val raw = try { Base64.getDecoder().decode(identityKeyBase64) }
        catch (_: IllegalArgumentException) { throw IllegalArgumentException("Nieprawidłowy identity key.") }
        require(raw.isNotEmpty() && raw.size <= 4096) { "Nieprawidłowy identity key." }
        return MessageDigest.getInstance("SHA-256").digest(raw).joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
