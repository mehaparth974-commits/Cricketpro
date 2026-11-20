package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.domain.BallEvent
import com.example.domain.ExtraType
import com.example.ui.theme.LocalCricketColors

@Composable
fun StadiumHeroBanner(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    badgeText: String? = null
) {
    val primary = MaterialTheme.colorScheme.primary
    val cricketColors = LocalCricketColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF06150E),
                        Color(0xFF0A2417),
                        Color(0xFF040A07)
                    )
                )
            )
            .border(1.dp, primary.copy(alpha = 0.28f), MaterialTheme.shapes.large)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            // Stadium floodlight glow arcs
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(w * 0.85f, h * 0.15f),
                    radius = w * 0.55f
                ),
                center = Offset(w * 0.85f, h * 0.15f),
                radius = w * 0.55f
            )
            // Pitch boundary ovals
            drawOval(
                color = cricketColors.pitchCrease.copy(alpha = 0.16f),
                topLeft = Offset(w * 0.52f, h * 0.22f),
                size = androidx.compose.ui.geometry.Size(w * 0.55f, h * 0.95f),
                style = Stroke(width = 2f)
            )
            drawOval(
                color = primary.copy(alpha = 0.25f),
                topLeft = Offset(w * 0.62f, h * 0.36f),
                size = androidx.compose.ui.geometry.Size(w * 0.35f, h * 0.65f),
                style = Stroke(width = 1.5f)
            )
            // Pitch strip
            drawRect(
                color = primary.copy(alpha = 0.18f),
                topLeft = Offset(w * 0.77f, h * 0.50f),
                size = androidx.compose.ui.geometry.Size(w * 0.05f, h * 0.38f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!badgeText.isNullOrBlank()) {
                Surface(
                    color = primary.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, primary.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFBBD3C5),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TeamCrestBadge(
    shortName: String,
    logoData: String? = null,
    size: Dp = 44.dp,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val decodedBitmap = remember(logoData) {
        if (!logoData.isNullOrBlank() && logoData.startsWith("data:image")) {
            runCatching {
                val base64Part = logoData.substringAfter("base64,")
                val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        } else null
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(accentColor.copy(alpha = 0.25f), MaterialTheme.colorScheme.surfaceContainerHigh)
                )
            )
            .border(1.5.dp, accentColor.copy(alpha = 0.65f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when {
            decodedBitmap != null -> {
                Image(
                    bitmap = decodedBitmap,
                    contentDescription = "$shortName logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            !logoData.isNullOrBlank() && logoData.startsWith("http") -> {
                AsyncImage(
                    model = logoData,
                    contentDescription = "$shortName logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Text(
                    text = shortName.take(3).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun LivePulsePill(label: String = "LIVE") {
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val liveColor = LocalCricketColors.current.liveBadge
    Surface(
        color = liveColor.copy(alpha = 0.16f),
        shape = RoundedCornerShape(50),
        border = androidx.compose.foundation.BorderStroke(1.dp, liveColor.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(liveColor.copy(alpha = alpha))
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = liveColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SyncStatusBadge(pendingCount: Int) {
    val synced = pendingCount == 0
    val color = if (synced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (synced) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                contentDescription = if (synced) "Cloud synced" else "Pending offline sync",
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (synced) "Synced" else "$pendingCount offline",
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@Composable
fun CricketBallChip(
    ball: BallEvent,
    modifier: Modifier = Modifier
) {
    val cricketColors = LocalCricketColors.current
    val (bg, fg) = when {
        ball.wicket -> cricketColors.wicket to Color.White
        ball.runsScored == 6 -> cricketColors.boundarySix to Color.Black
        ball.runsScored == 4 -> cricketColors.boundaryFour to Color.Black
        ball.extraTypeEnum != ExtraType.NONE -> cricketColors.extraAmber.copy(alpha = 0.24f) to cricketColors.extraAmber
        ball.runsScored == 0 -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }

    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(bg)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ball.badgeLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            maxLines = 1
        )
    }
}

@Composable
fun CricketLoadingCard(message: String = "Loading live telemetry...") {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CricketEmptyStateCard(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SportsCricket,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier.testTag("empty_state_action_button")
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun CricketErrorCard(
    message: String,
    onRetry: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Something went wrong",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (onRetry != null) {
                Spacer(Modifier.width(4.dp))
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}
