package com.hackathon.ansimmessagepractice.ui.onboarding

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hackathon.ansimmessagepractice.detection.WarningNotification

/**
 * 먼저 앱 전체 알림 허용 여부를 확인합니다.
 * Android 13+ 알림 권한 또는 앱 전체 알림이 꺼진 상태에서는 채널 설정 화면의
 * 'Show notifications' 스위치가 비활성화되므로 앱 단위 설정을 열어야 합니다.
 */
object LockScreenSettings {
    fun open(context: Context) {
        WarningNotification.ensureChannel(context)

        val hasPostPermission = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        val appNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()

        val appIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        val channelIntent = if (Build.VERSION.SDK_INT >= 26) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, WarningNotification.CHANNEL)
            }
        } else null

        // 앱 단위 차단이 먼저 해제되어야 개별 채널 스위치를 변경할 수 있습니다.
        val intents = if (!hasPostPermission || !appNotificationsEnabled) {
            listOfNotNull(appIntent, channelIntent, Intent(Settings.ACTION_SETTINGS))
        } else {
            listOfNotNull(channelIntent, appIntent, Intent(Settings.ACTION_SETTINGS))
        }
        val opened = intents.any { intent ->
            runCatching {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            }.getOrDefault(false)
        }
        if (!opened) {
            Toast.makeText(context, "휴대폰 설정 → 앱 → 안심문자 → 알림에서 허용해 주세요.", Toast.LENGTH_LONG).show()
        }
    }
}
