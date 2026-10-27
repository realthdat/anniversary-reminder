package com.mine.reminder

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Decorative only: dates and countdown remain accessible Compose text. */
internal fun DrawScope.drawMilestoneCardArt() {
    if (size.width <= 0f || size.height <= 0f) return
    drawRect(Brush.linearGradient(
        listOf(Color(0xFF294C57), Color(0xFF1B3540), Color(0xFF142C37)),
        Offset.Zero, Offset(size.width, size.height),
    ))
    val halo = Offset(size.width * .94f, size.height * .22f)
    drawCircle(
        Brush.radialGradient(listOf(Color(0x266ED5CC), Color.Transparent), halo, size.width * .70f),
        size.width * .70f, halo,
    )
    val center = Offset(size.width * 1.10f, size.height * .44f)
    for (fraction in listOf(.34f, .46f, .58f)) {
        drawCircle(
            Brush.horizontalGradient(listOf(Color.Transparent, Color(0x26E7CF9E)), 0f, size.width),
            size.width * fraction, center, style = Stroke(.75.dp.toPx()),
        )
    }
    // A quiet left-hand veil keeps the countdown legible over the artwork.
    drawRect(Brush.horizontalGradient(listOf(Color(0xB3233F49), Color.Transparent)))
    drawRoundRect(
        Brush.linearGradient(listOf(Color(0x40E7CF9E), Color(0x08E7CF9E)), Offset.Zero, Offset(size.width, size.height)),
        cornerRadius = CornerRadius(26.dp.toPx()), style = Stroke(1.dp.toPx()),
    )
}
