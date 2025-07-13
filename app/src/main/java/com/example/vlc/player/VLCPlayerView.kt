package com.example.vlc.player

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

@Composable
fun VLCPlayerView(
    modifier: Modifier = Modifier,
    mediaPlayer: MediaPlayer,
    videoUrl: String,
    onTracksLoaded: (List<Pair<Int, String>>) -> Unit,
    onSubtitleLoaded: (List<Pair<Int, String>>) -> Unit,
    onPlaybackStateChanged: (Boolean) -> Unit,
    onBufferingChanged: (Boolean) -> Unit,
    onDurationChanged: (Long) -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val surfaceView = SurfaceView(context)

            surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    GlobalScope.launch(Dispatchers.Default) {
                        try {
                            val dm = context.resources.displayMetrics
                            val w = dm.widthPixels
                            val h = dm.heightPixels

                            val vout = mediaPlayer.vlcVout
                            vout.setVideoView(surfaceView)
                            vout.setWindowSize(w, h)
                            vout.attachViews(null)

                            mediaPlayer.setAspectRatio("$w:$h")
                            mediaPlayer.setScale(0f)

                            mediaPlayer.setEventListener { event ->
                                when (event.type) {
                                    MediaPlayer.Event.Playing -> onPlaybackStateChanged(true)
                                    MediaPlayer.Event.Paused, MediaPlayer.Event.Stopped, MediaPlayer.Event.EndReached ->
                                        onPlaybackStateChanged(false)

                                    MediaPlayer.Event.Buffering -> {
                                        onBufferingChanged(event.buffering != 100f)
                                    }

                                    MediaPlayer.Event.LengthChanged -> {
                                        onDurationChanged(event.lengthChanged)
                                    }

                                    MediaPlayer.Event.ESAdded -> {
                                        onTracksLoaded(mediaPlayer.audioTracks?.map {
                                            it.id to (it.name ?: "Audio ${it.id}")
                                        } ?: emptyList())

                                        onSubtitleLoaded(mediaPlayer.spuTracks?.map {
                                            it.id to (it.name ?: "Sub ${it.id}")
                                        } ?: emptyList())
                                    }

                                    MediaPlayer.Event.EncounteredError -> {
                                        println("❗ Error encontrado en reproducción")
                                        onBufferingChanged(false)
                                    }
                                }
                            }

                            val media = Media(mediaPlayer.libVLC, android.net.Uri.parse(videoUrl))
                            media.setHWDecoderEnabled(true, true)
                            mediaPlayer.media = media
                            media.release()
                            mediaPlayer.play()

                        } catch (e: Exception) {
                            println("❌ Error al crear SurfaceView: ${e.message}")
                        }
                    }
                }

                override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, h2: Int) {}

                override fun surfaceDestroyed(h: SurfaceHolder) {
                    GlobalScope.launch {
                        try {
                            mediaPlayer.stop()
                            mediaPlayer.vlcVout.detachViews()
                        } catch (e: Exception) {
                            println("⚠️ Error al destruir Surface: ${e.message}")
                        }
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
                println("❌ Error en onRelease VLCPlayerView: ${e.message}")
            }
        }
    )
}

