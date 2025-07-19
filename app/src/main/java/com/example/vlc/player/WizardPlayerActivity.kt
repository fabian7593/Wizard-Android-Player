package com.example.vlc.player

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import com.example.vlc.ui.theme.VLCTheme
import com.example.vlc.utils.GeneralUtils
import com.example.vlc.viewmodel.VideoViewModel
import com.example.vlc.widgets.CustomVideoSlider
import com.example.vlc.widgets.ScrollableDialogList
import com.example.vlc.widgets.TvIconButton
import kotlinx.coroutines.delay
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class WizardPlayerActivity : ComponentActivity() {

    private lateinit var libVLC: LibVLC
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = intent.getParcelableExtra<PlayerConfig>("player_config")

        val videoItems = config?.videoItems ?: emptyList()
        val index = config?.startIndex ?: 0

        if (videoItems.isEmpty()) {
            finish()
            return
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        libVLC = LibVLC(
            this, arrayListOf(
                "--no-drop-late-frames",
                "--no-skip-frames",
                "--avcodec-fast",
                "--avcodec-hw=any",
                "--file-caching=3000",
                "--network-caching=7777",
                "--codec=avcodec"
            )
        )

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
23
                val audioTracks = remember { mutableStateListOf<Pair<Int, String>>() }
                val subtitleTracks = remember { mutableStateListOf<Pair<Int, String>>() }

                val isPlayFocused = remember { mutableStateOf(false) }
                val isSubFocused = remember { mutableStateOf(false) }
                val isAudioFocused = remember { mutableStateOf(false) }
                val isAspectFocused = remember { mutableStateOf(false) }

                val currentIndex = remember { mutableStateOf(index) }
                val currentItem = remember(currentIndex.value) { videoItems.getOrNull(currentIndex.value) }


                val backCallback = rememberUpdatedState {
                    if (viewModel.showControls.value) {
                        viewModel.toggleControls(false)
                    } else {
                        viewModel.requestExit()
                    }
                }

                LaunchedEffect(currentItem?.url) {
                    currentItem?.let {
                        viewModel.setVideoUrl(it.url)
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
                    modifier = Modifier.Companion
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
                        modifier = Modifier.Companion
                            .fillMaxSize()
                            .background(Color.Black)
                    ) {
                        if (videoUrl.isNotEmpty()) {
                            WizardPlayerView(
                                modifier = Modifier.Companion.fillMaxSize(),
                                mediaPlayer = mediaPlayer,
                                videoUrl = videoUrl,
                                onTracksLoaded = { audioTrack ->
                                    audioTracks.clear();
                                    audioTracks.addAll(audioTrack)
                                },
                                onSubtitleLoaded = { subtitleTrack ->
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
                            modifier = Modifier.Companion.align(Alignment.Companion.Center),
                            color = Color.Companion.White
                        )
                    }

                    AnimatedVisibility(
                        visible = showControls,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.Companion
                                .fillMaxSize()
                                .padding(bottom = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                                    Text(currentItem?.title ?: "", color = Color.White)
                                    Text(currentItem?.subtitle ?: "", color = Color.LightGray)
                                }

                                if (videoItems.size > 1 && currentIndex.value < videoItems.lastIndex) {
                                    Button(
                                        onClick = {
                                            mediaPlayer.stop()
                                            currentIndex.value += 1
                                        },
                                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                        modifier = Modifier.align(Alignment.CenterEnd)
                                    ) {
                                        Text("Siguiente ▶")
                                    }
                                }
                            }


                            Spacer(modifier = Modifier.Companion.weight(1f))

                            Column(
                                modifier = Modifier.Companion
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp)
                            ) {
                                Text(
                                    text = "${GeneralUtils.formatTime(currentTime)} / ${
                                        GeneralUtils.formatTime(
                                            videoLength
                                        )
                                    }",
                                    modifier = Modifier.Companion.align(Alignment.Companion.End),
                                    color = Color.Companion.White
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
                                    modifier = Modifier.Companion.focusRequester(
                                        sliderFocusRequester
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.Companion
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Companion.CenterVertically
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
                                    tint = Color.Companion.White,
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
                                        tint = Color.Companion.White,
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
                                        tint = Color.Companion.White,
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
                                        tint = Color.Companion.White,
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
                                0 to "Auto Fit",
                                5 to "Fill",
                                8 to "Cinematic",
                                1 to "16:9",
                                2 to "4:3"
                            ),
                            onItemSelected = {
                                when (it) {
                                    0 -> mediaPlayer.setAspectRatio("")
                                        .also { mediaPlayer.setScale(0f) }

                                    1 -> mediaPlayer.setAspectRatio("16:9")
                                        .also { mediaPlayer.setScale(0f) }

                                    2 -> mediaPlayer.setAspectRatio("4:3")
                                        .also { mediaPlayer.setScale(0f) }

                                    5 -> mediaPlayer.setAspectRatio("21:9").also {
                                        mediaPlayer.setScale(1f)
                                        mediaPlayer.setVideoScale(MediaPlayer.ScaleType.SURFACE_FILL)
                                    }

                                    8 -> mediaPlayer.setAspectRatio("2:1")
                                        .also { mediaPlayer.setScale(0f) }
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
                            modifier = Modifier.Companion
                                .align(Alignment.Companion.BottomCenter)
                                .padding(bottom = 32.dp)
                        ) {
                            Surface(
                                color = Color.Companion.Black.copy(alpha = 0.8f),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(
                                    text = "Presiona nuevamente para salir",
                                    color = Color.Companion.White,
                                    modifier = Modifier.Companion.padding(
                                        horizontal = 16.dp,
                                        vertical = 10.dp
                                    )
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