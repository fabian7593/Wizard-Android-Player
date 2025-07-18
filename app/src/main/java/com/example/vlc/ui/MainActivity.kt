package com.example.vlc

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.vlc.player.VLCPlayerView
import com.example.vlc.ui.theme.VLCTheme
import com.example.vlc.ui.theme.black
import com.example.vlc.utils.GeneralUtils
import com.example.vlc.viewmodel.VideoViewModel
import com.example.vlc.widgets.CustomVideoSlider
import com.example.vlc.widgets.ScrollableDialogList
import com.example.vlc.widgets.TvIconButton
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

                val playFocusRequester = remember { FocusRequester() }
                val sliderFocusRequester = remember { FocusRequester() }

                val viewModel = remember { VideoViewModel() }
                viewModel.mediaPlayer = mediaPlayer

                val isPlaying by viewModel.isPlaying.collectAsState()
                val isBuffering by viewModel.isBuffering.collectAsState()
                val currentTime by viewModel.currentTime.collectAsState()
                val videoLength by viewModel.videoLength.collectAsState()
                val showControls by viewModel.showControls.collectAsState()
                val videoUrl by viewModel.videoUrl.collectAsState()
                val shouldExitApp by viewModel.shouldExitApp.collectAsState()
                val showExitPrompt by viewModel.showExitPrompt.collectAsState()

                val showAudioDialog = remember { mutableStateOf(false) }
                var showSubtitlesDialog by remember { mutableStateOf(false) }
                var showAspectRatioDialog by remember { mutableStateOf(false) }

                val audioTracks = remember { mutableStateListOf<Pair<Int, String>>() }
                val subtitleTracks = remember { mutableStateListOf<Pair<Int, String>>() }

                val isPlayFocused = remember { mutableStateOf(false) }
                val isSubFocused = remember { mutableStateOf(false) }
                val isAudioFocused = remember { mutableStateOf(false) }
                val isAspectFocused = remember { mutableStateOf(false) }

                var movieNumber by remember { mutableStateOf("") }

                val backCallback = rememberUpdatedState {
                    if (viewModel.showControls.value) {
                        viewModel.toggleControls(false)
                    } else {
                        viewModel.requestExit()
                    }
                }

                BackHandler {
                    backCallback.value()
                }

                LaunchedEffect(shouldExitApp) {
                    if (shouldExitApp) finish()
                }

                LaunchedEffect(showControls) {
                    if (showControls) {
                        delay(200)
                        playFocusRequester.requestFocus()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures {
                                viewModel.toggleControls(true)
                                viewModel.setUserInteracting(true)
                                viewModel.startUserInteractionTimeout()
                            }
                        }
                        .onKeyEvent { keyEvent ->
                            val isBack = keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK
                            val isDown = keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN

                            if (isDown && isBack) {
                                if (viewModel.showControls.value) {
                                    viewModel.toggleControls(false)
                                } else {
                                    viewModel.requestExit()
                                }
                                true
                            } else if (isDown) {
                                viewModel.toggleControls(true)
                                viewModel.setUserInteracting(true)
                                viewModel.startUserInteractionTimeout()
                                false
                            } else {
                                false
                            }

                         }
                        .focusable()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(black)
                    ) {
                        if (videoUrl.isNotEmpty()) {
                            VLCPlayerView(
                                modifier = Modifier.fillMaxSize(),
                                mediaPlayer = mediaPlayer,
                                videoUrl = videoUrl,
                                onTracksLoaded = { audioTrack ->
                                    audioTracks.clear();
                                    audioTracks.addAll(audioTrack)
                                },
                                onSubtitleLoaded =  { subtitleTrack ->
                                    subtitleTracks.clear();
                                    subtitleTracks.addAll(subtitleTrack)
                                },
                                onPlaybackStateChanged = { viewModel.onPlaybackChanged(it) },
                                onBufferingChanged = { viewModel.onBufferingChanged(it) },
                                onDurationChanged = { viewModel.onDurationChanged(it) }
                            )
                        }
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
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text("Película Destacada", color = Color.White)
                                    Text("Subtítulo interesante", color = Color.LightGray)
                                }
                            }



                            Row(
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .padding(top = 12.dp, end = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = movieNumber,
                                    onValueChange = {
                                        if (it.all { ch -> ch.isDigit() }) {
                                            movieNumber = it
                                        }
                                    },
                                    label = { Text("ID") },
                                    singleLine = true,
                                    modifier = Modifier.width(80.dp)
                                )

                                Button(
                                    onClick = {
                                        if (movieNumber.isNotEmpty()) {
                                            val newUrl = "http://161.97.128.152:80/movie/test777/test777/${movieNumber}.mkv"
                                            viewModel.setVideoUrl(newUrl)
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("Cargar")
                                }
                            }


                            Spacer(modifier = Modifier.weight(1f))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp)
                            ) {
                                Text(
                                    text = "${GeneralUtils.formatTime(currentTime)} / ${GeneralUtils.formatTime(videoLength)}",
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
                                    },
                                    onFocusDown = { playFocusRequester.requestFocus() },
                                    onUserInteracted = {
                                        viewModel.setUserInteracting(true)
                                        viewModel.startUserInteractionTimeout()
                                    },
                                    modifier = Modifier.focusRequester(sliderFocusRequester)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TvIconButton(
                                    onClick = {
                                        if (isPlaying) mediaPlayer.pause() else mediaPlayer.play()
                                        viewModel.setIsPlaying(!isPlaying)
                                        viewModel.toggleControls(true)
                                    },
                                    focusRequester = playFocusRequester,
                                    isFocused = isPlayFocused,
                                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    description = "Play/Pause",
                                    tint = Color.White,
                                    enabled = videoUrl.isNotEmpty() && videoLength > 0,
                                    onUserInteracted = {
                                        viewModel.setUserInteracting(true)
                                        viewModel.startUserInteractionTimeout()
                                    }
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    TvIconButton(
                                        onClick = {
                                            if (subtitleTracks.isNotEmpty()) {
                                                showSubtitlesDialog = true
                                            }
                                        },
                                        isFocused = isSubFocused,
                                        icon = Icons.Default.Subtitles,
                                        description = "Subtítulos",
                                        tint = Color.White,
                                        enabled = subtitleTracks.isNotEmpty() && videoUrl.isNotEmpty() && videoLength > 0,
                                        onUserInteracted = {
                                            viewModel.setUserInteracting(true)
                                            viewModel.startUserInteractionTimeout()
                                        }
                                    )

                                    TvIconButton(
                                        onClick = {
                                            if (audioTracks.isNotEmpty()) {
                                                showAudioDialog.value = true
                                            }
                                        },
                                        isFocused = isAudioFocused,
                                        icon = Icons.Default.VolumeUp,
                                        description = "Idioma",
                                        tint = Color.White,
                                        enabled = audioTracks.isNotEmpty() && videoUrl.isNotEmpty() && videoLength > 0,
                                        onUserInteracted = {
                                            viewModel.setUserInteracting(true)
                                            viewModel.startUserInteractionTimeout()
                                        }
                                    )

                                    TvIconButton(
                                        onClick = { showAspectRatioDialog = true },
                                        isFocused = isAspectFocused,
                                        icon = Icons.Default.AspectRatio,
                                        description = "Aspect Ratio",
                                        tint = Color.White,
                                        enabled = videoUrl.isNotEmpty() && videoLength > 0,
                                        onUserInteracted = {
                                            viewModel.setUserInteracting(true)
                                            viewModel.startUserInteractionTimeout()
                                        }
                                    )
                                }
                            }



                        }
                    }

                    if (showAudioDialog.value && audioTracks.isNotEmpty()) {
                        ScrollableDialogList(
                            title = "Selecciona un idioma",
                            items = audioTracks,
                            onItemSelected = { id -> mediaPlayer.setAudioTrack(id) },
                            onDismiss = { showAudioDialog.value = false },
                            onUserInteracted = {
                                viewModel.setUserInteracting(true)
                                viewModel.startUserInteractionTimeout()
                            }
                        )
                    }

                    if (showSubtitlesDialog && subtitleTracks.isNotEmpty()) {
                        ScrollableDialogList(
                            title = "Seleccionar subtítulo",
                            items = subtitleTracks,
                            onItemSelected = { id -> mediaPlayer.setSpuTrack(id) },
                            onDismiss = { showSubtitlesDialog = false },
                            onUserInteracted = {
                                viewModel.setUserInteracting(true)
                                viewModel.startUserInteractionTimeout()
                            }
                        )
                    }

                    if (showAspectRatioDialog) {
                        ScrollableDialogList(
                            title = "Aspect Ratio",
                            items = listOf(
                                0 to "Auto Fit", 5 to "Fill", 8 to "Cinematic", 1 to "16:9", 2 to "4:3"
                            ),
                            onItemSelected = {
                                when (it) {
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
                            onDismiss = { showAspectRatioDialog = false },
                            onUserInteracted = {
                                viewModel.setUserInteracting(true)
                                viewModel.startUserInteractionTimeout()
                            }
                        )
                    }

                    if (showExitPrompt) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 32.dp)
                        ) {
                            Surface(color = Color.Black.copy(alpha = 0.8f), shape = MaterialTheme.shapes.medium) {
                                Text(
                                    text = "Presiona nuevamente para salir",
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.stop()
        mediaPlayer.release()
        libVLC.release()
    }
}
