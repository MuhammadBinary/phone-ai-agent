package com.phoneai.agent

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.result.ActivityResultLauncher

class ScreenCaptureHelper(private val context: Context) {
    fun requestCapture(launcher: ActivityResultLauncher<Intent>) {
        val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = manager.createScreenCaptureIntent()
        launcher.launch(intent)
    }
}
