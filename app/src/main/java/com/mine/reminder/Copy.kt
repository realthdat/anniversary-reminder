package com.mine.reminder

import android.content.Context
import java.text.NumberFormat

fun Mark.digits(): String = NumberFormat.getIntegerInstance().format(count)

fun Context.markWord(mark: Mark): String = resources.getQuantityString(
    when (mark.unit) {
        MarkUnit.YEAR -> R.plurals.unit_year
        MarkUnit.MONTH -> R.plurals.unit_month
        MarkUnit.WEEK -> R.plurals.unit_week
        MarkUnit.DAY -> R.plurals.unit_day
    },
    mark.count,
)

fun Context.markPhrase(mark: Mark): String = "${mark.digits()} ${markWord(mark)}"

fun Context.hitPhrase(hit: Hit): String = hit.labels.joinToString(" · ") { markPhrase(it) }

fun Context.earlyPhrase(days: Int, hit: Hit): String {
    val whenText = if (days == 1) getString(R.string.tomorrow) else getString(R.string.in_days, days)
    return "$whenText · ${hitPhrase(hit)}"
}
