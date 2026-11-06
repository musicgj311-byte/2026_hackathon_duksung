package com.hackathon.ansimmessagepractice.detection

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.hackathon.ansimmessagepractice.EXTRA_WARNING_RISK
import com.hackathon.ansimmessagepractice.EXTRA_WARNING_SOURCE
import com.hackathon.ansimmessagepractice.EXTRA_WARNING_REASON
import com.hackathon.ansimmessagepractice.EXTRA_SCAM_TYPE
import com.hackathon.ansimmessagepractice.EXTRA_WARNING_TITLE
import com.hackathon.ansimmessagepractice.EXTRA_WARNING_EXPLANATION
import com.hackathon.ansimmessagepractice.EXTRA_WARNING_ACTION
import com.hackathon.ansimmessagepractice.EXTRA_WARNING_EASY
import com.hackathon.ansimmessagepractice.EXTRA_WARNING_LANGUAGE
import com.hackathon.ansimmessagepractice.MainActivity
import com.hackathon.ansimmessagepractice.R

/** 잠금화면 등 시스템 알림으로 표시하는 경고입니다. 메시지 원문이나 발신자 정보는 포함하지 않습니다. */
object WarningNotification {
    // 새로운 채널 ID: 구 버전에서 PRIVATE로 생성된 채널 설정은 코드로 바꿀 수 없습니다.
    const val CHANNEL = "ansim_risk_alert_public_v2"
    const val CAUTION_CHANNEL = "ansim_caution_alert_v1"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL, "모두안심 위험 경고", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "잠금 화면에 개인 정보 없이 위험을 알립니다"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                enableVibration(true)
            })
            manager.createNotificationChannel(NotificationChannel(
                CAUTION_CHANNEL, "모두안심 주의 안내", NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "주의 단계 메시지를 개인 정보 없이 알려드립니다"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                enableVibration(false)
            })
        }
    }
    fun notify(context: Context, level: String, source: String, reason: String = "", scamType: String = "none",
               serverTitle: String? = null, serverExplanation: String? = null,
               serverAction: String? = null, serverEasySentence: String? = null,
               serverLanguage: String? = null) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(context)
        val details = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_WARNING_RISK, level)
            putExtra(EXTRA_WARNING_SOURCE, source)
            putExtra(EXTRA_WARNING_REASON, reason.take(300))
            putExtra(EXTRA_SCAM_TYPE, scamType.take(50))
            serverTitle?.take(300)?.let { putExtra(EXTRA_WARNING_TITLE, it) }
            serverExplanation?.take(1000)?.let { putExtra(EXTRA_WARNING_EXPLANATION, it) }
            serverAction?.take(500)?.let { putExtra(EXTRA_WARNING_ACTION, it) }
            serverEasySentence?.take(250)?.let { putExtra(EXTRA_WARNING_EASY, it) }
            serverLanguage?.take(8)?.let { putExtra(EXTRA_WARNING_LANGUAGE, it) }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val notificationId = if (level == "danger") 4201 else 4202
        val pending = PendingIntent.getActivity(context, notificationId,
            details, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val danger = level == "danger"
        val kakao = source == "kakao"
        val link = com.hackathon.ansimmessagepractice.ui.warning.isLinkScam(scamType, danger)
        val koreanTitle = when {
            level == "timeout" -> "문자 확인을 완료하지 못했어요"
            danger && kakao -> "위험한 카카오톡이 왔어요"
            danger -> "위험한 문자가 왔어요"
            kakao -> "주의가 필요한 카카오톡이 왔어요"
            else -> "주의가 필요한 문자가 왔어요"
        }
        // 사기 유형에 맞는 한 줄: 링크 유도형은 '링크를 누르지 마세요', 송금 유도형은 '돈을 보내지 마세요'
        val koreanBody = when {
            level == "timeout" -> "안전 여부를 알 수 없어요. 링크나 송금은 직접 확인해 주세요."
            link -> "링크를 누르지 마세요."
            danger -> "돈을 보내지 마세요."
            else -> "돈을 보내기 전에 확인하세요."
        }
        // 외국어 설정이면 제목은 선택 언어, 본문은 '선택 언어 · 한국어'로 표시합니다.
        val foreign = com.hackathon.ansimmessagepractice.ui.warning.foreignCopyForDisplayLanguage(
            context.getSharedPreferences("ansim_settings", Context.MODE_PRIVATE).getString("language", "한국어").orEmpty())
        val title = when {
            foreign == null -> koreanTitle
            level == "timeout" -> foreign.timeoutTitle.replace("\n", " ")
            danger -> foreign.notifyDanger
            else -> foreign.notifyCaution
        }
        val body = when {
            foreign == null -> koreanBody
            level == "timeout" -> "${foreign.timeoutWarn}\n$koreanBody"
            else -> "${foreign.headline(danger, link).substringAfter("\n")}\n$koreanBody"
        }
        val channel = if (danger) CHANNEL else CAUTION_CHANNEL
        // 잠금화면 알림 제목을 빨간 굵은 글씨로 표시합니다(런처가 지원하는 경우).
        val styledTitle: CharSequence = if (danger) android.text.SpannableString(title).apply {
            setSpan(android.text.style.ForegroundColorSpan(Color.rgb(192, 38, 46)), 0, length, 0)
            setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, length, 0)
        } else title
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_ansim)
            .setContentTitle(styledTitle)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setColor(if (danger) Color.rgb(27, 78, 161) else Color.rgb(138, 90, 0))
            // 알림에는 문자 원문, 연락처, 보낸 사람 정보를 포함하지 않습니다.
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(if (danger) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        manager.notify(notificationId, notification)
    }
}