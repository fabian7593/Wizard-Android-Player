package com.example.vlc.player.config

import org.videolan.libvlc.MediaPlayer

object Config {

    fun createLibVlcConfig(): ArrayList<String> {
        return arrayListOf(
            "--avcodec-fast",
            "--avcodec-hw=mediacodec",
            "--codec=avcodec",

            "--file-caching=3500",
            "--network-caching=5000",

            "--drop-late-frames",
            "--skip-frames",

            "--clock-jitter=500",
            "--no-osd",
            "--no-video-title-show",

            /*"--subsdec-encoding=UTF-8",
            "--freetype-rel-fontsize=14",
            "--freetype-outline-thickness=1",
            "--freetype-shadow-opacity=128"*/
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
            println("❌ Error applying aspect ratio: ${e.message}")
        }
    }
}