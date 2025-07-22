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
import com.example.vlc.ui.theme.VLCTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var showPlayer by remember { mutableStateOf(false) }
            var config by remember { mutableStateOf<PlayerConfig?>(null) }
            val labels = PlayerLabels()

            if (showPlayer && config != null) {
                VLCTheme(darkTheme = true) {
                    WizardVideoPlayer(
                        config = config!!,
                        labels = labels,
                        onAudioChanged = { println("New Audio changed: $it") },
                        onSubtitleChanged = { println("New Subtitle changed: $it") },
                        onAspectRatioChanged = { println("New Aspect Ratio: $it") },
                        onGetCurrentTime = { println("Current time: $it") },
                        onGetCurrentItem = { println("Current item: $it") },
                        onExit = {
                            showPlayer = false
                        }
                    )
                }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxHeight()
                .padding(top = 32.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                OutlinedTextField(
                    value = currentId,
                    onValueChange = { currentId = it },
                    label = { Text("ID numérico") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Lista horizontal de capítulos agregados
                    if (ids.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            ids.forEach { id ->
                                Text(
                                    text = id,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Button(
                        onClick = {
                            if (currentId.isNotBlank()) {
                                ids.add(currentId)
                                currentId = ""
                            }
                        }
                    ) {
                        Text("Agregar")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f)) // empuja el botón de abajo solo si hay espacio libre

            // Botón final
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
                                hasExternalSubtitles = id.toInt() == 63
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp) // ⛑️ asegura altura fija para que no se achique
            ) {
                Text("▶ Reproducir todos los capítulos")
            }
        }
    }
}
