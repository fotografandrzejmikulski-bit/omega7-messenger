package com.omega7.messenger.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MessageTest {
    @Test fun localOnlyIsDistinctFromNetworkDeliveryStates() {
        assertNotEquals(Message.Status.LOCAL_ONLY, Message.Status.SENT)
        assertEquals("LOCAL_ONLY", Message.Status.LOCAL_ONLY.name)
    }
}
