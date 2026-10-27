package com.mine.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MilestonesTest {
    @Test
    fun feb29AnniversaryLandsOnFeb28() {
        val start = LocalDate.of(2024, 2, 29)
        val hit = nextHit(start, LocalDate.of(2025, 2, 28), includeToday = true)
        assertEquals(LocalDate.of(2025, 2, 28), hit!!.date)
        assertEquals(listOf(Mark(1, MarkUnit.YEAR)), hit.labels)
    }

    @Test
    fun twelveMonthsIsOnlyTheAnniversary() {
        val start = LocalDate.of(2020, 6, 15)
        assertEquals(listOf(Mark(1, MarkUnit.YEAR)), labelsOn(start, start.plusYears(1)))
    }

    @Test
    fun threeHundredMonthsIsThe25YearAnniversary() {
        val start = LocalDate.of(2000, 1, 31)
        val at = start.plusMonths(300)
        assertEquals(start.plusYears(25), at)
        assertEquals(listOf(Mark(25, MarkUnit.YEAR)), labelsOn(start, at))
    }

    @Test
    fun roundMarksKeepTheirOwnLabels() {
        val start = LocalDate.of(2020, 1, 1)
        assertEquals(listOf(Mark(100, MarkUnit.DAY)), labelsOn(start, start.plusDays(100)))
        assertEquals(listOf(Mark(1000, MarkUnit.DAY)), labelsOn(start, start.plusDays(1000)))
        assertEquals(listOf(Mark(100, MarkUnit.WEEK)), labelsOn(start, start.plusWeeks(100)))
        assertEquals(listOf(Mark(100, MarkUnit.MONTH)), labelsOn(start, start.plusMonths(100)))
    }

    @Test
    fun sameDayLabelsShareOneNotification() {
        val start = LocalDate.of(2016, 1, 31)
        val dates = (1..40).map { start.plusYears(it.toLong()) } +
            listOf(100, 200, 300, 500, 1000, 1500, 2000, 3000, 5000, 10000).map { start.plusDays(it.toLong()) } +
            listOf(100, 200, 500, 1000).map { start.plusWeeks(it.toLong()) } +
            listOf(100, 200, 500, 1000).map { start.plusMonths(it.toLong()) }
        val collision = dates.firstOrNull { labelsOn(start, it).size > 1 } ?: return
        val hit = nextHit(start, collision, includeToday = true)
        assertEquals(collision, hit!!.date)
        assertEquals(labelsOn(start, collision), hit.labels)
    }

    @Test
    fun todayAlreadyPassedIsSkippedOnceNotified() {
        val today = LocalDate.of(2026, 10, 27)
        val start = today.minusDays(100)
        assertEquals(today, nextHit(start, today, includeToday = true)!!.date)
        assertTrue(nextHit(start, today, includeToday = false)!!.date.isAfter(today))
        assertTrue(firesImmediately(today, 9, 0, today.atTime(9, 0)))
        assertFalse(firesImmediately(today, 9, 0, today.atTime(8, 59)))
        assertTrue(stillAhead(today, 9, 0, today.atTime(8, 59)))
        assertFalse(stillAhead(today, 9, 0, today.atTime(9, 0)))
        assertEquals(today, nextHit(start, today, stillAhead(today, 9, 0, today.atTime(8, 59)))!!.date)
    }

    @Test
    fun pastDayMarkIsNotReminded() {
        val start = LocalDate.of(2003, 1, 30)
        val cycles = listOf(DayCycle(1000, true), DayCycle(10000, true), DayCycle(1200, false))
        val today = LocalDate.of(2026, 10, 27)
        assertEquals(LocalDate.of(2005, 10, 26), start.plusDays(1000))
        assertEquals(LocalDate.of(2030, 6, 17), start.plusDays(10000))
        val future = cycles.filter { it.on }.map { start.plusDays(it.days.toLong()) }.filter { it.isAfter(today) }
        assertEquals(listOf(LocalDate.of(2030, 6, 17)), future)
        val hit = nextHit(start, today, includeToday = false, cycles)!!
        assertTrue(hit.date.isAfter(today))
        assertFalse(hit.labels.any { it == Mark(1000, MarkUnit.DAY) || it == Mark(1200, MarkUnit.DAY) })
    }

    @Test
    fun earlyReminderFiresBeforeTheDayUnlessThatLeadHasPassed() {
        val today = LocalDate.of(2026, 10, 27)
        val start = LocalDate.of(2024, 2, 14)
        assertEquals(LocalDate.of(2026, 11, 10), start.plusDays(1000))
        assertEquals(LocalDate.of(2026, 11, 7), nextAlarm(start, today, includeToday = true, defaultCycles(), 3))
        assertEquals(LocalDate.of(2026, 11, 10), nextAlarm(start, today, includeToday = true, defaultCycles(), 0))
        val soon = today.plusDays(2).minusDays(100)
        assertEquals(today.plusDays(2), nextAlarm(soon, today, includeToday = true, defaultCycles(), 3))
        val leadToday = today.plusDays(3).minusDays(100)
        assertEquals(today.plusDays(3), nextAlarm(leadToday, today, includeToday = false, defaultCycles(), 3))
        assertEquals(today, nextAlarm(leadToday, today, includeToday = true, defaultCycles(), 3))
    }

    @Test
    fun earliestEventWins() {
        val today = LocalDate.of(2026, 10, 27)
        val soon = Event(1, "A", today.minusDays(100))
        val later = Event(2, "B", today.minusDays(50))
        val due = earliest(listOf(later, soon), today) { true }
        assertEquals(1L, due!!.event.id)
        assertEquals(today, due.hit.date)
        assertEquals(listOf(Mark(100, MarkUnit.DAY)), due.hit.labels)
    }
}
