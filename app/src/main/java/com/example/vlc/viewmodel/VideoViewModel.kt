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

    // ───────────────────────────────────────────────────────────────
    // ▶ Playback State
    // ───────────────────────────────────────────────────────────────

    // Indicates whether the video is currently playing
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    // Indicates whether the video is buffering
    private val _isBuffering = MutableStateFlow(true)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    // Current playback time in seconds
    private val _currentTime = MutableStateFlow(0L)
    val currentTime: StateFlow<Long> = _currentTime.asStateFlow()

    // Total duration of the video in seconds
    private val _videoLength = MutableStateFlow(0L)
    val videoLength: StateFlow<Long> = _videoLength.asStateFlow()

    // Flag to indicate seeking in progress
    private var seeking: Boolean = false

    // Last known playback position in milliseconds
    private var lastKnownPosition: Long = 0

    // ───────────────────────────────────────────────────────────────
    // 👤 User Interaction State
    // ───────────────────────────────────────────────────────────────

    // Indicates whether the user is currently interacting with controls
    private val _isUserInteracting = MutableStateFlow(false)
    val isUserInteracting: StateFlow<Boolean> = _isUserInteracting.asStateFlow()

    // ───────────────────────────────────────────────────────────────
    // 🎛 UI Controls Visibility
    // ───────────────────────────────────────────────────────────────

    // Whether the video controls are currently shown on screen
    private val _showControls = MutableStateFlow(true)
    val showControls: StateFlow<Boolean> = _showControls.asStateFlow()

    // ───────────────────────────────────────────────────────────────
    // 📺 Media Player & Video Source
    // ───────────────────────────────────────────────────────────────

    // Video URL to be played
    private val _videoUrl = MutableStateFlow("")
    val videoUrl: StateFlow<String> = _videoUrl.asStateFlow()

    // VLC media player instance (must be initialized externally)
    lateinit var mediaPlayer: MediaPlayer

    // ───────────────────────────────────────────────────────────────
    // 🚪 App Exit Management (Double back press)
    // ───────────────────────────────────────────────────────────────

    // Timestamp of the last back press
    private var lastBackPressTime: Long = 0

    // Show toast/prompt to press back again to exit
    private val _showExitPrompt = MutableStateFlow(false)
    val showExitPrompt: StateFlow<Boolean> = _showExitPrompt.asStateFlow()

    // Flag that triggers actual app exit
    private val _shouldExitApp = MutableStateFlow(false)
    val shouldExitApp: StateFlow<Boolean> = _shouldExitApp.asStateFlow()

    // ───────────────────────────────────────────────────────────────
    // 🔁 INIT block - Setup Observers
    // ───────────────────────────────────────────────────────────────

    init {
        observeVideoPosition()
        detectFreeze()
        autoHideControlsWhenPlaying()
    }

    // ───────────────────────────────────────────────────────────────
    // 🎞 Load & Play Video
    // ───────────────────────────────────────────────────────────────

    fun setVideoUrl(url: String) {
        if (_videoUrl.value == url) return // Avoid reloading the same URL

        _videoUrl.value = url

        try {
            mediaPlayer.stop()

            val media = Media(mediaPlayer.libVLC, Uri.parse(url))
            media.setHWDecoderEnabled(true, false)

            mediaPlayer.media = media
            media.release()

            mediaPlayer.play()
        } catch (e: Exception) {
            println("❌ Failed to load media: ${e.message}")
        }
    }

    // ───────────────────────────────────────────────────────────────
    // 🕓 Track Playback Time
    // ───────────────────────────────────────────────────────────────

    private fun observeVideoPosition() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                try {
                    if (_isPlaying.value && !seeking) {
                        _currentTime.value = mediaPlayer.time / 1000
                    }
                } catch (e: Exception) {
                    println("❌ Error getting playback time: ${e.message}")
                }
            }
        }
    }

    // ───────────────────────────────────────────────────────────────
    // 🧊 Detect Frozen Playback & Auto-Recover
    // ───────────────────────────────────────────────────────────────

    private fun detectFreeze() {
        viewModelScope.launch {
            while (true) {
                delay(15000)

                try {
                    val current = mediaPlayer.time
                    val duration = mediaPlayer.length.coerceAtLeast(1)

                    if (current > 0 && current == lastKnownPosition && _isPlaying.value) {
                        println("⛔ Playback freeze detected at $current ms. Restarting...")

                        val position = (current.toFloat() / duration).coerceIn(0f, 0.95f)

                        mediaPlayer.stop()
                        delay(300)

                        val media = Media(mediaPlayer.libVLC, Uri.parse(videoUrl.value))
                        media.setHWDecoderEnabled(true, true)
                        mediaPlayer.media = media
                        media.release()

                        mediaPlayer.play()
                        delay(300)

                        if (position >= 0f) {
                            mediaPlayer.setPosition(position)
                        }

                        println("✅ Recovered playback at position: $position (${current / 1000}s)")
                    }

                    lastKnownPosition = current
                } catch (e: Exception) {
                    println("❌ Error during freeze detection: ${e.message}")
                }
            }
        }
    }

    // ───────────────────────────────────────────────────────────────
    // 🙈 Auto-Hide Controls After Inactivity
    // ───────────────────────────────────────────────────────────────

    fun autoHideControlsWhenPlaying() {
        viewModelScope.launch {
            while (true) {
                if (_isPlaying.value && _showControls.value && !_isUserInteracting.value) {
                    delay(10_000)
                    if (!_isUserInteracting.value) {
                        _showControls.value = false
                    }
                }
                delay(1000)
            }
        }
    }

    // ───────────────────────────────────────────────────────────────
    // 🧑 User Interaction Tracking
    // ───────────────────────────────────────────────────────────────

    fun setUserInteracting(interacting: Boolean) {
        _isUserInteracting.value = interacting
    }

    fun startUserInteractionTimeout() {
        viewModelScope.launch {
            delay(10_000)
            _isUserInteracting.value = false
        }
    }

    // ───────────────────────────────────────────────────────────────
    // 🎮 UI Controls State
    // ───────────────────────────────────────────────────────────────

    fun toggleControls(visible: Boolean = true) {
        _showControls.value = visible
    }

    // ───────────────────────────────────────────────────────────────
    // 🔁 Playback Event Callbacks
    // ───────────────────────────────────────────────────────────────

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

    // ───────────────────────────────────────────────────────────────
    // ⏩ Seek Operations
    // ───────────────────────────────────────────────────────────────

    fun onSeekStart() {
        seeking = true
    }

    fun onSeekUpdate(newValue: Long) {
        _currentTime.value = newValue
    }

    fun onSeekFinished() {
        try {
            val safeSeek = minOf(_currentTime.value, _videoLength.value - 3)
            val seekFraction = (safeSeek.toFloat() / _videoLength.value).coerceIn(0f, 0.95f)
            mediaPlayer.setPosition(seekFraction)
        } catch (e: Exception) {
            println("❌ Error while seeking: ${e.message}")
        } finally {
            seeking = false
        }
    }

    // ───────────────────────────────────────────────────────────────
    // ❌ Exit App (Double Back Press)
    // ───────────────────────────────────────────────────────────────

    fun requestExit() {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime < 2000) {
            _shouldExitApp.value = true
        } else {
            _showExitPrompt.value = true
            lastBackPressTime = now
            viewModelScope.launch {
                delay(2000)
                _showExitPrompt.value = false
            }
        }
    }
}
