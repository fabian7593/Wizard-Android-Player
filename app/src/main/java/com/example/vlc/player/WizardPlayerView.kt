package com.example.vlc.player

import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
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
    mediaPlayer: MediaPlayer,
    videoUrl: String,
    onTracksLoaded: (List<Pair<Int, String>>) -> Unit,
    onSubtitleLoaded: (List<Pair<Int, String>>) -> Unit,
    onPlaybackStateChanged: (Boolean) -> Unit,
    onEndReached: () -> Unit,
    onBufferingChanged: (Boolean) -> Unit,
    onDurationChanged: (Long) -> Unit
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
                            mediaPlayer.setAspectRatio("$width:$height")
                            mediaPlayer.setScale(0f)

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
                                        MediaPlayer.Event.Playing -> {
                                            onPlaybackStateChanged(true)
                                        }

                                        MediaPlayer.Event.EndReached -> {
                                            println("🎬 VLC Event.EndReached received at ${System.currentTimeMillis()}")
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
                                            } catch (e: Exception) {
                                                println("⚠️ Error loading audio tracks: ${e.message}")
                                            }

                                            // Load subtitle tracks
                                            try {
                                                val subtitleTracks = mediaPlayer.spuTracks?.map {
                                                    it.id to (it.name ?: "Sub ${it.id}")
                                                } ?: emptyList()
                                                onSubtitleLoaded(subtitleTracks)
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


