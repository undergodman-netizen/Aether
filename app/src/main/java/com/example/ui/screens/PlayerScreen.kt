@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Rational
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.first
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import com.example.model.AnnotationConfig
import com.example.model.AnnotationTool
import com.example.ui.components.AdjustableSplitScreen
import com.example.ui.components.PdfViewer
import com.example.ui.components.VideoPlayer
import com.example.ui.components.rememberExoPlayer
import com.example.ui.components.AiChatSidebar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

import androidx.compose.material3.ExperimentalMaterial3Api

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(videoUri: String, viewModel: com.example.viewmodel.PlayerViewModel, startPositionMs: Long = -1L, onBack: () -> Unit, onOpenVideoShots: () -> Unit = {}) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    var isPlaying by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var showAiChat by remember { mutableStateOf(false) }
    var showNotesBottomSheet by remember { mutableStateOf(false) }
    var totalDuration by remember { mutableStateOf(0L) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    var userInteracted by remember { mutableStateOf(0L) }
    
    var playbackState by remember { mutableStateOf(Player.STATE_IDLE) }
    var isLongPressSpeedUp by remember { mutableStateOf(false) }
    
    // Seek indicator overlay
    var showForwardSeek by remember { mutableStateOf(false) }
    var showRewindSeek by remember { mutableStateOf(false) }

    // Volume & Brightness overlays
    var brightnessLevel by remember { mutableStateOf(-1f) }
    var volumeLevel by remember { mutableStateOf(-1f) }
    var showBrightnessIndicator by remember { mutableStateOf(false) }
    var showVolumeIndicator by remember { mutableStateOf(false) }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    var studyPdfUri by remember { mutableStateOf<Uri?>(null) }
    var showPdfSelection by remember { mutableStateOf(false) }
    val videoSubject by viewModel.videoSubject.collectAsState()
    
    val studyPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore
            }
            studyPdfUri = uri
        }
    }

    val sharedPreferences = remember { context.getSharedPreferences("StudyMemory", Context.MODE_PRIVATE) }
    val apiKeyPrefs = sharedPreferences.getString("gemini_api_key", "") ?: ""
    val apiKey = if (apiKeyPrefs.isNotBlank()) apiKeyPrefs else try { com.example.BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
    val geminiModel = sharedPreferences.getString("gemini_model", "models/gemini-3.5-flash-lite") ?: "models/gemini-3.5-flash-lite"
    val customSystemPrompt = sharedPreferences.getString("gemini_custom_prompt", "") ?: ""
    val pdfScrollState = rememberLazyListState()
    var showGoToPageDialog by remember { mutableStateOf(false) }
    var goToPageInput by remember { mutableStateOf("") }
    
    var pdfConfig by remember { mutableStateOf(AnnotationConfig()) }
    var pdfScale by remember { mutableFloatStateOf(1f) }
    
    var aiChatWidth by remember { mutableFloatStateOf(sharedPreferences.getFloat("ai_chat_width", 380f)) }

    LaunchedEffect(videoUri) {
        val savedPdf = sharedPreferences.getString(videoUri, null)
        if (savedPdf != null) {
            val uri = Uri.parse(savedPdf)
            val hasPermission = try {
                context.checkUriPermission(
                    uri,
                    android.os.Process.myPid(),
                    android.os.Process.myUid(),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                false
            }
            if (hasPermission) {
                studyPdfUri = uri
            } else {
                sharedPreferences.edit().remove(videoUri).apply()
            }
        }

        // Keep watch history updated in SharedPreferences
        try {
            val historyString = sharedPreferences.getString("history", "") ?: ""
            val historyList = if (historyString.startsWith("[")) {
                try {
                    val jsonArray = org.json.JSONArray(historyString)
                    List(jsonArray.length()) { jsonArray.getString(it) }.toMutableList()
                } catch (e: Exception) {
                    historyString.split(",").filter { it.isNotEmpty() }.toMutableList()
                }
            } else {
                historyString.split(",").filter { it.isNotEmpty() }.toMutableList()
            }
            historyList.remove(videoUri)
            historyList.add(0, videoUri)
            val cappedHistory = historyList.take(20)
            val jsonArray = org.json.JSONArray(cappedHistory)
            sharedPreferences.edit().putString("history", jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    LaunchedEffect(studyPdfUri) {
        if (studyPdfUri != null) {
            val pdfStr = studyPdfUri.toString()
            sharedPreferences.edit().putString(videoUri, pdfStr).apply()
            val savedPdfIndex = sharedPreferences.getInt("pdf_index_$pdfStr", 0)
            val savedPdfOffset = sharedPreferences.getInt("pdf_offset_$pdfStr", 0)
            
            androidx.compose.runtime.snapshotFlow { pdfScrollState.layoutInfo.totalItemsCount }
                .first { it > 0 }
                
            pdfScrollState.scrollToItem(savedPdfIndex, savedPdfOffset)
        } else {
            sharedPreferences.edit().remove(videoUri).apply()
        }
    }

    LaunchedEffect(pdfScrollState.firstVisibleItemIndex, pdfScrollState.firstVisibleItemScrollOffset) {
        val pdfStr = studyPdfUri?.toString()
        if (pdfStr != null) {
            sharedPreferences.edit()
                .putInt("pdf_index_$pdfStr", pdfScrollState.firstVisibleItemIndex)
                .putInt("pdf_offset_$pdfStr", pdfScrollState.firstVisibleItemScrollOffset)
                .apply()
        }
    }

    val exoPlayer = viewModel.playerManager.player
    val videoTitle by viewModel.videoTitle.collectAsState()
    val videoShots by viewModel.getShotsForVideo(videoUri).collectAsState(initial = emptyList())
    val videoNotes by viewModel.getNotesForVideo(videoUri).collectAsState(initial = emptyList())
    
    val coroutineScope = rememberCoroutineScope()
    val viewConfiguration = LocalViewConfiguration.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    LaunchedEffect(videoUri) {
        playbackSpeed = sharedPreferences.getFloat("speed_$videoUri", 1f)
        exoPlayer.setPlaybackSpeed(playbackSpeed)
        val pos = if (startPositionMs >= 0) startPositionMs else sharedPreferences.getLong("video_$videoUri", 0L)
        viewModel.loadVideo(videoUri, pos)
    }
    
    DisposableEffect(lifecycleOwner, videoUri) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_PAUSE) {
                exoPlayer.pause()
                sharedPreferences.edit().putLong("video_$videoUri", exoPlayer.currentPosition).apply()
            } else if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                if (exoPlayer.playWhenReady) {
                    exoPlayer.play()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sharedPreferences.edit().putLong("video_$videoUri", exoPlayer.currentPosition).apply()
        }
    }
    
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                isPlaying = isPlayingChanged
            }
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                if (state == Player.STATE_READY) {
                    totalDuration = exoPlayer.duration
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.pause()
            exoPlayer.removeListener(listener)
        }
    }

    LaunchedEffect(showControls, isPlaying, userInteracted, showAiChat) {
        if (showControls && isPlaying && !showAiChat) {
            delay(4000)
            showControls = false
        }
    }

    LaunchedEffect(showBrightnessIndicator, showVolumeIndicator) {
        if (showBrightnessIndicator || showVolumeIndicator) {
            delay(2000)
            showBrightnessIndicator = false
            showVolumeIndicator = false
        }
    }

    // Auto-hide seek indicators
    LaunchedEffect(showForwardSeek) {
        if (showForwardSeek) {
            delay(500)
            showForwardSeek = false
        }
    }
    LaunchedEffect(showRewindSeek) {
        if (showRewindSeek) {
            delay(500)
            showRewindSeek = false
        }
    }

    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val window = activity?.window
        var insetsController: WindowInsetsControllerCompat? = null

        if (window != null) {
            insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                val keyCode = keyEvent.nativeKeyEvent.keyCode
                val isPlayPauseKey = 
                    keyCode == 251 || // KEYCODE_BUTTON_11 (S-Pen)
                    keyCode == 308 || // KEYCODE_BUTTON_PRIMARY (Stylus primary side button)
                    keyCode == 309 || // KEYCODE_BUTTON_SECONDARY (Stylus secondary side button)
                    keyCode == android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                    keyCode == android.view.KeyEvent.KEYCODE_MEDIA_PLAY ||
                    keyCode == android.view.KeyEvent.KEYCODE_MEDIA_PAUSE
                if (isPlayPauseKey) {
                    if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                        if (isPlaying) {
                            exoPlayer.pause()
                        } else {
                            exoPlayer.play()
                        }
                    }
                    true
                } else {
                    false
                }
            }
    ) {
        AdjustableSplitScreen(
            studyPdfUri = studyPdfUri,
            onClosePdf = { studyPdfUri = null },
            isPdfDrawingMode = pdfConfig.isEnabled,
            onTogglePdfDrawing = { pdfConfig = pdfConfig.copy(isEnabled = !pdfConfig.isEnabled) },
            onGoToPageClick = { showGoToPageDialog = true },
            videoContent = { modifier ->
            Box(
                modifier = modifier
                    .background(Color.Black)
                    .pointerInput(studyPdfUri) {
                        detectTapGestures(
                            onTap = { 
                                showControls = !showControls 
                                userInteracted = System.currentTimeMillis()
                            },
                            onDoubleTap = { offset ->
                                userInteracted = System.currentTimeMillis()
                                val screenWidth = size.width
                                if (offset.x < screenWidth / 2) {
                                    exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0))
                                    showRewindSeek = true
                                } else {
                                    exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration))
                                    showForwardSeek = true
                                }
                            },
                            onPress = {
                                userInteracted = System.currentTimeMillis()
                                val waitJob = coroutineScope.launch {
                                    delay(viewConfiguration.longPressTimeoutMillis)
                                    if (isPlaying) {
                                        isLongPressSpeedUp = true
                                        exoPlayer.setPlaybackSpeed(2f)
                                    }
                                }
                                tryAwaitRelease()
                                waitJob.cancel()
                                if (isLongPressSpeedUp) {
                                    isLongPressSpeedUp = false
                                    exoPlayer.setPlaybackSpeed(playbackSpeed)
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        var initialY = 0f
                        var isLeftHalf = false
                        var startBrightness = -1f
                        var startVolume = -1
                        var dragCanceled = false
                        val activity = context.findActivity()

                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                userInteracted = System.currentTimeMillis()
                                val inHorizontalDeadZone = offset.x > size.width * 0.3f && offset.x < size.width * 0.7f
                                val inVerticalDeadZone = offset.y > size.height * 0.75f
                                if (inHorizontalDeadZone || inVerticalDeadZone) {
                                    dragCanceled = true
                                    return@detectVerticalDragGestures
                                }
                                dragCanceled = false

                                initialY = offset.y
                                isLeftHalf = offset.x < size.width / 2
                                if (isLeftHalf) {
                                    startBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
                                    if (startBrightness < 0) {
                                        try {
                                            startBrightness = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
                                        } catch (e: Settings.SettingNotFoundException) {
                                            startBrightness = 0.5f
                                        }
                                    }
                                    brightnessLevel = startBrightness
                                    showBrightnessIndicator = true
                                    showVolumeIndicator = false
                                } else {
                                    startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    volumeLevel = startVolume.toFloat() / maxVolume
                                    showVolumeIndicator = true
                                    showBrightnessIndicator = false
                                }
                            },
                            onDragEnd = {
                                dragCanceled = false
                                showBrightnessIndicator = false
                                showVolumeIndicator = false
                            },
                            onDragCancel = {
                                dragCanceled = false
                                showBrightnessIndicator = false
                                showVolumeIndicator = false
                            },
                            onVerticalDrag = { change, dragAmount ->
                                if (dragCanceled) return@detectVerticalDragGestures
                                change.consume()
                                userInteracted = System.currentTimeMillis()
                                val dragDelta = (initialY - change.position.y) / size.height // Positive is up
                                
                                if (isLeftHalf) {
                                    val newBrightness = (startBrightness + dragDelta * 2f).coerceIn(0f, 1f) // Multiply by 2 for faster swipe
                                    brightnessLevel = newBrightness
                                    activity?.let {
                                        val layoutParams = it.window.attributes
                                        layoutParams.screenBrightness = newBrightness
                                        it.window.attributes = layoutParams
                                    }
                                } else {
                                    val volumeDelta = (dragDelta * maxVolume * 2f).toInt()
                                    val newVolume = (startVolume + volumeDelta).coerceIn(0, maxVolume)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                                    volumeLevel = newVolume.toFloat() / maxVolume
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val nativeEvent = try {
                                    val field = event.javaClass.getDeclaredField("motionEvent")
                                    field.isAccessible = true
                                    field.get(event) as? android.view.MotionEvent
                                } catch (e: Exception) {
                                    null
                                }
                                if (nativeEvent != null) {
                                    val action = nativeEvent.actionMasked
                                    val isStylus = nativeEvent.pointerCount > 0 && nativeEvent.getToolType(0) == android.view.MotionEvent.TOOL_TYPE_STYLUS
                                    if (isStylus && action == android.view.MotionEvent.ACTION_BUTTON_PRESS) {
                                        val button = nativeEvent.actionButton
                                        if (button == android.view.MotionEvent.BUTTON_STYLUS_PRIMARY ||
                                            button == android.view.MotionEvent.BUTTON_STYLUS_SECONDARY) {
                                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                        }
                                    }
                                }
                            }
                        }
                    }
            ) {
                VideoPlayer(
                    exoPlayer = exoPlayer,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Network Buffering Indication
                if (playbackState == Player.STATE_BUFFERING) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp),
                        strokeWidth = 3.dp,
                        color = Color(0xFFD4AF37)
                    )
                }
                
                // 2x Speed Indicator Overlay
                AnimatedVisibility(
                    visible = isLongPressSpeedUp,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(200)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 72.dp)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "2x Speed",
                                color = Color(0xFFF5F5F5),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = null,
                                tint = Color(0xFFD4AF37),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                // Seek overlays (Double tap left/right)
                AnimatedVisibility(
                    visible = showRewindSeek,
                    enter = fadeIn(tween(120)) + slideInHorizontally(tween(120)) { -it / 2 },
                    exit = fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { -it / 2 },
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 56.dp)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = null,
                                tint = Color(0xFFD4AF37),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "-10s",
                                color = Color(0xFFF5F5F5),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = showForwardSeek,
                    enter = fadeIn(tween(120)) + slideInHorizontally(tween(120)) { it / 2 },
                    exit = fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { it / 2 },
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 56.dp)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "+10s",
                                color = Color(0xFFF5F5F5),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = null,
                                tint = Color(0xFFD4AF37),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Sleek Vertical HUD Gauges (Brightness - Left, Volume - Right)
                AnimatedVisibility(
                    visible = showBrightnessIndicator,
                    enter = fadeIn(tween(150)) + slideInHorizontally(tween(150)) { -it / 2 },
                    exit = fadeOut(tween(250)),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 28.dp)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                    ) {
                        Column(
                            modifier = Modifier
                                .width(42.dp)
                                .padding(vertical = 14.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrightnessMedium,
                                contentDescription = null,
                                tint = Color(0xFFD4AF37),
                                modifier = Modifier.size(20.dp)
                            )
                            // Vertical gauge track
                            Box(
                                modifier = Modifier
                                    .width(5.dp)
                                    .height(90.dp)
                                    .clip(RoundedCornerShape(2.5.dp))
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(brightnessLevel.coerceIn(0f, 1f))
                                        .clip(RoundedCornerShape(2.5.dp))
                                        .background(Color(0xFFD4AF37))
                                )
                            }
                            Text(
                                text = "${(brightnessLevel * 100).toInt()}%",
                                color = Color(0xFFF5F5F5),
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFeatureSettings = "tnum"
                                )
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showVolumeIndicator,
                    enter = fadeIn(tween(150)) + slideInHorizontally(tween(150)) { it / 2 },
                    exit = fadeOut(tween(250)),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 28.dp)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                    ) {
                        Column(
                            modifier = Modifier
                                .width(42.dp)
                                .padding(vertical = 14.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = Color(0xFFD4AF37),
                                modifier = Modifier.size(20.dp)
                            )
                            // Vertical gauge track
                            Box(
                                modifier = Modifier
                                    .width(5.dp)
                                    .height(90.dp)
                                    .clip(RoundedCornerShape(2.5.dp))
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(volumeLevel.coerceIn(0f, 1f))
                                        .clip(RoundedCornerShape(2.5.dp))
                                        .background(Color(0xFFD4AF37))
                                )
                            }
                            Text(
                                text = "${(volumeLevel * 100).toInt()}%",
                                color = Color(0xFFF5F5F5),
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFeatureSettings = "tnum"
                                )
                            )
                        }
                    }
                }

                com.example.ui.components.PlayerOverlayControls(
                    showControls = showControls,
                    title = videoTitle,
                    isPlaying = isPlaying,
                    playbackSpeed = playbackSpeed,
                    isLongPressSpeedUp = isLongPressSpeedUp,
                    currentPositionProvider = { exoPlayer.currentPosition },
                    bufferedPositionProvider = { exoPlayer.bufferedPosition },
                    totalDuration = totalDuration,
                    exoPlayer = exoPlayer,
                    context = context,
                    activity = context.findActivity(),
                    videoShots = videoShots,
                    videoNotes = videoNotes,
                    onBack = onBack,
                    onStudyPdfClick = { showPdfSelection = true },
                    onOpenVideoShots = onOpenVideoShots,
                    onPlaybackSpeedChange = { newSpeed ->
                        playbackSpeed = newSpeed
                        sharedPreferences.edit().putFloat("speed_$videoUri", newSpeed).apply()
                        if (!isLongPressSpeedUp) {
                            exoPlayer.setPlaybackSpeed(playbackSpeed)
                        }
                    },
                    onCaptureVideoShot = {
                        val pos = exoPlayer.currentPosition
                        android.widget.Toast.makeText(context, "Capturing shot...", android.widget.Toast.LENGTH_SHORT).show()
                        viewModel.captureVideoShot(context, videoUri, pos) { success ->
                            if (success) {
                                android.widget.Toast.makeText(context, "Shot captured and saved", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                android.widget.Toast.makeText(context, "Failed to capture shot", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onToggleAiChat = { showAiChat = !showAiChat },
                    onToggleNotes = { showNotesBottomSheet = true },
                    onSeekForward = {
                        exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(totalDuration))
                        showForwardSeek = true
                    },
                    onSeekBackward = {
                        exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0))
                        showRewindSeek = true
                    },
                    onInteraction = { userInteracted = System.currentTimeMillis() },
                    onBackgroundClick = {
                        showControls = false
                    }
                )
            }
        },
        pdfContent = { modifier, onPdfTap ->
            if (studyPdfUri != null) {
                Box(modifier = modifier.fillMaxSize()) {
                    PdfViewer(
                        url = studyPdfUri.toString(),
                        config = pdfConfig,
                        scale = pdfScale,
                        onScaleChange = { pdfScale = it },
                        modifier = Modifier.fillMaxSize(),
                        lazyListState = pdfScrollState,
                        onTap = onPdfTap
                    )
                    if (pdfConfig.isEnabled) {
                        com.example.ui.components.AnnotationToolbar(
                            config = pdfConfig,
                            onConfigChange = { pdfConfig = it },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                        )
                    }
                }
            }
        }
    )

        AnimatedVisibility(
            visible = showAiChat,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(durationMillis = 400)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(durationMillis = 400)
            ),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(aiChatWidth.dp)
        ) {
            val density = LocalDensity.current
            Row(modifier = Modifier.fillMaxHeight()) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(20.dp)
                        .background(Color.Transparent)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    sharedPreferences.edit().putFloat("ai_chat_width", aiChatWidth).apply()
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                val newWidth = aiChatWidth - (dragAmount / density.density)
                                aiChatWidth = newWidth.coerceIn(250f, 800f)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    )
                }

                AiChatSidebar(
                    modifier = Modifier.weight(1f),
                    videoContextProvider = { "Currently watching: '$videoTitle'. The user paused at ${formatTime(exoPlayer.currentPosition)}/${formatTime(totalDuration)}. Respond concisely using medical knowledge suited for an MBBS student or doctor." },
                    apiKey = apiKey,
                    geminiModel = geminiModel,
                    customSystemPrompt = customSystemPrompt,
                    onSaveResponseAsNote = { text ->
                        viewModel.addNote(videoUri, text, exoPlayer.currentPosition)
                        android.widget.Toast.makeText(context, "Saved tutor response as note at ${formatTime(exoPlayer.currentPosition)}", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onClose = { showAiChat = false }
                )
            }
        }
    }

    if (showNotesBottomSheet) {
        NotesBottomSheet(
            videoUri = videoUri,
            viewModel = viewModel,
            currentPositionProvider = { exoPlayer.currentPosition },
            onDismiss = { showNotesBottomSheet = false }
        )
    }

    if (showPdfSelection) {
        PdfSelectionBottomSheet(
            subject = videoSubject,
            viewModel = viewModel,
            onPdfSelected = { uri ->
                studyPdfUri = uri
            },
            onBrowseDevice = {
                studyPdfLauncher.launch(arrayOf("application/pdf"))
            },
            onDismiss = { showPdfSelection = false }
        )
    }
    
    if (showGoToPageDialog) {
        val coroutineScope = rememberCoroutineScope()
        AlertDialog(
            onDismissRequest = {
                showGoToPageDialog = false
                goToPageInput = ""
            },
            title = {
                Text(
                    text = "Go to Page",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                OutlinedTextField(
                    value = goToPageInput,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            goToPageInput = newValue
                        }
                    },
                    label = { Text("Page number") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val pageInt = goToPageInput.toIntOrNull()
                            if (pageInt != null && pageInt > 0) {
                                // 0-indexed assuming the pdfScrollState uses 0-based page mapping
                                pdfScrollState.animateScrollToItem(pageInt - 1)
                            }
                            showGoToPageDialog = false
                            goToPageInput = ""
                        }
                    }
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showGoToPageDialog = false
                        goToPageInput = ""
                    }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PdfSelectionBottomSheet(
    subject: String,
    viewModel: com.example.viewmodel.PlayerViewModel,
    onPdfSelected: (Uri) -> Unit,
    onBrowseDevice: () -> Unit,
    onDismiss: () -> Unit
) {
    val pdfs by viewModel.getPdfsForSubject(subject).collectAsState(initial = emptyList())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            androidx.compose.material3.Text(
                text = "Select PDF for $subject",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                items(pdfs) { pdf ->
                    androidx.compose.material3.ListItem(
                        headlineContent = { androidx.compose.material3.Text(pdf.title) },
                        leadingContent = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable {
                            onPdfSelected(Uri.parse(pdf.uri))
                            onDismiss()
                        }
                    )
                }
            }
            
            androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            androidx.compose.material3.ListItem(
                headlineContent = { androidx.compose.material3.Text("Browse device...") },
                leadingContent = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                modifier = Modifier.clickable {
                    onBrowseDevice()
                    onDismiss()
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NotesBottomSheet(
    videoUri: String,
    viewModel: com.example.viewmodel.PlayerViewModel,
    currentPositionProvider: () -> Long,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        val notes by viewModel.getNotesForVideo(videoUri).collectAsState(initial = emptyList())
        var newNoteText by remember { mutableStateOf("") }
        val expandedNoteIds = remember { mutableStateListOf<Int>() }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Notes",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                items(notes) { note ->
                    val isExpanded = expandedNoteIds.contains(note.id)
                    val isAiNote = note.noteText.length > 70 || note.noteText.contains("Here is") || note.noteText.contains("The ") || note.noteText.contains("Mechanism") || note.noteText.contains("Intracellular")

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAiNote) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.clickable {
                                            viewModel.playerManager.seekTo(note.timestamp)
                                        }
                                    ) {
                                        Text(
                                            text = formatTime(note.timestamp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    if (isAiNote) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.SmartToy,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "AI Response",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.deleteNote(note.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Note", modifier = Modifier.size(16.dp))
                                }
                            }

                            Text(
                                text = note.noteText,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .clickable {
                                        viewModel.playerManager.seekTo(note.timestamp)
                                    }
                            )

                            if (note.noteText.length > 70) {
                                TextButton(
                                    onClick = {
                                        if (isExpanded) expandedNoteIds.remove(note.id)
                                        else expandedNoteIds.add(note.id)
                                    },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .height(28.dp)
                                        .padding(top = 2.dp)
                                ) {
                                    Text(
                                        text = if (isExpanded) "Show less" else "Expand inline...",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newNoteText,
                    onValueChange = { newNoteText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add a note...") },
                    maxLines = 3
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newNoteText.isNotBlank()) {
                            viewModel.addNote(videoUri, newNoteText, currentPositionProvider())
                            newNoteText = ""
                        }
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Save Note", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun formatTime(millis: Long): String {
    val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(millis) - java.util.concurrent.TimeUnit.HOURS.toMinutes(hours)
    val seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(millis) - java.util.concurrent.TimeUnit.MINUTES.toSeconds(java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(millis))
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

