package com.mine.reminder

import androidx.compose.runtime.snapshots.Snapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CelebrationTimelineTest {
    @Test fun initialFrameReadsObservableStateBeforeClockStarts() {
        val timeline = CelebrationTimeline()
        val reads = mutableSetOf<Any>()
        Snapshot.observe(readObserver = { reads += it }) {
            assertEquals(0f, timeline.elapsed, 0f)
        }
        assertTrue("First render must subscribe to animation state", reads.isNotEmpty())
    }

    @Test fun framesAdvanceThroughLaunchClinkAndFinish() {
        val timeline = CelebrationTimeline()
        val start = 42_000_000_000L
        timeline.onFrame(start)
        assertEquals(0f, timeline.elapsed, 0f)
        for (seconds in listOf(.1f, .55f, .85f, 2.35f, 5.5f)) {
            timeline.onFrame(start + (seconds * 1_000_000_000L).toLong())
            assertEquals(seconds, timeline.elapsed, .00001f)
        }
    }

    @Test fun newOverlayStartsFromZeroForReplay() {
        val first = CelebrationTimeline()
        first.onFrame(0L)
        first.onFrame(5_500_000_000L)
        val replay = CelebrationTimeline()
        replay.onFrame(20_000_000_000L)
        assertEquals(0f, replay.elapsed, 0f)
        replay.onFrame(20_500_000_000L)
        assertEquals(.5f, replay.elapsed, 0f)
    }
}
