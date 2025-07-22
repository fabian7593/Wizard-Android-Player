package com.example.vlc.ui


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vlc.player.PlayerConfig
import com.example.vlc.player.PlayerLabels
import com.example.vlc.player.VideoItem
import com.example.vlc.player.WizardVideoPlayer
import androidx.core.view.ViewCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var showPlayer by remember { mutableStateOf(false) }
            var config by remember { mutableStateOf<PlayerConfig?>(null) }
            val labels = PlayerLabels()

            if (showPlayer && config != null) {
                WizardVideoPlayer(
                    config = config!!,
                    labels = labels,
                    onAudioChanged = { println("New Audio changed: $it") },
                    onSubtitleChanged = { println("New Subtitle changed: $it")  },
                    onAspectRatioChanged = { println("New Aspect Ratio: $it") },
                    onGetCurrentTime ={ println("Current time: $it") },
                    onGetCurrentItem = { println("Current item: $it") },
                    onExit = {
                        showPlayer = false
                    }
                )
            } else {
                MainScreen(
                    onStartPlayer = {
                        config = it
                        showPlayer = true
                    }
                )
            }
        }
    }
}

@Composable
fun MainScreen(onStartPlayer: (PlayerConfig) -> Unit) {
    var currentId by remember { mutableStateOf("") }
    val ids = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Agregar ID del capítulo:")

        OutlinedTextField(
            value = currentId,
            onValueChange = { currentId = it },
            label = { Text("ID numérico") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (currentId.isNotBlank()) {
                    ids.add(currentId)
                    currentId = ""
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Agregar")
        }

        if (ids.isNotEmpty()) {
            Text("Capítulos agregados: ${ids.joinToString()}")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (ids.isNotEmpty()) {
                    val videoItems = ids.map { id ->
                        VideoItem(
                            title = "Video $id",
                            subtitle = "Subtítulo $id",
                            url = "http://161.97.128.152:80/movie/test777/test777/$id.mkv",
                            season = 1,
                            episodeNumber = id.toInt(),
                            lastSecondView = 0,
                            //lastSecondView = if (id.toInt() == 63) 5600 else 0,
                            hasExternalSubtitles = if (id.toInt() == 63) true else false
                        )
                    }

                    val config = PlayerConfig(
                        videoItems = videoItems,
                        startEpisodeNumber = 27,
                        iconSizeDp = 32,
                        showSubtitleButton = true,
                        showAudioButton = true,
                        showAspectRatioButton = true,
                        autoPlay = true
                    )

                    onStartPlayer(config)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("▶ Reproducir todos los capítulos")
        }
    }
}
