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

fun anniversaryPhrase(hit: Hit): String = hit.labels.joinToString(" and ") { mark ->
    val unit = when (mark.unit) {
        MarkUnit.YEAR -> if (mark.count == 1) "year" else "years"
        MarkUnit.MONTH -> if (mark.count == 1) "month" else "months"
        MarkUnit.WEEK -> if (mark.count == 1) "week" else "weeks"
        MarkUnit.DAY -> if (mark.count == 1) "day" else "days"
    }
    "${mark.count} $unit"
}

fun congratsLine(hit: Hit): String = "Congratulations on your ${anniversaryPhrase(hit)} anniversary"

fun earlyNotice(days: Int, hit: Hit): String {
    val mark = anniversaryPhrase(hit)
    return if (days == 1) "Your $mark anniversary is tomorrow" else "Your $mark anniversary is in $days days"
}
