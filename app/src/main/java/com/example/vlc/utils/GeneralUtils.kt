package com.example.vlc.utils

import android.content.Context
import android.content.pm.PackageManager

object GeneralUtils {

    fun formatTime(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(mins, secs)
    }

    fun Context.isTelevision(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }
}