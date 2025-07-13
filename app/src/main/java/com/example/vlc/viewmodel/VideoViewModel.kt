package com.example.vlc.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

class VideoViewModel : ViewModel() {

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isBuffering = MutableStateFlow(true)
    val isBuffering = _isBuffering.asStateFlow()

    private val _currentTime = MutableStateFlow(0L)
    val currentTime = _currentTime.asStateFlow()

    private val _videoLength = MutableStateFlow(0L)
    val videoLength = _videoLength.asStateFlow()

    private var lastKnownPosition: Long = 0
    private var seeking = false

    private val _isUserInteracting = MutableStateFlow(false)
    val isUserInteracting = _isUserInteracting.asStateFlow()

    lateinit var mediaPlayer: MediaPlayer
    private val _videoUrl = MutableStateFlow("")
    val videoUrl = _videoUrl.asStateFlow()

    private val _showControls = MutableStateFlow(true)
    val showControls = _showControls.asStateFlow()

    fun setVideoUrl(url: String) {
        _videoUrl.value = url
    }

    fun toggleControls(visible: Boolean = true) {
        _showControls.value = visible
    }

    fun setUserInteracting(interacting: Boolean) {
        _isUserInteracting.value = interacting
    }

    init {
        observeVideoPosition()
        detectFreeze()
        autoHideControlsWhenPlaying()
    }

    fun autoHideControlsWhenPlaying() {
        viewModelScope.launch {
            while (true) {
                if (_isPlaying.value && _showControls.value && !_isUserInteracting.value) {
                    delay(10000)
                    if (!_isUserInteracting.value) {
                        _showControls.value = false
                    }
                }
                delay(1000)
            }
        }
    }

    private fun observeVideoPosition() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_isPlaying.value && !seeking) {
                    _currentTime.value = mediaPlayer.time / 1000
                }
            }
        }
    }

    private fun detectFreeze() {
        viewModelScope.launch {
            while (true) {
                delay(2000)

                try {
                    val current = mediaPlayer.time
                    val duration = mediaPlayer.length.coerceAtLeast(1)

                    if (current > 0 && current == lastKnownPosition && _isPlaying.value) {
                        println("🛑 Freeze detectado en $current ms. Reiniciando...")

                        val position = (current.toFloat() / duration).coerceIn(0f, 0.95f)

                        mediaPlayer.stop()
                        delay(300)

                        val media = Media(mediaPlayer.libVLC, Uri.parse(videoUrl.value))
                        media.setHWDecoderEnabled(true, true)
                        mediaPlayer.media = media
                        media.release()

                        mediaPlayer.play()
                        delay(300)
                        mediaPlayer.setPosition(position)

                        println("✅ Recuperado desde posición: $position (${current / 1000}s)")
                    }

                    lastKnownPosition = current

                } catch (e: Exception) {
                    println("❌ Error detectando freeze: ${e.message}")
                }
            }
        }
    }

    fun setIsPlaying(value: Boolean) {
        _isPlaying.value = value
    }

    fun onPlaybackChanged(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun onBufferingChanged(buffering: Boolean) {
        _isBuffering.value = buffering
    }

    fun onDurationChanged(durationMs: Long) {
        _videoLength.value = durationMs / 1000
    }

    fun onSeekStart() {
        seeking = true
    }

    fun onSeekUpdate(newValue: Long) {
        _currentTime.value = newValue
    }

    fun onSeekFinished() {
        val safeSeek = minOf(_currentTime.value, _videoLength.value - 3)
        val seekFraction = (safeSeek.toFloat() / _videoLength.value).coerceIn(0f, 0.95f)
        mediaPlayer.setPosition(seekFraction)
        seeking = false
    }
}
