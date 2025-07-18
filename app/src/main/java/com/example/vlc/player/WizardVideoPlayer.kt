package com.example.vlc.player

import android.content.Context
import android.content.Intent

object WizardVideoPlayer {

    fun launch(
        context: Context,
        config: PlayerConfig
    ) {
        val intent = Intent(context, WizardPlayerActivity::class.java).apply {
            putExtra("player_config", config)
        }
        context.startActivity(intent)
    }
}