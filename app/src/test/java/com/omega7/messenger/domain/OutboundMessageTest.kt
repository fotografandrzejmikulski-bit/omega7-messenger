package com.omega7.messenger.domain

import org.junit.Test
import kotlin.test.assertFailsWith

class OutboundMessageTest {
    @Test fun rejectsEmptyCiphertext() {
        assertFailsWith<IllegalArgumentException> {
            OutboundMessage("m", "g", "d", byteArrayOf(), 1L, "idk")
        }
    }

    @Test fun rejectsOversizedCiphertext() {
        assertFailsWith<IllegalArgumentException> {
            OutboundMessage("m", "g", "d", ByteArray(OutboundMessage.MAX_CIPHERTEXT + 1), 1L, "idk")
        }
    }
}
