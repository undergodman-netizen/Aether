package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AnnotationConfig
import com.example.media.PlayerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val showControls: Boolean = true,
    val playbackSpeed: Float = 1f,
    val isLongPressSpeedUp: Boolean = false,
    val showForwardSeek: Boolean = false,
    val showRewindSeek: Boolean = false,
    val brightnessLevel: Float = -1f,
    val volumeLevel: Float = -1f,
    val showBrightnessIndicator: Boolean = false,
    val showVolumeIndicator: Boolean = false,
    val studyPdfUri: String? = null,
    val pdfConfig: AnnotationConfig = AnnotationConfig(),
    val pdfScale: Float = 1f
)

class PlayerViewModel(
    val playerManager: PlayerManager,
    private val repository: com.example.data.VideoRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _videoTitle = MutableStateFlow("Video Player")
    val videoTitle: StateFlow<String> = _videoTitle.asStateFlow()

    private val _videoSubject = MutableStateFlow("Other")
    val videoSubject: StateFlow<String> = _videoSubject.asStateFlow()

    fun getPdfsForSubject(subject: String) = repository.getPdfsBySubject(subject)

    private var currentVideoEntity: com.example.data.VideoEntity? = null
    private var lastSavedPosition = 0L
    private var loadVideoJob: Job? = null

    init {
        viewModelScope.launch {
            while (isActive) {
                if (playerManager.isPlaying.value) {
                    val pos = playerManager.player.currentPosition
                    playerManager.updatePosition(pos)
                    
                    val duration = playerManager.player.duration
                    val diff = pos - lastSavedPosition
                    if (pos > 0 && duration > 0 && (diff >= 10000 || diff <= -10000)) {
                        saveCurrentPosition(pos, duration)
                    }
                }
                delay(500)
            }
        }
    }

    private fun saveCurrentPosition(pos: Long = playerManager.player.currentPosition, duration: Long = playerManager.player.duration) {
        val video = currentVideoEntity
        if (video != null && pos >= 0 && duration > 0) {
            val updatedVideo = video.copy(lastPlayedPosition = pos, duration = duration)
            currentVideoEntity = updatedVideo
            lastSavedPosition = pos
            viewModelScope.launch {
                repository.updateVideo(updatedVideo)
            }
        }
    }

    fun loadVideo(videoUri: String, startPositionMs: Long) {
        saveCurrentPosition() // flush previous position if any
        currentVideoEntity = null // reset before loading new video
        lastSavedPosition = startPositionMs
        
        loadVideoJob?.cancel()
        loadVideoJob = viewModelScope.launch {
            repository.allVideos.collect { videos ->
                val video = videos.find { it.uri == videoUri }
                if (video != null) {
                    currentVideoEntity = video
                    val title = video.title
                    if (title.isNotBlank()) _videoTitle.value = title
                    val subject = video.subject
                    if (subject.isNotBlank()) _videoSubject.value = subject
                }
            }
        }
        playerManager.initialize(videoUri, startPositionMs)
    }

    fun handleIntent(intent: PlayerIntent) {
        when (intent) {
            is PlayerIntent.ToggleControls -> {
                _uiState.update { it.copy(showControls = !it.showControls) }
            }
            is PlayerIntent.HideControls -> {
                _uiState.update { it.copy(showControls = false) }
            }
            is PlayerIntent.SeekForward -> {
                playerManager.seekTo((playerManager.player.currentPosition + 10000).coerceAtMost(playerManager.player.duration))
                _uiState.update { it.copy(showForwardSeek = true) }
                hideSeekIndicatorsDelayed()
            }
            is PlayerIntent.SeekBackward -> {
                playerManager.seekTo((playerManager.player.currentPosition - 10000).coerceAtLeast(0))
                _uiState.update { it.copy(showRewindSeek = true) }
                hideSeekIndicatorsDelayed()
            }
            is PlayerIntent.SetLongPressSpeedUp -> {
                _uiState.update { it.copy(isLongPressSpeedUp = intent.isSpeedUp) }
                if (intent.isSpeedUp) {
                    playerManager.setPlaybackSpeed(2f)
                } else {
                    playerManager.setPlaybackSpeed(_uiState.value.playbackSpeed)
                }
            }
            is PlayerIntent.SetPlaybackSpeed -> {
                _uiState.update { it.copy(playbackSpeed = intent.speed) }
                if (!_uiState.value.isLongPressSpeedUp) {
                    playerManager.setPlaybackSpeed(intent.speed)
                }
            }
            is PlayerIntent.UpdateBrightness -> {
                _uiState.update { it.copy(brightnessLevel = intent.level, showBrightnessIndicator = true, showVolumeIndicator = false) }
                hideOverlayIndicatorsDelayed()
            }
            is PlayerIntent.UpdateVolume -> {
                _uiState.update { it.copy(volumeLevel = intent.level, showVolumeIndicator = true, showBrightnessIndicator = false) }
                hideOverlayIndicatorsDelayed()
            }
            is PlayerIntent.SetStudyPdfUri -> {
                _uiState.update { it.copy(studyPdfUri = intent.uri) }
            }
            is PlayerIntent.SetPdfScale -> {
                _uiState.update { it.copy(pdfScale = intent.scale) }
            }
        }
    }

    private fun hideSeekIndicatorsDelayed() {
        viewModelScope.launch {
            delay(500)
            _uiState.update { it.copy(showForwardSeek = false, showRewindSeek = false) }
        }
    }

    private fun hideOverlayIndicatorsDelayed() {
        viewModelScope.launch {
            delay(2000)
            _uiState.update { it.copy(showBrightnessIndicator = false, showVolumeIndicator = false) }
        }
    }

    fun captureVideoShot(context: Context, videoUri: String, positionMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var retriever: android.media.MediaMetadataRetriever? = null
            try {
                retriever = android.media.MediaMetadataRetriever()
                if (videoUri.startsWith("content://") || videoUri.startsWith("file://")) {
                    retriever.setDataSource(context, android.net.Uri.parse(videoUri))
                } else {
                    retriever.setDataSource(videoUri, HashMap<String, String>())
                }
                val bitmap = retriever.getFrameAtTime(positionMs * 1000, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (bitmap != null) {
                    val file = java.io.File(context.cacheDir, "shot_${System.currentTimeMillis()}.png")
                    java.io.FileOutputStream(file).use { out ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                    }
                    repository.insertVideoShot(com.example.data.VideoShotEntity(videoUri = videoUri, timestamp = positionMs, imagePath = file.absolutePath))
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { onResult(true) }
                    return@launch
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try { retriever?.release() } catch (e: Exception) {}
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { onResult(false) }
        }
    }

    fun getShotsForVideo(videoUri: String) = repository.getShotsForVideo(videoUri)

    fun getNotesForVideo(videoUri: String) = repository.getNotesForVideo(videoUri)

    fun addNote(videoUri: String, text: String, timestamp: Long) {
        viewModelScope.launch {
            repository.insertNote(com.example.data.VideoNoteEntity(videoUri = videoUri, noteText = text, timestamp = timestamp))
        }
    }

    fun deleteNote(noteId: Int) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveCurrentPosition()
        loadVideoJob?.cancel()
        playerManager.release()
    }
}

sealed class PlayerIntent {
    object ToggleControls : PlayerIntent()
    object HideControls : PlayerIntent()
    object SeekForward : PlayerIntent()
    object SeekBackward : PlayerIntent()
    data class SetLongPressSpeedUp(val isSpeedUp: Boolean) : PlayerIntent()
    data class SetPlaybackSpeed(val speed: Float) : PlayerIntent()
    data class UpdateBrightness(val level: Float) : PlayerIntent()
    data class UpdateVolume(val level: Float) : PlayerIntent()
    data class SetStudyPdfUri(val uri: String?) : PlayerIntent()
    data class SetPdfScale(val scale: Float) : PlayerIntent()
}
