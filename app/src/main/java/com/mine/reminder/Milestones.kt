package com.mine.reminder

import java.time.LocalDate
import java.time.LocalDateTime

data class DayCycle(val days: Int, val on: Boolean)

data class Event(
    val id: Long,
    val name: String,
    val start: LocalDate,
    val cycles: List<DayCycle> = defaultCycles(),
)

enum class MarkUnit { YEAR, MONTH, WEEK, DAY }

data class Mark(val count: Int, val unit: MarkUnit)

data class Hit(val date: LocalDate, val labels: List<Mark>)

data class Due(val event: Event, val hit: Hit)

val DEFAULT_DAY_MARKS = intArrayOf(100, 200, 300, 500, 1000)

fun defaultCycles(): List<DayCycle> = DEFAULT_DAY_MARKS.map { DayCycle(it, true) }
private val WEEK_MARKS = intArrayOf(100, 200, 500, 1000)

// 300 months is 25 years. Multiples of 12 are skipped so the anniversary is not sent twice.
private val MONTH_MARKS = intArrayOf(100, 200, 300, 500, 1000)

fun labelsOn(start: LocalDate, date: LocalDate, cycles: List<DayCycle> = defaultCycles()): List<Mark> {
    if (!date.isAfter(start)) return emptyList()
    val labels = ArrayList<Mark>(4)
    val years = date.year - start.year
    if (years >= 1 && start.plusYears(years.toLong()) == date) {
        labels += Mark(years, MarkUnit.YEAR)
    }
    for (n in MONTH_MARKS) {
        if (n % 12 == 0) continue
        if (start.plusMonths(n.toLong()) == date) labels += Mark(n, MarkUnit.MONTH)
    }
    for (n in WEEK_MARKS) {
        if (start.plusWeeks(n.toLong()) == date) labels += Mark(n, MarkUnit.WEEK)
    }
    val days = java.time.temporal.ChronoUnit.DAYS.between(start, date)
    if (days <= Int.MAX_VALUE && cycles.any { it.on && it.days.toLong() == days }) {
        labels += Mark(days.toInt(), MarkUnit.DAY)
    }
    return labels
}

fun nextHit(start: LocalDate, today: LocalDate, includeToday: Boolean, cycles: List<DayCycle> = defaultCycles()): Hit? {
    val earliest = if (includeToday) today else today.plusDays(1)
    val dates = ArrayList<LocalDate>(16)
    val yearSpan = (earliest.year - start.year).coerceAtLeast(1)
    var anniversary = start.plusYears(yearSpan.toLong())
    if (anniversary < earliest) anniversary = start.plusYears(yearSpan.toLong() + 1)
    if (anniversary.isAfter(start)) dates += anniversary
    for (cycle in cycles) if (cycle.on) dates += start.plusDays(cycle.days.toLong())
    for (n in WEEK_MARKS) dates += start.plusWeeks(n.toLong())
    for (n in MONTH_MARKS) if (n % 12 != 0) dates += start.plusMonths(n.toLong())
    for (date in dates.distinct().sorted()) {
        if (date < earliest) continue
        val labels = labelsOn(start, date, cycles)
        if (labels.isNotEmpty()) return Hit(date, labels)
    }
    return null
}

fun leadDate(hit: LocalDate, today: LocalDate, includeToday: Boolean, early: Int): LocalDate? {
    if (early <= 0) return null
    val soon = hit.minusDays(early.toLong())
    return if (soon > today || (soon == today && includeToday)) soon else null
}

fun nextAlarm(start: LocalDate, today: LocalDate, includeToday: Boolean, cycles: List<DayCycle>, early: Int): LocalDate? {
    val hit = nextHit(start, today, includeToday, cycles) ?: return null
    return leadDate(hit.date, today, includeToday, early) ?: hit.date
}

fun earliest(events: List<Event>, today: LocalDate, includeToday: (Long) -> Boolean): Due? =
    events.mapNotNull { event ->
        nextHit(event.start, today, includeToday(event.id), event.cycles)?.let { Due(event, it) }
    }.minByOrNull { it.hit.date }

fun firesImmediately(date: LocalDate, hour: Int, minute: Int, now: LocalDateTime): Boolean =
    !date.atTime(hour, minute).isAfter(now)

fun stillAhead(date: LocalDate, hour: Int, minute: Int, now: LocalDateTime): Boolean =
    now.isBefore(date.atTime(hour, minute))
