package com.omega7.messenger.security

/** Jedno źródło prawdy dla stanu sesji aplikacji. */
class SessionController {
    var state: SessionState = SessionState.LOCKED
        private set

    fun unlock() {
        check(state != SessionState.PANIC_WIPED) { "Aplikacja została wymazana." }
        state = SessionState.UNLOCKED
    }

    fun lock() {
        if (state != SessionState.PANIC_WIPED) state = SessionState.LOCKED
    }

    fun panicWipe() { state = SessionState.PANIC_WIPED }
}
