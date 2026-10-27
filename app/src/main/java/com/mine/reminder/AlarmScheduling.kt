package com.mine.reminder

/** Permission can be revoked between the capability check and the platform call. */
internal fun scheduleWithExactFallback(
    canScheduleExact: () -> Boolean,
    scheduleExact: () -> Unit,
    scheduleInexact: () -> Unit,
) {
    try {
        if (canScheduleExact()) {
            scheduleExact()
            return
        }
    } catch (_: SecurityException) {
        // Use the permission-free path; never crash startup or saving an event.
    }
    scheduleInexact()
}
