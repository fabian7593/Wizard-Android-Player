package com.example.vlc.player

import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.vlc.utils.LanguageMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

/**
 * WizardPlayerView is a Composable that embeds a native SurfaceView
 * and initializes the VLC media player with all necessary listeners.
 *
 * @param modifier Modifier to be applied to the SurfaceView.
 * @param mediaPlayer Instance of VLC's MediaPlayer.
 * @param videoUrl The video URL to load and play.
 * @param onTracksLoaded Callback that receives available audio tracks.
 * @param onSubtitleLoaded Callback that receives available subtitle tracks.
 * @param onPlaybackStateChanged Callback called when playback starts or stops.
 * @param onBufferingChanged Callback indicating if buffering is active.
 * @param onDurationChanged Callback triggered when the media duration is known.
 */
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
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val surfaceView = SurfaceView(context)

            // Attach lifecycle callbacks to the SurfaceHolder
            surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    // Initialize VLC media playback on background thread
                    GlobalScope.launch(Dispatchers.Default) {
                        try {


                            val displayMetrics = context.resources.displayMetrics
                            val width = displayMetrics.widthPixels
                            val height = displayMetrics.heightPixels

                            //fit by default
                            // Apply aspect ratio preference
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
                            mediaPlayer.vlcVout.detachViews()
                            vout.setVideoView(surfaceView)
                            vout.setWindowSize(width, height)
                            vout.attachViews(null)

                            if (videoUrl.isNotEmpty()) {
                                val media = Media(mediaPlayer.libVLC, Uri.parse(videoUrl))
                                media.setHWDecoderEnabled(true, false)
                                mediaPlayer.media = media
                                media.release()
                                mediaPlayer.play()
                            }

                            // Set media player event listener to handle state changes
                            mediaPlayer.setEventListener { event ->
                                try {
                                    when (event.type) {

                                        MediaPlayer.Event.Vout -> {
                                            onStart()
                                        }

                                        MediaPlayer.Event.Playing -> {
                                            onPlaybackStateChanged(true)
                                        }

                                        MediaPlayer.Event.EndReached -> {
                                            onPlaybackStateChanged(false)
                                            onEndReached()
                                        }

                                        MediaPlayer.Event.Paused,
                                        MediaPlayer.Event.Stopped -> {
                                            onPlaybackStateChanged(false)
                                        }

                                        MediaPlayer.Event.Buffering -> {
                                            onBufferingChanged(event.buffering != 100f)
                                        }

                                        MediaPlayer.Event.LengthChanged -> {
                                            onDurationChanged(event.lengthChanged)
                                        }

                                        MediaPlayer.Event.ESAdded -> {
                                            // Load audio tracks
                                            try {
                                                val audioTracks = mediaPlayer.audioTracks?.map {
                                                    it.id to (it.name ?: "Audio ${it.id}")
                                                } ?: emptyList()

                                                onTracksLoaded(audioTracks)

                                                val preferredAudioTrack = audioTracks.find {
                                                    LanguageMatcher.matchesLanguage(it.second, config.preferenceLanguage)
                                                }

                                                preferredAudioTrack?.let {
                                                    mediaPlayer.setAudioTrack(it.first)
                                                    onAudioChanged?.invoke(it.second)
                                                }

                                            } catch (e: Exception) {
                                                println("⚠️ Error loading audio tracks: ${e.message}")
                                            }

                                            // Load subtitle tracks
                                            try {
                                                val subtitleTracks = mediaPlayer.spuTracks?.map {
                                                    it.id to (it.name ?: "Sub ${it.id}")
                                                } ?: emptyList()

                                                println("📜 SUBTITLE TRACKS DETECTED:")
                                                subtitleTracks.forEach { (id, name) ->
                                                    println(" - ID: $id, Name: $name")
                                                }

                                                onSubtitleLoaded(subtitleTracks)

                                                val preferredSubtitleTrack = subtitleTracks.find {
                                                    LanguageMatcher.matchesLanguage(it.second, config.preferenceSubtitle)
                                                }

                                                preferredSubtitleTrack?.let {
                                                    mediaPlayer.setSpuTrack(it.first)
                                                    val code = LanguageMatcher.detectSubtitleCode(it.second, config.preferenceSubtitle)
                                                    onSubtitleChanged?.invoke(code ?: "es")
                                                }

                                            } catch (e: Exception) {
                                                println("⚠️ Error loading subtitle tracks: ${e.message}")
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
                            println("❌ Error initializing SurfaceView and media player: ${e.message}")
                        }
                    }
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {
                    // No special handling required for size changes
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    // Clean up VLC views on background thread
                    GlobalScope.launch {
                        try {
                            mediaPlayer.stop()
                            mediaPlayer.vlcVout.detachViews()
                        } catch (e: Exception) {
                            println("⚠️ Error while destroying SurfaceView: ${e.message}")
                        }
                    }
                }
            })

            surfaceView
        },

        // Cleanup logic when AndroidView is released
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


}


