package com.example.vlc

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.vlc.player.VLCPlayerView
import com.example.vlc.ui.theme.VLCTheme
import com.example.vlc.viewmodel.VideoViewModel
import com.example.vlc.widgets.CustomVideoSlider
import com.example.vlc.widgets.ScrollableDialogList
import kotlinx.coroutines.delay
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.MediaPlayer

class MainActivity : ComponentActivity() {

    private lateinit var libVLC: LibVLC
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        libVLC = LibVLC(this, arrayListOf(
            "--no-drop-late-frames",
            "--no-skip-frames",
            "--avcodec-fast",
            "--avcodec-hw=any",
            "--file-caching=3000",
            "--network-caching=7777",
            "--codec=avcodec"
        ))

        mediaPlayer = MediaPlayer(libVLC)

        setContent {
            VLCTheme(darkTheme = true) {
                val viewModel = remember { VideoViewModel() }
                viewModel.mediaPlayer = mediaPlayer

                val isPlaying by viewModel.isPlaying.collectAsState()
                val isBuffering by viewModel.isBuffering.collectAsState()
                val currentTime by viewModel.currentTime.collectAsState()
                val videoLength by viewModel.videoLength.collectAsState()
                val showControls by viewModel.showControls.collectAsState()
                val videoUrl by viewModel.videoUrl.collectAsState()

                val showAudioDialog = remember { mutableStateOf(false) }
                var showSubtitlesDialog by remember { mutableStateOf(false) }
                var showAspectRatioDialog by remember { mutableStateOf(false) }

                val audioTracks = remember { mutableStateListOf<Pair<Int, String>>() }
                val subtitleTracks = remember { mutableStateListOf<Pair<Int, String>>() }

                val focusColor = Color(0xFF1976D2)

                val isPlayFocused = remember { mutableStateOf(false) }
                val isSubFocused = remember { mutableStateOf(false) }
                val isAudioFocused = remember { mutableStateOf(false) }
                val isAspectFocused = remember { mutableStateOf(false) }

                val playButtonFocusRequester = remember { FocusRequester() }

                LaunchedEffect(Unit) {
                    delay(600)
                    playButtonFocusRequester.requestFocus()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures {
                                viewModel.toggleControls(true)
                                viewModel.setUserInteracting(true)
                            }
                        }
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                viewModel.toggleControls(true)
                                viewModel.setUserInteracting(true)
                            }
                            false
                        }
                        .focusable()
                ) {
                    viewModel.setVideoUrl("http://movie.mkv")

                    if (videoUrl.isNotEmpty()) {
                        VLCPlayerView(
                            modifier = Modifier.fillMaxSize(),
                            mediaPlayer = mediaPlayer,
                            videoUrl = videoUrl,
                            onTracksLoaded = {
                                audioTracks.clear(); audioTracks.addAll(it)
                            },
                            onSubtitleLoaded = {
                                subtitleTracks.clear(); subtitleTracks.addAll(it)
                            },
                            onPlaybackStateChanged = { viewModel.onPlaybackChanged(it) },
                            onBufferingChanged = { viewModel.onBufferingChanged(it) },
                            onDurationChanged = { viewModel.onDurationChanged(it) }
                        )
                    }

                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.White
                        )
                    }

                    AnimatedVisibility(
                        visible = showControls,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { finish() },
                                    modifier = Modifier.size(40.dp).focusable()
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                                }

                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text("Película Destacada", color = Color.White)
                                    Text("Subtítulo interesante", color = Color.LightGray)
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp)
                            ) {
                                Text(
                                    text = "${formatTime(currentTime)} / ${formatTime(videoLength)}",
                                    modifier = Modifier.align(Alignment.End),
                                    color = Color.White
                                )

                                CustomVideoSlider(
                                    currentTime = currentTime,
                                    videoLength = videoLength,
                                    onSeekChanged = {
                                        viewModel.onSeekStart()
                                        viewModel.onSeekUpdate(it)
                                    },
                                    onSeekFinished = {
                                        viewModel.onSeekFinished()
                                    }
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (isPlaying) mediaPlayer.pause() else mediaPlayer.play()
                                        viewModel.setIsPlaying(!isPlaying)
                                        viewModel.toggleControls(true)
                                    },
                                    modifier = Modifier
                                        .focusRequester(playButtonFocusRequester)
                                        .focusable()
                                        .onFocusChanged { focusState ->
                                            isPlayFocused.value = focusState.isFocused
                                            viewModel.setUserInteracting(focusState.isFocused)
                                        }
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = if (isPlayFocused.value) focusColor else Color.White
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    IconButton(
                                        onClick = { showSubtitlesDialog = true },
                                        modifier = Modifier
                                            .focusable()
                                            .onFocusChanged {
                                                isSubFocused.value = it.isFocused
                                                viewModel.setUserInteracting(it.isFocused)
                                            }
                                    ) {
                                        Icon(
                                            Icons.Default.Subtitles,
                                            contentDescription = "Subtítulos",
                                            tint = if (isSubFocused.value) focusColor else Color.White
                                        )
                                    }

                                    IconButton(
                                        onClick = { showAudioDialog.value = true },
                                        modifier = Modifier
                                            .focusable()
                                            .onFocusChanged {
                                                isAudioFocused.value = it.isFocused
                                                viewModel.setUserInteracting(it.isFocused)
                                            }
                                    ) {
                                        Icon(
                                            Icons.Default.VolumeUp,
                                            contentDescription = "Idioma",
                                            tint = if (isAudioFocused.value) focusColor else Color.White
                                        )
                                    }

                                    IconButton(
                                        onClick = { showAspectRatioDialog = true },
                                        modifier = Modifier
                                            .focusable()
                                            .onFocusChanged {
                                                isAspectFocused.value = it.isFocused
                                                viewModel.setUserInteracting(it.isFocused)
                                            }
                                    ) {
                                        Icon(
                                            Icons.Default.AspectRatio,
                                            contentDescription = "Aspect Ratio",
                                            tint = if (isAspectFocused.value) focusColor else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (showAudioDialog.value) {
                        ScrollableDialogList(
                            title = "Selecciona un idioma",
                            items = audioTracks,
                            onItemSelected = { id -> mediaPlayer.setAudioTrack(id) },
                            onDismiss = { showAudioDialog.value = false }
                        )
                    }

                    if (showSubtitlesDialog) {
                        ScrollableDialogList(
                            title = "Seleccionar subtítulo",
                            items = subtitleTracks,
                            onItemSelected = { id -> mediaPlayer.setSpuTrack(id) },
                            onDismiss = { showSubtitlesDialog = false }
                        )
                    }

                    if (showAspectRatioDialog) {
                        ScrollableDialogList(
                            title = "Aspect Ratio",
                            items = listOf(
                                0 to "Auto Fit",
                                5 to "Fill",
                                8 to "Cinematic",
                                1 to "16:9",
                                2 to "4:3"
                            ),
                            onItemSelected = { selected ->
                                when (selected) {
                                    0 -> mediaPlayer.setAspectRatio("").also { mediaPlayer.setScale(0f) }
                                    1 -> mediaPlayer.setAspectRatio("16:9").also { mediaPlayer.setScale(0f) }
                                    2 -> mediaPlayer.setAspectRatio("4:3").also { mediaPlayer.setScale(0f) }
                                    5 -> mediaPlayer.setAspectRatio("21:9").also {
                                        mediaPlayer.setScale(1f)
                                        mediaPlayer.setVideoScale(MediaPlayer.ScaleType.SURFACE_FILL)
                                    }
                                    8 -> mediaPlayer.setAspectRatio("2:1").also { mediaPlayer.setScale(0f) }
                                }
                            },
                            onDismiss = { showAspectRatioDialog = false }
                        )
                    }
                }
            }
        }
    }

    private fun formatTime(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(mins, secs)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.stop()
        mediaPlayer.release()
        libVLC.release()
    }
}
