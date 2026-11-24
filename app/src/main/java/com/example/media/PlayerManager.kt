package com.example.media

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlayerManager(
    private val context: Context
) {
    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                _isPlaying.value = isPlayingChanged
            }

            override fun onPlaybackStateChanged(state: Int) {
                _playbackState.value = state
                if (state == Player.STATE_READY) {
                    _duration.value = player.duration
                }
            }
        })
    }

    fun initialize(uri: String, startPositionMs: Long) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        if (startPositionMs > 0) {
            player.seekTo(startPositionMs)
        }
        player.playWhenReady = true
    }

    fun play() {
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun release() {
        player.release()
    }

    fun updatePosition(position: Long) {
        _currentPosition.value = position
    }
}
