package com.omega7.messenger.security

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionControllerTest {
    @Test fun lockAndUnlock() {
        val s = SessionController()
        assertEquals(SessionState.LOCKED, s.state)
        s.unlock()
        assertEquals(SessionState.UNLOCKED, s.state)
        s.lock()
        assertEquals(SessionState.LOCKED, s.state)
    }

    @Test fun panicWipeIsTerminal() {
        val s = SessionController()
        s.unlock(); s.panicWipe(); s.lock()
        assertEquals(SessionState.PANIC_WIPED, s.state)
    }
}
