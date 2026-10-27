package com.mine.reminder

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

internal data class Firework(val x: Float, val y: Float, val born: Float, val color: Color)
internal data class Ember(val shell: Firework, val angle: Float, val speed: Float, val life: Float, val size: Float, val phase: Float)

internal val fireworkShells = listOf(
    Firework(.50f, .19f, .55f, Color(0xFFFFD58A)),
    Firework(.24f, .25f, 1.12f, Color(0xFF9ADFFF)),
    Firework(.77f, .22f, 1.53f, Color(0xFFFFE7B6)),
    Firework(.34f, .15f, 1.96f, Color(0xFFFFD58A)),
    Firework(.67f, .17f, 2.35f, Color(0xFF9ADFFF)),
)

internal fun celebrationEmbers(): List<Ember> {
    val random = Random(42)
    return fireworkShells.flatMap { shell ->
        List(76) { n ->
            Ember(shell, (n * 2 * PI / 76 + random.nextFloat() * .035).toFloat(),
                .21f + random.nextFloat() * .43f, 1.55f + random.nextFloat() * .95f,
                .55f + random.nextFloat() * .65f, random.nextFloat() * 6.28f)
        }
    }
}

internal fun DrawScope.drawBeerToast(elapsed: Float) {
    val p = (elapsed / .85f).coerceIn(0f, 1f)
    val ease = 1f - (1f - p).let { it * it * it }
    val recoil = if (elapsed > .85f) sin(((elapsed - .85f) / .4f).coerceIn(0f, 1f) * PI).toFloat() else 0f
    val unit = minOf(size.width / 390f, size.height / 720f)
    val opacity = (elapsed / .25f).coerceIn(0f, 1f)
    for (side in listOf(-1f, 1f)) {
        withTransform({
            translate(size.width / 2 + side * (90 - 62 * ease + 5 * recoil) * unit, size.height * .68f)
            scale(unit * side, unit, Offset.Zero)
            rotate(-16 * ease + 7 * recoil, Offset.Zero)
        }) {
            drawRoundRect(Color(0xFFFFE9B5), Offset(19f, -20f), Size(24f, 32f), CornerRadius(10f), alpha = opacity, style = Stroke(5f))
            drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFFD98716), Color(0xFFFFD66D), Color(0xFFEFA624)), -24f, 24f),
                Offset(-24f, -32f), Size(48f, 66f), CornerRadius(9f), alpha = opacity)
            drawRoundRect(Color(0xFFFFF0C5), Offset(-24f, -32f), Size(48f, 66f), CornerRadius(9f), alpha = opacity, style = Stroke(2f))
            drawRoundRect(Color.White, Offset(-16f, -19f), Size(5f, 41f), CornerRadius(2f), alpha = opacity * .33f)
            for ((x, y, radius) in listOf(Triple(-17f,-32f,10f), Triple(-4f,-37f,12f), Triple(10f,-35f,11f), Triple(20f,-30f,8f))) {
                drawCircle(Color(0xFFFFF9E9), radius, Offset(x,y), alpha = opacity)
            }
            for (point in listOf(Offset(-3f,12f), Offset(10f,-9f), Offset(5f,24f))) drawCircle(Color(0xFFFFF4C3), 2f, point, alpha = opacity)
        }
    }
    val impact = ((elapsed - .85f) / .45f).coerceIn(0f, 1f)
    if (elapsed >= .85f && impact < 1f) {
        val origin = Offset(size.width / 2, size.height * .68f - 44 * unit)
        for (j in 0..4) {
            val angle = PI + j * PI / 4
            val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
            val radius = (14 + impact * 20) * unit
            drawLine(Color(0xFFFFE6A6).copy(alpha = 1 - impact), origin + direction * radius,
                origin + direction * (radius + 8 * unit), 2 * unit, StrokeCap.Round)
        }
    }
}

internal fun DrawScope.drawCelebration(elapsed: Float, embers: List<Ember>) {
    val reach = size.minDimension
    fun glow(point: Offset, color: Color, radius: Float, alpha: Float) {
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)), point, radius), radius, point)
    }
    for (shell in fireworkShells) {
        val origin = Offset(shell.x * size.width, shell.y * size.height)
        val age = elapsed - shell.born
        if (age in -.50f..0f) {
            val progress = (1f + age / .5f).coerceIn(0f, 1f)
            val head = origin + Offset(-reach * .04f * (1f - progress), reach * .34f * (1f - progress) * (1f - progress))
            for (j in 1..10) {
                val alpha = (1f - j / 11f) * progress
                drawCircle(shell.color.copy(alpha = alpha * .65f), (.5f + alpha * .5f).dp.toPx(), head + Offset(-j * reach * .0015f, j * reach * .006f))
            }
            glow(head, shell.color, 9.dp.toPx(), .35f * progress)
            drawCircle(Color(0xFFFFF5DE).copy(alpha = progress), 1.3.dp.toPx(), head)
        }
        if (age in 0f.. .5f) glow(origin, shell.color, reach * (.045f + age * .15f), .26f * (1f - age / .5f))
    }
    for (ember in embers) {
        val age = elapsed - ember.shell.born
        if (age <= 0f || age >= ember.life) continue
        fun position(t: Float): Offset {
            val travel = ember.speed * reach * (1f - exp(-1.35f * t)) / 1.35f
            return Offset(ember.shell.x * size.width + cos(ember.angle) * travel,
                ember.shell.y * size.height + sin(ember.angle) * travel * .86f + reach * .032f * t * t)
        }
        val head = position(age)
        val room = ((size.height * .43f - head.y) / (reach * .09f)).coerceIn(0f, 1f)
        val fade = (1f - age / ember.life).let { it * it } * room
        val shimmer = if (age > .65f) .65f + .35f * sin(age * 19f + ember.phase) else 1f
        val alpha = fade * shimmer
        // Sample the trajectory so trails curve naturally as gravity takes over.
        for (j in 0..4) {
            val t1 = (age - j * .035f).coerceAtLeast(0f)
            val t2 = (age - (j + 1) * .035f).coerceAtLeast(0f)
            drawLine(ember.shell.color.copy(alpha = alpha * (1f - j / 5f) * .8f), position(t2), position(t1),
                ember.size.dp.toPx(), StrokeCap.Round)
        }
        glow(head, ember.shell.color, (ember.size * 3.5f).dp.toPx(), alpha * .18f)
        drawCircle(ember.shell.color.copy(alpha = alpha), ember.size.dp.toPx(), head)
        drawCircle(Color(0xFFFFFAED).copy(alpha = alpha * .85f), (ember.size * .45f).dp.toPx(), head)
    }
}
