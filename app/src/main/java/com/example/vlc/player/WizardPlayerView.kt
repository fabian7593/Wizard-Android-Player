package com.example.vlc.player

import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.vlc.utils.GeneralUtils.shouldForceHWDecoding
import com.example.vlc.utils.LanguageMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

@Composable
fun WizardPlayerView(
    modifier: Modifier = Modifier,
    config: PlayerConfig,
    mediaPlayer: MediaPlayer,
    videoUrl: String,
    onTracksLoaded: (List<Pair<Int, String>>) -> Unit,
    onSubtitleLoaded: (List<Pair<Int, String>>) -> Unit,
    onPlaybackStateChanged: (Boolean) -> Unit,
    onEndReached: () -> Unit,
    onBufferingChanged: (Boolean) -> Unit,
    onDurationChanged: (Long) -> Unit,
    onStart: () -> Unit,
    onAspectRatioChanged: ((String) -> Unit)? = null,
    onAudioChanged: ((String) -> Unit)? = null,
    onSubtitleChanged: ((String) -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    var surfaceHolder by remember { mutableStateOf<SurfaceHolder?>(null) }
    var surfaceViewRef by remember { mutableStateOf<SurfaceView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val surfaceView = SurfaceView(context)
            surfaceViewRef = surfaceView

            surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    surfaceHolder = holder
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    surfaceHolder = null
                    try {
                        if (mediaPlayer.isReleased.not()) {
                            mediaPlayer.stop()
                            mediaPlayer.vlcVout.detachViews()
                        }
                    } catch (e: Exception) {
                        println("Error during surfaceDestroyed: ${e.message}")
                    }
                }
            })

            surfaceView
        },
        onRelease = {
            try {
                mediaPlayer.stop()
                mediaPlayer.vlcVout.detachViews()
                mediaPlayer.setEventListener(null)
            } catch (e: Exception) {
                println("❌ Error during VLC player release: ${e.message}")
            }
        }
    )

    LaunchedEffect(surfaceHolder, videoUrl) {
        if (surfaceHolder == null || surfaceViewRef == null || videoUrl.isBlank()) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            try {
                val context = surfaceViewRef!!.context
                val displayMetrics = context.resources.displayMetrics
                val width = displayMetrics.widthPixels
                val height = displayMetrics.heightPixels

                when (config.preferenceVideoSize.lowercase()) {
                    "autofit" -> {
                        mediaPlayer.setAspectRatio("")
                        mediaPlayer.setScale(0f)
                        onAspectRatioChanged?.invoke("autofit")
                    }
                    "fill" -> {
                        mediaPlayer.setAspectRatio("21:9")
                        mediaPlayer.setScale(1f)
                        mediaPlayer.setVideoScale(MediaPlayer.ScaleType.SURFACE_FILL)
                        onAspectRatioChanged?.invoke("fill")
                    }
                    "cinematic" -> {
                        mediaPlayer.setAspectRatio("2:1")
                        mediaPlayer.setScale(0f)
                        onAspectRatioChanged?.invoke("cinematic")
                    }
                    "16:9" -> {
                        mediaPlayer.setAspectRatio("16:9")
                        mediaPlayer.setScale(0f)
                        onAspectRatioChanged?.invoke("16:9")
                    }
                    "4:3" -> {
                        mediaPlayer.setAspectRatio("4:3")
                        mediaPlayer.setScale(0f)
                        onAspectRatioChanged?.invoke("4:3")
                    }
                    else -> {
                        mediaPlayer.setAspectRatio("")
                        mediaPlayer.setScale(0f)
                        onAspectRatioChanged?.invoke("autofit")
                    }
                }

                val vout = mediaPlayer.vlcVout
                vout.detachViews()
                vout.setVideoView(surfaceViewRef)
                vout.setWindowSize(width, height)
                vout.attachViews(null)

                val media = Media(mediaPlayer.libVLC, Uri.parse(videoUrl))
                val forceHW = shouldForceHWDecoding()
                media.setHWDecoderEnabled(forceHW, false)
                mediaPlayer.media = media
                media.release()
                mediaPlayer.play()

                mediaPlayer.setEventListener { event ->
                    try {
                        when (event.type) {
                            MediaPlayer.Event.Vout -> onStart()
                            MediaPlayer.Event.Playing -> onPlaybackStateChanged(true)
                            MediaPlayer.Event.EndReached -> {
                                onPlaybackStateChanged(false)
                                onEndReached()
                            }
                            MediaPlayer.Event.Paused,
                            MediaPlayer.Event.Stopped -> onPlaybackStateChanged(false)
                            MediaPlayer.Event.Buffering -> onBufferingChanged(event.buffering != 100f)
                            MediaPlayer.Event.LengthChanged -> onDurationChanged(event.lengthChanged)

                            MediaPlayer.Event.ESAdded -> {

                                    try {
                                        val audioTracks = mediaPlayer.audioTracks?.map {
                                            it.id to (it.name ?: "Audio ${it.id}")
                                        } ?: emptyList()
                                        onTracksLoaded(audioTracks)

                                        val preferredAudio = audioTracks.find {
                                            LanguageMatcher.matchesLanguage(
                                                it.second,
                                                config.preferenceLanguage
                                            )
                                        }
                                        preferredAudio?.let {
                                            mediaPlayer.setAudioTrack(it.first)
                                            onAudioChanged?.invoke(it.second)
                                        }

                                        val subtitleTracks = mediaPlayer.spuTracks?.map {
                                            it.id to (it.name ?: "Sub ${it.id}")
                                        } ?: emptyList()
                                        onSubtitleLoaded(subtitleTracks)

                                        val preferredSub = subtitleTracks.find {
                                            LanguageMatcher.matchesLanguage(
                                                it.second,
                                                config.preferenceSubtitle
                                            )
                                        }
                                        preferredSub?.let {
                                            mediaPlayer.setSpuTrack(it.first)
                                            val code = LanguageMatcher.detectSubtitleCode(
                                                it.second,
                                                config.preferenceSubtitle
                                            )
                                            onSubtitleChanged?.invoke(code ?: "es")
                                        }

                                    } catch (e: Exception) {
                                        println("⚠️ Error loading media tracks: ${e.message}")
                                    }

                            }

                            MediaPlayer.Event.EncounteredError -> {
                                println("❌ Playback error encountered")
                                onBufferingChanged(false)
                            }
                        }
                    } catch (e: Exception) {
                        println("❌ Error inside VLC event listener: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                println("❌ Error initializing VLC player: ${e.message}")
            }
        }
    }
}
