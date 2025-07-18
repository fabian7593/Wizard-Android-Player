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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vlc.player.PlayerConfig
import com.example.vlc.player.WizardVideoPlayer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen()
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
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
                    val urls = ids.map { id ->
                        "http://161.97.128.152:80/movie/test777/test777/${id}.mkv"
                    }

                    val config = PlayerConfig(
                        videoUrls = urls,
                        startIndex = 0,
                        primaryColor = 0xFF00FF00.toInt(), // Verde
                        iconSizeDp = 32
                    )

                    WizardVideoPlayer.launch(context, config)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("▶ Reproducir todos los capítulos")
        }
    }
}
