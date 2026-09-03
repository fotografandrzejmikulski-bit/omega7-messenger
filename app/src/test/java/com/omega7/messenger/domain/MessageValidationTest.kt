package com.omega7.messenger.domain

import org.junit.Test
import kotlin.test.assertFailsWith

class MessageValidationTest {
    @Test fun rejectsEmptyBody() {
        assertFailsWith<IllegalArgumentException> { Message("m", "Ty", "", 1L) }
    }

    @Test fun rejectsOversizedBody() {
        assertFailsWith<IllegalArgumentException> { Message("m", "Ty", "x".repeat(Message.MAX_BODY_LENGTH + 1), 1L) }
    }
}
