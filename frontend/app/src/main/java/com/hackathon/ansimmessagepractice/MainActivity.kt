package com.hackathon.ansimmessagepractice

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hackathon.ansimmessagepractice.detection.WarningNotification
import com.hackathon.ansimmessagepractice.guardian.GuardianCall
import com.hackathon.ansimmessagepractice.guardian.PairingIntegration
import com.hackathon.ansimmessagepractice.navigation.AnsimApp
import com.hackathon.ansimmessagepractice.ui.theme.AnsimMessagePracticeTheme

class MainActivity : ComponentActivity() {
    private var warningLaunch by mutableStateOf<WarningLaunch?>(null)
    private var launcherOpenKey by mutableIntStateOf(0)

    /** 통화를 자동으로 시작하지 않습니다. 저장된 가족 번호를 입력한 상태로 기본 전화 앱을 엽니다. */
    fun startGuardianCall() {
        if (!GuardianCall.validNumber(GuardianCall.savedNumber(this))) {
            Toast.makeText(this, "먼저 가족 전화번호를 등록해 주세요.", Toast.LENGTH_LONG).show()
            return
        }
        GuardianCall.requestCall(this)
    }

    private fun readWarningIntent(from: Intent?) {
        val risk = from?.getStringExtra(EXTRA_WARNING_RISK)
        if (risk == "danger" || risk == "caution" || risk == "checking" || risk == "timeout") {
            warningLaunch = WarningLaunch(
                riskLevel = risk,
                requestId = SystemClock.elapsedRealtimeNanos(),
                receivedMessage = from.getStringExtra(EXTRA_WARNING_MESSAGE)?.take(4000),
                senderLabel = from.getStringExtra(EXTRA_WARNING_SENDER)?.take(120),
                analysisReason = from.getStringExtra(EXTRA_WARNING_REASON)?.take(1000),
                guardianNotified = from.getBooleanExtra(EXTRA_GUARDIAN_NOTIFIED, false),
                scamType = from.getStringExtra(EXTRA_SCAM_TYPE),
                title = from.getStringExtra(EXTRA_WARNING_TITLE),
                explanation = from.getStringExtra(EXTRA_WARNING_EXPLANATION),
                action = from.getStringExtra(EXTRA_WARNING_ACTION),
                easySentence = from.getStringExtra(EXTRA_WARNING_EASY),
                language = from.getStringExtra(EXTRA_WARNING_LANGUAGE),
                notificationStatus = from.getStringExtra(EXTRA_NOTIFICATION_STATUS),
                sourceApp = from.getStringExtra(EXTRA_WARNING_SOURCE) ?: "sms"
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // 잠금화면 중 발생한 위험 경고는 사용자가 잠금을 해제하고 앱을 열 때까지 보류합니다.
        // 안드로이드는 잠금 해제 직후 백그라운드 앱 화면의 자동 실행을 보장하지 않습니다.
        if (warningLaunch == null &&
            getSharedPreferences("ansim_settings", MODE_PRIVATE).getString("role", "protected") != "guardian") {
            val pending = com.hackathon.ansimmessagepractice.detection.AlertStore.takePendingDanger(this)
            if (pending != null) readWarningIntent(Intent().apply {
                putExtra(EXTRA_WARNING_RISK, "danger")
                putExtra(EXTRA_WARNING_SOURCE, pending.source)
                putExtra(EXTRA_SCAM_TYPE, pending.scamType)
                putExtra(EXTRA_WARNING_REASON, pending.reason)
            })
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readWarningIntent(intent)
        if (intent.getStringExtra(EXTRA_WARNING_RISK) == "danger") {
            com.hackathon.ansimmessagepractice.detection.AlertStore.clearPendingDanger(this)
        }
        // 앱 아이콘으로 다시 실행하면 이전 화면 대신 시작 화면을 표시합니다.
        // 경고 상세로 이동하는 Intent에는 ACTION_MAIN/CATEGORY_LAUNCHER가 없습니다.
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
            warningLaunch = null
            launcherOpenKey++
        }
        if (intent.getBooleanExtra(EXTRA_CALL_GUARDIAN, false)) {
            intent.removeExtra(EXTRA_CALL_GUARDIAN)
            startGuardianCall()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android 8 이상에서는 첫 의심 문자 수신 전, 앱 실행 시 알림 채널을 미리 생성합니다.
        // 채널 생성만으로 알림 권한이 허용되는 것은 아닙니다.
        // POST_NOTIFICATIONS 권한은 PermissionScreen에서 사용자가 직접 허용해야 합니다.
        WarningNotification.ensureChannel(applicationContext)
        PairingIntegration.configure(applicationContext)
        enableEdgeToEdge()
        readWarningIntent(intent)
        if (intent?.getStringExtra(EXTRA_WARNING_RISK) == "danger") {
            com.hackathon.ansimmessagepractice.detection.AlertStore.clearPendingDanger(this)
        }
        setContent {
            AnsimMessagePracticeTheme {
                AnsimApp(
                    warningLaunch = warningLaunch,
                    onWarningLaunchHandled = { warningLaunch = null },
                    launcherOpenKey = launcherOpenKey
                )
            }
        }
        if (intent?.getBooleanExtra(EXTRA_CALL_GUARDIAN, false) == true) {
            intent.removeExtra(EXTRA_CALL_GUARDIAN)
            window.decorView.post { startGuardianCall() }
        }
    }
}

data class WarningLaunch(
    val riskLevel: String,
    val requestId: Long,
    val receivedMessage: String? = null,
    val senderLabel: String? = null,
    val analysisReason: String? = null,
    val guardianNotified: Boolean = false,
    val scamType: String? = null,
    val title: String? = null,
    val explanation: String? = null,
    val action: String? = null,
    val easySentence: String? = null,
    val language: String? = null,
    val notificationStatus: String? = null,
    val sourceApp: String = "sms"
)

const val EXTRA_WARNING_RISK = "com.hackathon.ansimmessagepractice.WARNING_RISK"
const val EXTRA_WARNING_MESSAGE = "com.hackathon.ansimmessagepractice.WARNING_MESSAGE"
const val EXTRA_WARNING_SENDER = "com.hackathon.ansimmessagepractice.WARNING_SENDER"
const val EXTRA_WARNING_REASON = "com.hackathon.ansimmessagepractice.WARNING_REASON"
const val EXTRA_GUARDIAN_NOTIFIED = "com.hackathon.ansimmessagepractice.GUARDIAN_NOTIFIED"
const val EXTRA_CALL_GUARDIAN = "com.hackathon.ansimmessagepractice.CALL_GUARDIAN"

const val EXTRA_SCAM_TYPE = "com.hackathon.ansimmessagepractice.SCAM_TYPE"
const val EXTRA_WARNING_TITLE = "com.hackathon.ansimmessagepractice.WARNING_TITLE"
const val EXTRA_WARNING_EXPLANATION = "com.hackathon.ansimmessagepractice.WARNING_EXPLANATION"
const val EXTRA_WARNING_ACTION = "com.hackathon.ansimmessagepractice.WARNING_ACTION"
const val EXTRA_WARNING_EASY = "com.hackathon.ansimmessagepractice.WARNING_EASY"
const val EXTRA_WARNING_LANGUAGE = "com.hackathon.ansimmessagepractice.WARNING_LANGUAGE"
const val EXTRA_NOTIFICATION_STATUS = "com.hackathon.ansimmessagepractice.NOTIFICATION_STATUS"

const val EXTRA_WARNING_SOURCE = "com.hackathon.ansimmessagepractice.WARNING_SOURCE"