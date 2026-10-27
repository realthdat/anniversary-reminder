package com.mine.reminder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

/** Always observable, including before the first frame arrives. */
internal class CelebrationTimeline {
    private var startedAt: Long? = null
    var elapsed by mutableFloatStateOf(0f)
        private set

    fun onFrame(frameNanos: Long) {
        val start = startedAt ?: frameNanos.also { startedAt = it }
        elapsed = ((frameNanos - start) / 1_000_000_000f).coerceAtLeast(0f)
    }
}
