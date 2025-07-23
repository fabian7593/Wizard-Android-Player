package com.example.vlc.player.config

import com.example.vlc.utils.AppLogger
import org.videolan.libvlc.MediaPlayer

object Config {

    val SHOW_LOGS = true

    fun createLibVlcConfig(): ArrayList<String> {
        return arrayListOf(
            "--avcodec-fast",
            "--avcodec-hw=mediacodec",
            "--codec=avcodec",

            "--file-caching=3500",
            "--network-caching=7777",

            "--drop-late-frames",
            "--skip-frames",

            "--clock-jitter=500",
            "--no-osd",
            "--no-video-title-show",

            "--sub-filter=marq",                // Usa subtítulos simplificados
            "--freetype-rel-fontsize=20",         // Tamaño relativo de fuente
            "--freetype-outline-thickness=1",     // Grosor del borde para legibilidad
            "--freetype-color=0xffffff",        // Blanco
            "--freetype-shadow-opacity=0",
            "--sub-fps=3",
        )
    }

    val AspectRatioOptions = listOf(
        "autofit" to "Auto Fit",
        "fill" to "Fill",
        "cinematic" to "Cinematic",
        "16:9" to "16:9",
        "4:3" to "4:3"
    )


    /**
     * Applies the desired aspect ratio setting to the MediaPlayer.
     * @param mediaPlayer The VLC media player instance
     * @param aspectRatio One of: "autofit", "fill", "cinematic", "16:9", "4:3"
     * @param onApplied Optional callback invoked with the applied mode (useful for state sync)
     */
    fun applyAspectRatio(
        mediaPlayer: MediaPlayer?,
        aspectRatio: String,
        onApplied: ((String) -> Unit)? = null
    ) {
        try {
            when (aspectRatio.lowercase()) {
                "autofit" -> {
                    mediaPlayer?.aspectRatio = ""
                    mediaPlayer?.scale = 0f
                    onApplied?.invoke("autofit")
                }
                "fill" -> {
                    mediaPlayer?.aspectRatio = "21:9"
                    mediaPlayer?.scale = 1f
                    mediaPlayer?.videoScale = MediaPlayer.ScaleType.SURFACE_FILL
                    onApplied?.invoke("fill")
                }
                "cinematic" -> {
                    mediaPlayer?.aspectRatio = "2:1"
                    mediaPlayer?.scale = 0f
                    onApplied?.invoke("cinematic")
                }
                "16:9" -> {
                    mediaPlayer?.aspectRatio = "16:9"
                    mediaPlayer?.scale = 0f
                    onApplied?.invoke("16:9")
                }
                "4:3" -> {
                    mediaPlayer?.aspectRatio = "4:3"
                    mediaPlayer?.scale = 0f
                    onApplied?.invoke("4:3")
                }
                else -> {
                    mediaPlayer?.aspectRatio = ""
                    mediaPlayer?.scale = 0f
                    onApplied?.invoke("autofit")
                }
            }
        } catch (e: Exception) {
            AppLogger.error("Config", "❌ Error applying aspect ratio: ${e.message}")
        }
    }
}