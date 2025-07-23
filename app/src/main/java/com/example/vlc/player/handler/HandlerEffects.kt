package com.example.vlc.player.handler

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.focus.FocusRequester
import com.example.vlc.player.config.VideoItem
import com.example.vlc.player.config.Config.createLibVlcConfig
import com.example.vlc.utils.NetworkMonitor
import com.example.vlc.viewmodel.VideoViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.MediaPlayer

@Composable
fun HandlePlayerCleanup(
    viewModel: VideoViewModel,
    libVLC: LibVLC?
) {
    DisposableEffect(Unit) {
        onDispose {
            try {
                viewModel.disposePlayer()
                libVLC?.release()
            } catch (e: Exception) {
                println("Cleanup error: ${e.message}")
            }
        }
    }
}

@Composable
fun HandleConnectionLoss(
    isConnected: Boolean,
    coroutineScope: CoroutineScope,
    mediaPlayer: MediaPlayer?,
    viewModel: VideoViewModel,
    showConnectionWarning: MutableState<Boolean>
) {
    LaunchedEffect(isConnected) {
        if (!isConnected) {
            coroutineScope.launch(Dispatchers.IO) {
                mediaPlayer?.pause()
                viewModel.setIsPlaying(false)
            }
            showConnectionWarning.value = true
            delay(3000)
            showConnectionWarning.value = false
        }
    }
}

@Composable
fun HandleNetworkMonitor(context: Context) {
    LaunchedEffect(Unit) {
        NetworkMonitor.start(context)
    }
}

@Composable
fun HandleMediaPlayerReinit(
    currentItem: VideoItem?,
    context: Context,
    mediaPlayer: MediaPlayer?,
    libVLC: LibVLC?,
    setMediaPlayer: (MediaPlayer?) -> Unit,
    setLibVLC: (LibVLC?) -> Unit,
    viewModel: VideoViewModel
) {
    LaunchedEffect(currentItem) {
        currentItem?.let { item ->
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                libVLC?.release()

                val options = createLibVlcConfig()
                val newLib = LibVLC(context, options)
                val newPlayer = MediaPlayer(newLib)

                setLibVLC(newLib)
                setMediaPlayer(newPlayer)
                viewModel.mediaPlayer = newPlayer
                viewModel.prepareVideoUrl(item.url)

            } catch (e: Exception) {
                println("⚠️ Error switching media player: ${e.message}")
            }
        }
    }
}

@Composable
fun HandleResumePlayback(
    isConnected: Boolean,
    currentItem: VideoItem?,
    mediaPlayer: MediaPlayer?,
    viewModel: VideoViewModel,
    coroutineScope: CoroutineScope
) {
    LaunchedEffect(isConnected, currentItem) {
        if (isConnected && !viewModel.isPlaying.value) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    mediaPlayer?.play()
                    viewModel.setIsPlaying(true)
                } catch (e: Exception) {
                    println("❌ Resume playback error: ${e.message}")
                }
            }
        }
    }
}

@Composable
fun HandleExitRequest(
    shouldExitApp: Boolean,
    currentTime: Long,
    onExit: () -> Unit,
    onGetCurrentTime: (Long) -> Unit
) {
    LaunchedEffect(shouldExitApp) {
        if (shouldExitApp) {
            onGetCurrentTime(currentTime)
            onExit()
        }
    }
}

@Composable
fun ReportCurrentPlaybackStatus(
    currentItem: VideoItem?,
    currentTime: Long,
    onGetCurrentTime: (Long) -> Unit,
    onGetCurrentItem: (VideoItem?) -> Unit
) {
    LaunchedEffect(currentItem, currentTime) {
        onGetCurrentTime(currentTime)
        onGetCurrentItem(currentItem)
    }
}


@Composable
fun HandleInitialFocus(
    showControls: Boolean,
    playFocusRequester: FocusRequester
) {
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(200) // necesario para que Compose termine la recomposición
            playFocusRequester.requestFocus()
        }
    }
}

