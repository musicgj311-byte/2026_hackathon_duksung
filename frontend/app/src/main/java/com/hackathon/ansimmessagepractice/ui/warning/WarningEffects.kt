package com.hackathon.ansimmessagepractice.ui.warning

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.ContextCompat

internal fun audioFileName(risk: String, language: String): String {
    val suffix = when (language) {
        "English" -> "en"
        "Tiếng Việt" -> "vn"
        "中文" -> "ch"
        else -> "ko"
    }
    return "warning_${risk}_$suffix"
}

/** 재생기 하나를 관리합니다. 녹음 파일이 없거나 지원되지 않아도 경고 화면이 종료되지 않도록 처리합니다. */
internal class WarningSound(context: Context, fileName: String) {
    private val player: MediaPlayer? = runCatching {
        val resId = context.resources.getIdentifier(fileName, "raw", context.packageName)
        if (resId == 0) null else MediaPlayer.create(context, resId)
    }.getOrNull()

    val available get() = player != null

    fun replay(): Boolean {
        val media = player ?: return false
        return runCatching {
            if (media.isPlaying) media.pause()
            media.seekTo(0)
            media.start()
        }.isSuccess
    }

    fun release() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
    }
}

internal fun vibrateSafely(context: Context, strong: Boolean = true) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE) !=
        PackageManager.PERMISSION_GRANTED) return
    runCatching {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = if (strong) longArrayOf(0, 260, 130, 320) else longArrayOf(0, 150)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(if (strong) 520L else 150L)
        }
    }
}