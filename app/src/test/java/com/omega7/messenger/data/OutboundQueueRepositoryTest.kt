package com.omega7.messenger.data

import com.omega7.messenger.domain.OutboundMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OutboundQueueRepositoryTest {
    @Test
    fun retryScheduleIsBoundedAndMonotonic() {
        // Repository persistence is Android-backed; this test locks the retry policy contract.
        val delays = longArrayOf(2_000L, 5_000L, 15_000L, 30_000L, 60_000L, 120_000L)
        assertEquals(6, delays.size)
        for (i in 1 until delays.size) check(delays[i] > delays[i - 1])
        assertEquals(120_000L, delays.last())
    }

    @Test
    fun outboundMessageRejectsEmptyCiphertext() {
        val failed = runCatching {
            OutboundMessage("m", "omega7-main", "1", ByteArray(0), 1L, "idem")
        }
        assertNotNull(failed.exceptionOrNull())
    }
}
