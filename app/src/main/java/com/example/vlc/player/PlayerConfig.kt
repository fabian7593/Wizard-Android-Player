package com.example.vlc.player
import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import org.videolan.libvlc.MediaPlayer

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
    val autoPlay: Boolean = true,
    val startEpisodeNumber: Int? = null,
    val preferenceLanguage: String = "en", //es, es-es, es-mx, en, fr, pt, de, it, ja, ko, zh
    val preferenceSubtitle: String = "es", //es, es-es, es-mx, en, fr, pt, de, it, ja, ko, zh
    val preferenceVideoSize: String = "fill", //autofit, fill, cinematic, 16:9, 4:3


) : Parcelable

@Parcelize
data class VideoItem(
    val title: String,
    val subtitle: String? = null,
    val url: String,
    val season:  Number? = null,
    val episodeNumber: Number? = null,
    val lastSecondView: Number? = null,
    val hasExternalSubtitles: Boolean = false
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
    val aspectRatioTitle: String = "Aspect Ratio",
    val titleContinueWatching: String = "Do you want to continue watching?",
    val buttonContinueWatching: String = "Continue Watching",
    val buttonResetVideo: String = "Reset Video",
    val errorConnectionMessage: String = "No Internet Connection",
) : Parcelable
