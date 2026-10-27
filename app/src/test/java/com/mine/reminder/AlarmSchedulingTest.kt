package com.mine.reminder

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmSchedulingTest {
    @Test fun savingAndReopeningWithoutPermissionUsesInexactAlarms() {
        var fallbackCalls = 0
        repeat(2) { // Saving and then onResume with the persisted event.
            scheduleWithExactFallback(
                { false },
                { throw AssertionError("Must not call the restricted API") },
                { fallbackCalls++ },
            )
        }
        assertEquals(2, fallbackCalls)
    }

    @Test fun permissionGrantedPreservesExactAlarm() {
        var exactCalls = 0
        scheduleWithExactFallback({ true }, { exactCalls++ }, { throw AssertionError("Unexpected fallback") })
        assertEquals(1, exactCalls)
    }

    @Test fun permissionRevokedAfterCheckDoesNotCrash() {
        var fallbackCalls = 0
        scheduleWithExactFallback({ true }, { throw SecurityException("Permission revoked") }, { fallbackCalls++ })
        assertEquals(1, fallbackCalls)
    }

    @Test fun laterPermissionGrantRestoresExactScheduling() {
        var granted = false
        val calls = mutableListOf<String>()
        fun schedule() = scheduleWithExactFallback({ granted }, { calls += "exact" }, { calls += "inexact" })
        schedule()
        granted = true
        schedule()
        assertEquals(listOf("inexact", "exact"), calls)
    }
}
