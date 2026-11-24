package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import com.example.ui.components.parseMarkdown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import com.example.model.AnnotationConfig
import com.example.model.AnnotationTool
import com.example.ui.components.AnnotationToolbar
import com.example.viewmodel.VideoViewModel
import java.io.File
import java.util.Locale

data class DrawStroke(
    val color: Color,
    val strokeWidth: Float,
    val points: List<Offset>,
    val blendMode: BlendMode = BlendMode.SrcOver
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VideoShotsScreen(
    videoUri: String,
    viewModel: VideoViewModel,
    onBack: () -> Unit,
    onShotClick: (Long) -> Unit
) {
    val shots by viewModel.getShotsForVideo(videoUri).collectAsState(initial = emptyList())
    val notes by viewModel.getNotesForVideo(videoUri).collectAsState(initial = emptyList())
    val drawings = remember { mutableStateMapOf<Long, MutableList<DrawStroke>>() }
    var config by remember { mutableStateOf(AnnotationConfig()) }
    var showNotesDialog by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { shots.size })
    val currentShot = shots.getOrNull(pagerState.currentPage)

    androidx.activity.compose.BackHandler {
        onBack()
    }
    val currentShotNotes = remember(currentShot, notes) {
        if (currentShot == null) emptyList()
        else {
            notes.filter { note ->
                kotlin.math.abs(note.timestamp - currentShot.timestamp) <= 10000L
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Fullscreen Content / Pager
        if (shots.isEmpty()) {
            Text(
                "No video shots found.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp)
            )
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !config.isEnabled
            ) { page ->
                val shot = shots[page]
                val shotDrawings = drawings.getOrPut(shot.timestamp) { mutableStateListOf() }

                var scale by remember { mutableFloatStateOf(1f) }
                var offsetX by remember { mutableFloatStateOf(0f) }
                var offsetY by remember { mutableFloatStateOf(0f) }
                var boxSize by remember { mutableStateOf(IntSize.Zero) }
                val currentPoints = remember(shot.timestamp) { mutableStateListOf<Offset>() }

                var lastUndoTrigger by remember(config.undoTrigger) { mutableIntStateOf(config.undoTrigger) }
                var lastRedoTrigger by remember(config.redoTrigger) { mutableIntStateOf(config.redoTrigger) }
                var lastClearTrigger by remember(config.clearTrigger) { mutableIntStateOf(config.clearTrigger) }
                var redoDrawings by remember { mutableStateOf<List<DrawStroke>>(emptyList()) }

                LaunchedEffect(config.undoTrigger) {
                    if (config.undoTrigger > lastUndoTrigger) {
                        if (shotDrawings.isNotEmpty()) {
                            val last = shotDrawings.last()
                            redoDrawings = redoDrawings + last
                            shotDrawings.removeAt(shotDrawings.lastIndex)
                        }
                        lastUndoTrigger = config.undoTrigger
                    }
                }

                LaunchedEffect(config.redoTrigger) {
                    if (config.redoTrigger > lastRedoTrigger) {
                        if (redoDrawings.isNotEmpty()) {
                            val next = redoDrawings.last()
                            redoDrawings = redoDrawings.dropLast(1)
                            shotDrawings.add(next)
                        }
                        lastRedoTrigger = config.redoTrigger
                    }
                }

                LaunchedEffect(config.clearTrigger) {
                    if (config.clearTrigger > lastClearTrigger) {
                        shotDrawings.clear()
                        redoDrawings = emptyList()
                        currentPoints.clear()
                        lastClearTrigger = config.clearTrigger
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RectangleShape)
                        .onSizeChanged { boxSize = it }
                        .pointerInput(config.isEnabled, scale, boxSize, config) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    val isMultiTouch = event.changes.size > 1
                                    if (isMultiTouch) {
                                        if (currentPoints.isNotEmpty()) {
                                            val isEraser = config.tool == AnnotationTool.ERASER
                                            val c = if (isEraser) Color.Transparent else if (config.tool == AnnotationTool.HIGHLIGHTER) config.color.copy(alpha = 0.4f) else config.color
                                            val mode = if (isEraser) BlendMode.Clear else BlendMode.SrcOver
                                            shotDrawings.add(DrawStroke(c, config.strokeWidth, currentPoints.toList(), mode))
                                            redoDrawings = emptyList()
                                            currentPoints.clear()
                                        }
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        scale = (scale * zoomChange).coerceIn(1f, 5f)
                                        if (scale > 1f) {
                                            val maxX = (boxSize.width * (scale - 1)) / 2f
                                            val maxY = (boxSize.height * (scale - 1)) / 2f
                                            offsetX = (offsetX + panChange.x).coerceIn(-maxX, maxX)
                                            offsetY = (offsetY + panChange.y).coerceIn(-maxY, maxY)
                                        } else {
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                                    } else {
                                        if (config.isEnabled) {
                                            val change = event.changes.firstOrNull()
                                            if (change != null) {
                                                if (change.pressed) {
                                                    val adjustedX = (change.position.x - boxSize.width / 2f - offsetX) / scale + boxSize.width / 2f
                                                    val adjustedY = (change.position.y - boxSize.height / 2f - offsetY) / scale + boxSize.height / 2f
                                                    val normOffset = Offset(adjustedX / boxSize.width, adjustedY / boxSize.height)
                                                    currentPoints.add(normOffset)
                                                    change.consume()
                                                } else {
                                                    if (currentPoints.isNotEmpty()) {
                                                        val isEraser = config.tool == AnnotationTool.ERASER
                                                        val c = if (isEraser) Color.Transparent else if (config.tool == AnnotationTool.HIGHLIGHTER) config.color.copy(alpha = 0.4f) else config.color
                                                        val mode = if (isEraser) BlendMode.Clear else BlendMode.SrcOver
                                                        shotDrawings.add(DrawStroke(c, config.strokeWidth, currentPoints.toList(), mode))
                                                        redoDrawings = emptyList()
                                                        currentPoints.clear()
                                                    }
                                                }
                                            }
                                        } else {
                                            val zoomChange = event.calculateZoom()
                                            val panChange = event.calculatePan()
                                            if (scale > 1f) {
                                                scale = (scale * zoomChange).coerceIn(1f, 5f)
                                                val maxX = (boxSize.width * (scale - 1)) / 2f
                                                val maxY = (boxSize.height * (scale - 1)) / 2f
                                                offsetX = (offsetX + panChange.x).coerceIn(-maxX, maxX)
                                                offsetY = (offsetY + panChange.y).coerceIn(-maxY, maxY)
                                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                                            }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                        .pointerInput(config.isEnabled) {
                            if (!config.isEnabled) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (scale > 1f) {
                                            scale = 1f
                                            offsetX = 0f
                                            offsetY = 0f
                                        } else {
                                            scale = 2.5f
                                        }
                                    },
                                    onLongPress = {
                                        onShotClick(shot.timestamp)
                                    }
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    ) {
                        AsyncImage(
                            model = File(shot.imagePath),
                            contentDescription = "Shot at ${formatTime(shot.timestamp)}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        
                        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }) {
                            fun drawPoints(pts: List<Offset>, c: Color, w: Float, blend: BlendMode) {
                                if (pts.isEmpty()) return
                                val path = Path()
                                var currentX = pts[0].x * size.width
                                var currentY = pts[0].y * size.height
                                path.moveTo(currentX, currentY)
                                for (i in 1 until pts.size) {
                                    val pt = pts[i]
                                    val ptX = pt.x * size.width
                                    val ptY = pt.y * size.height
                                    val midX = (currentX + ptX) / 2
                                    val midY = (currentY + ptY) / 2
                                    if (i == 1) {
                                        path.lineTo(midX, midY)
                                    } else {
                                        path.quadraticTo(currentX, currentY, midX, midY)
                                    }
                                    currentX = ptX
                                    currentY = ptY
                                }
                                path.lineTo(currentX, currentY)
                                drawPath(
                                    path = path,
                                    color = c,
                                    style = Stroke(
                                        width = w / scale,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    ),
                                    blendMode = blend
                                )
                            }

                            shotDrawings.forEach { stroke ->
                                drawPoints(stroke.points, stroke.color, stroke.strokeWidth, stroke.blendMode)
                            }
                            if (currentPoints.isNotEmpty()) {
                                val isEraser = config.tool == AnnotationTool.ERASER
                                val c = if (isEraser) Color.Transparent else if (config.tool == AnnotationTool.HIGHLIGHTER) config.color.copy(alpha = 0.4f) else config.color
                                val mode = if (isEraser) BlendMode.Clear else BlendMode.SrcOver
                                drawPoints(currentPoints, c, config.strokeWidth, mode)
                            }
                        }
                    }

                    // Long press hint at bottom
                    AnimatedVisibility(
                        visible = scale == 1f && !config.isEnabled,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 28.dp),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.55f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Text(
                                "Long press to jump to ${formatTime(shot.timestamp)}",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Top Subtle Glass Controls Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.75f),
                            Color.Transparent
                        )
                    )
                )
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Centered Shot Counter Pill
                if (shots.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${shots.size}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Drawing Toggle Button
                if (shots.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (config.isEnabled) MaterialTheme.colorScheme.primary
                                else Color.Black.copy(alpha = 0.45f)
                            )
                            .border(
                                0.5.dp,
                                if (config.isEnabled) Color.Transparent else Color.White.copy(alpha = 0.15f),
                                CircleShape
                            )
                            .clickable { config = config.copy(isEnabled = !config.isEnabled) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (config.isEnabled) Icons.Default.PanTool else Icons.Default.Brush,
                            contentDescription = "Toggle Annotation",
                            tint = if (config.isEnabled) MaterialTheme.colorScheme.onPrimary else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Annotation Toolbar when drawing mode is enabled
            if (config.isEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                AnnotationToolbar(
                    config = config,
                    onConfigChange = { config = it }
                )
            }
        }

        // Saved Notes FAB (Bottom Right)
        AnimatedVisibility(
            visible = !config.isEnabled && currentShotNotes.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(20.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            FloatingActionButton(
                onClick = { showNotesDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape
            ) {
                BadgedBox(
                    badge = {
                        Badge {
                            Text(currentShotNotes.size.toString())
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Saved Notes"
                    )
                }
            }
        }

        // Notes Dialog
        if (showNotesDialog && currentShot != null) {
            AlertDialog(
                onDismissRequest = { showNotesDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Saved References",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(currentShotNotes) { note ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Saved at ${formatTime(note.timestamp)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(
                                            onClick = { viewModel.deleteNote(note.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete note",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = parseMarkdown(note.noteText, MaterialTheme.colorScheme.primary),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showNotesDialog = false }) {
                        Text("Close")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
