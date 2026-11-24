package com.example.ui.components

import android.app.Activity
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.example.data.VideoNoteEntity
import com.example.data.VideoShotEntity
import kotlinx.coroutines.isActive
import java.io.File

// Design Tokens - Quiet Cinema
private val GoldAccent = Color(0xFFD4AF37)
private val PrimaryControlText = Color(0xFFF5F5F5)
private val SecondaryControlText = Color(0xFFAAAAAA)
private val InactiveTrackColor = Color.White.copy(alpha = 0.22f)
private val BufferedTrackColor = Color.White.copy(alpha = 0.42f)
private val MainPlayButtonSurface = Color.White.copy(alpha = 0.14f)
private val OverlaySurfaceColor = Color.Black.copy(alpha = 0.65f)
private val HairlineBorderColor = Color.White.copy(alpha = 0.12f)
private val MenuBackgroundColor = Color(0xFF1A1A1A)

data class TimelineMarker(
    val id: String,
    val timestampMs: Long,
    val isShot: Boolean,
    val text: String,
    val imagePath: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerOverlayControls(
    showControls: Boolean,
    title: String,
    isPlaying: Boolean,
    playbackSpeed: Float,
    isLongPressSpeedUp: Boolean,
    currentPositionProvider: () -> Long,
    bufferedPositionProvider: () -> Long,
    totalDuration: Long,
    exoPlayer: ExoPlayer,
    context: Context,
    activity: Activity?,
    videoShots: List<VideoShotEntity> = emptyList(),
    videoNotes: List<VideoNoteEntity> = emptyList(),
    onBack: () -> Unit,
    onStudyPdfClick: () -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onCaptureVideoShot: () -> Unit,
    onOpenVideoShots: () -> Unit,
    onToggleAiChat: () -> Unit,
    onToggleNotes: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onInteraction: () -> Unit,
    onBackgroundClick: () -> Unit
) {
    AnimatedVisibility(
        visible = showControls,
        enter = fadeIn(tween(160)),
        exit = fadeOut(tween(140)),
        modifier = Modifier.fillMaxSize()
    ) {
        var showSettingsMenu by remember { mutableStateOf(false) }
        var showSpeedDialog by remember { mutableStateOf(false) }
        var wasPlayingBeforeMenu by remember { mutableStateOf(false) }

        var localPosition by remember { mutableLongStateOf(currentPositionProvider()) }
        var bufferedPosition by remember { mutableLongStateOf(bufferedPositionProvider()) }
        var isDraggingSlider by remember { mutableStateOf(false) }
        var dragPosition by remember { mutableFloatStateOf(0f) }
        var selectedMarker by remember { mutableStateOf<TimelineMarker?>(null) }
        var showRemainingTime by remember { mutableStateOf(false) }

        val markers = remember(videoShots, videoNotes) {
            val shotMarkers = videoShots.map {
                TimelineMarker(
                    id = "shot_${it.id}",
                    timestampMs = it.timestamp,
                    isShot = true,
                    text = "Video Shot",
                    imagePath = it.imagePath
                )
            }
            val noteMarkers = videoNotes.map {
                TimelineMarker(
                    id = "note_${it.id}",
                    timestampMs = it.timestamp,
                    isShot = false,
                    text = it.noteText.take(60),
                    imagePath = null
                )
            }
            (shotMarkers + noteMarkers).sortedBy { it.timestampMs }
        }

        LaunchedEffect(isPlaying, isDraggingSlider) {
            while (isActive) {
                if (!isDraggingSlider) {
                    localPosition = currentPositionProvider()
                }
                bufferedPosition = bufferedPositionProvider()
                kotlinx.coroutines.delay(500)
            }
        }

        LaunchedEffect(Unit) {
            localPosition = currentPositionProvider()
            bufferedPosition = bufferedPositionProvider()
        }

        val displayValue = if (isDraggingSlider) {
            dragPosition
        } else {
            if (totalDuration > 0) (localPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f) else 0f
        }
        val bufferedValue = if (totalDuration > 0) (bufferedPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f) else 0f

        val safeCurrentMillis = if (isDraggingSlider) {
            (dragPosition * totalDuration.coerceAtLeast(0L)).toLong()
        } else {
            localPosition.coerceAtLeast(0L)
        }

        val formattedCurrentPosition by remember(safeCurrentMillis) {
            derivedStateOf {
                formatTime(safeCurrentMillis)
            }
        }

        fun dismissMenu(resumeIfWasPlaying: Boolean = true) {
            showSettingsMenu = false
            if (resumeIfWasPlaying && wasPlayingBeforeMenu) {
                exoPlayer.play()
                wasPlayingBeforeMenu = false
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        selectedMarker = null
                        onInteraction()
                        onBackgroundClick()
                    }
                )
        ) {
            val isWideDisplay = maxWidth > 600.dp
            val topHorizontalMargin = if (isWideDisplay) 24.dp else 16.dp
            val bottomHorizontalMargin = if (isWideDisplay) 24.dp else 20.dp

            // 1. Top Gradient Scrim (65% black fading to transparent over 160 dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                OverlaySurfaceColor,
                                Color.Transparent
                            )
                        )
                    )
            )

            // 2. Bottom Gradient Scrim (transparent to 75% black over 200 dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            )

            // 3. Top Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .displayCutoutPadding()
                    .padding(top = 8.dp)
                    .padding(horizontal = topHorizontalMargin),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        onInteraction()
                        onBack()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PrimaryControlText,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = title,
                    color = PrimaryControlText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Box {
                    IconButton(
                        onClick = {
                            onInteraction()
                            wasPlayingBeforeMenu = isPlaying
                            showSettingsMenu = true
                            if (isPlaying) exoPlayer.pause()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = PrimaryControlText,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showSettingsMenu,
                        onDismissRequest = {
                            onInteraction()
                            dismissMenu(resumeIfWasPlaying = true)
                        },
                        shape = RoundedCornerShape(14.dp),
                        containerColor = MenuBackgroundColor,
                        border = BorderStroke(1.dp, HairlineBorderColor)
                    ) {

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "AI",
                                    color = PrimaryControlText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = SecondaryControlText,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            onClick = {
                                onInteraction()
                                dismissMenu(resumeIfWasPlaying = false)
                                onToggleAiChat()
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onTap = {
                                                    onInteraction()
                                                    dismissMenu(resumeIfWasPlaying = true)
                                                    onCaptureVideoShot()
                                                },
                                                onLongPress = {
                                                    onInteraction()
                                                    dismissMenu(resumeIfWasPlaying = false)
                                                    onOpenVideoShots()
                                                }
                                            )
                                        }
                                        .padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Capture Video Shot",
                                        color = PrimaryControlText,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = SecondaryControlText,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            onClick = {
                                onInteraction()
                                dismissMenu(resumeIfWasPlaying = true)
                                onCaptureVideoShot()
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Study with PDF",
                                    color = PrimaryControlText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Book,
                                    contentDescription = null,
                                    tint = SecondaryControlText,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            onClick = {
                                onInteraction()
                                dismissMenu(resumeIfWasPlaying = true)
                                onStudyPdfClick()
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Playback Speed",
                                    color = PrimaryControlText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = SecondaryControlText,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            onClick = {
                                onInteraction()
                                showSettingsMenu = false
                                showSpeedDialog = true
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Timestamp Notes",
                                    color = PrimaryControlText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Notes,
                                    contentDescription = null,
                                    tint = SecondaryControlText,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            onClick = {
                                onInteraction()
                                dismissMenu(resumeIfWasPlaying = false)
                                onToggleNotes()
                            }
                        )
                    }
                }
            }

            // Playback Speed Dialog
            if (showSpeedDialog) {
                AlertDialog(
                    onDismissRequest = {
                        onInteraction()
                        showSpeedDialog = false
                        if (wasPlayingBeforeMenu) {
                            exoPlayer.play()
                            wasPlayingBeforeMenu = false
                        }
                    },
                    containerColor = MenuBackgroundColor,
                    shape = RoundedCornerShape(16.dp),
                    title = {
                        Text(
                            text = "Playback Speed",
                            color = PrimaryControlText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = String.format(java.util.Locale.US, "%.1fx", playbackSpeed),
                                color = GoldAccent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Slider(
                                value = playbackSpeed,
                                onValueChange = { speed ->
                                    onInteraction()
                                    onPlaybackSpeedChange(speed)
                                },
                                valueRange = 0.8f..2.5f,
                                colors = SliderDefaults.colors(
                                    thumbColor = GoldAccent,
                                    activeTrackColor = GoldAccent,
                                    inactiveTrackColor = InactiveTrackColor
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onInteraction()
                                showSpeedDialog = false
                                if (wasPlayingBeforeMenu) {
                                    exoPlayer.play()
                                    wasPlayingBeforeMenu = false
                                }
                            }
                        ) {
                            Text("Close", color = GoldAccent, fontWeight = FontWeight.Medium)
                        }
                    }
                )
            }

            // 4. Centre Playback Controls
            Row(
                modifier = Modifier
                    .align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                IconButton(
                    onClick = {
                        onInteraction()
                        onSeekBackward()
                    },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind 10 seconds",
                        tint = PrimaryControlText,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = {
                        onInteraction()
                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MainPlayButtonSurface)
                        .border(1.dp, HairlineBorderColor, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = PrimaryControlText,
                        modifier = Modifier.size(34.dp)
                    )
                }

                IconButton(
                    onClick = {
                        onInteraction()
                        onSeekForward()
                    },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Forward 10 seconds",
                        tint = PrimaryControlText,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // 5. Floating Study-Marker Preview Card (Restrained & positioned above timeline)
            AnimatedVisibility(
                visible = selectedMarker != null,
                enter = fadeIn(tween(150)) + slideInVertically(tween(150)) { it / 3 },
                exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { it / 3 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 88.dp)
                    .zIndex(10f)
            ) {
                selectedMarker?.let { marker ->
                    Surface(
                        color = OverlaySurfaceColor,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 0.dp,
                        border = BorderStroke(1.dp, HairlineBorderColor),
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .clickable {
                                onInteraction()
                                exoPlayer.seekTo(marker.timestampMs)
                                localPosition = marker.timestampMs
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (marker.isShot && marker.imagePath != null && File(marker.imagePath).exists()) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 56.dp, height = 36.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black)
                                ) {
                                    AsyncImage(
                                        model = File(marker.imagePath),
                                        contentDescription = "Shot Thumbnail",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (marker.isShot) GoldAccent.copy(alpha = 0.2f)
                                            else Color.White.copy(alpha = 0.12f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (marker.isShot) Icons.Default.PhotoCamera else Icons.AutoMirrored.Filled.Notes,
                                        contentDescription = null,
                                        tint = if (marker.isShot) GoldAccent else PrimaryControlText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.widthIn(max = 200.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = formatTime(marker.timestampMs),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = GoldAccent,
                                        style = TextStyle(fontFeatureSettings = "tnum")
                                    )
                                    Text(
                                        text = if (marker.isShot) "• Shot" else "• Note",
                                        fontSize = 11.sp,
                                        color = SecondaryControlText
                                    )
                                }
                                Text(
                                    text = marker.text,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = PrimaryControlText
                                )
                            }

                            IconButton(
                                onClick = {
                                    onInteraction()
                                    selectedMarker = null
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close preview",
                                    tint = SecondaryControlText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 6. Bottom Timeline Column (Max 960 dp on wide displays)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .displayCutoutPadding()
                    .padding(bottom = 12.dp)
                    .padding(horizontal = bottomHorizontalMargin),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 960.dp)
                ) {
                    // Floating Scrubbing Precision Pill during drag
                    AnimatedVisibility(
                        visible = isDraggingSlider,
                        enter = fadeIn(tween(120)) + slideInVertically(tween(120)) { it / 2 },
                        exit = fadeOut(tween(120)) + slideOutVertically(tween(120)) { it / 2 },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 6.dp)
                    ) {
                        Surface(
                            color = OverlaySurfaceColor,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, HairlineBorderColor),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = formattedCurrentPosition,
                                    color = PrimaryControlText,
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFeatureSettings = "tnum"
                                    )
                                )
                                val deltaMs = safeCurrentMillis - localPosition
                                if (kotlin.math.abs(deltaMs) >= 1000L) {
                                    val isPositive = deltaMs > 0
                                    val deltaSec = kotlin.math.abs(deltaMs) / 1000L
                                    Text(
                                        text = if (isPositive) "+${deltaSec}s" else "-${deltaSec}s",
                                        color = if (isPositive) GoldAccent else SecondaryControlText,
                                        style = TextStyle(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFeatureSettings = "tnum"
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Time-label row above timeline
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formattedCurrentPosition,
                            color = PrimaryControlText,
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                fontFeatureSettings = "tnum"
                            )
                        )

                        val durationText = if (totalDuration <= 0L) {
                            "00:00"
                        } else if (showRemainingTime) {
                            "-" + formatTime((totalDuration - safeCurrentMillis).coerceAtLeast(0L))
                        } else {
                            formatTime(totalDuration)
                        }

                        Text(
                            text = durationText,
                            color = SecondaryControlText,
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                fontFeatureSettings = "tnum"
                            ),
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onInteraction()
                                showRemainingTime = !showRemainingTime
                            }
                        )
                    }

                    // Timeline Slider & discrete study markers
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val totalWidth = maxWidth

                        val trackHeight by animateDpAsState(
                            targetValue = if (isDraggingSlider) 5.dp else 3.dp,
                            animationSpec = tween(150),
                            label = "trackHeight"
                        )

                        // Inactive track: muted neutral (22% white)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(trackHeight)
                                .clip(RoundedCornerShape(trackHeight / 2))
                                .background(InactiveTrackColor)
                        )

                        // Buffered track: subdued white (42% white)
                        if (bufferedValue > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(bufferedValue)
                                    .height(trackHeight)
                                    .align(Alignment.CenterStart)
                                    .clip(RoundedCornerShape(trackHeight / 2))
                                    .background(BufferedTrackColor)
                            )
                        }

                        // Interactive Study Markers (Small 5-6 dp mark, no glowing ring, 36 dp hit target)
                        if (totalDuration > 0) {
                            markers.forEach { marker ->
                                val fraction = (marker.timestampMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
                                val thumbPadding = 6.dp
                                val usableWidth = totalWidth - (thumbPadding * 2)
                                val markerOffset = thumbPadding + (usableWidth * fraction)

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .offset(x = markerOffset - 18.dp)
                                        .size(36.dp)
                                        .zIndex(3f)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            onInteraction()
                                            selectedMarker = marker
                                            exoPlayer.seekTo(marker.timestampMs)
                                            localPosition = marker.timestampMs
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (marker.isShot) GoldAccent else SecondaryControlText)
                                    )
                                }
                            }
                        }

                        val thumbRadius by animateDpAsState(
                            targetValue = if (isDraggingSlider) 7.dp else 4.dp,
                            animationSpec = tween(150),
                            label = "thumbRadius"
                        )

                        Slider(
                            value = displayValue,
                            onValueChange = { value ->
                                onInteraction()
                                isDraggingSlider = true
                                dragPosition = value
                            },
                            onValueChangeFinished = {
                                isDraggingSlider = false
                                val newPosition = if (totalDuration > 0) (dragPosition * totalDuration).toLong() else 0L
                                exoPlayer.seekTo(newPosition)
                                localPosition = newPosition
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = GoldAccent,
                                activeTrackColor = GoldAccent,
                                inactiveTrackColor = Color.Transparent
                            ),
                            thumb = {
                                Box(
                                    modifier = Modifier
                                        .size(thumbRadius * 2)
                                        .clip(CircleShape)
                                        .background(GoldAccent)
                                        .border(1.dp, Color.Black.copy(alpha = 0.5f), CircleShape)
                                )
                            },
                            track = { sliderState ->
                                SliderDefaults.Track(
                                    sliderState = sliderState,
                                    modifier = Modifier.height(trackHeight),
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = GoldAccent,
                                        inactiveTrackColor = Color.Transparent
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .zIndex(4f)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    if (millis <= 0L) return "00:00"
    val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(millis) - java.util.concurrent.TimeUnit.HOURS.toMinutes(hours)
    val seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(millis) - java.util.concurrent.TimeUnit.MINUTES.toSeconds(java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(millis))
    return if (hours > 0) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }
}
