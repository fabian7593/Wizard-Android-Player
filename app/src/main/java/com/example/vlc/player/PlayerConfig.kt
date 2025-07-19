package com.example.vlc.player
import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
data class PlayerConfig(
    val videoItems: List<VideoItem>,
    val startIndex: Int = 0,
    val primaryColor: Int = 0xFF1976D2.toInt(),
    val iconSizeDp: Int = 24,
    val showSubtitleButton: Boolean = true,
    val showAudioButton: Boolean = true,
    val showAspectRatioButton: Boolean = true,
    val autoPlay: Boolean = true
) : Parcelable


@Parcelize
data class VideoItem(
    val title: String,
    val subtitle: String,
    val url: String,
    val season:  Number? = null,
    val episodeNumber: Number? = null
) : Parcelable