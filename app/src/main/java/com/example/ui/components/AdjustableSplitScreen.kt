package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun VideoControlsOverlay(
    showControls: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    AnimatedVisibility(
        visible = showControls,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha=0.3f), CircleShape)
                    .size(if (isExpanded) 48.dp else 36.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, 
                    contentDescription = "Toggle Fullscreen", 
                    tint = Color.White, 
                    modifier = Modifier.size(if (isExpanded) 24.dp else 20.dp)
                )
            }
        }
    }
}

@Composable
fun PdfControlsOverlay(
    showControls: Boolean,
    isExpanded: Boolean,
    isMenuExpanded: Boolean,
    onMenuToggle: () -> Unit,
    onToggleExpand: () -> Unit,
    onClosePdf: () -> Unit,
    isPdfDrawingMode: Boolean,
    onTogglePdfDrawing: () -> Unit,
    onGoToPageClick: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = showControls || isMenuExpanded,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isMenuExpanded) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .pointerInput(Unit) { detectTapGestures(onTap = { onMenuToggle() }) }
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(0.7f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = { onGoToPageClick(); onMenuToggle() }) {
                        Icon(
                            Icons.Default.Search, 
                            contentDescription = "Go to Page", 
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { onTogglePdfDrawing(); onMenuToggle() }) {
                        Icon(
                            Icons.Default.Create, 
                            contentDescription = "Toggle Draw", 
                            tint = if (isPdfDrawingMode) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                    IconButton(onClick = { onToggleExpand(); onMenuToggle() }) {
                        Icon(
                            if (isExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, 
                            contentDescription = "Toggle Fullscreen", 
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onClosePdf) {
                        Icon(Icons.Default.Close, contentDescription = "Close PDF", tint = Color.White)
                    }
                }
            } else {
                IconButton(
                    onClick = onMenuToggle,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(0.5f), CircleShape)
                        .size(if (isExpanded) 48.dp else 36.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert, 
                        contentDescription = "More Options", 
                        tint = Color.White, 
                        modifier = Modifier.size(if (isExpanded) 24.dp else 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AdjustableSplitScreen(
    studyPdfUri: Uri?,
    onClosePdf: () -> Unit,
    isPdfDrawingMode: Boolean = false,
    onTogglePdfDrawing: () -> Unit = {},
    onGoToPageClick: () -> Unit = {},
    videoContent: @Composable (Modifier) -> Unit,
    pdfContent: @Composable (Modifier, () -> Unit) -> Unit
) {
    if (studyPdfUri == null) {
        videoContent(Modifier.fillMaxSize())
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        var targetRatio by remember { mutableFloatStateOf(0.5f) }
        val animatedRatio by animateFloatAsState(targetValue = targetRatio, animationSpec = tween(300), label = "splitRatio")
        var isVideoExpanded by remember { mutableStateOf(false) }
        var isPdfExpanded by remember { mutableStateOf(false) }
        val boundsWidth = constraints.maxWidth
        val boundsHeight = constraints.maxHeight
        
        var showVideoControls by remember { mutableStateOf(true) }
        var showPdfControls by remember { mutableStateOf(true) }
        var isPdfMenuExpanded by remember { mutableStateOf(false) }
        var videoInteractionCount by remember { mutableIntStateOf(0) }
        var pdfInteractionCount by remember { mutableIntStateOf(0) }

        LaunchedEffect(showVideoControls, videoInteractionCount) {
            if (showVideoControls) {
                delay(3000)
                showVideoControls = false
            }
        }

        LaunchedEffect(showPdfControls, isPdfMenuExpanded, pdfInteractionCount) {
            if (showPdfControls && !isPdfMenuExpanded) {
                delay(3000)
                showPdfControls = false
            }
        }
        
        val videoModifier = Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.any { it.pressed }) {
                        showVideoControls = true
                        videoInteractionCount++
                    }
                }
            }
        }

        val onPdfTap: () -> Unit = {
            showPdfControls = true
            pdfInteractionCount++
        }

        if (isVideoExpanded) {
            Box(Modifier.fillMaxSize().then(videoModifier)) {
                videoContent(Modifier.fillMaxSize())
                VideoControlsOverlay(
                    showControls = showVideoControls,
                    isExpanded = true,
                    onToggleExpand = { isVideoExpanded = false }
                )
            }
            return@BoxWithConstraints
        }

        if (isPdfExpanded) {
            Box(Modifier.fillMaxSize()) {
                pdfContent(Modifier.fillMaxSize(), onPdfTap)
                PdfControlsOverlay(
                    showControls = showPdfControls,
                    isExpanded = true,
                    isMenuExpanded = isPdfMenuExpanded,
                    onMenuToggle = { isPdfMenuExpanded = !isPdfMenuExpanded },
                    onToggleExpand = { isPdfExpanded = false },
                    onClosePdf = onClosePdf,
                    isPdfDrawingMode = isPdfDrawingMode,
                    onTogglePdfDrawing = onTogglePdfDrawing,
                    onGoToPageClick = onGoToPageClick
                )
            }
            return@BoxWithConstraints
        }

        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(animatedRatio.coerceIn(0.01f, 0.99f)).fillMaxHeight().clipToBounds().then(videoModifier)) {
                    videoContent(Modifier.fillMaxSize())
                    VideoControlsOverlay(
                        showControls = showVideoControls,
                        isExpanded = false,
                        onToggleExpand = { isVideoExpanded = true }
                    )
                }
                
                Box(modifier = Modifier.weight((1f - animatedRatio).coerceIn(0.01f, 0.99f)).fillMaxHeight().clipToBounds()) {
                    pdfContent(Modifier.fillMaxSize(), onPdfTap)
                    PdfControlsOverlay(
                        showControls = showPdfControls,
                        isExpanded = false,
                        isMenuExpanded = isPdfMenuExpanded,
                        onMenuToggle = { isPdfMenuExpanded = !isPdfMenuExpanded },
                        onToggleExpand = { isPdfExpanded = true },
                        onClosePdf = onClosePdf,
                        isPdfDrawingMode = isPdfDrawingMode,
                        onTogglePdfDrawing = onTogglePdfDrawing,
                        onGoToPageClick = onGoToPageClick
                    )
                }
            }
            
            // Smart Divider Overlay
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .fillMaxHeight()
                    .offset(x = maxWidth * animatedRatio - 16.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaRatio = dragAmount / boundsWidth
                            targetRatio = (targetRatio + deltaRatio).coerceIn(0.2f, 0.8f)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { targetRatio = 0.5f }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(animatedRatio.coerceIn(0.01f, 0.99f)).fillMaxWidth().clipToBounds().then(videoModifier)) {
                    videoContent(Modifier.fillMaxSize())
                    VideoControlsOverlay(
                        showControls = showVideoControls,
                        isExpanded = false,
                        onToggleExpand = { isVideoExpanded = true }
                    )
                }
                
                Box(modifier = Modifier.weight((1f - animatedRatio).coerceIn(0.01f, 0.99f)).fillMaxWidth().clipToBounds()) {
                    pdfContent(Modifier.fillMaxSize(), onPdfTap)
                    PdfControlsOverlay(
                        showControls = showPdfControls,
                        isExpanded = false,
                        isMenuExpanded = isPdfMenuExpanded,
                        onMenuToggle = { isPdfMenuExpanded = !isPdfMenuExpanded },
                        onToggleExpand = { isPdfExpanded = true },
                        onClosePdf = onClosePdf,
                        isPdfDrawingMode = isPdfDrawingMode,
                        onTogglePdfDrawing = onTogglePdfDrawing,
                        onGoToPageClick = onGoToPageClick
                    )
                }
            }
            
            // Smart Divider Overlay
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .fillMaxWidth()
                    .offset(y = maxHeight * animatedRatio - 16.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaRatio = dragAmount / boundsHeight
                            targetRatio = (targetRatio + deltaRatio).coerceIn(0.2f, 0.8f)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { targetRatio = 0.5f }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .width(64.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                )
            }
        }
    }
}
