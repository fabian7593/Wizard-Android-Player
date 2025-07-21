package com.example.vlc.utils

import android.content.Context
import android.content.pm.PackageManager

object GeneralUtils {

    fun formatTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return if (hours > 0)
            "%02d:%02d:%02d".format(hours, minutes, secs)
        else
            "%02d:%02d".format(minutes, secs)
    }

    fun Context.isTelevision(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }
}