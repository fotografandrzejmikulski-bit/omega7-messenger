package com.omega7.messenger.security

object DeviceTrust {
    enum class State { UNKNOWN, VERIFIED, CHANGED, REVOKED }
    data class TrustedDevice(
        val deviceId: String,
        val displayName: String,
        val fingerprint: String,
        val state: State
    )
}
