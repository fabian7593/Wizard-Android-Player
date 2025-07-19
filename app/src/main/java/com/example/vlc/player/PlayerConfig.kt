package com.example.vlc.player
import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
data class PlayerConfig(
    val videoItems: List<VideoItem>,
    val primaryColor: Int = 0xFF5C7EE9.toInt(), //active color, primary color app
    val focusColor: Int = 0xFFFFFFFF.toInt(), // WHITE
    val inactiveColor: Int = 0xFF888888.toInt(), // GRAY
    val diameterButtonCircleDp: Int = 48,
    val iconSizeDp: Int = 24,
    val showSubtitleButton: Boolean = true,
    val showAudioButton: Boolean = true,
    val showAspectRatioButton: Boolean = true,

    //Faltantes
    val preferenceLanguage: String = "en",
    val preferenceSubtitle: String = "es",
    val preferenceVideoSize: String = "Auto fit",

    val autoPlay: Boolean = true,
    val startIndex: Int = 0,
) : Parcelable

@Parcelize
data class VideoItem(
    val title: String,
    val subtitle: String,
    val url: String,
    val season:  Number? = null,
    val episodeNumber: Number? = null
) : Parcelable

@Parcelize
data class PlayerLabels(
    val nextLabel: String = "Next ▶",
    val audioLabel: String = "Audio",
    val subtitleLabel: String = "Subtitles",
    val aspectRatioLabel: String = "Aspect",
    val exitPrompt: String = "Press back again to exit",
    val selectAudioTitle: String = "Select Audio",
    val selectSubtitleTitle: String = "Select Subtitles",
    val aspectRatioTitle: String = "Aspect Ratio"
) : Parcelable
