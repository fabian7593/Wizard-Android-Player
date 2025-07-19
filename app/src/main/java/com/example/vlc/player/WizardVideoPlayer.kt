package com.example.vlc.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalContext
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

/**
 * WizardVideoPlayer composable
 * Displays fullscreen VLC-based video playback with customizable controls and multilingual labels.
 */
@Composable
fun WizardVideoPlayer(
    config: PlayerConfig,
    labels: PlayerLabels,
    onExit: () -> Unit
) {
    VLCTheme(darkTheme = true) {
        val context = LocalContext.current
        val activity = context as? Activity

        SideEffect {
            try {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                WindowCompat.setDecorFitsSystemWindows(activity!!.window, true)
                WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
                    hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
                    systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } catch (e: Exception) {
                println("⚠️ Error configuring fullscreen landscape mode: ${e.message}")
            }
        }

        // ───────────────────────────────────────────────────────
        // 🎞️ VLC engine and media player setup
        // ───────────────────────────────────────────────────────
        val libVLC = remember {
            LibVLC(
                context,
                arrayListOf(
                    "--no-drop-late-frames",
                    "--no-skip-frames",
                    "--avcodec-fast",
                    "--avcodec-hw=any",
                    "--file-caching=3000",
                    "--network-caching=7777",
                    "--codec=avcodec"
                )
            )
        }

        val mediaPlayer = remember { MediaPlayer(libVLC) }

        // ───────────────────────────────────────────────────────
        // 🔁 State and UI holders
        // ───────────────────────────────────────────────────────
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

        val currentIndex = remember { mutableStateOf(config.startIndex) }
        val currentItem = remember(currentIndex.value) {
            config.videoItems.getOrNull(currentIndex.value)
        }

        // ───────────────────────────────────────────────────────
        // 📽️ Load video and manage playback state
        // ───────────────────────────────────────────────────────
        LaunchedEffect(currentItem?.url) {
            try {
                currentItem?.let { viewModel.setVideoUrl(it.url) }
            } catch (e: Exception) {
                println("❌ Failed to load video URL: ${e.message}")
            }
        }

        LaunchedEffect(showControls) {
            if (showControls) {
                delay(200)
                playFocusRequester.requestFocus()
            }
        }

        LaunchedEffect(shouldExitApp) {
            if (shouldExitApp) {
                onExit()
            }
        }

        BackHandler {
            if (viewModel.showControls.value) {
                viewModel.toggleControls(false)
            } else {
                viewModel.requestExit()
            }
        }

        // ───────────────────────────────────────────────────────
        // 🧱 Main player container with interactions
        // ───────────────────────────────────────────────────────
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
                    } else false
                }
                .focusable()
        ) {
            // ───── Video Background ─────
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
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

            // ───── Buffer Indicator ─────
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }

            // ───── UI Controls Overlay ─────
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(bottom = 16.dp)) {

                    // ─── Header Section (Titles + Next) ───
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Column(modifier = Modifier.align(Alignment.CenterStart)) {
                            Text(currentItem?.title ?: "", color = Color.White)
                            Text(currentItem?.subtitle ?: "", color = Color.LightGray)
                        }

                        if (config.videoItems.size > 1 && currentIndex.value < config.videoItems.lastIndex) {
                            Button(
                                onClick = {
                                    try {
                                        mediaPlayer.stop()
                                        currentIndex.value += 1
                                    } catch (e: Exception) {
                                        println("⚠️ Failed to switch video: ${e.message}")
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                Text(labels.nextLabel)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // ─── Progress & Timestamp ───
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)) {
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
                            activeColor =  Color(config.primaryColor),
                            focusColor = Color(config.focusColor),
                            inactiveColor = Color(config.inactiveColor),
                            modifier = Modifier.focusRequester(sliderFocusRequester)
                        )
                    }

                    // ─── Playback Buttons ───
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TvIconButton(
                            onClick = {
                                try {
                                    if (isPlaying) mediaPlayer.pause() else mediaPlayer.play()
                                    viewModel.setIsPlaying(!isPlaying)
                                    viewModel.toggleControls(true)
                                } catch (e: Exception) {
                                    println("❌ Playback toggle failed: ${e.message}")
                                }
                            },
                            focusRequester = playFocusRequester,
                            isFocused = isPlayFocused,
                            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            description = labels.nextLabel,
                            activeColor =  Color(config.primaryColor),
                            focusColor = Color(config.focusColor),
                            enabled = videoUrl.isNotEmpty() && videoLength > 0,
                            onUserInteracted = {
                                viewModel.setUserInteracting(true)
                                viewModel.startUserInteractionTimeout()
                            },
                            diameterButtonCircleDp = config.diameterButtonCircleDp.dp,
                            iconSizeDp = config.iconSizeDp.dp
                        )

                        // ─── Extra Buttons Row ───
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (config.showSubtitleButton) {
                                TvIconButton(
                                    onClick = { if (subtitleTracks.isNotEmpty()) showSubtitlesDialog = true },
                                    isFocused = isSubFocused,
                                    icon = Icons.Default.Subtitles,
                                    description = labels.subtitleLabel,
                                    activeColor =  Color(config.primaryColor),
                                    focusColor = Color(config.focusColor),
                                    enabled = subtitleTracks.isNotEmpty(),
                                    onUserInteracted = {
                                        viewModel.setUserInteracting(true)
                                        viewModel.startUserInteractionTimeout()
                                    },
                                    diameterButtonCircleDp = config.diameterButtonCircleDp.dp,
                                    iconSizeDp = config.iconSizeDp.dp
                                )
                            }

                            if (config.showAudioButton) {
                                TvIconButton(
                                    onClick = { if (audioTracks.isNotEmpty()) showAudioDialog.value = true },
                                    isFocused = isAudioFocused,
                                    icon = Icons.Default.VolumeUp,
                                    description = labels.audioLabel,
                                    activeColor =  Color(config.primaryColor),
                                    focusColor = Color(config.focusColor),
                                    enabled = audioTracks.isNotEmpty(),
                                    onUserInteracted = {
                                        viewModel.setUserInteracting(true)
                                        viewModel.startUserInteractionTimeout()
                                    },
                                    diameterButtonCircleDp = config.diameterButtonCircleDp.dp,
                                    iconSizeDp = config.iconSizeDp.dp
                                )
                            }

                            if (config.showAspectRatioButton) {
                                TvIconButton(
                                    onClick = { showAspectRatioDialog = true },
                                    isFocused = isAspectFocused,
                                    icon = Icons.Default.AspectRatio,
                                    description = labels.aspectRatioLabel,
                                    activeColor =  Color(config.primaryColor),
                                    focusColor = Color(config.focusColor),
                                    enabled = true,
                                    onUserInteracted = {
                                        viewModel.setUserInteracting(true)
                                        viewModel.startUserInteractionTimeout()
                                    },
                                    diameterButtonCircleDp = config.diameterButtonCircleDp.dp,
                                    iconSizeDp = config.iconSizeDp.dp
                                )
                            }
                        }
                    }
                }
            }

            // ───── Audio / Subtitle / Aspect Dialogs ─────
            if (showAudioDialog.value) {
                ScrollableDialogList(
                    title = labels.selectAudioTitle,
                    items = audioTracks,
                    onItemSelected = { mediaPlayer.setAudioTrack(it) },
                    onDismiss = { showAudioDialog.value = false },
                    onUserInteracted = {
                        viewModel.setUserInteracting(true)
                        viewModel.startUserInteractionTimeout()
                    }
                )
            }

            if (showSubtitlesDialog) {
                ScrollableDialogList(
                    title = labels.selectSubtitleTitle,
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
                    title = labels.aspectRatioTitle,
                    items = listOf(
                        0 to "Auto Fit",
                        5 to "Fill",
                        8 to "Cinematic",
                        1 to "16:9",
                        2 to "4:3"
                    ),
                    onItemSelected = {
                        try {
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
                        } catch (e: Exception) {
                            println("❌ Error changing aspect ratio: ${e.message}")
                        }
                    },
                    onDismiss = { showAspectRatioDialog = false },
                    onUserInteracted = {
                        viewModel.setUserInteracting(true)
                        viewModel.startUserInteractionTimeout()
                    }
                )
            }

            // ───── Exit Prompt (double back) ─────
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
                            text = labels.exitPrompt,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}
