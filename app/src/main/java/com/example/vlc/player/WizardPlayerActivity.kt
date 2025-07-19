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
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeUp
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
import com.example.vlc.ui.theme.VLCTheme
import com.example.vlc.utils.GeneralUtils
import com.example.vlc.viewmodel.VideoViewModel
import com.example.vlc.widgets.CustomVideoSlider
import com.example.vlc.widgets.ScrollableDialogList
import com.example.vlc.widgets.TvIconButton
import kotlinx.coroutines.delay
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.MediaPlayer

class WizardPlayerActivity : ComponentActivity() {

    // VLC media engine core components
    private lateinit var libVLC: LibVLC
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ─────────────────────────────────────────────────────────
        // 🎬 Load player config and validate
        // ─────────────────────────────────────────────────────────

        val config = intent.getParcelableExtra<PlayerConfig>("player_config")
        val videoItems = config?.videoItems ?: emptyList()
        val index = config?.startIndex ?: 0

        if (videoItems.isEmpty()) {
            finish()
            return
        }

        // ─────────────────────────────────────────────────────────
        // 🖥 Configure window for immersive landscape video playback
        // ─────────────────────────────────────────────────────────

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // ─────────────────────────────────────────────────────────
        // 🎥 Initialize VLC
        // ─────────────────────────────────────────────────────────

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

        // ─────────────────────────────────────────────────────────
        // 🖼 Compose UI Content
        // ─────────────────────────────────────────────────────────

        setContent {
            VLCTheme(darkTheme = true) {
                val playFocusRequester = remember { FocusRequester() }
                val sliderFocusRequester = remember { FocusRequester() }

                val viewModel = remember { VideoViewModel() }
                viewModel.mediaPlayer = mediaPlayer

                // State collection
                val isPlaying by viewModel.isPlaying.collectAsState()
                val isBuffering by viewModel.isBuffering.collectAsState()
                val currentTime by viewModel.currentTime.collectAsState()
                val videoLength by viewModel.videoLength.collectAsState()
                val showControls by viewModel.showControls.collectAsState()
                val videoUrl by viewModel.videoUrl.collectAsState()
                val shouldExitApp by viewModel.shouldExitApp.collectAsState()
                val showExitPrompt by viewModel.showExitPrompt.collectAsState()

                // Dialog control
                val showAudioDialog = remember { mutableStateOf(false) }
                var showSubtitlesDialog by remember { mutableStateOf(false) }
                var showAspectRatioDialog by remember { mutableStateOf(false) }

                // Track lists
                val audioTracks = remember { mutableStateListOf<Pair<Int, String>>() }
                val subtitleTracks = remember { mutableStateListOf<Pair<Int, String>>() }

                // Focus states for buttons
                val isPlayFocused = remember { mutableStateOf(false) }
                val isSubFocused = remember { mutableStateOf(false) }
                val isAudioFocused = remember { mutableStateOf(false) }
                val isAspectFocused = remember { mutableStateOf(false) }

                // Index and current video
                val currentIndex = remember { mutableStateOf(index) }
                val currentItem = remember(currentIndex.value) { videoItems.getOrNull(currentIndex.value) }

                // Back press handling
                val backCallback = rememberUpdatedState {
                    if (viewModel.showControls.value) {
                        viewModel.toggleControls(false)
                    } else {
                        viewModel.requestExit()
                    }
                }

                // Load video on change
                LaunchedEffect(currentItem?.url) {
                    try {
                        currentItem?.let { viewModel.setVideoUrl(it.url) }
                    } catch (e: Exception) {
                        println("❌ Error loading video URL: ${e.message}")
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

                // ─────────────────────────────────────────────────────────
                // 📺 Main video player UI container
                // ─────────────────────────────────────────────────────────

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
                    // ───── Background and video ─────
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    ) {
                        if (videoUrl.isNotEmpty()) {
                            WizardPlayerView(
                                modifier = Modifier.fillMaxSize(),
                                mediaPlayer = mediaPlayer,
                                videoUrl = videoUrl,
                                onTracksLoaded = {
                                    audioTracks.clear()
                                    audioTracks.addAll(it)
                                },
                                onSubtitleLoaded = {
                                    subtitleTracks.clear()
                                    subtitleTracks.addAll(it)
                                },
                                onPlaybackStateChanged = { viewModel.onPlaybackChanged(it) },
                                onBufferingChanged = { viewModel.onBufferingChanged(it) },
                                onDurationChanged = { viewModel.onDurationChanged(it) }
                            )
                        }
                    }

                    // ───── Buffering Indicator ─────
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.White
                        )
                    }

                    // ───── Playback Controls ─────
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
                            // Video title and next button
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
                                            try {
                                                mediaPlayer.stop()
                                                currentIndex.value += 1
                                            } catch (e: Exception) {
                                                println("⚠️ Error switching to next video: ${e.message}")
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                        modifier = Modifier.align(Alignment.CenterEnd)
                                    ) {
                                        Text("Siguiente ▶")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Progress bar
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
                                    onSeekFinished = { viewModel.onSeekFinished() },
                                    onFocusDown = { playFocusRequester.requestFocus() },
                                    onUserInteracted = {
                                        viewModel.setUserInteracting(true)
                                        viewModel.startUserInteractionTimeout()
                                    },
                                    modifier = Modifier.focusRequester(sliderFocusRequester)
                                )
                            }

                            // Buttons
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
                                        onClick = { if (subtitleTracks.isNotEmpty()) showSubtitlesDialog = true },
                                        isFocused = isSubFocused,
                                        icon = Icons.Default.Subtitles,
                                        description = "Subtitles",
                                        tint = Color.White,
                                        enabled = subtitleTracks.isNotEmpty() && videoUrl.isNotEmpty() && videoLength > 0,
                                        onUserInteracted = {
                                            viewModel.setUserInteracting(true)
                                            viewModel.startUserInteractionTimeout()
                                        }
                                    )

                                    TvIconButton(
                                        onClick = { if (audioTracks.isNotEmpty()) showAudioDialog.value = true },
                                        isFocused = isAudioFocused,
                                        icon = Icons.Default.VolumeUp,
                                        description = "Audio Track",
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

                    // ───── Dialogs ─────
                    if (showAudioDialog.value && audioTracks.isNotEmpty()) {
                        ScrollableDialogList(
                            title = "Select audio",
                            items = audioTracks,
                            onItemSelected = { mediaPlayer.setAudioTrack(it) },
                            onDismiss = { showAudioDialog.value = false },
                            onUserInteracted = {
                                viewModel.setUserInteracting(true)
                                viewModel.startUserInteractionTimeout()
                            }
                        )
                    }

                    if (showSubtitlesDialog && subtitleTracks.isNotEmpty()) {
                        ScrollableDialogList(
                            title = "Select subtitles",
                            items = subtitleTracks,
                            onItemSelected = { mediaPlayer.setSpuTrack(it) },
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

                    // ───── Exit prompt ─────
                    if (showExitPrompt) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 32.dp)
                        ) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.8f),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(
                                    text = "Press back again to exit",
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

    // ─────────────────────────────────────────────────────────
    // ♻ Clean up VLC resources on destroy
    // ─────────────────────────────────────────────────────────

    override fun onDestroy() {
        super.onDestroy()
        try {
            mediaPlayer.stop()
            mediaPlayer.release()
            libVLC.release()
        } catch (e: Exception) {
            println("❌ Error while releasing media player: ${e.message}")
        }
    }
}
