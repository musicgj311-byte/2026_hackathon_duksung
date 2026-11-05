package com.hackathon.ansimmessagepractice.detection

import android.Manifest
import android.app.KeyguardManager
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.ContactsContract
import android.net.Uri
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.content.ContextCompat
import com.hackathon.ansimmessagepractice.data.api.BackendConfig
import com.hackathon.ansimmessagepractice.data.api.JudgeApi
import com.hackathon.ansimmessagepractice.data.api.JudgeVerdict
import com.hackathon.ansimmessagepractice.overlay.WarningOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** 기기 내 1차 필터 → /judge. 서버 주소가 없을 때만 종전 로컬 시연 경고를 사용합니다. */
class NotificationService : NotificationListenerService() {
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // 같은 알림이 반복 게시/갱신될 때 중복 분석과 서버 요청을 줄입니다.
    // 시간 제한 때문에 동일한 본문으로 실제 새 문자 수신 시 일부 생략될 수 있습니다.
    private val recentMessages = mutableMapOf<String, Long>()
    private val duplicateWindowMs = 15_000L
    private val supported = setOf(
        "com.google.android.apps.messaging", "com.samsung.android.messaging",
        "com.android.mms", "com.kakao.talk"
    )
    private var overlay: WarningOverlay? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        overlay = WarningOverlay(applicationContext)
        Log.d("AnsimNotification", "알림 접근 서비스 연결 완료")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val settings = getSharedPreferences("ansim_settings", Context.MODE_PRIVATE)
        if (settings.getString("role", "protected") != "protected" ||
            !settings.getBoolean("onboarded", false)) return
        if (sbn.packageName !in supported) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val extras: Bundle = sbn.notification.extras ?: return
        val content = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        if (content.isBlank()) return
        val sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        if (isDuplicateNotification(sbn.packageName, sender, content)) {
            Log.d("AnsimNotification", "중복 알림 갱신 감지 → 처리 생략")
            return
        }
        val source = if (sbn.packageName == "com.kakao.talk") "kakao" else "sms"

        // 연락처 조회는 I/O 작업이므로 서비스 콜백 스레드에서 직접 실행하지 않습니다.
        scope.launch {
            val isContact = lookupSavedContact(sender, source)
            val international = isInternationalSender(sender, source)
            val alert = MessageFilter.classify(content)
            if (!MessageFilter.shouldSendToServer(content, source, isContact, international)) {
                Log.d("AnsimNotification", "1차 필터 통과: $source (원문 미기록)")
                return@launch
            }
            Log.d("AnsimNotification", "1차 추가 검사 대상: $source, " +
                    "contentRule=${alert != null}, contact=${contactLabel(isContact)}, international=$international")

            if (!BackendConfig.isConfigured(applicationContext)) {
                // 서버가 없는 시연 모드: 모르는 번호라는 이유만으로 경고창을 띄우지 않습니다.
                if (alert != null) handler.post { showLocalPreview(alert, content, sender, source) }
                else Log.d("AnsimNotification", "서버 미설정: 발신정보 기반 추가 검사는 서버 연결 후 실행")
                return@launch
            }
            handler.post {
                if (canOverlayNow()) {
                    (overlay ?: WarningOverlay(applicationContext).also { overlay = it }).showChecking()
                }
            }
            val verdict = runCatching {
                JudgeApi.judge(applicationContext, source, sender, content,
                    // 현재 백엔드 스키마는 bool만 허용: '확인 불가'는 false로 전달합니다.
                    // 이 경우 미등록 번호라고 확정된 뜻은 아닙니다.
                    isContact = isContact == true, isInternational = international)
            }
            handler.post {
                if (getSharedPreferences("ansim_settings", Context.MODE_PRIVATE)
                        .getString("role", "protected") != "protected") {
                    overlay?.dismissChecking()
                    return@post
                }
                verdict.onSuccess { result -> showServerVerdict(result, source, sender, content) }
                    .onFailure {
                        Log.w("AnsimNotification", "판정 API 오류 (${it.javaClass.simpleName})")
                        overlay?.dismissChecking()
                        // 휴대폰을 쓰는 중이면 '확인 지연' 카드를 바로 띄우고(알림 방식별 소리·진동 포함),
                        // 잠금 상태이거나 카드 표시에 실패하면 알림으로 대신합니다.
                        val shown = canOverlayNow() &&
                                (overlay ?: WarningOverlay(applicationContext).also { overlay = it })
                                    .show("timeout", sourceApp = source)
                        if (!shown) WarningNotification.notify(applicationContext, "timeout", source)
                    }
            }
        }
    }

    /** 앱/발신자/본문이 같은 알림을 짧은 시간 내 다시 받으면 재분석하지 않습니다. */
    private fun isDuplicateNotification(app: String, sender: String, content: String): Boolean {
        val now = android.os.SystemClock.elapsedRealtime()
        val key = "$app\u0000$sender\u0000$content"
        synchronized(recentMessages) {
            // 오래된 항목을 정리하여 서비스가 오래 실행돼도 저장량이 늘지 않도록 합니다.
            recentMessages.entries.removeAll { now - it.value >= duplicateWindowMs }
            val lastSeen = recentMessages[key]
            if (lastSeen != null && now - lastSeen < duplicateWindowMs) return true
            recentMessages[key] = now
            return false
        }
    }

    private fun contactLabel(value: Boolean?): String = when (value) {
        true -> "saved"
        false -> "not_saved"
        null -> "unknown"
    }

    /** 이름/단체 대화방 제목은 번호로 취급하지 않으며, 국제번호도 추정하지 않습니다. */
    private fun normalizedSmsNumber(sender: String, source: String): String? {
        if (source != "sms") return null
        val cleaned = sender.trim().replace(Regex("[\\s().-]"), "")
        return cleaned.takeIf { it.matches(Regex("\\+?[0-9]{7,15}")) ||
                it.matches(Regex("00[0-9]{7,17}")) }
    }

    // 010/02 등 국내 번호는 false, +82/0082도 국내 국가번호로 취급합니다.
    // +1, +84, 0011... 등 비한국 국가번호는 true가 정상입니다.
    private fun isInternationalSender(sender: String, source: String): Boolean {
        val number = normalizedSmsNumber(sender, source) ?: return false
        return when {
            number.startsWith("+82") || number.startsWith("0082") -> false
            number.startsWith("+") || number.startsWith("00") -> true
            else -> false
        }
    }

    /** null은 권한 거부, 카카오톡 발신자 이름, 번호 미확인 또는 조회 실패를 의미합니다. */
    private fun lookupSavedContact(sender: String, source: String): Boolean? {
        val number = normalizedSmsNumber(sender, source) ?: return null
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED) return null
        return runCatching {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)
                ?.use { it.moveToFirst() }
        }.getOrNull()
    }

    private fun canOverlayNow(): Boolean {
        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val power = getSystemService(Context.POWER_SERVICE) as PowerManager
        val view = overlay ?: return false
        return !keyguard.isDeviceLocked && power.isInteractive && view.hasPermission()
    }

    /** READ_CONTACTS가 이미 허용된 경우만 연락처 판정; 새 권한을 요청하지 않습니다. */
    private fun isSavedContact(sender: String, source: String): Boolean {
        if (source != "sms" || sender.isBlank() ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED) return false
        return runCatching {
            val uri = android.net.Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI, android.net.Uri.encode(sender))
            contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)
                ?.use { it.moveToFirst() } ?: false
        }.getOrDefault(false)
    }

    private fun showServerVerdict(result: JudgeVerdict, source: String, sender: String, content: String) {
        Log.i("AnsimNotification", "판정 출처=server level=${result.level} scamType=${result.scamType} (원문 미기록)")
        overlay?.dismissChecking()
        if (result.level == "safe") return
        val type = when (result.scamType) {
            "family_impersonation" -> "가족 사칭 의심"
            "delivery" -> "택배 사칭 의심"
            else -> "사기 의심"
        }
        AlertStore.add(applicationContext, result.level, type, source)
        if (canOverlayNow()) {
            val shown = (overlay ?: WarningOverlay(applicationContext).also { overlay = it }).show(
                result.level, receivedMessage = content.take(2000), senderLabel = sender.take(120),
                analysisReason = result.explanation, sourceApp = source, scamType = result.scamType,
                serverTitle = result.title, serverExplanation = result.explanation,
                serverAction = result.action, serverEasySentence = result.easySentence,
                serverLanguage = result.language)
            // WindowManager.addView()가 성공한 경우에만 기록합니다.
            if (shown) {
                result.incidentId?.let { id -> scope.launch {
                    runCatching { JudgeApi.recordWarningShown(applicationContext, id) }
                        .onFailure { Log.w("AnsimNotification", "warning_shown 전송 실패", it) }
                } }
            } else {
                // 권한 변경 등의 이유로 표시 실패 시에는 경고 알림으로 대체합니다.
                if (result.level == "danger") AlertStore.setPendingDanger(
                    applicationContext, source, result.scamType, result.explanation)
                WarningNotification.notify(applicationContext, result.level, source,
                    reason = result.explanation, scamType = result.scamType,
                    serverTitle = result.title, serverExplanation = result.explanation,
                    serverAction = result.action, serverEasySentence = result.easySentence,
                    serverLanguage = result.language)
            }
        } else {
            if (result.level == "danger") AlertStore.setPendingDanger(
                applicationContext, source, result.scamType, result.explanation)
            WarningNotification.notify(applicationContext, result.level, source,
                reason = result.explanation, scamType = result.scamType,
                serverTitle = result.title, serverExplanation = result.explanation,
                serverAction = result.action, serverEasySentence = result.easySentence,
                serverLanguage = result.language)
        }
    }

    private fun showLocalPreview(alert: MessageFilter.Alert, content: String, sender: String, source: String) {
        Log.i("AnsimNotification", "판정 출처=local level=${alert.risk} scamType=${alert.scamType} (원문 미기록)")
        val type = when (alert.scamType) {
            "family_impersonation" -> "가족 사칭 의심"
            "delivery" -> "택배 사칭 의심"
            else -> "사기 의심"
        }
        AlertStore.add(applicationContext, alert.risk, type, source)
        if (canOverlayNow()) {
            (overlay ?: WarningOverlay(applicationContext).also { overlay = it }).show(
                alert.risk, receivedMessage = content.take(2000), senderLabel = sender.take(120),
                analysisReason = "기기 내부 키워드 시연 결과입니다. ${alert.reason}",
                sourceApp = source, scamType = alert.scamType)
        } else {
            if (alert.risk == "danger") AlertStore.setPendingDanger(applicationContext, source,
                alert.scamType, "기기 내부 시연 결과: ${alert.reason}")
            WarningNotification.notify(applicationContext, alert.risk, source, "로컬 시연: ${alert.reason}", alert.scamType)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        handler.post { overlay?.dismiss() }
        overlay = null
        super.onDestroy()
    }
}