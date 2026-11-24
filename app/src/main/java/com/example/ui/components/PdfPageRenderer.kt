package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.PdfAnnotationEntity
import com.example.model.AnnotationConfig
import com.example.model.AnnotationTool
import com.example.model.PointData
import com.example.model.StrokeData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PdfPageRenderer(
    url: String,
    pageNumber: Int,
    pdfState: PdfRendererState?,
    config: AnnotationConfig,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).pdfAnnotationDao() }

    val coroutineScope = rememberCoroutineScope()
    var strokes by remember { mutableStateOf<List<StrokeData>>(emptyList()) }
    var currentStroke by remember { mutableStateOf<StrokeData?>(null) }
    var redoStrokes by remember { mutableStateOf<List<StrokeData>>(emptyList()) }
    var savedStrokesLoaded by remember { mutableStateOf(false) }

    var lastUndoTrigger by remember(config.undoTrigger) { mutableStateOf(config.undoTrigger) }
    var lastRedoTrigger by remember(config.redoTrigger) { mutableStateOf(config.redoTrigger) }
    var lastClearTrigger by remember(config.clearTrigger) { mutableStateOf(config.clearTrigger) }

    // Undo handler
    LaunchedEffect(config.undoTrigger) {
        if (config.undoTrigger > lastUndoTrigger) {
            if (strokes.isNotEmpty()) {
                val last = strokes.last()
                redoStrokes = redoStrokes + last
                val newStrokes = strokes.dropLast(1)
                strokes = newStrokes
                if (savedStrokesLoaded) {
                    coroutineScope.launch(Dispatchers.IO) {
                        val json = Json.encodeToString(newStrokes)
                        dao.insertAnnotation(PdfAnnotationEntity(pdfUri = url, pageNumber = pageNumber, annotationsJson = json))
                    }
                }
            }
            lastUndoTrigger = config.undoTrigger
        }
    }

    // Redo handler
    LaunchedEffect(config.redoTrigger) {
        if (config.redoTrigger > lastRedoTrigger) {
            if (redoStrokes.isNotEmpty()) {
                val next = redoStrokes.last()
                redoStrokes = redoStrokes.dropLast(1)
                val newStrokes = strokes + next
                strokes = newStrokes
                if (savedStrokesLoaded) {
                    coroutineScope.launch(Dispatchers.IO) {
                        val json = Json.encodeToString(newStrokes)
                        dao.insertAnnotation(PdfAnnotationEntity(pdfUri = url, pageNumber = pageNumber, annotationsJson = json))
                    }
                }
            }
            lastRedoTrigger = config.redoTrigger
        }
    }

    // Clear handler
    LaunchedEffect(config.clearTrigger) {
        if (config.clearTrigger > lastClearTrigger) {
            strokes = emptyList()
            redoStrokes = emptyList()
            currentStroke = null
            if (savedStrokesLoaded) {
                coroutineScope.launch(Dispatchers.IO) {
                    dao.insertAnnotation(PdfAnnotationEntity(pdfUri = url, pageNumber = pageNumber, annotationsJson = "[]"))
                }
            }
            lastClearTrigger = config.clearTrigger
        }
    }

    var bitmap by remember(pdfState, pageNumber) { mutableStateOf<Bitmap?>(null) }
    val density = context.resources.displayMetrics.density

    LaunchedEffect(pdfState, pageNumber) {
        if (pdfState != null) {
            withContext(Dispatchers.IO) {
                val newBitmap = pdfState.getPageBitmap(pageNumber, density)
                bitmap = newBitmap
            }
        }
    }

    // Load persisted annotations from Room DB
    LaunchedEffect(url, pageNumber) {
        withContext(Dispatchers.IO) {
            val entity = dao.getAnnotations(url, pageNumber)
            if (entity != null) {
                try {
                    val loadedStrokes = Json.decodeFromString<List<StrokeData>>(entity.annotationsJson)
                    strokes = loadedStrokes
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            savedStrokesLoaded = true
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(if (bitmap != null) bitmap!!.width.toFloat() / bitmap!!.height.toFloat() else 0.707f)
            .pointerInput(config.isEnabled) {
                if (!config.isEnabled) return@pointerInput

                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        
                        // Detect multi-touch (2+ fingers) -> stop active stroke so pan/zoom takes over seamlessly
                        val isMultiTouch = event.changes.count { it.pressed } > 1
                        if (isMultiTouch) {
                            if (currentStroke != null) {
                                val newStrokes = strokes + currentStroke!!
                                strokes = newStrokes
                                currentStroke = null
                                if (savedStrokesLoaded) {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val json = Json.encodeToString(newStrokes)
                                        dao.insertAnnotation(PdfAnnotationEntity(pdfUri = url, pageNumber = pageNumber, annotationsJson = json))
                                    }
                                }
                            }
                            continue
                        }

                        val stylusChange = event.changes.firstOrNull { 
                            it.type == PointerType.Stylus || 
                            it.type == PointerType.Eraser || 
                            (config.isEnabled && it.type == PointerType.Touch)
                        }

                        if (stylusChange != null) {
                            if (stylusChange.pressed) {
                                val pointsToAdd = stylusChange.historical.map { 
                                    PointData(
                                        (it.position.x / size.width).coerceIn(0f, 1f), 
                                        (it.position.y / size.height).coerceIn(0f, 1f)
                                    ) 
                                } + PointData(
                                    (stylusChange.position.x / size.width).coerceIn(0f, 1f), 
                                    (stylusChange.position.y / size.height).coerceIn(0f, 1f)
                                )

                                if (currentStroke == null) {
                                    val isEraser = stylusChange.type == PointerType.Eraser || config.tool == AnnotationTool.ERASER
                                    val colorValue = if (isEraser) Color.Transparent.value 
                                    else if (config.tool == AnnotationTool.HIGHLIGHTER) config.color.copy(alpha = 0.45f).value 
                                    else config.color.value 
                                    
                                    currentStroke = StrokeData(
                                        colorValue = colorValue,
                                        width = config.strokeWidth,
                                        isEraser = isEraser,
                                        points = pointsToAdd
                                    )
                                } else {
                                    val existing = currentStroke!!
                                    currentStroke = existing.copy(points = existing.points + pointsToAdd)
                                }
                                stylusChange.consume()
                            } else {
                                if (currentStroke != null) {
                                    val newStrokes = strokes + currentStroke!!
                                    strokes = newStrokes
                                    redoStrokes = emptyList()
                                    currentStroke = null
                                    if (savedStrokesLoaded) {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            val json = Json.encodeToString(newStrokes)
                                            dao.insertAnnotation(
                                                PdfAnnotationEntity(
                                                    pdfUri = url,
                                                    pageNumber = pageNumber,
                                                    annotationsJson = json
                                                )
                                            )
                                        }
                                    }
                                }
                                stylusChange.consume()
                            }
                        }
                    }
                }
            }
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Page ${pageNumber + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        // Vector Path Canvas with Offscreen compositing
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Render existing completed strokes
            strokes.forEach { strokeData ->
                if (strokeData.points.isNotEmpty()) {
                    val path = Path()
                    val firstPoint = strokeData.points.first()
                    var currentX = firstPoint.x * canvasWidth
                    var currentY = firstPoint.y * canvasHeight
                    path.moveTo(currentX, currentY)

                    for (i in 1 until strokeData.points.size) {
                        val pt = strokeData.points[i]
                        val ptX = pt.x * canvasWidth
                        val ptY = pt.y * canvasHeight
                        
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
                        color = if (strokeData.isEraser) Color.Transparent else Color(strokeData.colorValue),
                        style = Stroke(
                            width = strokeData.width,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        ),
                        blendMode = if (strokeData.isEraser) BlendMode.Clear else DrawScope.DefaultBlendMode
                    )
                }
            }

            // Render active live stroke
            currentStroke?.let { strokeData ->
                if (strokeData.points.isNotEmpty()) {
                    val path = Path()
                    val firstPoint = strokeData.points.first()
                    var currentX = firstPoint.x * canvasWidth
                    var currentY = firstPoint.y * canvasHeight
                    path.moveTo(currentX, currentY)

                    for (i in 1 until strokeData.points.size) {
                        val pt = strokeData.points[i]
                        val ptX = pt.x * canvasWidth
                        val ptY = pt.y * canvasHeight
                        
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
                        color = if (strokeData.isEraser) Color.Transparent else Color(strokeData.colorValue),
                        style = Stroke(
                            width = strokeData.width,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        ),
                        blendMode = if (strokeData.isEraser) BlendMode.Clear else DrawScope.DefaultBlendMode
                    )
                }
            }
        }

        // Active Drawing Mode indicator badge
        if (config.isEnabled) {
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isEraser = config.tool == AnnotationTool.ERASER
                    val displayColor = if (isEraser) Color.White else if (config.tool == AnnotationTool.HIGHLIGHTER) config.color.copy(alpha = 0.5f) else config.color
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(displayColor)
                            .border(1.dp, Color.White, CircleShape)
                    )
                    Text(
                        text = if (isEraser) "Eraser" else if (config.tool == AnnotationTool.HIGHLIGHTER) "Highlighter" else "Pen",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
