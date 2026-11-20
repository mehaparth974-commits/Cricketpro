package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.domain.CricketMatch
import com.example.domain.MatchComputedState
import com.example.ui.components.LivePulsePill
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun BroadcastStudioTab(
    match: CricketMatch,
    computed: MatchComputedState,
    isScorer: Boolean,
    onSaveStreamConfig: (streamUrl: String, rtmpServer: String, rtmpKey: String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraEnabled by remember { mutableStateOf(false) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var playStreamActive by remember { mutableStateOf(false) }

    var rtmpServer by remember(match.rtmpServer) {
        mutableStateOf(match.rtmpServer ?: "rtmp://live.cricketpro.app/ingest")
    }
    var rtmpKey by remember(match.rtmpKey) {
        mutableStateOf(match.rtmpKey ?: "cp_${match.id.take(8)}")
    }
    var streamUrl by remember(match.streamUrl) {
        mutableStateOf(match.streamUrl ?: "")
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasCameraPerm = results[Manifest.permission.CAMERA] == true
        if (hasCameraPerm) cameraEnabled = true
    }

    val active = computed.activeInnings

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // CameraX Pitchside Broadcast Preview with Live Score Ticker
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Pitchside Camera Broadcast", style = MaterialTheme.typography.titleMedium)
                    }
                    if (cameraEnabled) LivePulsePill("ON AIR")
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color(0xFF060B08)),
                    contentAlignment = Alignment.Center
                ) {
                    if (cameraEnabled && hasCameraPerm) {
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            update = { previewView ->
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                                cameraProviderFuture.addListener({
                                    try {
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.surfaceProvider = previewView.surfaceProvider
                                        }
                                        val selector = if (useFrontCamera) {
                                            CameraSelector.DEFAULT_FRONT_CAMERA
                                        } else {
                                            CameraSelector.DEFAULT_BACK_CAMERA
                                        }
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
                                    } catch (_: Exception) {
                                    }
                                }, ContextCompat.getMainExecutor(context))
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(Icons.Default.LiveTv, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                "Start CameraX Broadcast Preview with live score overlay",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                            Button(
                                onClick = {
                                    if (hasCameraPerm) {
                                        cameraEnabled = true
                                    } else {
                                        permLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                                    }
                                },
                                modifier = Modifier.testTag("start_camera_broadcast_button")
                            ) {
                                Text("Enable Camera Preview")
                            }
                        }
                    }

                    // Broadcast Score Ticker Overlay
                    Surface(
                        color = Color(0xCC070C09),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "${active.battingTeamName} ${active.totalRuns}/${active.wickets} (${active.oversDisplay})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${active.currentStriker}* | ${active.currentBowler}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                "CRR ${String.format(Locale.US, "%.2f", active.crr)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }
                }

                if (cameraEnabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { useFrontCamera = !useFrontCamera }) {
                            Icon(Icons.Default.Cameraswitch, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Flip Camera")
                        }
                        OutlinedButton(onClick = { cameraEnabled = false }) {
                            Text("Stop Preview")
                        }
                    }
                }
            }
        }

        // Media3 ExoPlayer Live Stream & Highlight Replay Viewer
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Live Stream & Replay Viewer (HLS / MP4)", style = MaterialTheme.typography.titleMedium)
                if (streamUrl.isNotBlank() && playStreamActive) {
                    val exoPlayer = remember(streamUrl) {
                        ExoPlayer.Builder(context).build().apply {
                            setMediaItem(MediaItem.fromUri(streamUrl))
                            prepare()
                            playWhenReady = true
                        }
                    }
                    DisposableEffect(exoPlayer) {
                        onDispose { exoPlayer.release() }
                    }
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                    OutlinedButton(onClick = { playStreamActive = false }) {
                        Text("Close Player")
                    }
                } else {
                    Text(
                        text = if (streamUrl.isBlank()) "No HLS/MP4 stream URL attached yet. Add a live or replay URL below."
                        else "Stream ready: $streamUrl",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (streamUrl.isNotBlank()) {
                        Button(
                            onClick = { playStreamActive = true },
                            modifier = Modifier.testTag("play_stream_button")
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Play Stream / Replay")
                        }
                    }
                }
            }
        }

        // OBS & RTMP Stream Configuration
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("OBS / RTMP & Stream Endpoint", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = rtmpServer,
                    onValueChange = { rtmpServer = it },
                    label = { Text("RTMP Ingest Server URL (for OBS)") },
                    singleLine = true,
                    readOnly = !isScorer,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rtmpKey,
                    onValueChange = { rtmpKey = it },
                    label = { Text("RTMP Stream Key") },
                    singleLine = true,
                    readOnly = !isScorer,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = streamUrl,
                    onValueChange = { streamUrl = it },
                    label = { Text("Viewer HLS (.m3u8) or MP4 Replay URL") },
                    singleLine = true,
                    readOnly = !isScorer,
                    modifier = Modifier.fillMaxWidth().testTag("stream_url_input")
                )
                if (isScorer) {
                    Button(
                        onClick = { onSaveStreamConfig(streamUrl, rtmpServer, rtmpKey) },
                        modifier = Modifier.fillMaxWidth().testTag("save_stream_config_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Save Stream Configuration")
                    }
                }
            }
        }
    }
}
