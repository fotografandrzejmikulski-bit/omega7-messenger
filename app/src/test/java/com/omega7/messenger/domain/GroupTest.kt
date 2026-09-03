package com.omega7.messenger.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GroupTest {
    @Test fun groupAllowsAtMostSevenMembers() {
        val group = Group("g", "Test", 7)
        assertEquals(7, group.memberCount)
        assertEquals(7, group.maxMembers)
    }

    @Test(expected = IllegalArgumentException::class)
    fun groupRejectsEightMembers() {
        Group("g", "Test", 8)
    }
}
