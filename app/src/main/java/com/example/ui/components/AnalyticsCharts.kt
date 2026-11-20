package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.BallEvent
import com.example.domain.OverSummary
import com.example.domain.PartnershipStat
import com.example.ui.theme.LocalCricketColors
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun WagonWheelCanvas(
    balls: List<BallEvent>,
    modifier: Modifier = Modifier,
    selectedAngleDeg: Int? = null,
    onSelectAngle: ((Int) -> Unit)? = null
) {
    val cricketColors = LocalCricketColors.current
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val creaseColor = cricketColors.pitchCrease

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .aspectRatio(1f)
                .clip(CircleShape)
                .pointerInput(onSelectAngle) {
                    if (onSelectAngle != null) {
                        detectTapGestures { offset ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val rad = atan2((offset.y - cy).toDouble(), (offset.x - cx).toDouble())
                            var deg = Math.toDegrees(rad).toInt() + 90
                            if (deg < 0) deg += 360
                            onSelectAngle(deg % 360)
                        }
                    }
                }
        ) {
            val radius = min(size.width, size.height) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Outfield turf circles
            drawCircle(color = cricketColors.pitchTurf, radius = radius, center = center)
            drawCircle(
                color = creaseColor,
                radius = radius * 0.96f,
                center = center,
                style = Stroke(width = 2.5f)
            )
            // 30-yard inner circle
            drawCircle(
                color = creaseColor.copy(alpha = 0.55f),
                radius = radius * 0.55f,
                center = center,
                style = Stroke(width = 1.5f)
            )

            // 8 sector lines
            for (i in 0 until 8) {
                val angleRad = Math.toRadians((i * 45 - 90).toDouble())
                val endX = center.x + (radius * cos(angleRad)).toFloat()
                val endY = center.y + (radius * sin(angleRad)).toFloat()
                drawLine(
                    color = creaseColor.copy(alpha = 0.25f),
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 1f
                )
            }

            // Center pitch rectangle
            drawRect(
                color = surfaceColor.copy(alpha = 0.45f),
                topLeft = Offset(center.x - radius * 0.05f, center.y - radius * 0.14f),
                size = Size(radius * 0.10f, radius * 0.28f)
            )

            // Selected scorer angle indicator
            if (selectedAngleDeg != null) {
                val selRad = Math.toRadians((selectedAngleDeg - 90).toDouble())
                val sx = center.x + (radius * 0.90f * cos(selRad)).toFloat()
                val sy = center.y + (radius * 0.90f * sin(selRad)).toFloat()
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = center,
                    end = Offset(sx, sy),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }

            // Plot ball shot rays
            for (ball in balls) {
                if (ball.runsScored <= 0) continue
                val angleRad = Math.toRadians((ball.shotAngleDeg - 90).toDouble())
                val dist = (ball.shotDistanceNorm.toFloat().coerceIn(0.25f, 0.96f)) * radius
                val ex = center.x + (dist * cos(angleRad)).toFloat()
                val ey = center.y + (dist * sin(angleRad)).toFloat()
                val rayColor = when (ball.runsScored) {
                    6 -> cricketColors.boundarySix
                    4 -> cricketColors.boundaryFour
                    else -> Color.White.copy(alpha = 0.8f)
                }
                drawLine(
                    color = rayColor,
                    start = center,
                    end = Offset(ex, ey),
                    strokeWidth = if (ball.runsScored >= 4) 4.5f else 2.5f,
                    cap = StrokeCap.Round
                )
                drawCircle(color = rayColor, radius = if (ball.runsScored >= 4) 5f else 3f, center = Offset(ex, ey))
            }
        }

        // Legend
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(Color.White, "1s/2s/3s")
            LegendDot(cricketColors.boundaryFour, "Fours")
            LegendDot(cricketColors.boundarySix, "Sixes")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(color))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ManhattanChartCanvas(
    overs: List<OverSummary>,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val wicketColor = LocalCricketColors.current.wicket
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    if (overs.isEmpty()) {
        Text(
            text = "Manhattan chart will appear after the first over.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val maxRuns = (overs.maxOfOrNull { it.runs } ?: 12).coerceAtLeast(10)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val w = size.width
            val h = size.height
            val bottomPad = 16f
            val topPad = 18f
            val chartH = h - bottomPad - topPad

            // Baseline & mid guide
            drawLine(gridColor, Offset(0f, h - bottomPad), Offset(w, h - bottomPad), strokeWidth = 2f)
            drawLine(gridColor.copy(alpha = 0.2f), Offset(0f, topPad + chartH / 2f), Offset(w, topPad + chartH / 2f), strokeWidth = 1f)

            val slotW = w / overs.size.coerceAtLeast(1)
            val barW = (slotW * 0.62f).coerceIn(8f, 34f)

            overs.forEachIndexed { idx, over ->
                val ratio = over.runs.toFloat() / maxRuns.toFloat()
                val barH = (chartH * ratio).coerceAtLeast(4f)
                val x = idx * slotW + (slotW - barW) / 2f
                val y = (h - bottomPad) - barH
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(x, y),
                    size = Size(barW, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
                if (over.wickets > 0) {
                    repeat(over.wickets.coerceAtMost(3)) { wIdx ->
                        drawCircle(
                            color = wicketColor,
                            radius = 5.5f,
                            center = Offset(x + barW / 2f, (y - 10f - wIdx * 11f).coerceAtLeast(6f))
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Ov 1", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Max over: $maxRuns runs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Ov ${overs.last().overNumber}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun WormGraphCanvas(
    inn1Overs: List<OverSummary>,
    inn2Overs: List<OverSummary>,
    team1Label: String,
    team2Label: String,
    modifier: Modifier = Modifier
) {
    val team1Color = MaterialTheme.colorScheme.primary
    val team2Color = MaterialTheme.colorScheme.tertiary
    val wicketColor = LocalCricketColors.current.wicket
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    if (inn1Overs.isEmpty() && inn2Overs.isEmpty()) {
        Text(
            text = "Worm progression graph will populate as overs are bowled.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val maxOvers = maxOf(inn1Overs.size, inn2Overs.size, 1)
    val maxScore = maxOf(
        inn1Overs.maxOfOrNull { it.cumulativeRuns } ?: 0,
        inn2Overs.maxOfOrNull { it.cumulativeRuns } ?: 0,
        20
    )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val w = size.width
            val h = size.height
            val pad = 12f
            val chartW = w - pad * 2
            val chartH = h - pad * 2

            drawLine(gridColor, Offset(pad, h - pad), Offset(w - pad, h - pad), strokeWidth = 2f)
            drawLine(gridColor, Offset(pad, pad), Offset(pad, h - pad), strokeWidth = 2f)

            fun plotSeries(series: List<OverSummary>, lineColor: Color) {
                if (series.isEmpty()) return
                val path = Path()
                path.moveTo(pad, h - pad)
                series.forEachIndexed { idx, over ->
                    val x = pad + ((idx + 1).toFloat() / maxOvers.toFloat()) * chartW
                    val y = (h - pad) - (over.cumulativeRuns.toFloat() / maxScore.toFloat()) * chartH
                    path.lineTo(x, y)
                }
                drawPath(path = path, color = lineColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
                series.forEachIndexed { idx, over ->
                    if (over.wickets > 0) {
                        val x = pad + ((idx + 1).toFloat() / maxOvers.toFloat()) * chartW
                        val y = (h - pad) - (over.cumulativeRuns.toFloat() / maxScore.toFloat()) * chartH
                        drawCircle(color = wicketColor, radius = 6f, center = Offset(x, y))
                    }
                }
            }

            plotSeries(inn1Overs, team1Color)
            plotSeries(inn2Overs, team2Color)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(team1Color, team1Label)
            if (inn2Overs.isNotEmpty()) LegendDot(team2Color, team2Label)
            LegendDot(wicketColor, "Wicket")
        }
    }
}

@Composable
fun RunRateGraphCanvas(
    inn1Overs: List<OverSummary>,
    inn2Overs: List<OverSummary>,
    team1Label: String,
    team2Label: String,
    modifier: Modifier = Modifier
) {
    val team1Color = MaterialTheme.colorScheme.primary
    val team2Color = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    if (inn1Overs.isEmpty() && inn2Overs.isEmpty()) return

    val maxOvers = maxOf(inn1Overs.size, inn2Overs.size, 1)
    val maxRr = maxOf(
        inn1Overs.maxOfOrNull { it.runRateAtOver } ?: 0.0,
        inn2Overs.maxOfOrNull { it.runRateAtOver } ?: 0.0,
        10.0
    ).toFloat()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val w = size.width
            val h = size.height
            val pad = 12f
            val chartW = w - pad * 2
            val chartH = h - pad * 2

            drawLine(gridColor, Offset(pad, h - pad), Offset(w - pad, h - pad), strokeWidth = 2f)

            fun plotRr(series: List<OverSummary>, lineColor: Color) {
                if (series.isEmpty()) return
                val path = Path()
                series.forEachIndexed { idx, over ->
                    val x = pad + ((idx + 1).toFloat() / maxOvers.toFloat()) * chartW
                    val y = (h - pad) - (over.runRateAtOver.toFloat() / maxRr) * chartH
                    if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path = path, color = lineColor, style = Stroke(width = 3.5f, cap = StrokeCap.Round))
            }

            plotRr(inn1Overs, team1Color)
            plotRr(inn2Overs, team2Color)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(team1Color, "$team1Label CRR")
            if (inn2Overs.isNotEmpty()) LegendDot(team2Color, "$team2Label CRR")
        }
    }
}

@Composable
fun PartnershipBarsCard(partnerships: List<PartnershipStat>) {
    val primary = MaterialTheme.colorScheme.primary
    val maxRuns = (partnerships.maxOfOrNull { it.runs } ?: 1).coerceAtLeast(1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Partnerships", style = MaterialTheme.typography.titleMedium)
            if (partnerships.isEmpty()) {
                Text(
                    "No partnerships recorded yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                partnerships.forEach { p ->
                    val frac = (p.runs.toFloat() / maxRuns.toFloat()).coerceIn(0.08f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${p.wicketLabel}: ${p.batter1} & ${p.batter2}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${p.runs} (${p.balls}b)",
                                style = MaterialTheme.typography.labelMedium,
                                color = primary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(frac)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(primary)
                            )
                        }
                    }
                }
            }
        }
    }
}
