package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import java.io.File
import java.io.FileOutputStream
import java.net.URL

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.model.AnnotationConfig

import android.util.LruCache
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PdfRendererState(
    val renderer: PdfRenderer, 
    val pfd: ParcelFileDescriptor, 
    val pageCount: Int
) {
    private val mutex = Mutex()
    private val cache = object : LruCache<Int, Bitmap>(20) {}

    suspend fun getPageBitmap(pageIndex: Int, density: Float): Bitmap? = withContext(Dispatchers.IO) {
        cache.get(pageIndex)?.let { return@withContext it }
        mutex.withLock {
            try {
                if (pageIndex >= pageCount) return@withContext null
                val page = renderer.openPage(pageIndex)
                val width = (page.width * density).toInt()
                val height = (page.height * density).toInt()
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                cache.put(pageIndex, bitmap)
                bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun close() {
        try { renderer.close() } catch (e: Exception) {}
        try { pfd.close() } catch (e: Exception) {}
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PdfViewer(
    url: String,
    config: AnnotationConfig,
    scale: Float,
    onScaleChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    onTap: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    var keepScreenActive by remember { mutableStateOf(true) }

    DisposableEffect(keepScreenActive, view) {
        val originalValue = view.keepScreenOn
        view.keepScreenOn = keepScreenActive
        onDispose {
            view.keepScreenOn = originalValue
        }
    }

    LaunchedEffect(Unit) {
        // Keep device screen awake for 20 minutes natively, then release
        kotlinx.coroutines.delay(20 * 60 * 1000L)
        keepScreenActive = false
    }

    var pdfState by remember(url) { mutableStateOf<PdfRendererState?>(null) }
    var pageCount by remember(url) { mutableStateOf(0) }
    var errorMsg by remember(url) { mutableStateOf<String?>(null) }
    var isLoading by remember(url) { mutableStateOf(true) }

    var pan by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(url) {
        isLoading = true
        errorMsg = null
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(url)
                val pfd: ParcelFileDescriptor? = when {
                    uri.scheme == "content" -> {
                        context.contentResolver.openFileDescriptor(uri, "r")
                    }
                    url.startsWith("http://") || url.startsWith("https://") -> {
                        val safeName = url.replace(Regex("[^a-zA-Z0-9]"), "_") + ".pdf"
                        val tempFile = File(context.cacheDir, safeName)
                        if (!tempFile.exists() || tempFile.length() == 0L) {
                            URL(url).openStream().use { input ->
                                FileOutputStream(tempFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                        ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    }
                    else -> {
                        val file = File(uri.path ?: url)
                        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    }
                }

                if (pfd != null) {
                    val renderer = PdfRenderer(pfd)
                    val state = PdfRendererState(renderer, pfd, renderer.pageCount)
                    withContext(Dispatchers.Main) {
                        pdfState = state
                        pageCount = state.pageCount
                        isLoading = false
                    }
                } else {
                    throw Exception("Could not open file descriptor for PDF.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    errorMsg = "Failed to load PDF: ${e.localizedMessage}"
                    isLoading = false
                }
            }
        }
    }

    DisposableEffect(url) {
        onDispose {
            pdfState?.close()
        }
    }

    // Reset pan if scale gets back to 1f
    LaunchedEffect(scale) {
        if (scale == 1f) pan = Offset.Zero
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Color(0xFF1E1E1E))
            .onSizeChanged { viewportSize = it }
            .pointerInput(config.isEnabled) {
                if (config.isEnabled) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        if (event.changes.size == 1 && event.changes.first().pressed) {
                            val down = event.changes.first()
                            var moved = false
                            while (true) {
                                val moveEvent = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                if (moveEvent.changes.size > 1) {
                                    moved = true
                                    break
                                }
                                val change = moveEvent.changes.first()
                                if (change.isConsumed) {
                                    moved = true
                                    break
                                }
                                if (change.pressed && (change.position - down.position).getDistanceSquared() > 400f) {
                                    moved = true
                                    break
                                }
                                if (!change.pressed) {
                                    break
                                }
                            }
                            if (!moved) {
                                onTap()
                            }
                        }
                    }
                }
            }
            .pointerInput(config.isEnabled) {
                if (config.isEnabled) return@pointerInput
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            onScaleChange(1f)
                            pan = Offset.Zero
                        } else {
                            onScaleChange(2.5f)
                        }
                    }
                )
            }
            .pointerInput(config.isEnabled) {
                detectTransformGestures(panZoomLock = true) { centroid, panChange, zoomChange, _ ->
                    // When drawing is enabled, ignore single-finger pan gestures (zoomChange == 1f) so 1 finger draws ink
                    if (config.isEnabled && zoomChange == 1f) {
                        return@detectTransformGestures
                    }
                    val oldScale = scale
                    val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                    onScaleChange(newScale)
                    
                    val actualZoom = newScale / oldScale
                    val newPan = pan + panChange - (centroid - Offset(viewportSize.width / 2f, viewportSize.height / 2f)) * (actualZoom - 1f)
                    
                    if (newScale > 1f) {
                        val maxPanX = (viewportSize.width * (newScale - 1)) / 2f
                        val maxPanY = (viewportSize.height * (newScale - 1)) / 2f
                        pan = Offset(
                            newPan.x.coerceIn(-maxPanX, maxPanX),
                            newPan.y.coerceIn(-maxPanY, maxPanY)
                        )
                    } else {
                        pan = Offset.Zero
                    }
                }
            }
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        } else if (errorMsg != null) {
            Text(
                text = errorMsg ?: "",
                color = Color.Red,
                modifier = Modifier.align(Alignment.Center).padding(16.dp)
            )
        } else if (pageCount > 0) {
            CompositionLocalProvider(
                LocalOverscrollConfiguration provides null
            ) {
                LazyColumn(
                    state = lazyListState,
                    userScrollEnabled = scale == 1f,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = pan.x
                            translationY = pan.y
                        },
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(pageCount, key = { it }) { index ->
                        Card(
                            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            PdfPageRenderer(
                                url = url,
                                pageNumber = index,
                                pdfState = pdfState,
                                config = config,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            
            // Floating Page Indicator
            AnimatedVisibility(
                visible = lazyListState.isScrollInProgress,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                val currentPage = derivedStateOf { lazyListState.firstVisibleItemIndex + 1 }
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Page ${currentPage.value} of $pageCount",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
