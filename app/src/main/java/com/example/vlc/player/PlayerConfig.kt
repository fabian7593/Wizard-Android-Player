package com.example.vlc.player
import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
data class PlayerConfig(
    val videoUrls: List<String>,
    val startIndex: Int = 0, // Empieza por defecto en la primera
    val primaryColor: Int = 0xFF1976D2.toInt(),
    val iconSizeDp: Int = 24,
    val showSubtitleButton: Boolean = true,
    val showAudioButton: Boolean = true,
    val showAspectRatioButton: Boolean = true,
    val autoPlay: Boolean = true
) : Parcelable